package ru.yandex.market.pvz.core.test.factory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import javax.transaction.Transactional;

import lombok.Builder;
import lombok.Data;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.domain.vaccination.VaccinationEmployeeParams;
import ru.yandex.market.pvz.core.domain.vaccination.VaccinationPickupPointCommandService;
import ru.yandex.market.pvz.core.domain.vaccination.VaccinationPickupPointParams;

import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.OrganizationTestParams.DEFAULT_ORGANIZATION_NAME;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointLocationTestParams.DEFAULT_LOCALITY;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointLocationTestParams.DEFAULT_REGION;

@Transactional
public class TestVaccinationPickupPointFactory {

    @Autowired
    private VaccinationPickupPointCommandService vaccinationPickupPointCommandService;

    public VaccinationPickupPointParams create(VaccinationPickupPointTestParams params) {
        return vaccinationPickupPointCommandService.create(buildVaccinationPickupPoint(params));
    }

    public VaccinationPickupPointParams create() {
        return create(VaccinationPickupPointTestParams.builder().build());
    }

    private VaccinationPickupPointParams buildVaccinationPickupPoint(VaccinationPickupPointTestParams params) {
        return VaccinationPickupPointParams.builder()
                .id(params.getId())
                .partnerName(params.getPartnerName())
                .region(params.getRegion())
                .city(params.getCity())
                .address(params.getAddress())
                .amount(params.getAmount())
                .vaccinatedAmount(params.getVaccinatedAmount())
                .created(params.getCreated())
                .employees(StreamEx.of(params.getEmployees())
                        .map(e ->
                                VaccinationEmployeeParams.builder()
                                        .name(e.getName())
                                        .passport(e.getPassport())
                                        .birthday(e.getBirthday())
                                        .nationality(e.getNationality())
                                        .firstVaccinationDate(e.getFirstVaccinationDate())
                                        .secondVaccinationDate(e.getSecondVaccinationDate())
                                        .certificate(e.getCertificate())
                                        .signed(e.isSigned())
                                        .build())
                        .toList())
                .build();
    }

    @Data
    @Builder
    public static class VaccinationPickupPointTestParams {

        public static final String DEFAULT_PARTNER_NAME = DEFAULT_ORGANIZATION_NAME;
        public static final String DEFAULT_PVZ_REGION = DEFAULT_REGION;
        public static final String DEFAULT_CITY = DEFAULT_LOCALITY;
        public static final String DEFAULT_ADDRESS = "ул. Пушкина, дом 8";
        public static final short DEFAULT_AMOUNT = (short) 3;
        public static final short DEFAULT_VACCINATED_AMOUNT = (short) 1;
        public static final OffsetDateTime DEFAULT_CREATED = OffsetDateTime.of(LocalDateTime.of(2021, 7, 24, 14, 35,
                0), ZoneOffset.UTC);
        public static final List<VaccinationEmployeeTestParams> DEFAULT_EMPLOYEES =
                List.of(VaccinationEmployeeTestParams.builder().build());

        @Builder.Default
        private long id = RandomUtils.nextLong();

        @Builder.Default
        private String partnerName = DEFAULT_PARTNER_NAME;

        @Builder.Default
        private String region = DEFAULT_PVZ_REGION;

        @Builder.Default
        private String city = DEFAULT_CITY;

        @Builder.Default
        private String address = DEFAULT_CITY;

        @Builder.Default
        private short amount = DEFAULT_AMOUNT;

        @Builder.Default
        private short vaccinatedAmount = DEFAULT_VACCINATED_AMOUNT;

        @Builder.Default
        private OffsetDateTime created = DEFAULT_CREATED;

        @Builder.Default
        private List<VaccinationEmployeeTestParams> employees = DEFAULT_EMPLOYEES;
    }

    @Data
    @Builder
    public static class VaccinationEmployeeTestParams {

        public static final String DEFAULT_EMPLOYEE_NAME = "Петров Иван Сидорович";
        public static final String DEFAULT_EMPLOYEE_PASSPORT = "1450 563477";
        public static final String DEFAULT_NATIONALITY = "Русский";
        public static final LocalDate DEFAULT_BIRTHDAY = LocalDate.of(1994, 8, 15);
        public static final LocalDate DEFAULT_FIRST_VACCINATION_DATE = LocalDate.of(2021, 6, 5);
        public static final LocalDate DEFAULT_SECOND_VACCINATION_DATE = LocalDate.of(2021, 6, 27);
        public static final String DEFAULT_CERTIFICATE_LINK = "https://www.gosuslugi.ru/covid-cert/verify/" +
                "9770000018236415?lang=ru&ck=bf99cac3e4d2cedaga4852d46ff8f59b";
        public static final boolean DEFAULT_SIGNED = true;

        @Builder.Default
        private String name = DEFAULT_EMPLOYEE_NAME;

        @Builder.Default
        private String passport = DEFAULT_EMPLOYEE_PASSPORT;

        @Builder.Default
        private String nationality = DEFAULT_NATIONALITY;

        @Builder.Default
        private LocalDate birthday = DEFAULT_BIRTHDAY;

        @Builder.Default
        private LocalDate firstVaccinationDate = DEFAULT_FIRST_VACCINATION_DATE;

        @Builder.Default
        private LocalDate secondVaccinationDate = DEFAULT_SECOND_VACCINATION_DATE;

        @Builder.Default
        private String certificate = DEFAULT_CERTIFICATE_LINK;

        @Builder.Default
        private boolean signed = DEFAULT_SIGNED;
    }
}
