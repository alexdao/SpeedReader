package services;

import java.util.*;

class ValueVersion {
    private int version;

    private List<String> values;

    public ValueVersion () {
        this.version = 0;
        this.values = new ArrayList<>();
    }

    ValueVersion(int version, List<String> values) {
        this.version = version;
        this.values = values;
    }

    int getVersion() {
        return this.version;
    }

    List<String> getValues() {
        return this.values;
    }

    int setVersion(int newVersion) {
        this.version = newVersion;
        return this.version;
    }

    List<String> setValues (List<String> newValues) {
        this.values = newValues;
        return this.values;
    }

    List<String> setValues(String value) {
        this.values.clear();
        this.values.add(value);
        return this.values;
    }

    List<String> addValue(String newValue) {
        this.values.add(newValue);
        return this.values;
    }

    int getNumValues() {
        return values.size();
    }
 }
