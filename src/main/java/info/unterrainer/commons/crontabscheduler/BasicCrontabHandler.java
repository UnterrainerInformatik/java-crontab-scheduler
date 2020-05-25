package info.unterrainer.commons.crontabscheduler;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.Cron;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public abstract class BasicCrontabHandler {

	protected String name;
	protected Boolean enabled;
	protected String data;

	protected String cronDef;
	protected CronParser parser;
	protected CronDescriptor descriptor;

	protected Cron cron;
	protected String description;
	protected ExecutionTime executionTime;

	protected ZonedDateTime lastChecked;
	protected long millisTillNextExecution;

	public void initWith(final String cronDef) {
		this.cronDef = cronDef;
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
