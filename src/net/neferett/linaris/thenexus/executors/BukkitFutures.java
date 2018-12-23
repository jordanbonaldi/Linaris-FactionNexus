package net.neferett.linaris.thenexus.executors;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

public class BukkitFutures {
    // Represents empty classes
    private static Listener EMPTY_LISTENER = new Listener() {};

    /**
     * Retrieve a future representing the next invocation of the given event.
     * @param plugin - owner plugin.
     * @return Future event invocation.
     */
    public static <TEvent extends Event> ListenableFuture<TEvent> nextEvent(final Plugin plugin, final Class<TEvent> eventClass) {
        return BukkitFutures.nextEvent(plugin, eventClass, EventPriority.NORMAL, false);
    }

    /**
     * Retrieve a future representing the next invocation of the given event.
     * @param plugin - owner plugin.
     * @return Future event invocation.
     */
    public static <TEvent extends Event> ListenableFuture<TEvent> nextEvent(final Plugin plugin, final Class<TEvent> eventClass, final EventPriority priority, final boolean ignoreCancelled) {

        // Event and future
        final HandlerList list = BukkitFutures.getHandlerList(eventClass);
        final SettableFuture<TEvent> future = SettableFuture.create();

        final EventExecutor executor = new EventExecutor() {
            private final AtomicBoolean once = new AtomicBoolean();

            @SuppressWarnings("unchecked")
            @Override
            public void execute(final Listener listener, final Event event) throws EventException {
                // Fire the future
                if (!future.isCancelled() && !once.getAndSet(true)) {
                    future.set((TEvent) event);
                }
            }
        };
        final RegisteredListener listener = new RegisteredListener(BukkitFutures.EMPTY_LISTENER, executor, priority, plugin, ignoreCancelled) {
            @Override
            public void callEvent(final Event event) throws EventException {
                super.callEvent(event);
                list.unregister(this);
            }
        };

        // Ensure that the future is cleaned up when the plugin is disabled
        PluginDisabledListener.getListener(plugin).addFuture(future);

        // Add the listener
        list.register(listener);
        return future;
    }

    /**
     * Register a given event executor.
     * @param plugin - the owner plugin.
     * @param eventClass - the event to register.
     * @param priority - the event priority.
     * @param executor - the event executor.
     */
    public static void registerEventExecutor(final Plugin plugin, final Class<? extends Event> eventClass, final EventPriority priority, final EventExecutor executor) {
        BukkitFutures.getHandlerList(eventClass).register(new RegisteredListener(BukkitFutures.EMPTY_LISTENER, executor, priority, plugin, false));
    }

    /**
     * Retrieve the handler list associated with the given class.
     * @param clazz - given event class.
     * @return Associated handler list.
     */
    private static HandlerList getHandlerList(Class<? extends Event> clazz) {
        // Class must have Event as its superclass
        while (clazz.getSuperclass() != null && Event.class.isAssignableFrom(clazz.getSuperclass())) {
            try {
                final Method method = clazz.getDeclaredMethod("getHandlerList");
                method.setAccessible(true);
                return (HandlerList) method.invoke(null);
            } catch (final NoSuchMethodException e) {
                // Keep on searching
                clazz = clazz.getSuperclass().asSubclass(Event.class);
            } catch (final Exception e) {
                throw new IllegalPluginAccessException(e.getMessage());
            }
        }
        throw new IllegalPluginAccessException("Unable to find handler list for event " + clazz.getName());
    }
}
