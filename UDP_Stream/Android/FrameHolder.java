package com.anthonyalves.sandbox.util;

/**
 * Created by Anthony on 7/31/2015.
 */
public class FrameHolder {

    int frame;
    int count;
    int total;
    byte[][] chunks;

    FrameHolder(int total){
        this.total = total;
        chunks = new byte[total][];
    }

    public int addChunk(byte[] chunk, int chunkPos) {
        this.chunks[chunkPos-1] = chunk;
        this.count++;
        return count;
    }
}
