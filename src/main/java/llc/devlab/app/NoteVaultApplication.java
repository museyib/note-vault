package llc.devlab.app;

import llc.devlab.app.security.TokenProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(TokenProperties.class)
public class NoteVaultApplication {

    public static void main(String[] args) {
        SpringApplication.run(NoteVaultApplication.class, args);
    }

}
