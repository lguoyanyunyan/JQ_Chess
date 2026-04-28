package com.zangjiuqi.ai;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.zangjiuqi.core.AIState;
import com.zangjiuqi.core.BoardState;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class NativeAiClient implements AiClient {
    private static final int BEST_MOVE_BUFFER_SIZE = 2000;
    private static final String RESOURCE_PATH = "/native/win-x64/jqai.dll";

    private final JqAiLibrary library;

    public NativeAiClient(Path dllPath) {
        this.library = Native.load(dllPath.toAbsolutePath().toString(), JqAiLibrary.class);
    }

    public static NativeAiClient bundled() {
        return new NativeAiClient(resolveBundledDll());
    }

    public static NativeAiClient bundledIsolated(String instanceName) {
        try {
            return new NativeAiClient(extractResourceDll(instanceName));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to extract isolated native library: " + instanceName, ex);
        }
    }

    @Override
    public String requestMove(BoardState state, int searchDepth, int timeoutSeconds) {
        byte[] bestMove = new byte[BEST_MOVE_BUFFER_SIZE];
        AIState aiState = state.currentAiState();
        library.getAIMoveEx(
                state.toAiBoard(),
                aiState.code(),
                (byte) state.turn(),
                state.ruleConfig().boardSize(),
                searchDepth,
                timeoutSeconds,
                bestMove
        );
        return readNullTerminatedAscii(bestMove);
    }

    @Override
    public void destroyHashtable() {
        library.destroy_hashtable();
    }

    private static Path resolveBundledDll() {
        URL resource = NativeAiClient.class.getResource(RESOURCE_PATH);
        if (resource == null) {
            throw new IllegalStateException("Missing bundled native library: " + RESOURCE_PATH);
        }

        try {
            if ("file".equalsIgnoreCase(resource.getProtocol())) {
                return Paths.get(resource.toURI());
            }
            return extractResourceDll();
        } catch (URISyntaxException | IOException ex) {
            throw new IllegalStateException("Failed to resolve bundled native library.", ex);
        }
    }

    private static Path extractResourceDll() throws IOException {
        return extractResourceDll("jqai");
    }

    private static Path extractResourceDll(String instanceName) throws IOException {
        String safeName = instanceName == null || instanceName.isBlank()
                ? "jqai"
                : instanceName.replaceAll("[^A-Za-z0-9._-]", "_");
        Path tempFile = Files.createTempFile(safeName + "-", ".dll");
        tempFile.toFile().deleteOnExit();
        try (InputStream input = NativeAiClient.class.getResourceAsStream(RESOURCE_PATH)) {
            if (input == null) {
                throw new IOException("Resource not found: " + RESOURCE_PATH);
            }
            Files.copy(input, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        return tempFile;
    }

    private static String readNullTerminatedAscii(byte[] bytes) {
        int length = 0;
        while (length < bytes.length && bytes[length] != 0) {
            length++;
        }
        return new String(bytes, 0, length, StandardCharsets.US_ASCII).trim();
    }

    private interface JqAiLibrary extends Library {
        void getAIMoveEx(byte[] cb, byte state, byte side, int boardSize, int aiSearchDepth, int timeout, byte[] bestMove);

        void destroy_hashtable();
    }
}
