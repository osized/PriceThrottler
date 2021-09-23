package com.ayudin.throttler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PriceThrottler implements PriceProcessor {


    private List<PriceProcessorWrapper> processors = new ArrayList<>();
    private ExecutorService executor = Executors.newCachedThreadPool();


    @Override
    public void onPrice(String ccyPair, double rate) {
        processors.forEach(p -> p.onUpdate(ccyPair, rate));
    }

    @Override
    public void subscribe(PriceProcessor priceProcessor) {
        PriceProcessorWrapper wrapper = new PriceProcessorWrapper(priceProcessor, executor);
        wrapper.startPolling();
        processors.add(wrapper);
    }

    @Override
    public void unsubscribe(PriceProcessor priceProcessor) {
        processors.stream().filter(p -> p.isWrapperFor(priceProcessor)).forEach(PriceProcessorWrapper::stopPolling);
        processors.removeIf(p -> p.isWrapperFor(priceProcessor));
    }


}
