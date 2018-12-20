package tech.berty.bletesting;

import android.content.Context;

import java.util.concurrent.Semaphore;
import java.util.ArrayList;
import java.util.HashMap;

final class AppData {
    private static Context currContext;

    private static ArrayList<String> rolling1 = new ArrayList<>();
    private static ArrayList<String> rolling2 = new ArrayList<>();
    private static ArrayList<String> backBuffer = rolling1;

    private static Thread listRefresher;
    static Semaphore waitUpdate = new Semaphore(0);

    private static final HashMap<String, ArrayList<String>> messages = new HashMap<>();

    private AppData() {}

    // Context related
    static void setCurrContext(Context newContext) { currContext = newContext; }

    static Context getCurrContext() { return currContext; }


    // Device list related
    static void addDeviceToList(String address) {
        if (listRefresher == null) {
            listRefresher = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            synchronized (rolling1) {
                                synchronized (rolling2) {
                                    waitUpdate.release();
                                    Thread.sleep(420);
                                    waitUpdate.acquire();
                                    backBuffer = (backBuffer == rolling1) ? rolling2 : rolling1;
                                    backBuffer.clear();
                                }
                            }
                            Thread.sleep(5000);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            listRefresher.start();
        }

        synchronized (backBuffer) {
            if (!backBuffer.contains(address)) {
                backBuffer.add(address);

                if (MainActivity.getInstance() != null) {
                    MainActivity.getInstance().addDeviceToList(address);
                }
            }
        }
    }

    static ArrayList<String> getDeviceList() {
        synchronized (backBuffer) {
            return backBuffer;
        }
    }

    static void clearDeviceList() {
        synchronized (rolling1) {
            synchronized (rolling2) {
                rolling1.clear();
                rolling2.clear();
            }
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
