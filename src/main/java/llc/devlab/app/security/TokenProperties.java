package llc.devlab.app.security;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;

@PropertySource("classpath:application.yaml")
@ConfigurationProperties("app.jwt")
@Getter
public class TokenProperties {

    private final String secretKey;

    private final long accessTokenExpiration;

    private final long refreshTokenExpiration;

    public TokenProperties(String secretKey,
                           long accessTokenExpiration,
                           long refreshTokenExpiration) {
        this.secretKey = secretKey;
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }
}
