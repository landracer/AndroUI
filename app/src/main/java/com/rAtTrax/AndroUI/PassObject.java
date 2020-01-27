package com.rAtTrax.AndroUI;

public class PassObject {
    private static Object _object;
    private static int _type;

    //Set the object to be picked up by a separate activity.
    public static void setObject(Object obj){
        _object = obj;
    }

    public static void setType(int type){
        _type = type;
    }

    //Get the object that has been set.
    public static Object getObject(){
        Object obj = _object;

        // can get only once
        _object = null;
        return obj;
    }

    public static int getType(){
        int type = _type;

        //once
        _type = 0;
        return type;
    }
}
