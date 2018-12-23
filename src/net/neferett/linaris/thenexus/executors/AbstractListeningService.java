package net.neferett.linaris.thenexus.executors;

/*
 * This file is a modified version of 
 * http://gee.cs.oswego.edu/cgi-bin/viewcvs.cgi/jsr166/src/main/java/util/concurrent/AbstractExecutorService.java?revision=1.35
 * which contained the following notice:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166 Expert Group and released to the
 * public domain, as explained at http://creativecommons.org/publicdomain/zero/1.0/
 *
 * Rationale for copying:
 * Guava targets JDK5, whose AbstractExecutorService class lacks the newTaskFor protected
 * customization methods needed by MoreExecutors.listeningDecorator. This class is a copy of
 * AbstractExecutorService from the JSR166 CVS repository. It contains the desired methods.
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.common.util.concurrent.ListeningExecutorService;

/**
 * Provides default implementations of {@link ListeningExecutorService}
 * execution methods. This class implements the <tt>submit</tt>,
 * <tt>invokeAny</tt> and <tt>invokeAll</tt> methods using a
 * {@link ListenableFutureTask} returned by <tt>newTaskFor</tt>. For example,
 * the implementation of <tt>submit(Runnable)</tt> creates an associated
 * <tt>ListenableFutureTask</tt> that is executed and returned.
 * 
 * @author Doug Lea
 */
abstract class AbstractListeningService implements ListeningExecutorService {
    /**
     * Represents a runnable abstract listenable future task.
     * 
     * @author Kristian
     * @param <T>
     */
    public static abstract class RunnableAbstractFuture<T> extends AbstractFuture<T> implements RunnableFuture<T> {

    }

    /**
     * Returns a <tt>ListenableFutureTask</tt> for the given runnable and
     * default value.
     * 
     * @param runnable - the runnable task being wrapped
     * @param value - the default value for the returned future
     * @return a <tt>ListenableFutureTask</tt> which when run will run the
     *         underlying runnable and which, as a <tt>Future</tt>, will yield
     *         the given value as its result and provide for cancellation of the
     *         underlying task.
     */
    protected abstract <T> RunnableAbstractFuture<T> newTaskFor(final Runnable runnable, final T value);

    /**
     * Returns a <tt>ListenableFutureTask</tt> for the given callable task.
     * 
     * @param callable - the callable task being wrapped
     * @return a <tt>ListenableFutureTask</tt> which when run will call the
     *         underlying callable and which, as a <tt>Future</tt>, will yield
     *         the callable's result as its result and provide for cancellation
     *         of the underlying task.
     */
    protected abstract <T> RunnableAbstractFuture<T> newTaskFor(final Callable<T> callable);

    @Override
    public ListenableFuture<?> submit(final Runnable task) {
        if (task == null) { throw new NullPointerException(); }
        final RunnableAbstractFuture<Void> ftask = this.newTaskFor(task, null);
        this.execute(ftask);
        return ftask;
    }

    @Override
    public <T> ListenableFuture<T> submit(final Runnable task, final T result) {
        if (task == null) { throw new NullPointerException(); }
        final RunnableAbstractFuture<T> ftask = this.newTaskFor(task, result);
        this.execute(ftask);
        return ftask;
    }

    @Override
    public <T> ListenableFuture<T> submit(final Callable<T> task) {
        if (task == null) { throw new NullPointerException(); }
        final RunnableAbstractFuture<T> ftask = this.newTaskFor(task);
        this.execute(ftask);
        return ftask;
    }

