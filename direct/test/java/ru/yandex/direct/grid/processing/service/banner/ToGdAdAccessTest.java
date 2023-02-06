package ru.yandex.direct.grid.processing.service.banner;

import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBanner;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupAccess;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupTruncated;
import ru.yandex.direct.grid.processing.model.group.GdTextAdGroup;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.grid.processing.service.banner.BannerDataConverter.toGdAdAccess;

@ParametersAreNonnullByDefault
@RunWith(JUnitParamsRunner.class)
public class ToGdAdAccessTest {

    @Test
    @Parameters(method = "parametersForReplaceToGdAdAccessWorksCorrect")
    @TestCaseName("canEdit = {3}, {0}")
    public void replaceToGdAdAccessWorksCorrect(@SuppressWarnings("unused") String testDescription,
                                                boolean statusArchived, boolean canEdit, boolean expectedCanEdit) {
        GdiBanner internal = new GdiBanner()
                .withId(RandomNumberUtils.nextPositiveLong())
                .withStatusArchived(statusArchived);
        GdAdGroupTruncated group = new GdTextAdGroup()
                .withAccess(new GdAdGroupAccess()
                        .withCanEdit(canEdit));
        User operator = new User().withRole(RbacRole.CLIENT);
        User subjectUser = new User().withRole(RbacRole.CLIENT);

        boolean actualCanEdit = toGdAdAccess(operator, subjectUser, internal, group, Set.of(), Set.of()).getCanEdit();
        assertThat(actualCanEdit)
                .isEqualTo(expectedCanEdit);
    }

    public static Object[][] parametersForReplaceToGdAdAccessWorksCorrect() {
        return new Object[][]{
                {"когда баннер не архивный и группу можно редактировать", false, true, true},
                {"когда баннер архивный и группу можно редактировать", true, true, false},
                {"когда баннер не архивный и группу нельзя редактировать", false, false, false}
        };
    }
}
