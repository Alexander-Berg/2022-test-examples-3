package ru.yandex.market.logistics.management.controller.admin.partnerRoute;

import java.util.List;

import javax.annotation.Nonnull;
import javax.persistence.EntityManager;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.management.service.client.PartnerRelationService;
import ru.yandex.market.logistics.management.service.export.dynamic.validation.DynamicValidationAspect;
import ru.yandex.market.logistics.management.service.export.dynamic.validation.DynamicValidationService;
import ru.yandex.market.logistics.management.service.export.dynamic.validation.ValidationRule;
import ru.yandex.market.logistics.management.service.export.dynamic.validation.rule.PartnerRelationValidationRule;
import ru.yandex.market.logistics.management.service.export.dynamic.validation.rule.PartnerRouteExistsForPartnerRelationRule;
import ru.yandex.market.logistics.management.util.CleanDatabase;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;
import ru.yandex.market.logistics.management.util.tskv.ValidationExceptionLogger;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.controller.admin.partnerRoute.Helper.READ_WRITE;
import static ru.yandex.market.logistics.management.controller.admin.partnerRoute.Helper.uploadUpsert;

@CleanDatabase
@DisplayName("Загрузка файла и обновление расписания магистралей партнеров")
@DatabaseSetup("/data/controller/admin/partnerRoute/before/prepare_data.xml")
@Import(AdminPartnerRouteControllerUpsertTest.DynamicValidationConfiguration.class)
public class AdminPartnerRouteControllerUpsertTest extends AbstractContextualTest {

    @Test
    @DisplayName("Успешно добавлены все магистрали")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_WRITE)
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerRoute/after/upload_add.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void uploadCsvSuccess() throws Exception {
        mockMvc.perform(uploadUpsert().file(Helper.file(TestUtil.pathToJson(
                "data/controller/admin/partnerRoute/request/upload_add_success.csv"
            ))))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Успешно заменить все")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_WRITE)
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerRoute/after/upsert.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void uploadCsvReplaceAllSuccess() throws Exception {
        doUpload("data/controller/admin/partnerRoute/request/upsert_success.csv")
            .andExpect(status().isOk());
    }

    @Nonnull
    private ResultActions doUpload(String pathToFile) throws Exception {
        return mockMvc.perform(uploadUpsert().file(Helper.file(TestUtil.pathToJson(pathToFile))));
    }

    @Configuration
    public static class DynamicValidationConfiguration {
        @Autowired
        private FeatureProperties featureProperties;

        @Bean
        public PartnerRelationValidationRule firstValidationRule() {
            return new PartnerRouteExistsForPartnerRelationRule(featureProperties);
        }

        @Bean
        public DynamicValidationAspect dynamicValidationAspect(PartnerRelationService partnerRelationService) {
            List<ValidationRule> rules = List.of(firstValidationRule());
            DynamicValidationService validationService = new DynamicValidationService(rules);

            return new DynamicValidationAspect(
                Mockito.mock(EntityManager.class),
                partnerRelationService,
                validationService,
                Mockito.mock(ValidationExceptionLogger.class),
                true
            );
        }
    }
}
