package ru.yandex.direct.core.entity.banner.type.name;

import java.time.LocalDateTime;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.BannerNameStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithName;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerClientInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.NewTextBannerInfo;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;

@CoreTest
@RunWith(Parameterized.class)
public class BannerWithNameUpdatePositiveTest extends BannerClientInfoUpdateOperationTestBase {

    private static final String DEFAULT_NEW_NAME = "default name";

    private static final LocalDateTime DEFAULT_LAST_CHANGE = LocalDateTime.now().minusMinutes(7).withNano(0);

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public String oldName;

    @Parameterized.Parameter(2)
    public String newName;

    @Parameterized.Parameter(3)
    public BannerNameStatusModerate oldBannerNameStatusModerate;

    @Parameterized.Parameter(4)
    public BannerNameStatusModerate expectedBannerNameStatusModerate;

    @Parameterized.Parameter(5)
    public StatusBsSynced expectedStatusBsSynced;

    @Parameterized.Parameter(6)
    public Boolean lastChangeShouldBeChanged;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "Добавление названия",
                        null,
                        DEFAULT_NEW_NAME,
                        null,
                        BannerNameStatusModerate.READY,
                        StatusBsSynced.NO,
                        true
                },
                {
                        "Удаление названия",
                        DEFAULT_NEW_NAME,
                        null,
                        BannerNameStatusModerate.READY,
                        null,
                        StatusBsSynced.NO,
                        true
                },
                {
                        "Изменение названия",
                        "some name",
                        DEFAULT_NEW_NAME,
                        BannerNameStatusModerate.YES,
                        BannerNameStatusModerate.READY,
                        StatusBsSynced.NO,
                        true
                },
                {
                        "Название не изменилось",
                        DEFAULT_NEW_NAME,
                        DEFAULT_NEW_NAME,
                        BannerNameStatusModerate.YES,
                        BannerNameStatusModerate.YES,
                        StatusBsSynced.YES,
                        false
                }
        });
    }

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
        clientInfo = steps.clientSteps().createDefaultClient();
    }

    @Test
    public void update() {

        NewTextBannerInfo bannerInfo = createBanner();

        ModelChanges<TextBanner> modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(newName, BannerWithName.NAME);

        prepareAndApplyValid(modelChanges);

        TextBanner actualBanner = getBanner(bannerInfo.getBannerId());
        assertThat(actualBanner.getName()).isEqualTo(newName);
        assertThat(actualBanner.getNameStatusModerate()).isEqualTo(expectedBannerNameStatusModerate);
        assertThat(actualBanner.getStatusModerate()).isEqualTo(BannerStatusModerate.YES);
        assertThat(actualBanner.getStatusBsSynced()).isEqualTo(expectedStatusBsSynced);
        if (lastChangeShouldBeChanged) {
            assertThat(actualBanner.getLastChange()).isNotEqualTo(DEFAULT_LAST_CHANGE);
        } else {
            assertThat(actualBanner.getLastChange()).isEqualTo(DEFAULT_LAST_CHANGE);

        }
    }

    private NewTextBannerInfo createBanner() {
        TextBanner banner = fullTextBanner()
                .withName(oldName)
                .withNameStatusModerate(oldBannerNameStatusModerate)
                .withStatusModerate(BannerStatusModerate.YES)
                .withStatusBsSynced(StatusBsSynced.YES)
                .withLastChange(DEFAULT_LAST_CHANGE);

        return steps.textBannerSteps().createBanner(new NewTextBannerInfo()
                .withBanner(banner)
                .withClientInfo(clientInfo)
        );
    }
}
