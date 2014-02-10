/*
 * Binary omparison of files
 */
package com.anowak.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.StandardOpenOption.READ;

/**
 * Just a sample app for using NIO package. See also:
 * http://nadeausoftware.com/articles/2008/02/java_tip_how_read_files_quickly
 *
 * @author Alex
 *
 */
public class BinDiff {

    Path f1, f2;

    public BinDiff(String file1, String file2) {
        FileSystem fs = FileSystems.getDefault();
        f1 = fs.getPath(file1);
        f2 = fs.getPath(file2);
    }

    static public void main(String[] args) throws Exception {
        System.out.println("args: " + args.length);
        if (args.length != 2) {
            throw new IllegalArgumentException("This program needs two arguments <file1> <file2>");
        }

        System.out.println("args[0]='" + args[0] + "' args[1]='" + args[1] + "'");
        BinDiff bd = new BinDiff(args[0], args[1]);
        bd.compare();
    }

    public boolean compare() throws IOException {
        System.out.println("Reading f1: " + f1 + " ...");

        long size1 = Files.size(f1);
        long size2 = Files.size(f2);

        if (size1 != size2) {
            System.out.println("Files are of different size (file1=" + size1 + ", bytes file2=" + size2 + " bytes)");
            return false;
        }

        System.out.println("File size: " + size1 + " bytes");

        final int MAX_BUFF = Integer.MAX_VALUE;//200 * 1024 * 1024; // 2MB
        try (FileChannel ch1 = FileChannel.open(f1, READ);
                FileChannel ch2 = FileChannel.open(f2, READ)) {
            long red = 0L;
            long n = 0;
            do {
                long byteBlock = ch1.size() - red > MAX_BUFF ? MAX_BUFF : ch1.size() - red;
                MappedByteBuffer mb1 = ch1.map(MapMode.READ_ONLY, red, byteBlock);
                MappedByteBuffer mb2 = ch2.map(MapMode.READ_ONLY, red, byteBlock);
//                System.out.printf("[%d] " + n + "] byteBlock=" + byteBlock 
//                        + " limits: (" + mb1.limit() + "," + mb2.limit() + ") Red=" + red 
//                        + " Size=" + ch1.size()
//                        + " " + ((1.0*red/size1)*100.0) + "%");

                System.out.printf("[%d] byteBlock=%d limits (%d,%d), red=%d, size=%d, %d%%\n",
                        n,byteBlock,mb1.limit(),mb2.limit(),red,size1,(int)((1.0*red/size1)*100.0));
                        
                printIt(mb1);
//                printIt(mb2);

                red += byteBlock;
                n++;
            } while (red < ch1.size());
            System.out.println("DONE");
        }
        return true;
    }

    private void printIt(MappedByteBuffer mb) {
        for (int i = 0; i < mb.limit(); i++) {
            if (i < 16 || i > mb.limit() - 17)
                System.out.printf("%2x ", mb.get(i));
            if (i==16)
                System.out.print("|");

        }
           System.out.print("  |  ");

        for (int i = 0; i < mb.limit(); i++) {
            if (i < 16 || i > mb.limit() - 17) {
                byte b = mb.get(i);
                System.out.printf("%c", Character.isJavaIdentifierPart(b)? b : '.');
            }
            if (i==16)
                System.out.print("|");
        }
        System.out.println("");

    }
}
