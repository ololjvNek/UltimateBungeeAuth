package pl.jms.auth.utils;

public enum TimeUtil {
	
	    TICK(50, 50),
	    MILLISECOND(1, 1),
	    SECOND(1000, 1000),
	    MINUTE(60000, 60),
	    HOUR(3600000, 60),
	    DAY(86400000, 24),
	    WEEK(604800000, 7);

	    public static final int MPT = 50;
	    private final int time;
	    private final int timeMulti;

	    private TimeUtil(final int time, final int timeMulti) {
	        this.time = time;
	        this.timeMulti = timeMulti;
	    }

	    public int getMulti() {
	        return this.timeMulti;
	    }

	    public int getTime() {
	        return this.time;
	    }

	    public int getTick() {
	        return this.time / 50;
	    }

	    public int getTime(final int multi) {
	        return this.time * multi;
	    }

	    public int getTick(final int multi) {
	        return this.getTick() * multi;
	    }
	}