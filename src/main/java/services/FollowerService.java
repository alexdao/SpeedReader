package services;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class FollowerService {

    //key/version store
    private Map<String, ValueVersion> store = new HashMap<>();

    private final ExecutorService exec;
    private final int branching = 2;

    FollowerService() {
        exec = Executors.newFixedThreadPool(5);
    }

    /**
     * Gets a list of value versions for a given key
     *
     * @param key The key to read versions for
     * @return A list of versions of the value for the inputted key
     */
    ValueVersion read(String key) {
        return store.get(key);
    }

    /**
     * Replaces versions of a value with a single "resolved" value
     *
     * @param key The key to resolve
     * @param resolvedValue The value to resolve to
     * @param version The version to resolve to (one greater than the last seen version)
     * @return An object containing the list of values for the key after resolution
     */
    ValueVersion resolve(String key, String resolvedValue, int version) {
        return this.write(key, resolvedValue, version);
    }

    /**
     * Writes a version of a value for a given key
     *
     * @param key   The key to write to
     * @param value A value to add to the key's version list
     * @param version The version of the write (should be one greater than last seen version)
     * @return An object containing versions of the key's values after the new value is added
     */
    ValueVersion write(String key, String value, int version) {
        ValueVersion currentVersions = store.get(key);
        if (currentVersions == null) {
            List<String> newVersions = new ArrayList<>();
            newVersions.add(value);
            store.put(key, new ValueVersion(version, newVersions));
            return store.get(key);
        } else if (currentVersions.getVersion() >= version) {
            currentVersions.addValue(value);
            return currentVersions;
        } else {
            currentVersions.setValues(value);
            currentVersions.setVersion(version);
            return currentVersions;
        }
    }

    /**
     * Deletes a key and its values synchronously from this replica
     *
     * @param key The key to be deleted
     * @return The deleted values if the key exists, else null
     */
    ValueVersion delete(String key) {
        if (!store.containsKey(key)) {
            return null;
        } else {
            return store.remove(key);
        }
    }

    ValueVersion writeAsync (String key, String value, int version, List<FollowerService> replicas, FollowerService sender) {
        ValueVersion syncWriteResult = this.write(key, value, version);
        FollowerService thisService = this;
        if (replicas.size() > 0 && replicas.size() < branching) {
            Callable<Void> task = new Callable<Void>() {

                @Override
                public Void call() {

                    for (FollowerService replica : replicas) {
                        replica.writeAsync(key, value, version, new ArrayList<FollowerService>(), thisService);
                    }
                    return null;
                }
            };

            exec.submit(task);
        } else if (replicas.size() > 0) {
            List<FollowerService> nextWriters = new ArrayList<FollowerService>();
            List<List<FollowerService>> branches = new ArrayList<List<FollowerService>>();

            for (int i = 0; i < branching; i++) {
                nextWriters.add(replicas.remove(i));
            }

            for (int i = 0; i < replicas.size(); i+= replicas.size()/branching) {
                if (i + replicas.size()/branching > replicas.size()) {
                    branches.add(replicas.subList(i, replicas.size()));
                } else {
                    branches.add(replicas.subList(i, i + replicas.size() / branching));
                }
            }
            Callable<Void> task = new Callable<Void>() {

                @Override
                public Void call() {
                    for (int i = 0; i < nextWriters.size(); i++) {
                        FollowerService nextReplica = replicas.remove(i);
                        nextReplica.writeAsync(key, value, version, branches.remove(i), thisService);
                    }
                    return null;
                }
            };

            exec.submit(task);
        }
        return syncWriteResult;
    }

    /**
     * Adds a replica of a value list with a given key to a follower
     *
     * Only adds the list if the key does not already exist on the follower
     * @param key The key to add
     * @param valueVersions The value list to be replicated
     * @return The new value list, if the key does not already exist, else null
     */
    ValueVersion addReplica(String key, ValueVersion valueVersions) {
        ValueVersion currentVersions = store.get(key);
        if (currentVersions != null) {
            return null;
        } else {
            store.put(key, valueVersions);
            return store.get(key);
        }
    }

    /**
     * Dumps entire kv store for replication
     *
     * @return The entire store of this replica
     */
    public Map<String, ValueVersion> getStore() {
        return store;
    }
}
