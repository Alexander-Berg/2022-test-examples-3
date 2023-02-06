package ru.yandex.direct.core.entity.moderation.service.receiving;

import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.moderation.repository.bulk_update.BulkUpdateHolder;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.BannersStatusactive;
import ru.yandex.direct.dbschema.ppc.enums.BannersStatusbssynced;
import ru.yandex.direct.dbschema.ppc.enums.BannersStatusmoderate;
import ru.yandex.direct.dbschema.ppc.enums.BannersStatuspostmoderate;
import ru.yandex.direct.dbschema.ppc.enums.BannersStatusshow;
import ru.yandex.direct.dbschema.ppc.enums.PhrasesStatusbssynced;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNERS;
import static ru.yandex.direct.dbschema.ppc.Tables.PHRASES;

@CoreTest
@ExtendWith(SpringExtension.class)
public class BannerChangesValidatorTest {

    @Autowired
    private Steps steps;
    @Autowired
    private DslContextProvider dslContextProvider;
    @Autowired
    private BannerChangesValidator bannerChangesValidator;

    private DSLContext context;

    @BeforeEach
    public void setUp() {
        var clientInfo = steps.clientSteps().createDefaultClient();
        context = dslContextProvider.ppc(clientInfo.getShard());
    }

    @ParameterizedTest
    @CsvSource({"Yes, Yes, No, ", "No, No, No, ", "No, No, No, No", "No, Rejected, No, "})
    public void validateBannerChanges_validFieldSets(BannersStatusmoderate bannerStatusModerate,
                                                     BannersStatuspostmoderate bannerStatusPostmoderate,
                                                     BannersStatusbssynced bannerStatusBsSynced,
                                                     BannersStatusshow bannerStatusShow) {
        BulkUpdateHolder updatesContainer = fillBulkUpdatesContainer(bannerStatusModerate, bannerStatusPostmoderate,
                bannerStatusBsSynced, bannerStatusShow);

        assertDoesNotThrow(() -> updatesContainer.execute(context.configuration()));
    }

    @ParameterizedTest
    @CsvSource({
            // StatusPostmoderate не соответствует StatusModerate
            "Yes, No, No, ", "Yes, Rejected, No, ", "No, Yes, No, ",
            // Забыли переотправить в БК (StatusBsSynced)
            "Yes, Yes, , ", "No, No, , ", "No, Rejected, , ",
            // Транспорт не должен сам запускать показы
            "Yes, Yes, No, Yes", "No, No, No, Yes", "No, Rejected, No, Yes"
    })
    public void validateBannerChanges_invalidFieldSets(BannersStatusmoderate bannerStatusModerate,
                                                       BannersStatuspostmoderate bannerStatusPostmoderate,
                                                       BannersStatusbssynced bannerStatusBsSynced,
                                                       BannersStatusshow bannerStatusShow) {
        BulkUpdateHolder updatesContainer = fillBulkUpdatesContainer(bannerStatusModerate, bannerStatusPostmoderate,
                bannerStatusBsSynced, bannerStatusShow);

        Exception exception = assertThrows(IllegalStateException.class,
                () -> updatesContainer.execute(context.configuration()));

        assertThat(exception.getMessage(), startsWith("Attempt to execute incorrect update"));
    }

    @Test
    public void validateBannerChanges_unexpectedFieldsChanged() {
        var updatesContainer = new BulkUpdateHolder();
        updatesContainer.setValidator(bannerChangesValidator::validateChanges);
        var changes = updatesContainer.get(BANNERS.BID).forId(RandomNumberUtils.nextPositiveLong());
        changes.set(BANNERS.STATUS_ACTIVE, BannersStatusactive.Yes);

        Exception exception = assertThrows(IllegalStateException.class,
                () -> updatesContainer.execute(context.configuration()));

        assertThat(exception.getMessage(), startsWith("Attempt to execute incorrect update"));
    }

    @Test
    public void validateBannerChanges_whenThereAreChangesInAdGroups() {
        var updatesContainer = fillBulkUpdatesContainer(BannersStatusmoderate.Yes, BannersStatuspostmoderate.Yes,
                BannersStatusbssynced.No, null);
        updatesContainer.get(PHRASES.BID).forId(RandomNumberUtils.nextPositiveLong())
                .set(PHRASES.STATUS_BS_SYNCED, PhrasesStatusbssynced.No);

        assertDoesNotThrow(() -> updatesContainer.execute(context.configuration()));
    }

    private BulkUpdateHolder fillBulkUpdatesContainer(BannersStatusmoderate bannerStatusModerate,
                                                      BannersStatuspostmoderate bannerStatusPostmoderate,
                                                      BannersStatusbssynced bannerStatusBsSynced,
                                                      BannersStatusshow bannerStatusShow) {
        var updatesContainer = new BulkUpdateHolder();
        updatesContainer.setValidator(bannerChangesValidator::validateChanges);
        var changes = updatesContainer.get(BANNERS.BID).forId(RandomNumberUtils.nextPositiveLong());
        if (bannerStatusModerate != null) {
            changes.set(BANNERS.STATUS_MODERATE, bannerStatusModerate);
        }
        if (bannerStatusPostmoderate != null) {
            changes.set(BANNERS.STATUS_POST_MODERATE, bannerStatusPostmoderate);
        }
        if (bannerStatusBsSynced != null) {
            changes.set(BANNERS.STATUS_BS_SYNCED, bannerStatusBsSynced);
        }
        if (bannerStatusShow != null) {
            changes.set(BANNERS.STATUS_SHOW, bannerStatusShow);
        }
        return updatesContainer;
    }
}
