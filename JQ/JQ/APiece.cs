using System;
using System.Collections.Generic;
using System.Drawing;
using System.Text;

namespace JQ
{
    public class APiece
    {
        private Point pts;
        private int piece;

        public Point Pts
        {
            get { return pts; }
            set { pts = value; }
        }

        public int Piece
        {
            get { return piece; }
            set { piece = value; }
        }
        public APiece(Point pts, int piece)
        {
            this.pts = pts;
            this.piece = piece;
        }
    }
}
