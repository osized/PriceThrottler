package com.ayudin.throttler;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Runner {

    public static void main(String[] args) throws InterruptedException {
        PriceThrottler throttler = new PriceThrottler();
        PriceProcessor fp1 = new FastProcessor();
        PriceProcessor fp2 = new FastProcessor();
        PriceProcessor sp1 = new SlowProcessor();
        PriceProcessor sp2 = new SlowProcessor();
        PriceProcessor fp3 = new FastProcessor();



        throttler.subscribe(fp1);
        throttler.subscribe(fp2);
        throttler.subscribe(sp1);
        throttler.subscribe(sp2);
        throttler.subscribe(fp3);


        Random rnd = new Random();
        List<String> pairs = Stream.of(
                "EURUSD", "USDEUR", "RUBUSD", "USDRUB", "EURRUB", "RUBEUR")
                .collect(Collectors.toList());
        int i = 0;
        while (true) {
            Thread.sleep(rnd.nextInt(500));
            String pair = pairs.get(rnd.nextInt(pairs.size()));
            throttler.onPrice(pair, rnd.nextDouble());
            i++;
            if (i == 20) {
                //throttler.unsubscribe(fp1);
                //throttler.unsubscribe(fp2);
            }
        }

    }
}