package ru.yandex.market.pers.grade.admin;

import javax.servlet.http.HttpServletRequest;

import org.mockito.Matchers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import ru.yandex.common.framework.core.SecFilter;
import ru.yandex.common.framework.user.UserInfoField;
import ru.yandex.common.framework.user.UserInfoService;
import ru.yandex.common.framework.user.blackbox.BlackBoxUserInfo;
import ru.yandex.market.pers.grade.admin.config.PersGradeAdminCoreConfig;
import ru.yandex.market.pers.security.RequestUserInfoProvider;
import ru.yandex.market.pers.test.common.PersTestMocksHolder;
import ru.yandex.market.security.SecManager;
import ru.yandex.market.security.core.DefaultSecFilter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 26.11.2021
 */
@ComponentScan(basePackageClasses = {PersGradeAdminCoreConfig.class})
@ComponentScan( // all @Services in project, but not configurations
    basePackageClasses = {Launcher.class},
    excludeFilters = @ComponentScan.Filter(Configuration.class)
)
@Configuration
public class PersGradeAdminMockConfig {

    @Bean
    public SecFilter secFilter() {
        DefaultSecFilter result = new DefaultSecFilter();
        result.setSecManager(secManger());
        return result;
    }

    @Bean
    public SecManager secManger() {
        return PersTestMocksHolder.registerMock(SecManager.class, result -> {
            when(result.canDo(anyString(), any())).thenReturn(true);
            when(result.hasAuthority(anyString(), anyString(), any())).thenReturn(true);
        });
    }

    @Bean
    public RequestUserInfoProvider requestUserInfoProvider() {
        return new RequestUserInfoProvider() {
            @Override
            public Long getUid(HttpServletRequest request) {
                return 111L;
            }

            @Override
            public String getYandexUid(HttpServletRequest request) {
                return "stub_yandex_uid";
            }
        };
    }

    public static void initBlackBoxUserServiceMock(UserInfoService result) {
        when(result.getUserInfo(anyLong()))
            .thenAnswer(invocation -> {
                Long uid = (Long) invocation.getArguments()[0];
                BlackBoxUserInfo result1 = new BlackBoxUserInfo(uid);
                result1.addField(UserInfoField.LOGIN, "");
                return result1;
            });

        when(result.getUserInfo(anyString()))
            .thenAnswer(invocation -> {
                String login = (String) invocation.getArguments()[0];
                BlackBoxUserInfo result1 = new BlackBoxUserInfo(-1);
                result1.addField(UserInfoField.LOGIN, login);
                return result1;
            });

        when(result.getUserInfo(anyLong(), Matchers.<UserInfoField>anyVararg()))
            .then(invocation -> {
                Object[] args = invocation.getArguments();
                Long uid = (Long) args[0];
                BlackBoxUserInfo result1 = new BlackBoxUserInfo(uid);
                if (args.length > 1) {
                    for (int i = 1; i < args.length; i++) {
                        result1.addField((UserInfoField) args[i], "");
                    }
                }
                result1.addField(UserInfoField.LOGIN, "");
                return result1;
            });

        when(result.getUserInfo(anyString(), Matchers.<UserInfoField>anyVararg()))
            .then(invocation -> {
                Object[] args = invocation.getArguments();
                String login = (String) args[0];
                BlackBoxUserInfo result1 = new BlackBoxUserInfo(-1l);
                if (args.length > 1) {
                    for (int i = 1; i < args.length; i++) {
                        result1.addField((UserInfoField) args[i], "");
                    }
                }
                result1.addField(UserInfoField.LOGIN, login);
                return result1;
            });
    }

}
