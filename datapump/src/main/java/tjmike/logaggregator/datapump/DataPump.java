/*
 *
 */

package tjmike.logaggregator.datapump;


import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;



@SpringBootApplication
@EnableAsync
@EnableScheduling
public class DataPump {

    public static void main(String[] args)  {
        SpringApplication app = new SpringApplication(DataPump.class);
	// Turn Spring banner off
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);

    }

}
