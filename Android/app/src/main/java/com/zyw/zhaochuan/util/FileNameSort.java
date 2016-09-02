package com.zyw.zhaochuan.util;

import java.io.File;
import java.util.Comparator;

/**
 * Created by zyw on 2016/5/19.
 */
public class FileNameSort implements Comparator<File>{


    @Override
    public int compare(File p1, File p2)
    {
        // TODO: Implement this method
        if ((p1.isFile() && p2.isFile()) || (!p1.isFile() && !p2.isFile()))
        {
            return p1.getName().compareToIgnoreCase(p2.getName());
        }
        else if (p1.isFile() && !p2.isFile())
        {
            return 1;
        }
        else{
            return -1;
        }
    }

}
