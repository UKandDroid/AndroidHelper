package com.helper.lib;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Ubaid on 29/06/2016.
 */
public class Logger {
    private int iLogLevel = 3;
    private String LOG_TAG = "";
    private static Context context;
    private boolean bSaveToFile = false;
    private static FileOutputStream stream ;
    private boolean INTERNAL_STORAGE = true;
    private static String FILE_NAME = "debug.html";
    private static String DIRECTORY = "/Android/logs/";
    private static final String END = "</font>";
    private static final String FONT_RED ="<font face=\"sans-serif\" color=\"red\">";
    private static final String FONT_BLUE ="<font face=\"sans-serif\" color=\"blue\">";
    private static final String FONT_BLACK ="<font face=\"sans-serif\" color=\"black\">";

    public Logger(String sLogTag) { LOG_TAG = sLogTag;}
    public Logger() {
        String sCallingClass = Thread.currentThread().getStackTrace()[3].getClassName();
        String sSplitClass[] = sCallingClass.split("\\.");
        LOG_TAG = sSplitClass[sSplitClass.length-1];
    }

    public int getLogLevel() { return this.iLogLevel; }
    public void setLogLevel(int iLevel){ this.iLogLevel = iLevel; }

    // METHOD for logging
    public void d(String sLog){ d(1, sLog); }
    public void e(String sLog){ e(1, sLog); }
    public void w(String sLog){ w(1, sLog); }
    public void d(int iLevel, String sLog) {
        sLog = sLog == null ? "" : sLog;
        if(bSaveToFile) { logToFile( FONT_BLACK + sLog + END); }
        if(iLevel <= iLogLevel) { Log.d(LOG_TAG, sLog); } }

    public void e(int iLevel, String sLog){
        sLog = sLog == null ? "" : sLog;
        if(bSaveToFile) { logToFile(FONT_RED +sLog + END ); }
        if(iLevel <= iLogLevel) { Log.e(LOG_TAG, sLog); } }

    public void w(int iLevel, String sLog){
        sLog = sLog == null ? "" : sLog;
        if(bSaveToFile) { logToFile(FONT_BLUE +sLog + END ); }
        if(iLevel <= iLogLevel) { Log.w(LOG_TAG, sLog); }}

    public void saveToFile(Context con){
        context = con;
        bSaveToFile = true;
        if(stream == null){
            try {
                if(INTERNAL_STORAGE) {
                    stream = context.openFileOutput(FILE_NAME, Context.MODE_APPEND);
                } else {
                    File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + DIRECTORY);
                    if(!dir.exists()) dir.mkdirs();
                    File file = new File(dir, FILE_NAME);
                    if(!file.exists())
                        file.createNewFile();
                    stream = new FileOutputStream(file, true );
                }
            } catch (Exception e) { e.printStackTrace();}
        }
    }

    private void logToFile(String sLog){
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM HH:mm:ss");
        sLog = sdf.format(new Date()) + " "+ sLog + "<br>\n\r";
        try {
            if(stream != null)
                stream.write(sLog.getBytes());
        } catch (FileNotFoundException e) { e.printStackTrace();
        } catch (IOException e) { e.printStackTrace();
        }
    }

    public StringBuilder getLogFileData(){
        File file;
        StringBuilder sb = new StringBuilder();

        if(INTERNAL_STORAGE){
            file = context.getFileStreamPath(FILE_NAME);
        } else {
            File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + DIRECTORY);
            file = new File(dir, FILE_NAME);
        }

        if(!file.exists())
            return null;

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(new ReverseLineInputStream(file)));
            while(sb.length() < 1024*1024) {        // Read only 1MB
                String line = in.readLine();
                if (line == null) { break; }
                sb.append(line);
            }

            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return  sb;
    }

    public boolean deleteFile(){
        if(INTERNAL_STORAGE){
            File dir = context.getFilesDir();
            File file = new File(dir, FILE_NAME);
            file.delete();                                                                          // Delete old file
            stream = null;
            saveToFile(context);                                                                    // Create new one
        } else {
            File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + DIRECTORY);
            File file = new File(dir, FILE_NAME);
            PrintWriter writer = null;
            try {
                writer = new PrintWriter(file);
                writer.print("");
                writer.close();
            } catch (FileNotFoundException e) { e.printStackTrace();}
        }
        return true;
    }


    // Class to read Log file from End
    public class ReverseLineInputStream extends InputStream {

        RandomAccessFile in;

        long currentLineStart = -1;
        long currentLineEnd = -1;
        long currentPos = -1;
        long lastPosInFile = -1;
        int lastChar = -1;


        public ReverseLineInputStream(File file) throws FileNotFoundException {
            in = new RandomAccessFile(file, "r");
            currentLineStart = file.length();
            currentLineEnd = file.length();
            lastPosInFile = file.length() -1;
            currentPos = currentLineEnd;

        }

        private void findPrevLine() throws IOException {
            if (lastChar == -1) {
                in.seek(lastPosInFile);
                lastChar = in.readByte();
            }

            currentLineEnd = currentLineStart;

            // There are no more lines, since we are at the beginning of the file and no lines.
            if (currentLineEnd == 0) {
                currentLineEnd = -1;
                currentLineStart = -1;
                currentPos = -1;
                return;
            }

            long filePointer = currentLineStart -1;

            while ( true) {
                filePointer--;

                // we are at start of file so this is the first line in the file.
                if (filePointer < 0) {
                    break;
                }

                in.seek(filePointer);
                int readByte = in.readByte();

                // We ignore last LF in file. search back to find the previous LF.
                if (readByte == 0xA && filePointer != lastPosInFile ) {
                    break;
                }
            }
            // we want to start at pointer +1 so we are after the LF we found or at 0 the start of the file.
            currentLineStart = filePointer + 1;
            currentPos = currentLineStart;
        }

        public int read() throws IOException {

            if (currentPos < currentLineEnd ) {
                in.seek(currentPos++);
                int readByte = in.readByte();
                return readByte;
            } else if (currentPos > lastPosInFile && currentLineStart < currentLineEnd) {
                // last line in file (first returned)
                findPrevLine();
                if (lastChar != '\n' && lastChar != '\r') {
                    // last line is not terminated
                    return '\n';
                } else {
                    return read();
                }
            } else if (currentPos < 0) {
                return -1;
            } else {
                findPrevLine();
                return read();
            }
        }

        @Override
        public void close() throws IOException {
            if (in != null) {
                in.close();
                in = null;
            }
        }
    }
}
