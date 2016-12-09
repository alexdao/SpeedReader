package services;

import java.util.*;

/**
 * Created by jiaweizhang on 4/12/16.
 */
public class SlaveService {

    private Map<String, List<String>> store = new HashMap<String, List<String>>();

    public SlaveService() {

    }

    public List<String> read(String key) {
        return store.get(key);
    }

    public List<String> resolve(String key, List<String> seenValues, String resolvedValue) {
        List<String> currentValues = store.get(key);
        if (currentValues == null) {
            return null;
        }

        for (String value : seenValues) {
            if (currentValues.contains(value)) {
                currentValues.remove(value);
            }
        }

        currentValues.add(resolvedValue);

        return store.get(key);
    }

    public List<String> write (String key, String value) {
        List<String> currentVersions = store.get(key);
        if (currentVersions != null) {
            currentVersions.add(value);
            return currentVersions;
        } else {
            List<String> newVersions = new ArrayList<String>();
            newVersions.add(value);
            store.put(key, newVersions);
            return newVersions;
        }
    }

    public void move(String path, int newSlave) {

    }
}
