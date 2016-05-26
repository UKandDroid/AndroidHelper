package com.helper.lib;

// Class to create pool of objects for reusing, to save continuous object allocation
public class Pool {
    private Pool next;  // Reference to next object
    private static Pool sPool;
    private static int sPoolSize = 0;
    private static final int MAX_POOL_SIZE = 50;
    private static final Object sPoolSync = new Object();       // The lock used for synchronization

    // CONSTRUCTOR - Private
    private Pool() { }
    // METHOD get pool object only through this method, so no direct allocation are made
    public static Pool obtain() {
        synchronized (sPoolSync) {
            if (sPool != null) {
                Pool m = sPool;
                sPool = m.next;
                m.next = null;
                sPoolSize--;
                return m;
            }
        }        return new Pool();
    }

    // METHOD object added to the pool, to be reused
    public void recycle() {
        synchronized (sPoolSync) {
            if (sPoolSize < MAX_POOL_SIZE) {
                next = sPool;
                sPool = this;
                sPoolSize++;
            }
        }
    }

    // METHOD release pool, ready for garbage collection
    public static void releasePool(){
        sPool = null;
    }
}
