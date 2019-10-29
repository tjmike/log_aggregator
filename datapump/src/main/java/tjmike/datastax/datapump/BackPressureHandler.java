package tjmike.datastax.datapump;

public interface BackPressureHandler {
	void setDelaySeconds(int seconds);
}
