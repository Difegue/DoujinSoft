package com.difegue.doujinsoft.utils;

/**
 * Utility class for bit field manipulation, equivalent to the Python bf class
 * used in the mio_hd_thumbnail.py script.
 */
public class BitField {
    private long data;

    public BitField() {
        this.data = 0;
    }

    public BitField(long value) {
        this.data = value;
    }

    /**
     * Get a single bit at the specified index
     */
    public int getBit(int index) {
        return (int) ((data >> index) & 1);
    }

    /**
     * Set a single bit at the specified index
     */
    public void setBit(int index, int value) {
        long bitValue = (value & 1L) << index;
        long mask = 1L << index;
        data = (data & ~mask) | bitValue;
    }

    /**
     * Get a range of bits (equivalent to Python's slice notation)
     * @param start Start bit (inclusive)
     * @param end End bit (exclusive)
     */
    public long getBitRange(int start, int end) {
        long mask = (1L << (end - start)) - 1;
        return (data >> start) & mask;
    }

    /**
     * Set a range of bits (equivalent to Python's slice notation)
     * @param start Start bit (inclusive) 
     * @param end End bit (exclusive)
     * @param value Value to set
     */
    public void setBitRange(int start, int end, long value) {
        long mask = (1L << (end - start)) - 1;
        long maskedValue = (value & mask) << start;
        long rangeMask = mask << start;
        data = (data & ~rangeMask) | maskedValue;
    }

    /**
     * Get the underlying data value
     */
    public long getData() {
        return data;
    }

    /**
     * Set the underlying data value
     */
    public void setData(long value) {
        this.data = value;
    }
}