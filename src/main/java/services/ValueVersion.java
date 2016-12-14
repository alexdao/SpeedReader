package services;

import java.util.*;

class ValueVersion {
    private int version;

    private Set<String> values;

    public ValueVersion () {
        this.version = 0;
        this.values = new HashSet<>();
    }

    ValueVersion(int version, Set<String> values) {
        this.version = version;
        this.values = values;
    }

    int getVersion() {
        return this.version;
    }

    Set<String> getValues() {
        return this.values;
    }

    int setVersion(int newVersion) {
        this.version = newVersion;
        return this.version;
    }

    Set<String> setValues (Set<String> newValues) {
        this.values = newValues;
        return this.values;
    }

    Set<String> setValues(String value) {
        this.values.clear();
        this.values.add(value);
        return this.values;
    }

    Set<String> addValue(String newValue) {
        this.values.add(newValue);
        return this.values;
    }

    int getNumValues() {
        return values.size();
    }
 }
