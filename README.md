# SimpleDB Buffer Manager Implementation
CSC 540 - Fall 2024 - Project 2

## Project Overview
This project implements two major enhancements to SimpleDB's buffer management:
1. Using a Map data structure to track the buffer pool
2. Implementing LRU(K=3) buffer replacement policy

## Modified Files
- `src/simpledb/buffer/BufferMgr.java`: Enhanced with Map implementation and LRU(K=3)
- `src/simpledb/buffer/BufferMgrTest.java`: Test cases for the new functionality

## How to Run Tests

### Environment Setup
1. Ensure you have Java 17 installed
2. Set up your IDE (Eclipse or IntelliJ) with the SimpleDB source code

### Running the Tests
1. Compile the source files:
```bash
javac -d classes src/simpledb/buffer/*.java
```

2. Run the test:
```bash
java -cp classes simpledb.buffer.BufferMgrTest
```

### Expected Test Output
The test will run two main scenarios:

1. Basic Buffer Management Test:
```
Test 1: Testing Buffer Pool Map
Same block pinned twice. Buffers are same object: true
```

2. LRU(K=3) Replacement Test:
```
Test 2: Testing LRU(K=3) Replacement
Creating access pattern...
Block 0 accessed 3 times
Block 1 accessed 2 times
Block 2 accessed 1 time
...
```

## Implementation Details

### Buffer Pool Map
- Implemented using HashMap to track which blocks are assigned to which buffers
- Provides O(1) lookup time for finding existing buffers
- Maintained automatically during buffer assignment and replacement

### LRU(K=3) Replacement Policy
- Tracks K=3 most recent accesses for each buffer
- Uses backward K-distance to choose replacement candidates
- Handles cases with fewer than K accesses appropriately