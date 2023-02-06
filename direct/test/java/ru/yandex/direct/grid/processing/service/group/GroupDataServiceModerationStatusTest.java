package ru.yandex.direct.grid.processing.service.group;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroup;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroupModerationStatus;
import ru.yandex.direct.grid.core.entity.group.service.GridAdGroupUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.defaultGdiBaseGroup;

@RunWith(Parameterized.class)
public class GroupDataServiceModerationStatusTest {

    @Parameterized.Parameter
    public String testCaseDescription;

    @Parameterized.Parameter(1)
    public StatusModerate dbStatusModerate;

    @Parameterized.Parameter(2)
    public StatusPostModerate dbStatusPostModerate;

    @Parameterized.Parameter(3)
    public GdiGroupModerationStatus expectedModerationStatus;

    @Parameterized.Parameters(name = "case = {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                        {"Draft - черновик группы",
                                StatusModerate.NEW,
                                StatusPostModerate.NO,
                                GdiGroupModerationStatus.DRAFT
                        },
                        {"Moderation.1 - группа на модерации",
                                StatusModerate.READY,
                                StatusPostModerate.NO,
                                GdiGroupModerationStatus.MODERATION
                        },
                        {"Moderation.2 - группа на модерации",
                                StatusModerate.SENDING,
                                StatusPostModerate.NO,
                                GdiGroupModerationStatus.MODERATION
                        },
                        {"Moderation.3 - группа на модерации",
                                StatusModerate.SENT,
                                StatusPostModerate.NO,
                                GdiGroupModerationStatus.MODERATION
                        },
                        {"Moderation.4 - группа на модерации",
                                StatusModerate.READY,
                                StatusPostModerate.NEW,
                                GdiGroupModerationStatus.MODERATION
                        },
                        {"Moderation.5 - группа на модерации",
                                StatusModerate.READY,
                                StatusPostModerate.REJECTED,
                                GdiGroupModerationStatus.MODERATION
                        },
                        {"Moderation.6 - группа на модерации",
                                StatusModerate.READY,
                                StatusPostModerate.SENT,
                                GdiGroupModerationStatus.MODERATION
                        },
                        {"Preaccepted.1 - группа предаварительно принята на модерации",
                                StatusModerate.READY,
                                StatusPostModerate.YES,
                                GdiGroupModerationStatus.PREACCEPTED
                        },
                        {"Preaccepted.2 - группа предаварительно принята на модерации",
                                StatusModerate.SENDING,
                                StatusPostModerate.YES,
                                GdiGroupModerationStatus.PREACCEPTED
                        },
                        {"Preaccepted.3 - группа предаварительно принята на модерации",
                                StatusModerate.SENT,
                                StatusPostModerate.YES,
                                GdiGroupModerationStatus.PREACCEPTED
                        },
                        {"Accepted.1 - группа принята на модерации",
                                StatusModerate.YES,
                                StatusPostModerate.YES,
                                GdiGroupModerationStatus.ACCEPTED
                        },
                        {"Rejected.1 - группа отклонена на модерации",
                                StatusModerate.NO,
                                StatusPostModerate.YES,
                                GdiGroupModerationStatus.REJECTED
                        },
                        {"Rejected.2 - группа отклонена на модерации",
                                StatusModerate.NO,
                                StatusPostModerate.NO,
                                GdiGroupModerationStatus.REJECTED
                        },
                }
        );
    }


    @Test
    public void testBaseGroupModerationStatus() {
        GdiGroup internalGroup =
                getGroupWithUpdatedStatuses(defaultGdiBaseGroup(), dbStatusModerate, dbStatusPostModerate);
        GdiGroupModerationStatus result = GridAdGroupUtils.getGroupModerationStatus(internalGroup);

        assertThat(result).isEqualByComparingTo(expectedModerationStatus);
    }

    private static GdiGroup getGroupWithUpdatedStatuses(GdiGroup group, StatusModerate statusModerate,
                                                        StatusPostModerate statusPostModerate) {
        return group
                .withStatusModerate(statusModerate)
                .withStatusPostModerate(statusPostModerate);
    }
}
