package ru.yandex.chemodan.app.telemost.services;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.telemost.TelemostBaseContextTest;
import ru.yandex.chemodan.app.telemost.common.ShardProperties;
import ru.yandex.chemodan.app.telemost.common.ShardPropertiesZkRegistry;
import ru.yandex.chemodan.app.telemost.exceptions.TelemostOverloadException;
import ru.yandex.chemodan.app.telemost.services.model.Conference;
import ru.yandex.chemodan.app.telemost.services.model.User;
import ru.yandex.misc.test.Assert;

@ContextConfiguration(classes = {
        OverloadServiceTest.Context.class,
})
public class OverloadServiceTest extends TelemostBaseContextTest {
    @Autowired
    private ShardPropertiesZkRegistry shardPropertiesZkRegistry;
    @Autowired
    private OverloadService overloadService;

    private Conference conference;
    private User user1;
    private User user2;

    @Test
    public void testNotCheck() {
        Mockito.when(shardPropertiesZkRegistry.get()).thenReturn(
                ShardProperties.builder().currentParticipants(10000).maxParticipants(5000).checkOverload(false).build()
        );

        overloadService.checkOverload();
    }

    @Test
    public void testCorrect() {
        Mockito.when(shardPropertiesZkRegistry.get()).thenReturn(
                ShardProperties.builder().checkOverload(true).currentParticipants(1000).maxParticipants(5000).build());

        overloadService.checkOverload();
    }

    @Test
    public void testOverload() {
        Mockito.when(shardPropertiesZkRegistry.get()).thenReturn(
                ShardProperties.builder().checkOverload(true).currentParticipants(10000).maxParticipants(5000).build());

        Assert.assertThrows(() -> overloadService.checkOverload(), TelemostOverloadException.class);
    }

    @Test
    public void testOverloadUidNotJoin() {
        Mockito.when(shardPropertiesZkRegistry.get()).thenReturn(
                ShardProperties.builder().checkOverload(true).currentParticipants(10000).maxParticipants(5000).build());
        Assert.assertThrows(() -> overloadService.checkOverload(conference.getDbId(), Option.of(user2.getUid())), TelemostOverloadException.class);
    }

    @Test
    public void testOverloadAnonym() {
        Mockito.when(shardPropertiesZkRegistry.get()).thenReturn(
                ShardProperties.builder().checkOverload(true).currentParticipants(10000).maxParticipants(5000).build());
        Assert.assertThrows(() -> overloadService.checkOverload(conference.getDbId(), Option.empty()), TelemostOverloadException.class);
    }

    @Test
    public void testOverloadUidAlreadyJoin() {
        Mockito.when(shardPropertiesZkRegistry.get()).thenReturn(
                ShardProperties.builder().checkOverload(true).currentParticipants(10000).maxParticipants(5000).build());
        conferenceService.joinConference(Option.of(user2), conference.getUri(), Option.empty(), Option.empty());
        overloadService.checkOverload(conference.getDbId(), Option.of(user2.getUid()));
    }

    @Test
    public void testOverloadByUri() {
        Mockito.when(shardPropertiesZkRegistry.get()).thenReturn(
                ShardProperties.builder().checkOverload(true).currentParticipants(10000).maxParticipants(5000).build());
        Assert.assertThrows(() -> overloadService.checkOverload(conference.getUri(), Option.of(user2.getUid())), TelemostOverloadException.class);
    }

    @Before
    public void setUp() {
        user1 = createTestUserForUid(123321);
        userService.addUserIfNotExists(user1.getUid());
        user2 = createTestUserForUid(123322);
        userService.addUserIfNotExists(user2.getUid());
        conference = generateConference(user1);
    }

    @Configuration
    public static class Context {
        @Bean
        @Primary
        ShardPropertiesZkRegistry shardPropertiesZkRegistry() {
            return Mockito.mock(ShardPropertiesZkRegistry.class);
        }
    }
}
