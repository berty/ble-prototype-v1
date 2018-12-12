package tech.berty.bletesting;

import android.util.Log;

class Logger {

    private Logger() {}

    static void put(String level, String tag, String log) {
        switch (level) {
            case "debug":
                Log.d(tag, log);
                break;
            case "info":
                Log.i(tag, log);
                break;
            case "warn":
                Log.w(tag, log);
                break;
            case "error":
                Log.e(tag, log);
                break;
            default:
                Log.e(tag, log);
                break;
        }
    }
}