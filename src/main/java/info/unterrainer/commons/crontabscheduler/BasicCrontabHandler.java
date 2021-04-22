package info.unterrainer.commons.crontabscheduler;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.Cron;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

import lombok.Data;

@Data
public abstract class BasicCrontabHandler {

	protected final String name;
	protected final Boolean enabled;
	protected final String data;

	protected final String cronDef;
	protected final CronParser parser;
	protected final CronDescriptor descriptor;

	protected Cron cron;
	protected String description;
	protected ExecutionTime executionTime;

	protected ZonedDateTime lastChecked;
	protected Long millisTillNextExecution;

	public BasicCrontabHandler(final String name, final Boolean enabled, final String data, final String cronDef,
			final CronParser parser, final CronDescriptor descriptor) {
		super();
		this.name = name;
		this.enabled = enabled;
		this.data = data;
		this.cronDef = cronDef;
		this.parser = parser;
		this.descriptor = descriptor;
		cron = parser.parse(cronDef);
		cron.validate();
		description = descriptor.describe(cron);
		executionTime = ExecutionTime.forCron(cron);
	}

	public long getMillisFromLastExecution(final ZonedDateTime now) {
		Duration d = executionTime.timeFromLastExecution(now).get();
		return d.toMillis();
	}

	public long getMillisTillNextExecution(final ZonedDateTime now) {
		Duration d = executionTime.timeToNextExecution(now).get();
		return d.toMillis();
	}

	public synchronized void eventuallyHandle(final ZonedDateTime now) {
		long next = getMillisTillNextExecution(now);

		// Clear disabled items.
		if (enabled == null || !enabled) {
			millisTillNextExecution = null;
			return;
		}

		// Initialize new items.
		if (millisTillNextExecution == null)
			millisTillNextExecution = next;

		long duration = ChronoUnit.MILLIS.between(Optional.ofNullable(lastChecked).orElse(now), now);
		lastChecked = now;
		millisTillNextExecution -= duration;
		if (name.equals("25_on"))
			System.out.println("millis: " + millisTillNextExecution);
		if (millisTillNextExecution <= 0) {
			millisTillNextExecution = next;
			handle(now);
		}
	}

	public abstract void handle(final ZonedDateTime started);
}
