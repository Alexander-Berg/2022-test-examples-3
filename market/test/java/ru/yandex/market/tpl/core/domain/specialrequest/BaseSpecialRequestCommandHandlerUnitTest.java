package ru.yandex.market.tpl.core.domain.specialrequest;

import ru.yandex.market.tpl.api.model.specialrequest.SpecialRequestType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author sekulebyakin
 */
public abstract class BaseSpecialRequestCommandHandlerUnitTest {

    protected SpecialRequest createTestSpecialRequest() {
        var specialRequest = new SpecialRequest();
        var createCommand = SpecialRequestCommand.CreateSpecialRequest.builder()
                .specialRequestType(SpecialRequestType.LOCKER_INVENTORY)
                .requestSource("SOME_TICKET")
                .externalId("1")
                .deliveryServiceId(1L)
                .build();
        specialRequest.init(createCommand);
        assertThat(specialRequest.getStatus()).isEqualTo(SpecialRequestStatus.CREATED);
        return specialRequest;
    }

}
