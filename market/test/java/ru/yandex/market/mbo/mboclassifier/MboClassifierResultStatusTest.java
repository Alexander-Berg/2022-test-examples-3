package ru.yandex.market.mbo.mboclassifier;

import org.junit.Test;
import ru.yandex.market.mbo.http.OffersStorage;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static ru.yandex.common.util.collections.CollectionUtils.join;

/**
 * @author: astafurovme@yandex-team.ru
 */
public class MboClassifierResultStatusTest {

    private static final long USER_1 = 1000501;
    private static final long USER_2 = 1000502;
    private static final long USER_3 = 1000503;
    private static final long USER_4 = 1000504;
    private static final long OTHER_USER_1 = 1000001;
    private static final long OTHER_USER_2 = 1000002;
    private static final long OTHER_USER_3 = 1000003;
    private static final long OTHER_USER_4 = 1000004;
    private static final long OTHER_USER_5 = 1000004;
    private static final long OTHER_USER_6 = 1000004;

    private static final long TIME_TODAY = 1000;
    private static final long TIME_YESTERDAY = 900;
    private static final long TIME_FEW_DAYS_AGO = 800;
    private static final long TIME_LAST_MONTH = 500;
    private static final long TIME_LAST_YEAR = 100;

    private static final long CATEGORY_1 = 501;
    private static final long CATEGORY_2 = 502;
    private static final long CATEGORY_3 = 503;

    @Test
    public void testResultStatus() throws Exception {
        testForceApprovedByOneManager();
        testForceApprovedByManyManagers();
        testChecked();
        testApproved();
        testAutoApproved();
        testAssured();
        testUnassuredInTwoCategories();
        testUnassured();
        testTrash();
        testUnchecked();
    }

    private void testForceApprovedByOneManager() {
        OffersStorage.OfferMarkup markup = MarkupCalculator.calculateMarkup(
                join(
                        Arrays.asList(buildCategoryChange(USER_1, TIME_YESTERDAY, CATEGORY_1, true)),
                        buildOtherOperatorChanges()
                )
        );
        check(markup, OffersStorage.MarkupStatus.FORCE_APPROVED, CATEGORY_1, TIME_YESTERDAY, true);
    }

    private void testForceApprovedByManyManagers() {
        OffersStorage.OfferMarkup markup = MarkupCalculator.calculateMarkup(
                join(
                        Arrays.asList(buildCategoryChange(USER_1, TIME_YESTERDAY, CATEGORY_1, true)),
                        Arrays.asList(buildCategoryChange(USER_2, TIME_TODAY, CATEGORY_3, true)),
                        Arrays.asList(buildCategoryChange(USER_2, TIME_FEW_DAYS_AGO, CATEGORY_2, true)),
                        buildOtherOperatorChanges()
                )
        );
        check(markup, OffersStorage.MarkupStatus.FORCE_APPROVED, CATEGORY_3, TIME_TODAY, true);
    }

    private void testChecked() {
        OffersStorage.OfferMarkup markup = MarkupCalculator.calculateMarkup(
                join(
                        Arrays.asList(buildCategoryChange(USER_1, TIME_YESTERDAY, CATEGORY_1, false))
                )
        );
        check(markup, OffersStorage.MarkupStatus.CHECKED, CATEGORY_1, TIME_YESTERDAY, false);
    }

    private void testAutoApproved() {
        OffersStorage.OfferMarkup markup = MarkupCalculator.calculateMarkup(
                join(
                        Arrays.asList(buildCategoryChange(USER_1, TIME_TODAY, CATEGORY_1, false, true)),
                        Arrays.asList(buildCategoryChange(USER_1, TIME_YESTERDAY, CATEGORY_2, false))
                )
        );
        check(markup, OffersStorage.MarkupStatus.AUTO_APPROVED, CATEGORY_1, TIME_TODAY, false);
    }

