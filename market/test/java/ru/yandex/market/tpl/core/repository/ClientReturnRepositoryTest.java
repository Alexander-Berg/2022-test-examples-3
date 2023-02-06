package ru.yandex.market.tpl.core.repository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnStatus;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.pickup.generator.PickupPointGenerator;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ClientReturnRepositoryTest extends TplAbstractTest {
    private final ClientReturnRepository clientReturnRepository;
    private final ClientReturnGenerator generator;
    private final TransactionTemplate transactionTemplate;
    private final PickupPointRepository pickupPointRepository;


    @Test
    public void findByStatusAndPickupPointId() {
        transactionTemplate.execute(status -> {
            ClientReturn clientReturn1 = generator.generate();
            PickupPoint pickupPoint1 = PickupPointGenerator.generatePickupPoint(3463476346L);
            clientReturn1.setPickupPoint(pickupPoint1);
            clientReturn1.setStatus(ClientReturnStatus.READY_FOR_RECEIVED);
            pickupPointRepository.save(pickupPoint1);
            clientReturnRepository.save(clientReturn1);

            ClientReturn clientReturn12 = generator.generate();
            clientReturn12.setPickupPoint(pickupPoint1);
            clientReturn12.setStatus(ClientReturnStatus.ASSIGNED_TO_COURIER);
            clientReturnRepository.save(clientReturn12);

            ClientReturn clientReturn2 = generator.generate();
            PickupPoint pickupPoint2 = PickupPointGenerator.generatePickupPoint(3467456344L);
            clientReturn2.setPickupPoint(pickupPoint2);
            clientReturn2.setStatus(ClientReturnStatus.READY_FOR_RECEIVED);
            pickupPointRepository.save(pickupPoint2);
            clientReturnRepository.save(clientReturn2);

            ClientReturn clientReturn3 = generator.generate();
            clientReturn3.setPickupPoint(pickupPoint1);
            clientReturn3.setStatus(ClientReturnStatus.RECEIVED);
            clientReturnRepository.save(clientReturn3);

            List<ClientReturn> result =
                    clientReturnRepository.findByPickupPointIdAndStatusIn(pickupPoint1.getId(),
                            Set.of(ClientReturnStatus.READY_FOR_RECEIVED, ClientReturnStatus.ASSIGNED_TO_COURIER));
            Assertions.assertThat(result.size()).isEqualTo(2);
            Set<Long> idsResult = result.stream().map(ClientReturn::getId).collect(Collectors.toSet());
            Assertions.assertThat(idsResult).contains(clientReturn1.getId(), clientReturn12.getId());
            return status;
        });
    }
}
