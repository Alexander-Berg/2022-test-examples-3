package ru.yandex.market.mboc.tms.executors.notifications.creators;

import java.util.List;

import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mboc.common.notifications.model.Notification;
import ru.yandex.market.mboc.common.notifications.model.data.content.CategoriesWithoutManagerData;
import ru.yandex.market.mboc.common.notifications.repository.NotificationRepositoryMock;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.OfferRepositoryMock;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoRepositoryMock;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeService;
import ru.yandex.market.mboc.common.services.mbousers.MboUsersRepositoryMock;
import ru.yandex.market.mboc.common.services.mbousers.models.MboUser;
import ru.yandex.market.mboc.common.test.YamlTestUtil;

/**
 * @author galaev@yandex-team.ru
 * @since 21/01/2019.
 */
@SuppressWarnings("checkstyle:magicNumber")
public class NotifyContentsAboutCategoriesWithoutManagerExecutorTest {

    private CategoryInfoRepositoryMock categoryInfoRepository;
    private NotificationRepositoryMock notificationRepository;

    private NotifyContentsAboutCategoriesWithoutManagerExecutor executor;

    @Before
    public void setUp() {
        OfferRepositoryMock offerRepository = new OfferRepositoryMock();
        List<Offer> offers = YamlTestUtil.readOffersFromResources("notifications/content/offers.yml");
        offerRepository.insertOffers(offers);

        CategoryKnowledgeService categoryKnowledgeService = Mockito.mock(CategoryKnowledgeService.class);
        Mockito.when(categoryKnowledgeService.filterCategoriesWithKnowledge(Mockito.anyCollection()))
            .thenReturn(ImmutableSet.of(1L, 2L, 3L));

        CategoryCachingServiceMock categoryCachingService = new CategoryCachingServiceMock();

        MboUsersRepositoryMock mboUsersRepository = new MboUsersRepositoryMock();
        mboUsersRepository.insert(new MboUser(1, "the manager", "manager"));

        categoryInfoRepository = new CategoryInfoRepositoryMock(mboUsersRepository);
        categoryInfoRepository.insert(new CategoryInfo().setCategoryId(1).setInputManagerUid(1L));
        categoryInfoRepository.insert(new CategoryInfo().setCategoryId(2).setInputManagerUid(1L));

        notificationRepository = Mockito.spy(new NotificationRepositoryMock());

        executor = new NotifyContentsAboutCategoriesWithoutManagerExecutor(
            offerRepository, categoryKnowledgeService, categoryCachingService, categoryInfoRepository,
            notificationRepository);
    }

    @Test
    public void testCategoriesWithoutManagersReport() throws Exception {
        executor.execute();

        List<Notification> newNotifications = notificationRepository.findNewNotifications();
        Assertions.assertThat(newNotifications).hasSize(1);
        CategoriesWithoutManagerData data = (CategoriesWithoutManagerData) newNotifications.get(0).getData();
        Assertions.assertThat(data.getCategories()).hasSize(1);
        Assertions.assertThat(data.getCategories().get(0).getId()).isEqualTo(3);
    }

    @Test
    public void testAllCategoriesHaveManagers() throws Exception {
        categoryInfoRepository.insert(new CategoryInfo().setCategoryId(3).setInputManagerUid(1L));

        executor.execute();

        Mockito.verifyZeroInteractions(notificationRepository);
    }
}
