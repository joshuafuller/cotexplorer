package com.atakmap.android.cotexplorer.plugin;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;

public class PluginNativeLoader {

    private static final String TAG = "NativeLoader";
    private static String ndl = null;

    // Path to the desired directory
    private static final String COTEXPLORER_DIR = Environment.getExternalStorageDirectory() + "/atak/tools/cotexplorer";

    synchronized static public void init(final Context context) {
        if (ndl == null) {
            try {
                // Initialize native library directory
                ndl = context.getPackageManager()
                        .getApplicationInfo(context.getPackageName(), 0)
                        .nativeLibraryDir;

                // Create the /atak/tools/cotexplorer directory if it doesn't exist
                File dir = new File(COTEXPLORER_DIR);
                if (!dir.exists()) {
                    if (dir.mkdirs()) {
                        Log.i(TAG, "Directory created at: " + COTEXPLORER_DIR);
                    } else {
                        Log.e(TAG, "Failed to create directory at: " + COTEXPLORER_DIR);
                    }
                }
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "native library loading will fail, unable to grab the nativeLibraryDir from the package name");
            }
        }
    }

    public static void loadLibrary(final String name) {
        if (ndl != null) {
            final String lib = ndl + File.separator
                    + System.mapLibraryName(name);
            if (new File(lib).exists()) {
                System.load(lib);
            }
        } else {
            throw new IllegalArgumentException("NativeLoader not initialized");
        }
    }
    public static String getCotExplorerDir() {
        return COTEXPLORER_DIR;
    }
}
