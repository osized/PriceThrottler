package com.ayudin.throttler;

public class FastProcessor implements PriceProcessor {
    @Override
    public void onPrice(String ccyPair, double rate) {
        try {
            Thread.sleep(50L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(this.toString() + " have processed " + ccyPair + ": " + rate);
    }

    @Override
    public void subscribe(PriceProcessor priceProcessor) {

    }

    @Override
    public void unsubscribe(PriceProcessor priceProcessor) {

    }
}
