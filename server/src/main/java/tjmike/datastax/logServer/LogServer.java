package tjmike.datastax.logServer;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LogServer {

    public static void main(String[] args) {

        SpringApplication app = new SpringApplication(LogServer.class);
	    // Turn Spring banner off
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
    }
}

