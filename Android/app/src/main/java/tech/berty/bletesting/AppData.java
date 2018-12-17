package tech.berty.bletesting;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;

final class AppData {
    private static Context currContext;
    private static ArrayList<String> scanned = new ArrayList<>();
    private static final HashMap<String, ArrayList<String>> messages = new HashMap<>();

    private AppData() {}


    // Context related
    static void setCurrContext(Context newContext) { currContext = newContext; }

    static Context getCurrContext() { return currContext; }


    // Device list related
    static void addDeviceToList(String address) {
        synchronized (scanned) {
            if (!scanned.contains(address)) {
                scanned.add(address);

                if (MainActivity.getInstance() != null) {
                    MainActivity.getInstance().addDeviceToList(address);
                }
            }
        }
    }

    static ArrayList<String> getDeviceList() {
        synchronized (scanned) {
            return scanned;
        }
    }

    static void clearDeviceList() {
        synchronized (scanned) {
            scanned = new ArrayList<>();
        }
    }


    // Message list related
    static void addMessageToList(String address, String message) {
        synchronized (messages) {
            if (!messages.containsKey(address)) {
                messages.put(address, new ArrayList<String>());
            }

            messages.get(address).add(message);

            if (ConnectActivity.getInstance() != null) {
                ConnectActivity.getInstance().putMessage(address, message);
            }
        }
    }

    static ArrayList<String> getMessageList(String address) {
        synchronized (messages) {
            if (messages.containsKey(address)) {
                return messages.get(address);
            }
            return null;
        }
    }
}
