package ru.yandex.market.tpl.core.service.lms;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import ru.yandex.market.logistics.front.library.dto.detail.DetailData;
import ru.yandex.market.logistics.front.library.dto.grid.GridData;
import ru.yandex.market.logistics.front.library.dto.grid.GridItem;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.lms.configuration.LmsConfigurationCreateDto;
import ru.yandex.market.tpl.core.domain.lms.configuration.LmsConfigurationDeleteDto;
import ru.yandex.market.tpl.core.domain.lms.configuration.LmsConfigurationFilterDto;
import ru.yandex.market.tpl.core.domain.lms.configuration.LmsConfigurationUpdateDto;
import ru.yandex.market.tpl.core.service.lms.configuration.LmsConfigurationServiceFacade;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LmsConfigurationServiceFacadeTest {

    private final LmsConfigurationServiceFacade lmsConfigurationServiceFacade;

    @Test
    public void getConfigurations() {

        LmsConfigurationCreateDto lmsConfigurationCreateDto = LmsConfigurationCreateDto.builder().key(
                ConfigurationProperties.COMPANY_PERMISSION_ENABLED).value("TRUE").build();
        LmsConfigurationCreateDto lmsConfigurationCreateDto1 =
                LmsConfigurationCreateDto.builder().key(ConfigurationProperties.DROP_FAR_ORDERS_DELIVERY_SERVICES).value("9").build();

        long idConfiguration = lmsConfigurationServiceFacade.createConfiguration(lmsConfigurationCreateDto);
        long id1Configuration = lmsConfigurationServiceFacade.createConfiguration(lmsConfigurationCreateDto1);

        Pageable pageable = Mockito.mock(Pageable.class);
        Mockito.when(pageable.getPageSize()).thenReturn(10);
        Mockito.when(pageable.getOffset()).thenReturn(0L);

        GridData gridData = lmsConfigurationServiceFacade.getConfigurations(pageable, null);
        assertThat(gridData.getItems()).extracting(GridItem::getId).contains(idConfiguration,
                id1Configuration);

        GridData gridDataFilter = lmsConfigurationServiceFacade.getConfigurations(pageable,
                LmsConfigurationFilterDto.builder().key("COMPANY").build());
        assertThat(gridDataFilter.getItems()).extracting(GridItem::getId).containsExactly(idConfiguration);

    }

    @Test
    public void createConfiguration() {
        LmsConfigurationCreateDto lmsConfigurationCreateDto =
                LmsConfigurationCreateDto.builder().key(ConfigurationProperties.CREATE_MBI_CABINET_AT_CREATE_COMPANY).value(
                        "555").build();
        long idConfiguration = lmsConfigurationServiceFacade.createConfiguration(lmsConfigurationCreateDto);
        DetailData detailData = lmsConfigurationServiceFacade.getConfiguration(idConfiguration);
        assertThat(detailData.getItem().getId()).isEqualTo(idConfiguration);
    }

    @Test
    public void updateConfiguration() {
        LmsConfigurationCreateDto lmsConfigurationCreateDto =
                LmsConfigurationCreateDto.builder().key(ConfigurationProperties.CREATE_MBI_CABINET_AT_CREATE_COMPANY).value(
                        "555").build();
        long idConfiguration = lmsConfigurationServiceFacade.createConfiguration(lmsConfigurationCreateDto);
        DetailData detailData = lmsConfigurationServiceFacade.getConfiguration(idConfiguration);

        assertThat(detailData.getItem().getId()).isEqualTo(idConfiguration);

        LmsConfigurationUpdateDto lmsConfigurationUpdateDto =
                LmsConfigurationUpdateDto.builder().id(idConfiguration).key(lmsConfigurationCreateDto.getKey())
                        .value("6666").build();
        DetailData detailDataUpdate = lmsConfigurationServiceFacade.updateConfiguration(lmsConfigurationUpdateDto);

        assertThat(detailData.getItem().getId()).isEqualTo(detailDataUpdate.getItem().getId());
        assertThat(detailData.getItem().getValues().get("key"))
                .isEqualTo(detailDataUpdate.getItem().getValues().get("key"));
        assertThat(detailData.getItem().getValues().get("value"))
                .isNotEqualTo(detailDataUpdate.getItem().getValues().get("value"));

    }

    @Test
    public void deleteConfigurations() {
        LmsConfigurationCreateDto lmsConfigurationCreateDto =
                LmsConfigurationCreateDto.builder().key(ConfigurationProperties.CREATE_MBI_CABINET_AT_CREATE_COMPANY).value(
                        "555").build();
        long idConfiguration = lmsConfigurationServiceFacade.createConfiguration(lmsConfigurationCreateDto);

        LmsConfigurationCreateDto lmsConfigurationCreateDto1 =
                LmsConfigurationCreateDto.builder().key(ConfigurationProperties.IS_FLASH_MESSAGE_AVAILABLE).value(
                        "true").build();
        long id1Configuration = lmsConfigurationServiceFacade.createConfiguration(lmsConfigurationCreateDto1);

        Pageable pageable = Mockito.mock(Pageable.class);
        Mockito.when(pageable.getPageSize()).thenReturn(3);
        Mockito.when(pageable.getOffset()).thenReturn(0L);

        lmsConfigurationServiceFacade.deleteConfigurations(LmsConfigurationDeleteDto.builder().ids(List.of(idConfiguration, id1Configuration)).build());
        GridData gridData = lmsConfigurationServiceFacade.getConfigurations(pageable, null);
        assertThat(gridData.getItems()).extracting(GridItem::getId).doesNotContain(idConfiguration, id1Configuration);

    }


}
