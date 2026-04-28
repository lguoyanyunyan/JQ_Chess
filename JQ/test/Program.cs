using System;
using System.Collections.Generic;
using System.IO;
using System.Text;

namespace test
{
    class Program
    {
        static void Main(string[] args)
        {
            int[] xinc = { 1, 0 };
            int[] yinc = { 0, 1 };
            StringBuilder sb = new StringBuilder();
            sb.Append("{ ");
            for (int i = 0; i < 14; i++)
            {
                sb.Append("{ ");
                for (int j = 0; j < 14; j++)
                {
                    sb.Append("{ ");
                    sb.Append("{ ");
                    int count = 0;
                    for (int d = 0; d < 2; d++)
                    {
                        for (int c = -1; c < 2; c++)
                        {
                            int x = j + c * xinc[d];
                            int y = i + c * yinc[d];
                            if (x > -1 && x < 14 && y > -1 && y < 14)
                            {
                                sb.Append("{ " + x + ", " + y + " }, ");
                                count++;
                            }
                        }
                    }
                    sb.Append("}, ");
                    sb.Append(count + " ");
                    sb.Append("}, ");
                }
                sb.Append("}, ");
            }
            sb.Append("}");
            File.AppendAllText("1.txt", sb.ToString());
            Console.ReadLine();
        }
    }
}
