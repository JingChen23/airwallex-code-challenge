package com.airwallex.codechallenge;

import com.airwallex.codechallenge.input.CurrencyConversionRate;
import com.airwallex.codechallenge.input.RateChangeAlarm;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.*;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.LinkedList;


public class CurrencyConversionMonitor {

    private String currencyPair = null; // The Id of the object, such as "CNYAUD"

    public static enum Trend {Up, Down, NoChange}
    private int timeWindowForSpotChangeAlarm = 300; // Compute average based on this size
    private int timeWindowForTrendAlarm = 900;      // Send trend alarm based on this size
    private int ThrottleLogWaitingTime = 60;        // tread alarm's throttle time
    private double rateChangeRatioThreshold = 0.1;  // Threshold for trigger a spot change

    private Trend lastTrend = null;
    private Instant trendStartTime = null;
    private Instant lastTrendAlarmTs = null;

    // Defind the alarm type
    private String alarmSpotChange = "spotChange";
    private String rising = "rising";
    private String falling = "falling";

    // The rate history queue, the length will be adjusted based on timeWindowForSpotChangeAlarm
    private LinkedList<CurrencyConversionRate> rateHistoryQueue = new LinkedList<>();

    public CurrencyConversionMonitor(String currencyPair) {
        this.currencyPair = currencyPair;
    }

    public Trend getTrend(LinkedList<CurrencyConversionRate> rateHistoryQueue){
        /*
          Get the trend of the recent 2 data, up? down? or nochange?
         */
        int queueLength = rateHistoryQueue.size();
        double thisRate = rateHistoryQueue.get(queueLength-1).getRate();
        double lastRate = rateHistoryQueue.get(queueLength-2).getRate();
        if (thisRate > lastRate) {return Trend.Up;}
        else if (thisRate < lastRate) {return Trend.Down;}
        else {return Trend.NoChange;}
    }

    public Instant getTrendStartTime(Trend newTrend, LinkedList<CurrencyConversionRate> rateHistoryQueue){
        /*
          If trend doesn't change, then the start time is recorded in the member variable trendStartTime
          Else, the trend starts from the second data on the right side of the queue.
         */
        if (newTrend != this.lastTrend) {
            return rateHistoryQueue.get(rateHistoryQueue.size()-2).getTimestamp();
        }
        return this.trendStartTime;
    }

    public String generateTsString(Instant ts) {
        DecimalFormat decimalFormat = new DecimalFormat("#.000");
        return decimalFormat.format((double) (ts.toEpochMilli()) / 1000.0);
    }

    public String generateTrendAlarm(CurrencyConversionRate ccr, Trend newTrend){
        if (newTrend == Trend.NoChange) {
            return null;
        }
        Instant now = ccr.getTimestamp();
        long timeDiff = now.getEpochSecond() - this.trendStartTime.getEpochSecond();
        if (timeDiff > this.timeWindowForTrendAlarm) {
            if (this.lastTrendAlarmTs == null ||
                    now.getEpochSecond() - this.lastTrendAlarmTs.getEpochSecond() > this.ThrottleLogWaitingTime){
                RateChangeAlarm trendAlarm = new RateChangeAlarm(
                        this.generateTsString(now),
                        this.currencyPair,
                        (newTrend == Trend.Up)? this.rising : this.falling,
                        timeDiff);
                this.lastTrendAlarmTs = now;
                return trendAlarm.toString();
            }
        }
        return null;
    }

    public boolean timeGapExceedWindowSize(long oldestTime, long newTime) {
        // Help to compute if the time diff of 2 data exceeds the window size
        return newTime - oldestTime > timeWindowForSpotChangeAlarm ? true : false;
    }

    public void adjustTimeWindow(LinkedList<CurrencyConversionRate> rateHistoryQueue) {
        // Help to kick out the outdated data from the queue
        while
        (timeGapExceedWindowSize(
                rateHistoryQueue.peekFirst().getTimestamp().getEpochSecond(),
                rateHistoryQueue.peekLast().getTimestamp().getEpochSecond())) {
            rateHistoryQueue.pollFirst();
        }
    }

    public double computeAvgInWindow(LinkedList<CurrencyConversionRate> rateHistoryQueue) {
        double sum = 0.0;
        int len = rateHistoryQueue.size();
        for (int i = 0; i < len; i++) {
            sum += rateHistoryQueue.get(i).getRate();
        }
        double avg = sum / len;
        return avg;
    }

    public String generateSpotChangeAlarm(double rateChangeRatio, CurrencyConversionRate ccr) {
        if (Math.abs(rateChangeRatio) < this.rateChangeRatioThreshold) return null;
        String spotChangeAlarm = new RateChangeAlarm(
                this.generateTsString(ccr.getTimestamp()),
                this.currencyPair,
                this.alarmSpotChange,
                0).toString();
        return spotChangeAlarm;
    }

    public String getOutputFilePath() {
        return "example/output1.jsonl";
    }

    public void sendAlarm(String alarm) {
        if (alarm == null) {return;}
        try(FileWriter fw = new FileWriter(this.getOutputFilePath(), true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw))
        {
            out.println(alarm);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void handleOneCCRData(CurrencyConversionRate currencyConversionRate) {
        /*
            This is the main worker function which will handle one piece of
            currencyConversionRate data.
         */
        System.out.println("handle " + currencyConversionRate.toString());

        // Step 1: Append the new data into the queue.
        this.rateHistoryQueue.addLast(currencyConversionRate);

        if (this.rateHistoryQueue.size() == 1) {
            this.lastTrend = Trend.NoChange;
            this.trendStartTime = currencyConversionRate.getTimestamp();
            return;
        }

        // Step 2: Generate the trend alarm if 2 conditions are satisfied:
        // 1. Trend last for more than 15 mins.
        // 2. More than 1 min passed since the last log.
        Trend newTrend = getTrend(this.rateHistoryQueue);
        this.trendStartTime = this.getTrendStartTime(newTrend, this.rateHistoryQueue);
        String trendAlarm = this.generateTrendAlarm(currencyConversionRate, newTrend);
        this.sendAlarm(trendAlarm);
        this.lastTrend = newTrend;

        // Step 3: Adjust the window, kock out the > 5 min old data.
        adjustTimeWindow(this.rateHistoryQueue);

        // Step 4: Compute the average of the time window, compute the rate change ratio.
        double avgInTimeWindow = computeAvgInWindow(this.rateHistoryQueue);
        double rateDiff = currencyConversionRate.getRate() - avgInTimeWindow;
        double rateChangeRatio = rateDiff / avgInTimeWindow;

        // Step 5: Generate the spot change alarm if the abs(rateChangeRatio) > rateChangeRatioThreshold
        String spotChangeAlarm = generateSpotChangeAlarm(rateChangeRatio, currencyConversionRate);
        sendAlarm(spotChangeAlarm);
    }

}
