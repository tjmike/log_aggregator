package tjmike.datastax.logServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;


@SpringBootApplication
public class LogServer {


    private static Logger logger = LoggerFactory.getLogger(LogServer.class);

    public static void main(String[] args) {

        SpringApplication app = new SpringApplication(LogServer.class);
	    // Turn Spring banner off
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
    }


    /**
     * This just demonstrates a simple bean that implements CommandLineRunner. Since
     * it implements the interface it's automatically hooked up and called.
     * @param ctx
     * @return
     */
    @Bean()
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return (args) -> {
            logger.info("Log Server Starting:");
        };
//
    }


}

