package services;

import java.util.*;

/**
 * Created by gautamh on 12/9/2016.
 */
public class ValueVersion {

    private int version;

    private List<String> values;

    public ValueVersion () {
        this.version = 0;
        this.values = new ArrayList<>();
    }

    public ValueVersion (int version, List<String> values) {
        this.version = version;
        this.values = values;
    }

    public int getVersion () {
        return this.version;
    }

    public List<String> getValues () {
        return this.values;
    }

    public int setVersion (int newVersion) {
        this.version = newVersion;
        return this.version;
    }

    public List<String> setValues (List<String> newValues) {
        this.values = newValues;
        return this.values;
    }
 }
