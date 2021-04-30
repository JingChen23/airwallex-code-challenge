package com.airwallex.codechallenge;

import com.airwallex.codechallenge.input.CurrencyConversionRate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CurrencyConversionMonitorTest {

    CurrencyConversionMonitor monitor = null;
    LinkedList<CurrencyConversionRate> ccrList = generateCCRList();

    public LinkedList<CurrencyConversionRate> generateCCRList(){
        LinkedList<CurrencyConversionRate> ccrList = new LinkedList<>();
        ccrList.offer(new CurrencyConversionRate(
                Instant.ofEpochSecond(1554933784, 23_000_000),
                "CNYAUD",
                0.39282
        ));
        ccrList.offer(new CurrencyConversionRate(
                Instant.ofEpochSecond(1554934784, 23_000_000),
                "CNYAUD",
                0.39281
        ));
        return ccrList;
    }


    @BeforeEach
    void initCCM() {
        monitor = new CurrencyConversionMonitor("CNYAUD");
    }

    @Test
    void test_timeGapExceedWindowSize() {
        boolean res = monitor.timeGapExceedWindowSize(
                100, 500);
        assertEquals(true, res);
        res = monitor.timeGapExceedWindowSize(
                300, 500);
        assertEquals(false, res);
    }

    @Test
    void test_adjustTimeWindow() {
        LinkedList<CurrencyConversionRate> ccrList = generateCCRList();
        assertEquals(2, ccrList.size());
        monitor.adjustTimeWindow(ccrList);
        assertEquals(1, ccrList.size());
    }

    @Test
    void test_computeAvgInWindow() {
        double avg = monitor.computeAvgInWindow(this.ccrList);
        assertEquals(0.392815, avg);
    }

    @Test
    void test_getOutputFilePath() {
        assertEquals("example/output1.jsonl",
                monitor.getOutputFilePath());
    }

    @Test
    void test_generateSpotChangeAlarm() {
        CurrencyConversionRate ccr = new CurrencyConversionRate(
                Instant.ofEpochSecond(1554933784, 23_000_000),
                "CNYAUD",
                0.39282
        );
        double rateChangeRatio = 0.08;
        String res = monitor.generateSpotChangeAlarm(rateChangeRatio, ccr);
        assertEquals(null, res);

        rateChangeRatio = 0.11;
        res = monitor.generateSpotChangeAlarm(rateChangeRatio, ccr);
        String expected_res =
                "{ \"timestamp\": 1554933784.023, \"currencyPair\": \"CNYAUD\"," +
                        " \"alert\": \"spotChange\" }";
        assertEquals(expected_res, res);
    }

    @Test
    void test_generateTsString() {
        Instant ts = Instant.ofEpochSecond(1554933784, 23_000_000);
        String res = monitor.generateTsString(ts);
        assertEquals("1554933784.023", res);
    }

    @Test
    void test_getTrendStartTime() {
        CurrencyConversionMonitor.Trend trend = CurrencyConversionMonitor.Trend.Up;
        Instant res = monitor.getTrendStartTime(trend, this.ccrList);
        assertEquals(this.ccrList.get(this.ccrList.size()-2).getTimestamp(), res);
    }

    @Test
    void test_getTrend() {
        CurrencyConversionMonitor.Trend res = monitor.getTrend(this.ccrList);
        assertEquals(CurrencyConversionMonitor.Trend.Down, res);
    }
}