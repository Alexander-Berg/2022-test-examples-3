package ru.yandex.market.logistics.pechkin.app.notificator;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.market.logistic.pechkin.core.dto.MessageDto;
import ru.yandex.market.logistics.pechkin.app.component.ChannelPropertyReader;
import ru.yandex.market.logistics.pechkin.app.exception.ParsingException;

@Component
@AllArgsConstructor
public class PrintNotificator implements Notificator {

    private final ObjectMapper objectMapper;
    private final ChannelPropertyReader channelPropertyReader;

    @Override
    public boolean sendNotification(MessageDto messageDto) {
        String channel = messageDto.getChannel();
        List<String> groupIdList = channelPropertyReader.getChannelMap().get(channel);
        groupIdList.forEach(groupId -> {
            try {
                System.out.println(objectMapper.writeValueAsString(messageDto) + " group: " + groupId);
            } catch (JsonProcessingException e) {
                throw new ParsingException(e);
            }
        });
        return true;
    }
}
