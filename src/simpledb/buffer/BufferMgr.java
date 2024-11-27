package simpledb.buffer;

import simpledb.file.*;
import simpledb.log.LogMgr;

import java.util.*;

public class BufferMgr {
    private Buffer[] bufferpool;
    private int numAvailable;
    // Add Map to track buffers by block
    private Map<BlockId, Buffer> bufferPoolMap;
    private static final long MAX_TIME = 10000; // 10 seconds

    // Track access history for LRU(K)
    private Map<Buffer, LinkedList<Long>> accessHistory;
    private static final int K = 3;  // LRU-K where K=3

    public BufferMgr(FileMgr fm, LogMgr lm, int numbuffs) {
        bufferpool = new Buffer[numbuffs];
        numAvailable = numbuffs;
        bufferPoolMap = new HashMap<>();
        accessHistory = new HashMap<>();
        for (int i = 0; i < numbuffs; i++) {
            bufferpool[i] = new Buffer(fm, lm);
            // Initialize access history for each buffer
            accessHistory.put(bufferpool[i], new LinkedList<>());
        }
    }

    public synchronized int available() {
        return numAvailable;
    }

    public synchronized void flushAll(int txnum) {
        for (Buffer buff : bufferpool)
            if (buff.modifyingTx() == txnum)
                buff.flush();
    }

    public synchronized void unpin(Buffer buff) {
        buff.unpin();
        if (!buff.isPinned()) {
            numAvailable++;
            notifyAll();
        }
    }

    public synchronized Buffer pin(BlockId blk) {
        try {
            long timestamp = System.currentTimeMillis();
            Buffer buff = tryToPin(blk);
            while (buff == null && !waitingTooLong(timestamp)) {
                wait(MAX_TIME);
                buff = tryToPin(blk);
            }
            if (buff == null)
                throw new BufferAbortException();
            return buff;
        } catch (InterruptedException e) {
            throw new BufferAbortException();
        }
    }

    private boolean waitingTooLong(long starttime) {
        return System.currentTimeMillis() - starttime > MAX_TIME;
    }

    private Buffer tryToPin(BlockId blk) {
        Buffer buff = findExistingBuffer(blk);
        if (buff == null) {
            buff = chooseUnpinnedBuffer();
            if (buff == null)
                return null;
            // Remove old mapping before assigning new block
            if (buff.block() != null)
                bufferPoolMap.remove(buff.block());
            buff.assignToBlock(blk);
            // Create new mapping
            bufferPoolMap.put(blk, buff);
        }
        if (!buff.isPinned())
            numAvailable--;
        buff.pin();
        updateAccessHistory(buff);
        return buff;
    }

    private void updateAccessHistory(Buffer buff) {
        LinkedList<Long> history = accessHistory.get(buff);
        if (history == null) {
            history = new LinkedList<>();
            accessHistory.put(buff, history);
        }
        history.addLast(System.currentTimeMillis());
        while (history.size() > K)
            history.removeFirst();
    }

    private Buffer findExistingBuffer(BlockId blk) {
        // Use map instead of scanning array
        return bufferPoolMap.get(blk);
    }

    private Buffer chooseUnpinnedBuffer() {
        Buffer bestBuff = null;
        long maxKthAccessAge = -1;
        long currentTime = System.currentTimeMillis();

        for (Buffer buff : bufferpool) {
            if (!buff.isPinned()) {
                LinkedList<Long> history = accessHistory.get(buff);

                // Calculate backward K distance
                long kDistance;
                if (history.size() < K) {
                    // If fewer than K accesses, use maximum possible distance
                    kDistance = Long.MAX_VALUE;
                } else {
                    // Get the Kth most recent access time
                    kDistance = currentTime - history.get(history.size() - K);
                }

                // If this is our first unpinned buffer or it has a larger K-distance
                if (bestBuff == null || kDistance > maxKthAccessAge) {
                    bestBuff = buff;
                    maxKthAccessAge = kDistance;
                }
            }
        }
        return bestBuff;
    }
}