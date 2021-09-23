package com.ayudin.throttler;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class PriceProcessorWrapper {
    private LinkedHashMap<String, Double> pairs2Values = new LinkedHashMap<>();
    private final Deque<String> processed = new ArrayDeque<>();
    private final int PROCESSED_CAP = 20;
    private final PriceProcessor priceProcessor;
    private ReentrantLock pairsLock = new ReentrantLock();
    private Condition waitForPairCondition = pairsLock.newCondition();
    private final ExecutorService executor;
    private Future pollingTask;

    public PriceProcessorWrapper(PriceProcessor priceProcessor, ExecutorService executor) {
        this.priceProcessor = priceProcessor;
        this.executor = executor;
    }

    public void onUpdate(String pair, Double value) {
        /* If lock is free, add pair in current thread
         Pairs2Values is locked during pair selection, but is unlocked during onPrice execution
         So, for the cases where onPrice takes significant time, this lock will be mostly available
         */
        if (pairsLock.tryLock()) {
            try {
                pairs2Values.put(pair, value);
                waitForPairCondition.signal();
            } finally {
                pairsLock.unlock();
            }
        } else {
            // If lock is not free, schedule update in separate thread
            // So onUpdate() never blocks
            executor.execute(() -> {
                pairsLock.lock();
                try {
                    pairs2Values.put(pair, value);
                    waitForPairCondition.signal();
                } finally {
                    pairsLock.unlock();
                }
            });
        }
    }

    public boolean isWrapperFor(PriceProcessor priceProcessor) {
        return this.priceProcessor.equals(priceProcessor);
    }

    public void stopPolling() {
        if (this.pollingTask != null) {
            this.pollingTask.cancel(true);
        }
    }

    public void startPolling() {
        stopPolling();
        this.pollingTask = this.executor.submit(() -> {
            while (true) {
                // Run task in the same thread
                prepareTask().run();
            }
        });
    }

    private Runnable prepareTask() throws InterruptedException {

        pairsLock.lock();
        Optional<String> bestPick = Optional.empty();
        try {
            do {
                // Find first not recently processed
                bestPick = pairs2Values.keySet().stream().filter(key -> !processed.contains(key)).findFirst();
                if (bestPick.isEmpty()) {
                    // Find the oldest key in LinkedHashMap
                    bestPick = pairs2Values.keySet().stream().findFirst();
                }
                if (bestPick.isEmpty()) {
                    // Map is empty, wait for new pairs
                    waitForPairCondition.await();
                }
            } while (bestPick.isEmpty());

            String chosenPair = bestPick.get();
            Double chosenValue = pairs2Values.get(chosenPair);

            pairs2Values.remove(chosenPair);
            processed.add(chosenPair);
            if (processed.size() > PROCESSED_CAP) {
                processed.remove();
            }
            return () -> priceProcessor.onPrice(chosenPair, chosenValue);
        } finally {
            pairsLock.unlock();
        }
    }

}
