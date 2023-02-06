package ru.yandex.market.tpl.tms.service.personal_data;

import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.api.model.user.partner.PartnerUserPersonalDataDto;
import ru.yandex.market.tpl.common.dsm.client.model.CourierPersonalDataDto;
import ru.yandex.market.tpl.common.dsm.client.model.VaccinationInfoDto;
import ru.yandex.market.tpl.core.service.user.personal.data.UserPersonalDataMapper;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class UserPersonalDataMapperTest extends TplTmsAbstractTest {
    private final UserPersonalDataMapper userPersonalDataMapper;

    @Test
    void mapFromCourierPersonalDataDtoToPartnerUserPersonalDataDto() {
        CourierPersonalDataDto courierPersonalDataDto = new CourierPersonalDataDto();
        VaccinationInfoDto firstVaccinationDate = new VaccinationInfoDto();
        VaccinationInfoDto secondVaccinationDate = new VaccinationInfoDto();
        VaccinationInfoDto thirdVaccinationDate = new VaccinationInfoDto();

        firstVaccinationDate.date(LocalDate.now());
        secondVaccinationDate.date(LocalDate.now().plusDays(1));
        thirdVaccinationDate.date(LocalDate.now().plusDays(2));

        courierPersonalDataDto.setPassportNumber("345373991");
        courierPersonalDataDto.setVaccinationLink("TEST_LINK");
        courierPersonalDataDto.setVaccinated(true);
        courierPersonalDataDto.setBirthday(LocalDate.now());
        courierPersonalDataDto.setVaccinationInfos(List.of(
                firstVaccinationDate,
                secondVaccinationDate,
                thirdVaccinationDate
        ));

        PartnerUserPersonalDataDto result = userPersonalDataMapper.map(courierPersonalDataDto);

        assertThat(result.getBirthdayDate()).isEqualTo(courierPersonalDataDto.getBirthday());
        assertThat(result.getPassport()).isEqualTo(courierPersonalDataDto.getPassportNumber());
        assertThat(result.getLink()).isEqualTo(courierPersonalDataDto.getVaccinationLink());
        assertThat(result.getHasVaccination()).isEqualTo(courierPersonalDataDto.getVaccinated());
        assertThat(result.getFirstVaccinationDate()).isEqualTo(LocalDate.now().plusDays(1));
        assertThat(result.getSecondVaccinationDate()).isEqualTo(LocalDate.now().plusDays(2));
    }
}
