package net.neferett.linaris.thenexus.executors;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;

import org.bukkit.scheduler.BukkitTask;

import com.google.common.base.Throwables;

abstract class AbstractBukkitService extends AbstractListeningService implements BukkitScheduledExecutorService {

    private static final long MILLISECONDS_PER_TICK = 50;
    private static final long NANOSECONDS_PER_TICK = 1000000 * AbstractBukkitService.MILLISECONDS_PER_TICK;

    private volatile boolean shutdown;
    private final PendingTasks tasks;

    public AbstractBukkitService(final PendingTasks tasks) {
        this.tasks = tasks;
    }

    @Override
    protected <T> RunnableAbstractFuture<T> newTaskFor(final Runnable runnable, final T value) {
        return this.newTaskFor(Executors.callable(runnable, value));
    }

    @Override
    protected <T> RunnableAbstractFuture<T> newTaskFor(final Callable<T> callable) {
        this.validateState();
        return new CallableTask<T>(callable);
    }

    @Override
    public void execute(final Runnable command) {
        this.validateState();

        if (command instanceof RunnableFuture) {
            tasks.add(this.getTask(command), (Future<?>) command);
        } else {
            // Submit it first
            this.submit(command);
        }
    }

    // Bridge to Bukkit
    protected abstract BukkitTask getTask(final Runnable command);

    protected abstract BukkitTask getLaterTask(final Runnable task, final long ticks);

    protected abstract BukkitTask getTimerTask(final long ticksInitial, final long ticksDelay, final Runnable task);

    @Override
    public List<Runnable> shutdownNow() {
        this.shutdown();
        tasks.cancel();

        // We don't support this
        return Collections.emptyList();
    }

    @Override
    public void shutdown() {
        shutdown = true;
    }

    private void validateState() {
        if (shutdown) { throw new RejectedExecutionException("Executor service has shut down. Cannot start new tasks."); }
    }

    private long toTicks(final long delay, final TimeUnit unit) {
        return Math.round(unit.toMillis(delay) / (double) AbstractBukkitService.MILLISECONDS_PER_TICK);
    }

    @Override
    public ListenableScheduledFuture<?> schedule(final Runnable command, final long delay, final TimeUnit unit) {
        return this.schedule(Executors.callable(command), delay, unit);
    }

    @Override
    public <V> ListenableScheduledFuture<V> schedule(final Callable<V> callable, final long delay, final TimeUnit unit) {
        final long ticks = this.toTicks(delay, unit);

        // Construct future task and Bukkit task
        final CallableTask<V> task = new CallableTask<V>(callable);
        final BukkitTask bukkitTask = this.getLaterTask(task, ticks);

        tasks.add(bukkitTask, task);
        return task.getScheduledFuture(System.nanoTime() + delay * AbstractBukkitService.NANOSECONDS_PER_TICK, 0);
    }

    @Override
    public ListenableScheduledFuture<?> scheduleAtFixedRate(final Runnable command, final long initialDelay, final long period, final TimeUnit unit) {

        final long ticksInitial = this.toTicks(initialDelay, unit);
        final long ticksDelay = this.toTicks(period, unit);

        // Construct future task and Bukkit task
        final CallableTask<?> task = new CallableTask<Object>(Executors.callable(command)) {
            @Override
            protected void compute() {
                // Do nothing more. This future can only be finished by cancellation
                try {
                    compute.call();
                } catch (final Exception e) {
                    // Let Bukkit handle this
                    throw Throwables.propagate(e);
                }
            }
        };
        final BukkitTask bukkitTask = this.getTimerTask(ticksInitial, ticksDelay, task);

        tasks.add(bukkitTask, task);
        return task.getScheduledFuture(System.nanoTime() + ticksInitial * AbstractBukkitService.NANOSECONDS_PER_TICK, ticksDelay * AbstractBukkitService.NANOSECONDS_PER_TICK);
    }

    // Not supported!
    @Deprecated
    @Override
    public ListenableScheduledFuture<?> scheduleWithFixedDelay(final Runnable command, final long initialDelay, final long delay, final TimeUnit unit) {
        return this.scheduleAtFixedRate(command, initialDelay, delay, unit);
    }

    @Override
    public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
        return tasks.awaitTermination(timeout, unit);
    }

    @Override
    public boolean isShutdown() {
        return shutdown;
    }

    @Override
    public boolean isTerminated() {
        return tasks.isTerminated();
    }
}