package ru.yandex.calendar.test.generic;

import lombok.val;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;
import ru.yandex.calendar.boot.MicroCoreContextConfiguration;
import ru.yandex.calendar.logic.domain.PassportAuthDomainsHolder;
import ru.yandex.calendar.micro.MicroCoreContext;
import ru.yandex.mail.cerberus.ResourceId;
import ru.yandex.mail.cerberus.ResourceKey;
import ru.yandex.mail.cerberus.ResourceTypeName;
import ru.yandex.mail.cerberus.client.GrantClient;
import ru.yandex.mail.cerberus.client.UserClient;
import ru.yandex.mail.cerberus.client.dto.AllowedActions;

import java.util.Set;

import static java.util.Collections.emptySet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static ru.yandex.mail.micronaut.common.CerberusUtils.mapToList;

@Configuration
public class MicroCoreTestConfiguration {
    @Bean
    @Primary
    public MicroCoreContext microCoreContext(PassportAuthDomainsHolder domainsHolder) {
        val configuration = new MicroCoreContextConfiguration();
        val context = spy(configuration.microCore(domainsHolder));

        val grantClientMock = mock(GrantClient.class);
        when(grantClientMock.actions(any(), any(), any(), any()))
            .thenAnswer(invocation -> {
                val resourceType = (ResourceTypeName) invocation.getArgument(1);
                @SuppressWarnings("unchecked")
                val resourceIds = (Set<ResourceId>) invocation.getArgument(2);
                final var result = mapToList(resourceIds, id -> {
                    val key = new ResourceKey(id, resourceType);
                    return new AllowedActions.ResourceInfo(key, emptySet());
                });
                return Mono.just(new AllowedActions(result));
            });

        val userClientMock = mock(UserClient.class);
        when(userClientMock.addUsers(anyBoolean(), any()))
            .thenReturn(Mono.just(emptySet()));
        when(context.findBean(UserClient.class)).thenReturn(userClientMock);
        when(context.findBean(GrantClient.class)).thenReturn(grantClientMock);

        return context;
    }
}
