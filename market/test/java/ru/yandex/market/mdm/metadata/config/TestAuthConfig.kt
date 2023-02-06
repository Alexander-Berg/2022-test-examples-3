package ru.yandex.market.mdm.metadata.config

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter

@EnableWebSecurity
@Import(MdmMetadataRepositoryConfig::class)
@Configuration
open class TestAuthConfig : WebSecurityConfigurerAdapter() {
    override fun configure(http: HttpSecurity) {
        // Turn off Spring Security for tests... for now, as we don't use it
        http.authorizeRequests().anyRequest().permitAll()
        http.csrf().disable()
    }
}
