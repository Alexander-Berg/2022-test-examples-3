package ru.yandex.market.pers.grade.admin.util;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import ru.yandex.market.pers.security.PersUser;

@Service
public class PersDebugUserDetailsService implements UserDetailsService {
    @Value("${debug.user.uid:67282295}")
    private long userId;
    @Value("${debug.user.login:spbtester}")
    private String login;

    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        return new PersUser(login, userId, Arrays.asList("ROLE_ADMINISTRATOR"));
    }
}
