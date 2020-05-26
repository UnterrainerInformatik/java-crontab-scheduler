package info.unterrainer.commons.crontabscheduler;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

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
	protected long millisTillNextExecution;

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
		millisTillNextExecution = getMillisTillNextExecution();
	}

	public long getMillisFromLastExecution() {
		ZonedDateTime now = ZonedDateTime.now();
		return executionTime.timeFromLastExecution(now).get().toMillis();
	}

	public long getMillisTillNextExecution() {
		ZonedDateTime now = ZonedDateTime.now();
		return executionTime.timeToNextExecution(now).get().toMillis();
	}

	public boolean shouldRun(final ZonedDateTime now) {
		if (lastChecked == null)
			lastChecked = now;
		long duration = ChronoUnit.MILLIS.between(lastChecked, now);
		lastChecked = now;
		millisTillNextExecution -= duration;
		if (millisTillNextExecution < 0) {
			millisTillNextExecution = executionTime.timeToNextExecution(now).get().toMillis();
			return enabled;
		}
		return false;
	}

	public abstract void handle(final ZonedDateTime started);
}