    private void testApproved() {
        OffersStorage.OfferMarkup markup = MarkupCalculator.calculateMarkup(
                join(
                        Arrays.asList(buildCategoryChange(USER_1, TIME_TODAY, CATEGORY_1, false, true)),
                        Arrays.asList(buildCategoryChange(USER_2, TIME_TODAY, CATEGORY_1, false, true)),
                        Arrays.asList(buildCategoryChange(USER_2, TIME_YESTERDAY, CATEGORY_2, false)),
                        Arrays.asList(buildCategoryChange(USER_2, TIME_FEW_DAYS_AGO, CATEGORY_3, false)),
                        Arrays.asList(buildCategoryChange(USER_3, TIME_TODAY, CATEGORY_1, false)),
                        Arrays.asList(buildCategoryChange(USER_3, TIME_FEW_DAYS_AGO, CATEGORY_3, false)),
                        Arrays.asList(buildCategoryChange(OTHER_USER_1, TIME_YESTERDAY, CATEGORY_2, false))
                )
        );
        check(markup, OffersStorage.MarkupStatus.APPROVED, CATEGORY_1, TIME_TODAY, false);
    }

    private void testAssured() {
        OffersStorage.OfferMarkup markup = MarkupCalculator.calculateMarkup(
                join(
                        Arrays.asList(buildCategoryChange(USER_1, TIME_YESTERDAY, CATEGORY_1, false)),
                        Arrays.asList(buildCategoryChange(USER_2, TIME_YESTERDAY, CATEGORY_1, false, true)),
                        Arrays.asList(buildCategoryChange(USER_2, TIME_FEW_DAYS_AGO, CATEGORY_3, false)),
                        Arrays.asList(buildCategoryChange(USER_3, TIME_YESTERDAY, CATEGORY_1, false)),
                        Arrays.asList(buildCategoryChange(USER_3, TIME_FEW_DAYS_AGO, CATEGORY_3, false, true)),
                        Arrays.asList(buildCategoryChange(USER_4, TIME_FEW_DAYS_AGO, CATEGORY_1, false)),
                        Arrays.asList(buildCategoryChange(OTHER_USER_1, TIME_TODAY, CATEGORY_2, false)),
                        Arrays.asList(buildCategoryChange(OTHER_USER_2, TIME_YESTERDAY, CATEGORY_2, false)),
                        Arrays.asList(buildCategoryChange(OTHER_USER_3, TIME_FEW_DAYS_AGO, CATEGORY_2, false))
                )
        );
        check(markup, OffersStorage.MarkupStatus.ASSURED, CATEGORY_1, TIME_TODAY, false);
    }

    private void testUnassured() {
        OffersStorage.OfferMarkup markup = MarkupCalculator.calculateMarkup(
                join(
                        Arrays.asList(buildCategoryChange(USER_1, TIME_TODAY, CATEGORY_1, false)),
                        Arrays.asList(buildCategoryChange(USER_2, TIME_TODAY, CATEGORY_1, false, true)),
                        Arrays.asList(buildCategoryChange(USER_2, TIME_YESTERDAY, CATEGORY_2, false)),
                        Arrays.asList(buildCategoryChange(USER_2, TIME_FEW_DAYS_AGO, CATEGORY_3, false)),
                        Arrays.asList(buildCategoryChange(USER_3, TIME_TODAY, CATEGORY_1, false)),
                        Arrays.asList(buildCategoryChange(USER_3, TIME_FEW_DAYS_AGO, CATEGORY_3, false)),
                        Arrays.asList(buildCategoryChange(USER_4, TIME_FEW_DAYS_AGO, CATEGORY_1, false)),
                        Arrays.asList(buildCategoryChange(OTHER_USER_1, TIME_YESTERDAY, CATEGORY_2, false, true)),
                        Arrays.asList(buildCategoryChange(OTHER_USER_2, TIME_YESTERDAY, CATEGORY_2, false)),
                        Arrays.asList(buildCategoryChange(OTHER_USER_3, TIME_YESTERDAY, CATEGORY_2, false)),
                        Arrays.asList(buildCategoryChange(OTHER_USER_4, TIME_YESTERDAY, CATEGORY_3, false)),
                        Arrays.asList(buildCategoryChange(OTHER_USER_5, TIME_YESTERDAY, CATEGORY_3, false)),
                        Arrays.asList(buildCategoryChange(OTHER_USER_6, TIME_YESTERDAY, CATEGORY_3, false))
                )
        );
        check(markup, OffersStorage.MarkupStatus.UNASSURED, CATEGORY_1, TIME_TODAY, false);
    }

