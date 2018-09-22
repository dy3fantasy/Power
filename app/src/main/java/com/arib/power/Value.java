package com.arib.power;

/**
 * Created by aribdhuka on 9/5/18.
 */

public class Value {

    String type;
    double value;
    long time;

    public Value() {
        type = "";
        value = 0;
        time = 0;
    }

    public Value(String type, double value, long ms) {
        this.type = type;
        this.value = value;
        this.time = ms;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long ms) {
        this.time = ms;
    }

    public String toString() {
        return getType() + "-" + getValue() + "-" + getTime();
    }
}
