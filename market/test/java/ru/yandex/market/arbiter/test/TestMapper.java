package ru.yandex.market.arbiter.test;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ru.yandex.market.arbiter.api.client.dto.ArbiterConversationDto;
import ru.yandex.market.arbiter.api.client.dto.MessageDto;
import ru.yandex.market.arbiter.api.client.dto.MessageWithConversationDto;
import ru.yandex.market.arbiter.api.consumer.client.dto.RefundDto;


/**
 * @author moskovkin@yandex-team.ru
 * @since 25.05.2020
 */
@Mapper
public abstract class TestMapper {
    @Mapping(source = "message.id", target = "id")
    @Mapping(source = "message.creationTime", target = "creationTime")
    public abstract MessageWithConversationDto mapToMessageWithConversationDto(
            MessageDto message, ArbiterConversationDto conversation
    );

    public abstract RefundDto mapToClientRefundDto(ru.yandex.market.arbiter.api.server.dto.RefundDto source);
}
