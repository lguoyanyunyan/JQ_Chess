using System;
using System.Collections;
using System.Collections.Generic;
using System.Drawing;
using System.Text;

namespace JQ
{
    public class Move
    {
        private int piece;
        private ArrayList path;
        private ArrayList dead;

        public ArrayList Path
        {
            get { return path; }
            set { path = value; }
        }

        public ArrayList Dead
        {
            get { return dead; }
            set { dead = value; }
        }

        public int Piece
        {
            get { return piece; }
            set { piece = value; }
        }

        public Move(int piece)
        {
            this.Piece = piece;
            path = new ArrayList();
            dead = new ArrayList();
        }
    }
}
