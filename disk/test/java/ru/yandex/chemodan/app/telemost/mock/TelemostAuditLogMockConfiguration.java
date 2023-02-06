package ru.yandex.chemodan.app.telemost.mock;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.chemodan.app.telemost.config.TelemostRoomContextConfiguration;
import ru.yandex.chemodan.app.telemost.logging.TelemostAuditLog;
import ru.yandex.chemodan.app.telemost.services.model.Conference;
import ru.yandex.misc.test.Assert;

@Configuration
@Import({
        TelemostRoomContextConfiguration.class
})
public class TelemostAuditLogMockConfiguration {
    TelemostAuditLog spied;

    @Bean
    @Primary
    public TelemostAuditLog telemostAuditLog(){
        this.spied = Mockito.spy(new TelemostAuditLog());
        return this.spied;
    }

    public void reset() {
        Mockito.reset(spied);
    }

    public void assertConferenceLogged(String roomId, String shortUrlId)  {
        ArgumentCaptor<Conference> confCaptor =  ArgumentCaptor.forClass(Conference.class);
        Mockito.verify(spied, Mockito.times(1)).logConferenceCreated( confCaptor.capture());
        Conference conference = confCaptor.getValue();
        Assert.equals(roomId, conference.getRoomId());
        Assert.equals(shortUrlId, conference.getConferenceDto().getShortUrlId());
    }

}
