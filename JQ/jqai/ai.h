#pragma once
extern "C" {
	/*
	DLL导出函数
	unsigned char* cb	棋盘状态，长度为196的一维数组，cb[0]就是A1,cb[13]就是A14，cb[14]就是B1，cb[27]就是B14，元素值为1表示白棋子，元素值为2表示黑棋子，元素值为0表示空位置
	unsigned char state	表示当前进行到的局面阶段。1表示布局落子 2表示传统走子 3表示竞技化走子 4表示传统飞子 5表示竞技化飞子
	unsigned char side	表示当前请求AI走棋方位，1表示先手，0表示后手（注意，这里根据局面阶段不同，黑白子理解有变化），在布局阶段1表示先手就是白棋，在行棋阶段1表示先手就是黑棋
	int aisearchdepth	表示AI最大搜索深度
	int timeout			搜索超时时限
	char* bestmove		是界面程序需要接受的一个字符串，最大极端情况要考虑到可以跳吃98子，看走步格式A11,A13,C13，一个位置最多需要4个字符表达，看吃子格式TC-A12,B13，一个位置最多4个字符表达，极端最多跳步98步，那么需要800个左右的char，所以这个字符数组准备2000的字节数来接受结果字符串保险。
	*/
	// Compatibility entry point: expects a 14x14 board buffer.
	__declspec(dllexport) void getAIMove(unsigned char* cb, unsigned char state, unsigned char side, int aisearchdepth, int timeout, char* bestmove);
	// Size-aware entry point: boardSize is currently 8 or 14, and cb length is boardSize * boardSize.
	__declspec(dllexport) void getAIMoveEx(unsigned char* cb, unsigned char state, unsigned char side, int boardSize, int aisearchdepth, int timeout, char* bestmove);

	__declspec(dllexport) void destroy_hashtable();
}
