package services;

import java.util.*;

class FollowerService {

    //key/version store
    private Map<String, List<String>> store = new HashMap<>();

    FollowerService() {

    }

    /**
     * Gets a list of value versions for a given key
     *
     * @param key The key to read versions for
     * @return A list of versions of the value for the inputted key
     */
    List<String> read(String key) {
        return store.get(key);
    }

    /**
     * Replaces versions of a value with a single "resolved" value
     *
     * @param key           The key to resolve
     * @param seenValues    The versions of the value to replace
     * @param resolvedValue The value to replace the seen values with
     * @return The list of values after all seen values have been resolved
     */
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

    /**
     * Writes a version of a value for a given key
     *
     * @param key   The key to write to
     * @param value A value to add to the key's version list
     * @return A list of all versions of the key's values after the new value is added
     */
    List<String> write(String key, String value) {
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

    /**
     * Dumps entire kv store for replication
     *
     * @return The entire store of this replica
     */
    public Map<String, List<String>> getStore() {
        return store;
    }
}