    private void testUnassuredInTwoCategories() {
        OffersStorage.OfferMarkup markup = MarkupCalculator.calculateMarkup(
                join(
                        Arrays.asList(buildCategoryChange(USER_1, TIME_FEW_DAYS_AGO, CATEGORY_1, false)),
                        Arrays.asList(buildCategoryChange(USER_2, TIME_YESTERDAY, CATEGORY_1, false)),
                        Arrays.asList(buildCategoryChange(USER_3, TIME_TODAY, CATEGORY_2, false)),
                        Arrays.asList(buildCategoryChange(USER_4, TIME_FEW_DAYS_AGO, CATEGORY_2, false))
                )
        );
        check(markup, OffersStorage.MarkupStatus.UNASSURED, CATEGORY_2, TIME_TODAY, false);
    }

    private void testTrash() {
        OffersStorage.OfferMarkup markup = MarkupCalculator.calculateMarkup(
                join(
                        Arrays.asList(buildCategoryChange(USER_1, TIME_YESTERDAY, CATEGORY_1, false)),
                        Arrays.asList(buildCategoryChange(USER_2, TIME_FEW_DAYS_AGO, CATEGORY_2, false)),
                        Arrays.asList(buildCategoryChange(USER_3, TIME_TODAY, CATEGORY_3, false))
                )
        );
        check(markup, OffersStorage.MarkupStatus.TRASH, CATEGORY_3, TIME_TODAY, false);
    }

    private void testUnchecked() {
        OffersStorage.OfferMarkup markup = MarkupCalculator.calculateMarkup(Collections.EMPTY_LIST);
        assert markup.getStatus().equals(OffersStorage.MarkupStatus.UNCHECKED);
    }

    private void check(OffersStorage.OfferMarkup markup, OffersStorage.MarkupStatus status, long resultCategoryId,
                       long lastModificationTs, boolean isForceApproved) {
        assert markup.getStatus().equals(status);
        assert markup.getResultCategoryId() == resultCategoryId;
        assert markup.getModificationDate() == lastModificationTs;
        assert markup.getForceApproved() == isForceApproved;
    }

    private static OffersStorage.CategoryChange buildCategoryChange(long uid, long modificationTs, long categoryId,
                                                                    boolean forceApproved) {
        return buildCategoryChange(uid, modificationTs, categoryId, forceApproved, false);
    }

    private static OffersStorage.CategoryChange buildCategoryChange(long uid, long modificationTs, long categoryId,
                                                                    boolean forceApproved, boolean autoApproved) {
        OffersStorage.CategoryChange.Builder b = OffersStorage.CategoryChange.newBuilder();
        b.setOperatorUid(uid);
        b.setModificationDate(modificationTs);
        b.setOperatorCategoryId(categoryId);
        b.setForceApproved(forceApproved);
        b.setAutoApproved(autoApproved);
        return b.build();
    }

    private List<OffersStorage.CategoryChange> buildOtherOperatorChanges() {
        return Arrays.asList(
                buildCategoryChange(OTHER_USER_1, TIME_LAST_MONTH, CATEGORY_1, false),
                buildCategoryChange(OTHER_USER_1, TIME_LAST_YEAR, CATEGORY_2, false),
                buildCategoryChange(OTHER_USER_2, TIME_LAST_MONTH, CATEGORY_1, false),
                buildCategoryChange(OTHER_USER_2, TIME_LAST_YEAR, CATEGORY_2, false),
                buildCategoryChange(OTHER_USER_2, TIME_LAST_MONTH, CATEGORY_3, false),
                buildCategoryChange(OTHER_USER_3, TIME_LAST_YEAR, CATEGORY_1, false)
        );
    }

}
