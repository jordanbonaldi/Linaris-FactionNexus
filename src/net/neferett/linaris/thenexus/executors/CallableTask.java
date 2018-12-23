package net.neferett.linaris.thenexus.executors;

import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.base.Preconditions;

import net.neferett.linaris.thenexus.executors.AbstractListeningService.RunnableAbstractFuture;

class CallableTask<T> extends RunnableAbstractFuture<T> {
    protected final Callable<T> compute;

    public CallableTask(final Callable<T> compute) {
        Preconditions.checkNotNull(compute, "compute cannot be NULL");

        this.compute = compute;
    }

    public ListenableScheduledFuture<T> getScheduledFuture(final long startTime, final long nextDelay) {
        return new ListenableScheduledFuture<T>() {
            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) {
                return CallableTask.this.cancel(mayInterruptIfRunning);
            }

            @Override
            public T get() throws InterruptedException, ExecutionException {
                return CallableTask.this.get();
            }

            @Override
            public T get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return CallableTask.this.get(timeout, unit);
            }

            @Override
            public boolean isCancelled() {
                return CallableTask.this.isCancelled();
            }

            @Override
            public boolean isDone() {
                return CallableTask.this.isDone();
            }

            @Override
            public void addListener(final Runnable listener, final Executor executor) {
                CallableTask.this.addListener(listener, executor);
            }

            @Override
            public int compareTo(final Delayed o) {
                return Long.compare(this.getDelay(TimeUnit.NANOSECONDS), o.getDelay(TimeUnit.NANOSECONDS));
            }

            @Override
            public long getDelay(final TimeUnit unit) {
                final long current = System.nanoTime();

                // Calculate the correct delay
                if (current < startTime || !this.isPeriodic()) {
                    return unit.convert(startTime - current, TimeUnit.NANOSECONDS);
                } else {
                    return unit.convert((current - startTime) % nextDelay, TimeUnit.NANOSECONDS);
                }
            }

            @Override
            public boolean isPeriodic() {
                return nextDelay > 0;
            }

            @Override
            public void run() {
                CallableTask.this.compute();
            }
        };
    }

    /**
     * Invoked by the thread responsible for computing this future.
     */
    protected void compute() {
        try {
            // Save result
            if (!this.isCancelled()) {
                this.set(this.compute.call());
            }
        } catch (final Throwable e) {
            this.setException(e);
        }
    }

    @Override
    public void run() {
        this.compute();
    }
}