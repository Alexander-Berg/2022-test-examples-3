package ru.yandex.direct.core.entity.pricepackage.service.validation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackageCampaignOptions;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsCustom;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsFixed;
import ru.yandex.direct.core.entity.pricepackage.model.ViewType;
import ru.yandex.direct.core.entity.pricepackage.service.PricePackageAddOperationFactory;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectInfo;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.DEFAULT_GEO;
import static ru.yandex.direct.core.testing.data.TestPricePackages.DEFAULT_GEO_TYPE;
import static ru.yandex.direct.core.testing.data.TestPricePackages.DEFAULT_RETARGETING_CONDITION;
import static ru.yandex.direct.core.testing.data.TestPricePackages.allowedPricePackageClient;
import static ru.yandex.direct.operation.Applicability.PARTIAL;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.isEmptyCollection;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class PricePackageAddValidationTest {
    @Autowired
    private PricePackageAddOperationFactory addOperationFactory;

    @Autowired
    private Steps steps;

    @Test
    public void clientIdsNotEmpty() {
        var client = steps.clientSteps().createClient(defaultClient().withWorkCurrency(CurrencyCode.RUB));
        var pricePackage = clientPricePackage()
                .withClients(List.of(allowedPricePackageClient(client)));

        var result = validate(pricePackage);

        assertDefects(result, validationError(path(field(PricePackage.CLIENTS)), isEmptyCollection()));
    }

    private PricePackage clientPricePackage() {
        return new PricePackage()
                .withTitle("Title_1")
                .withTrackerUrl("http://ya.ru")
                .withPrice(BigDecimal.valueOf(2999))
                .withCurrency(CurrencyCode.RUB)
                .withOrderVolumeMin(1L)
                .withOrderVolumeMax(1L)
                .withTargetingsFixed(new TargetingsFixed()
                        .withGeo(DEFAULT_GEO)
                        .withGeoType(DEFAULT_GEO_TYPE)
                        .withViewTypes(List.of(ViewType.DESKTOP, ViewType.MOBILE))
                        .withAllowExpandedDesktopCreative(true))
                .withTargetingsCustom(new TargetingsCustom()
                        .withRetargetingCondition(DEFAULT_RETARGETING_CONDITION))
                .withDateStart(LocalDate.of(2020, 1, 1))
                .withDateEnd(LocalDate.of(2020, 1, 1))
                .withIsPublic(false)
                .withCampaignOptions(new PricePackageCampaignOptions())
                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_YNDX_FRONTPAGE))
                .withClients(emptyList());
    }

    private void assertDefects(Optional<MassResult<Long>> result, Matcher<DefectInfo<Defect>> defectMatcher) {
        assertThat(result).isNotEmpty();
        assertDefects(result.get().get(0), defectMatcher);
    }

    private void assertDefects(Result<Long> result, Matcher<DefectInfo<Defect>> defectMatcher) {
        assertThat(result.getValidationResult()).is(matchedBy(hasDefectWithDefinition(defectMatcher)));
    }

    private Optional<MassResult<Long>> validate(PricePackage pricePackage) {
        return addOperationFactory.newInstance(PARTIAL, List.of(pricePackage), new User().withUid(1L)).prepare();
    }

}
