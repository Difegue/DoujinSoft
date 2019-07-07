
package com.difegue.doujinsoft.wc24;

import com.sun.jna.Library;
import com.sun.jna.Native;

/**
 * JNA interface to CUE's GBA/DS decompressor.
 */
public interface LZSS extends Library {

    LZSS INSTANCE = Native.loadLibrary("lzss", LZSS.class);

    int LZS_VRAM = 0x01; // VRAM compatible, normal mode (LZ10)
    int LZS_WRAM = 0x00; // WRAM compatible, normal mode
    int LZS_WFAST = 0x80; // WRAM compatible, fast mode
    int LZS_VFAST = 0x81; // VRAM compatible, fast mode
    int LZS_WBEST = 0x40; // WRAM compatible, optimal mode (LZ-CUE)
    int LZS_VBEST = 0x41; // VRAM compatible, optimal mode (LZ-CUE)

    void LZS_Encode(String filename, int mode);
    void LZS_Decode(String filename);

}

