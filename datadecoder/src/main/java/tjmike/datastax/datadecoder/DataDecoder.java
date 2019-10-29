/*
 */

package tjmike.datastax.datadecoder;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;



@SpringBootApplication
@EnableAsync
public class DataDecoder { // implements CommandLineRunner {


    public static void main(String[] args) {

        SpringApplication app = new SpringApplication(DataDecoder.class);
	// Turn Spring banner off
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);

    }
}
