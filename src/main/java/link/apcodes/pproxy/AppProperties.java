package link.apcodes.pproxy;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Data
@Component
@ConfigurationProperties(prefix = "password-proxy")
public class AppProperties {
    /**
     * passed to {@link WebClient.Builder#baseUrl(String)} (default: <code>http://localhost:9000</code>)
     */
    String url = "http://localhost:9000";

    /**
     * Properties to describe default admin credentials
     */
    @NestedConfigurationProperty
    Admin admin = new Admin();

    @Data
    public static class Admin {
        /**
         * Is the default admin user enabled (default no)
         */
        boolean enabled = false;

        /**
         * Username for default/admin account (default: <code>admin</code>)
         */
        String username = "admin";

        /**
         * Password for default/admin account (default: <code>admin</code>)
         */
        String password = "admin";
    }
}
