package com.zzw.camear;

import java.io.File;

/**
 * Des:
 * Created by zzw on 2017/11/3.
 */

public class FileUtils {

    public static void deleteFile(String filePath){
        File file=new File(filePath);
        if(file.exists()) {
            file.delete();
        }
    }
}
