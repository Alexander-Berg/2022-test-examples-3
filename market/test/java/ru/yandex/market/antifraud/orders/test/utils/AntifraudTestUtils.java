package ru.yandex.market.antifraud.orders.test.utils;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import lombok.experimental.UtilityClass;
import org.hamcrest.Matcher;
import org.junit.Assert;
import org.mockito.ArgumentMatchers;

import ru.yandex.market.antifraud.orders.entity.AntifraudAction;
import ru.yandex.market.antifraud.orders.model.OrderDetectorResult;
import ru.yandex.market.antifraud.orders.service.RoleService;
import ru.yandex.market.antifraud.orders.storage.entity.roles.BuyerRole;
import ru.yandex.market.antifraud.orders.storage.entity.rules.BaseDetectorConfiguration;
import ru.yandex.market.antifraud.orders.storage.entity.rules.DetectorConfiguration;
import ru.yandex.yt.ytclient.proxy.SelectRowsRequest;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author dzvyagin
 */
@UtilityClass
public class AntifraudTestUtils {

    public static final String REQUEST_ID = "1/1";

    public static RoleService roleServiceSpy(DetectorConfiguration defaultConfiguration){
        RoleService roleService = spy(new RoleService(null, Collections.emptyList()));
        doReturn(Optional.empty()).when(roleService).getRoleByUid(anyString());
        Map<String, DetectorConfiguration> confMap = mock(Map.class);
        when(confMap.get(anyString())).thenReturn(defaultConfiguration);
        BuyerRole role = BuyerRole.builder().detectorConfigurations(confMap).build();
        doReturn(role).when(roleService).getDefaultRole();
        return roleService;
    }

    public static RoleService roleServiceSpy() {
        return roleServiceSpy(new BaseDetectorConfiguration(true));
    }

    public SelectRowsRequest ytQuery(String query) {
        return ytQueryThat(equalTo(query));
    }

    public SelectRowsRequest ytQueryThat(Matcher<String> queryMatcher) {
        return ArgumentMatchers.argThat(r -> {
            Assert.assertThat(r, hasProperty("query", queryMatcher));
            return true;
        });
    }

    public OrderDetectorResult okResult(String uniqName, String reason) {
        return OrderDetectorResult.builder()
                .ruleName(uniqName)
                .actions(Collections.singleton(AntifraudAction.NO_ACTION))
                .reason(reason)
                .answerText("OK")
                .build();
    }
}
