package com.rAtTrax.AndroUI;

public class Map16x1 {
    protected double[] mapData = new double[17];
    public Map16x1() {

    }
    public double mapValue(int dval) {
        dval &= 0xFF;
        int idx = dval >> 4;
        int w = dval & 0xF;
        return idx < 0xF ? (( ( w * mapData[idx+1] ) + ( -1 * (w-16) * mapData[idx]) ) / 16) : mapData[idx];
    }
}