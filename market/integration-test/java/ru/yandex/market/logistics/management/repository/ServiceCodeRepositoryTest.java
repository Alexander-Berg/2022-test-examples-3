package ru.yandex.market.logistics.management.repository;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.ServiceCode;
import ru.yandex.market.logistics.management.domain.entity.type.ServiceCodeName;
import ru.yandex.market.logistics.management.entity.type.ServiceType;
import ru.yandex.market.logistics.management.util.CleanDatabase;

@CleanDatabase
@SuppressWarnings("checkstyle:MagicNumber")
class ServiceCodeRepositoryTest extends AbstractContextualTest {

    private static final ServiceCode SERVICE_CODE_1 = new ServiceCode()
        .setName("Вознаграждение за перечисление денежных средств")
        .setCode(ServiceCodeName.CASH_SERVICE)
        .setType(ServiceType.INTERNAL)
        .setOptional(false);

    private static final ServiceCode SERVICE_CODE_2 = new ServiceCode()
        .setName("Ожидание курьера")
        .setCode(ServiceCodeName.WAIT_20)
        .setType(ServiceType.INTERNAL)
        .setOptional(false);

    @Autowired
    private ServiceCodeRepository serviceCodeRepository;

    @Test
    void saveAndGet() {
        serviceCodeRepository.saveAll(Arrays.asList(SERVICE_CODE_1, SERVICE_CODE_2));

        List<ServiceCode> serviceCodes = serviceCodeRepository.findAll();

        softly.assertThat(serviceCodes)
            .as("Services should be loaded")
            .hasSize(2);

        softly.assertThat(serviceCodes)
            .containsExactlyInAnyOrder(SERVICE_CODE_1, SERVICE_CODE_2);
    }
}
