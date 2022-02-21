package link.apcodes.pproxy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.util.ArrayList;

@Slf4j
@Configuration
public class AppConfiguration {

    @Autowired
    AppProperties appProperties;

    @Bean
    MapReactiveUserDetailsService userDetailsService() {
        ArrayList<UserDetails> list = new ArrayList<>();

        log.trace("configuring the application with {}", appProperties);
        if (appProperties.getAdmin().isEnabled()) {
            list.add(User.withUsername(appProperties.getAdmin().getUsername())
                    .password("{noop}" + appProperties.getAdmin().getPassword())
                    .roles("ADMIN")
                    .build());
        }

        return new MapReactiveUserDetailsService(list);
    }

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        // @formatter:off
        return http
                .csrf().disable()
                .authorizeExchange().pathMatchers("/**").hasRole("ADMIN")
                // .and().authorizeExchange().pathMatchers("/actuator/**").hasRole("ADMIN")
                .and().httpBasic()
                .and().build();
        // @formatter:on
    }
}
