package info.unterrainer.commons.crontabscheduler;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CrontabScheduler {

	protected ScheduledExecutorService executor;
	protected Map<String, BasicCrontabHandler> registeredHandlers = new HashMap<>();

	/**
	 * Sets new handlers or replaces existing ones in a way that no event-trigger
	 * gets lost.
	 *
	 * @param handlers the handlers to set or use to replace the old ones
	 */
	public synchronized void setHandlers(final Collection<BasicCrontabHandler> handlers) {
		if (handlers == null)
			throw new NullPointerException("Specify a valid collection of handlers.");
		for (BasicCrontabHandler handler : handlers)
			if (handler == null)
				throw new NullPointerException("The list to set contains a null-value as a handler.");

		Map<String, BasicCrontabHandler> newMap = new HashMap<>();
		for (BasicCrontabHandler handler : handlers)
			newMap.put(handler.getName(), handler);
		setHandlers(newMap);
	}

	/**
	 * Sets new handlers or replaces existing ones in a way that no event-trigger
	 * gets lost.
	 *
	 * @param handlers the handlers to set or use to replace the old ones
	 */
	public synchronized void setHandlers(final Map<String, BasicCrontabHandler> handlers) {
		ZonedDateTime now = ZonedDateTime.now();
		if (handlers == null)
			throw new NullPointerException("Specify a valid collection of handlers.");

		for (BasicCrontabHandler handler : handlers.values())
			if (handler == null)
				throw new NullPointerException("The list to set contains a null-value as a handler.");

		log.debug("Setting handlers to [{}].", String.join(",",
				handlers.values().stream().map(BasicCrontabHandler::getName).collect(Collectors.toList())));

		// Switch handlers.
		Map<String, BasicCrontabHandler> oldMap = registeredHandlers;
		registeredHandlers = handlers;

		for (BasicCrontabHandler handler : registeredHandlers.values())
			handler.initialize(now);
		pollAndAdvanceHandlers(now, oldMap);
	}

	/**
	 * Gets you a detached copy of the underlying handler-map to edit and manipulate
	 * in order to later set it again using {@link #setHandlers(Map)}.<br>
	 * Handlers are still references. So if you manipulate the handlers in this list
	 * directly, you manipulate the same objects as in the registered-handlers-map
	 * that's being currently used.
	 *
	 * @return a new handler-map
	 */
	public synchronized Map<String, BasicCrontabHandler> getCopyOfHandlerMap() {
		return new HashMap<>(registeredHandlers);
	}

	/**
	 * Gets you a detached copy of the underlying handler-maps' values to edit and
	 * manipulate in order to later set it again using
	 * {@link #setHandlers(Collection)}.<br>
	 * Handlers are still references. So if you manipulate the handlers in this list
	 * directly, you manipulate the same objects as in the registered-handlers-map
	 * that's being currently used.
	 *
	 * @return a new handler-map
	 */
	public synchronized List<BasicCrontabHandler> getCopyOfHandlers() {
		return new ArrayList<>(registeredHandlers.values());
	}

	public synchronized void clearHandlers() {
		log.debug("Clearing handler-map.");
		registeredHandlers.clear();
	}

	@Builder
	public CrontabScheduler(final long period, final TimeUnit timeUnit, final Consumer<CrontabScheduler> setupHandler) {

		if (setupHandler != null)
			setupHandler.accept(this);

		executor = Executors.newSingleThreadScheduledExecutor();
		executor.scheduleWithFixedDelay(() -> pollAndAdvanceHandlers(registeredHandlers), 0, period, timeUnit);
	}

	private synchronized void pollAndAdvanceHandlers(final Map<String, BasicCrontabHandler> handlers) {
		pollAndAdvanceHandlers(ZonedDateTime.now(), handlers);
	}

	private synchronized void pollAndAdvanceHandlers(final ZonedDateTime now,
			final Map<String, BasicCrontabHandler> handlers) {
		for (BasicCrontabHandler handler : handlers.values())
			try {
				handler.eventuallyHandle(now);
			} catch (Exception e) {
				log.error("Uncaught exception in Crontab-Scheduler loop for handler [" + handler.name + "]", e);
				e.printStackTrace();
			}
	}
}
