package ru.yandex.market.logistics.pechkin.app.telegram.processors;

import ru.yandex.market.logistic.pechkin.core.dto.MessageDto;

public class TestDebugProcessor implements Processor {

    private static final String TEMPLATE = "%s\n----------\nTesting | Sender : %s | Channel: %s";

    @Override
    public void process(MessageDto messageDto) {
        messageDto.setMessage(
            String.format(
                TEMPLATE,
                messageDto.getMessage(),
                messageDto.getSender(),
                messageDto.getChannel()
            )
        );
    }
}
