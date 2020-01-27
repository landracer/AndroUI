package com.rAtTrax.AndroUI;

public class Util {
    public Util() {

    }

    public static short byte2short(byte _bt) {
        short result = _bt;
        if (_bt < 0) {
            result = (short)(256 + _bt);
        }
        return result;
    }

    public static int byte2int(byte _bt1, byte _bt2) {
        int tmp1 = _bt1, tmp2 = _bt2;
        if (_bt1 < 0) {
            tmp1 = 256 + _bt1;
        }
        if (_bt2 < 0) {
            tmp2 = 256 + _bt2;
        }
        tmp2 <<= 8;

        int result;
        result = tmp2 + tmp1;
        return result;
    }

    public static long byte2long(byte _bt1, byte _bt2, byte _bt3, byte _bt4) {
        long tmp1 = _bt1, tmp2 = _bt2, tmp3 = _bt3, tmp4 = _bt4;
        if (_bt1 < 0) {
            tmp1 = 256 + _bt1;
        }
        if (_bt2 < 0) {
            tmp2 = 256 + _bt2;
        }
        tmp2 <<= 8;
        if (_bt3 < 0) {
            tmp3 = 256 + _bt3;
        }
        tmp3 <<= 16;
        if (_bt4 < 0) {
            tmp4 = 256 + _bt4;
        }
        tmp4 <<= 24;

        long result;
        result = (tmp1 + tmp2 + tmp3 + tmp4);
        return result;
    }
}
