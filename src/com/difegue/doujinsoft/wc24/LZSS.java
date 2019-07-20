
package com.difegue.doujinsoft.wc24;

import javax.servlet.*;

/**
 * Interface to the devKitPro gbalzss decompressor. 
 */
public class LZSS {

    private String binaryDir;

    public LZSS(ServletContext context) {
        binaryDir = context.getRealPath("/WEB-INF/lzss");
    }

    public void LZS_Encode(String filename, String output) throws Exception{
        Runtime rt = Runtime.getRuntime();

        if (System.getProperty("os.name").toLowerCase().contains("win"))
            rt.exec(binaryDir+"\\gbalzss.exe e "+filename+" "+output);
        else {
            rt.exec("chmod +x "+binaryDir+"/gbalzss").waitFor();
            rt.exec(binaryDir+"/gbalzss e "+filename+" "+output);
        }
            
    }
    public void LZS_Decode(String filename, String output) throws Exception {
        Runtime rt = Runtime.getRuntime();

        if (System.getProperty("os.name").toLowerCase().contains("win"))
            rt.exec(binaryDir+"\\gbalzss.exe d "+filename+" "+output).waitFor();
        else {
            rt.exec("chmod +x "+binaryDir+"/gbalzss").waitFor();
            rt.exec(binaryDir+"/gbalzss d "+filename+" "+output).waitFor();
        }
    }

}