    /**
     * The main mechanics of invokeAny.
     */
    private <T> T doInvokeAny(final Collection<? extends Callable<T>> tasks, final boolean timed, long nanos) throws InterruptedException, ExecutionException, TimeoutException {
        if (tasks == null) { throw new NullPointerException(); }
        int ntasks = tasks.size();
        if (ntasks == 0) { throw new IllegalArgumentException(); }
        final List<Future<T>> futures = new ArrayList<Future<T>>(ntasks);
        final ExecutorCompletionService<T> ecs = new ExecutorCompletionService<T>(this);

        // For efficiency, especially in executors with limited
        // parallelism, check to see if previously submitted tasks are
        // done before submitting more of them. This interleaving
        // plus the exception mechanics account for messiness of main
        // loop.

        try {
            // Record exceptions so that if we fail to obtain any
            // result, we can throw the last exception we got.
            ExecutionException ee = null;
            long lastTime = timed ? System.nanoTime() : 0;
            final Iterator<? extends Callable<T>> it = tasks.iterator();

            // Start one task for sure; the rest incrementally
            futures.add(ecs.submit(it.next()));
            --ntasks;
            int active = 1;

            for (;;) {
                Future<T> f = ecs.poll();
                if (f == null) {
                    if (ntasks > 0) {
                        --ntasks;
                        futures.add(ecs.submit(it.next()));
                        ++active;
                    } else if (active == 0) {
                        break;
                    } else if (timed) {
                        f = ecs.poll(nanos, TimeUnit.NANOSECONDS);
                        if (f == null) { throw new TimeoutException(); }
                        final long now = System.nanoTime();
                        nanos -= now - lastTime;
                        lastTime = now;
                    } else {
                        f = ecs.take();
                    }
                }
                if (f != null) {
                    --active;
                    try {
                        return f.get();
                    } catch (final ExecutionException eex) {
                        ee = eex;
                    } catch (final RuntimeException rex) {
                        ee = new ExecutionException(rex);
                    }
                }
            }

            if (ee == null) {
                ee = new ExecutionException(null);
            }
            throw ee;

        } finally {
            for (final Future<T> f : futures) {
                f.cancel(true);
            }
        }
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        try {
            return this.doInvokeAny(tasks, false, 0);
        } catch (final TimeoutException cannotHappen) {
            // assert false;
            return null;
        }
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return this.doInvokeAny(tasks, true, unit.toNanos(timeout));
    }

    @Override
    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks) throws InterruptedException {
        if (tasks == null) { throw new NullPointerException(); }
        final List<Future<T>> futures = new ArrayList<Future<T>>(tasks.size());
        boolean done = false;
        try {
            for (final Callable<T> t : tasks) {
                final RunnableAbstractFuture<T> f = this.newTaskFor(t);
                futures.add(f);
                this.execute(f);
            }
            for (final Future<T> f : futures) {
                if (!f.isDone()) {
                    try {
                        f.get();
                    } catch (final CancellationException ignore) {} catch (final ExecutionException ignore) {}
                }
            }
            done = true;
            return futures;
        } finally {
            if (!done) {
                for (final Future<T> f : futures) {
                    f.cancel(true);
                }
            }
        }
    }

    @Override
    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit) throws InterruptedException {
        if (tasks == null || unit == null) { throw new NullPointerException(); }
        long nanos = unit.toNanos(timeout);
        final List<Future<T>> futures = new ArrayList<Future<T>>(tasks.size());
        boolean done = false;
        try {
            for (final Callable<T> t : tasks) {
                futures.add(this.newTaskFor(t));
            }

            long lastTime = System.nanoTime();

            // Interleave time checks and calls to execute in case
            // executor doesn't have any/much parallelism.
            final Iterator<Future<T>> it = futures.iterator();
            while (it.hasNext()) {
                this.execute((Runnable) it.next());
                final long now = System.nanoTime();
                nanos -= now - lastTime;
                lastTime = now;
                if (nanos <= 0) { return futures; }
            }

            for (final Future<T> f : futures) {
                if (!f.isDone()) {
                    if (nanos <= 0) { return futures; }
                    try {
                        f.get(nanos, TimeUnit.NANOSECONDS);
                    } catch (final CancellationException ignore) {} catch (final ExecutionException ignore) {} catch (final TimeoutException toe) {
                        return futures;
                    }
                    final long now = System.nanoTime();
                    nanos -= now - lastTime;
                    lastTime = now;
                }
            }
            done = true;
            return futures;
        } finally {
            if (!done) {
                for (final Future<T> f : futures) {
                    f.cancel(true);
                }
            }
        }
    }
}
