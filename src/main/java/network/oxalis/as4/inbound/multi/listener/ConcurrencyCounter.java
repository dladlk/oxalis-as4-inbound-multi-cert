package network.oxalis.as4.inbound.multi.listener;

import java.util.concurrent.atomic.AtomicInteger;

import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class ConcurrencyCounter {

	protected AtomicInteger counter;
	protected AtomicInteger maxCounter;

	public ConcurrencyCounter() {
		counter = new AtomicInteger();
		maxCounter = new AtomicInteger();
	}

	public int notifyStart() {
		int v = counter.incrementAndGet();
		int max = maxCounter.get();
		if (max < v) {
			maxCounter.compareAndSet(max, v);
		}
		log.debug("START REQUEST: {}, max {}", v, max);
		return v;
	}

	public int notifyEnd() {
		int v = counter.decrementAndGet();
		log.debug("END REQUEST: {}", v);
		return v;
	}

	public int getCurrentCounter() {
		return this.counter.get();
	}

	public int getMaxCounter() {
		return this.maxCounter.get();
	}

}
