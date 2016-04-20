
# About

DDDFS's features:

1. File balancing (duplication) across servers
2. Server balancing (by moving files from most busy to least busy server)
3. File safety by mandating a duplication count of 2 or more

# Implementation Infrastructure

The implementation of the DDDFS includes a master server, which handles all client requests and stores metadata about all other servers. Redis was used a persistent data storage as it has the ability to maintain its entire structure within memory (with periodic flushes to disk). Requests are accepted by a Java Spark web API through HTTP requests. 

# Implementation - TODO by ALEX

Things to cover:

1. List of all sets/lists/hashes (file->originalServer, file->setOfServers, server->setOfFiles, list of reads, list of accesses to servers)

2. ReadBalance algorithm including naive filecount determination

3. ServerBalance algorithm including naive random move

4. The originalServer is kept as a simple guarantee that a file will always exist somewhere on the cluster. 

# Testing

A test suite is provided to simulate write (create) and read requests. File modifications are currently not supported. 

# Future Research

* Heuristics can be used instead of all read history in determining number of servers to duplicate a file across. 

* Instead of moves of random files from the most busy server to the least busy server, heavily accessed files can be moved (in near O(1) find speed).

* Currently, read balancing and server balancing are both performed at fixed intervals. The balancing should ideally be performed during lulls of activity. As the actual balancing process across slave servers is expensive, balance detection and calculation should be performed often but only executed when deemed necessary. 

* This system can be implemented at a higher level, with one 'super-master' containing metadata about multiple masters, and in turn distributing data across multiple clusters. This results in a hierarchical tree-distributed system, with each of the leaves having the properties of slaves and the non-leaves being masters with varying amounts of metadata depending on the granularity desired. Having multiple clusters also allows slave servers to move between clusters depending on desired read-performance.

* DDDFS was primarily built with speed (reads) in mind and does not plan for many file modifications (simpler to instead create a new file). Thus, older unread file metadata can be un-cached from memory (Redis) and are eventually garbage-collected (or backed up to disk). 

* DDDFS has only eventual consistency, which is fitting for a system with a large number of reads of unlinked data. 

* Files in DDDFS are not contained within a folder hierarchy, but are rather found through a unique hash generated during their creation. This means that relationships between files (such as being siblings or being in the same folder in a traditional file system) has no place in DDDFS. If required, however, a simulated folder hierarchy can be achieved through clever naming. 