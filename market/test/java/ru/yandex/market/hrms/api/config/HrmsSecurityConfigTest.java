package ru.yandex.market.hrms.api.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedGrantedAuthoritiesUserDetailsService;

import ru.yandex.market.hrms.api.security.LmsPreAuthenticatedDetailsSource;
import ru.yandex.market.hrms.api.security.LmsPreAuthenticatedProcessingFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class HrmsSecurityConfigTest extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .addFilter(filter())
                .csrf().disable()
                .logout().disable()
                .headers().disable();
    }

    @Bean
    public LmsPreAuthenticatedProcessingFilter filter() {
        LmsPreAuthenticatedProcessingFilter filter = new LmsPreAuthenticatedProcessingFilter();
        filter.setAuthenticationDetailsSource(authenticatedDetailsSource());
        filter.setAuthenticationManager(new ProviderManager(authenticationProvider()));
        return filter;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        PreAuthenticatedAuthenticationProvider provider = new PreAuthenticatedAuthenticationProvider();
        provider.setPreAuthenticatedUserDetailsService(authoritiesUserDetailsService());
        return provider;
    }

    @Bean
    public LmsPreAuthenticatedDetailsSource authenticatedDetailsSource() {
        return new LmsPreAuthenticatedDetailsSource();
    }

    @Bean
    public PreAuthenticatedGrantedAuthoritiesUserDetailsService authoritiesUserDetailsService() {
        return new PreAuthenticatedGrantedAuthoritiesUserDetailsService();
    }

}
