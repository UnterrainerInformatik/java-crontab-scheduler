package info.unterrainer.commons.crontabscheduler;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CrontabScheduler {

	protected ScheduledExecutorService executor;
	protected Map<String, BasicCrontabHandler> registeredHandlers = new HashMap<>();
	protected Map<String, BasicCrontabHandler> save = new HashMap<>();

	public void addHandler(final @NonNull BasicCrontabHandler handler, final String name) {
		synchronized (this) {
			registeredHandlers.put(name, handler);
		}
	}

	public void prepareUpdate() {
		synchronized (this) {
			Map<String, BasicCrontabHandler> temp = registeredHandlers;
			registeredHandlers = save;
			save = temp;
			registeredHandlers.clear();
		}
	}

	public void finishUpdate() {
		synchronized (this) {
			pollAndAdvanceHandlers(save);
			save.clear();
		}
	}

	@Builder
	public CrontabScheduler(final long period, final TimeUnit timeUnit, final Consumer<CrontabScheduler> setupHandler) {

		if (setupHandler != null)
			setupHandler.accept(this);

		executor = Executors.newSingleThreadScheduledExecutor();
		executor.scheduleAtFixedRate(() -> pollAndAdvanceHandlers(registeredHandlers), 0, period, timeUnit);
	}

	private void pollAndAdvanceHandlers(final Map<String, BasicCrontabHandler> handlers) {
		try {
			synchronized (this) {
				for (BasicCrontabHandler handler : handlers.values())
					if (handler.getEnabled() != null) {
						ZonedDateTime now = ZonedDateTime.now();
						if (handler.shouldRun(now))
							handler.handle(now);
					}
			}
		} catch (Exception e) {
			log.error("uncaught exception in Http-poller loop", e);
			e.printStackTrace();
		}
	}
}