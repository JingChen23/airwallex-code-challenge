package com.airwallex.codechallenge;

import com.airwallex.codechallenge.input.CurrencyConversionRate;
import com.airwallex.codechallenge.input.Reader;
import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;


public class App {

    // This is a map like {"AUDCNY": Monitor1, "CNYAUD": Monitor2, ""}
    public static HashMap<String, CurrencyConversionMonitor> currencyPair2MonitorObjMap = new HashMap<>();

    public static void clearTheOutputFile() {
        try {
            FileOutputStream writer = new FileOutputStream("example/output1.jsonl");
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    public static CurrencyConversionMonitor getMonitor(String currencyPair) {
        if (!currencyPair2MonitorObjMap.containsKey(currencyPair)) {
            CurrencyConversionMonitor monitor = new CurrencyConversionMonitor(currencyPair);
            currencyPair2MonitorObjMap.put(currencyPair, monitor);
            return monitor;
        }
        return currencyPair2MonitorObjMap.get(currencyPair);
    }

    public static void handleOneCCRData(@NotNull CurrencyConversionRate ccr) {
        String currencyPair = ccr.getCurrencyPair();
        CurrencyConversionMonitor monitor = getMonitor(currencyPair);
        monitor.handleOneCCRData(ccr);
    }

    public static void main(String[] args) {
        Reader reader = new Reader();
        App app = new App();
        String arg = "example/input1.jsonl";
        clearTheOutputFile();

        reader.read(arg)
                .forEach(currencyConversionRate -> handleOneCCRData(currencyConversionRate));
    }
}
