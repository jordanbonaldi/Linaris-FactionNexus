package net.neferett.linaris.thenexus.executors;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;

/**
 * Represents a listening scheduler service that returns {@link ListenableScheduledFuture} instead of {@link ScheduledFuture}.
 * @author Kristian
 */
public interface BukkitScheduledExecutorService extends ListeningScheduledExecutorService {
    @Override
    public ListenableScheduledFuture<?> schedule(final Runnable command, final long delay, final TimeUnit unit);

    @Override
    public <V> ListenableScheduledFuture<V> schedule(final Callable<V> callable, final long delay, final TimeUnit unit);

    @Override
    public ListenableScheduledFuture<?> scheduleAtFixedRate(final Runnable command, final long initialDelay, final long period, final TimeUnit unit);

    /**
     * This is not supported by the underlying Bukkit scheduler.
     */
    @Override
    @Deprecated
    public ListenableScheduledFuture<?> scheduleWithFixedDelay(final Runnable command, final long initialDelay, final long delay, final TimeUnit unit);
}