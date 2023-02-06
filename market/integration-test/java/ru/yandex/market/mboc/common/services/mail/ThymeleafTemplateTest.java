package ru.yandex.market.mboc.common.services.mail;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import ru.yandex.market.mboc.common.BaseIntegrationTestClass;
import ru.yandex.market.mboc.common.notifications.model.Notification;
import ru.yandex.market.mboc.common.notifications.model.data.catmans.CategoryOffersNeedActionData;
import ru.yandex.market.mboc.common.notifications.model.data.catmans.NewOffersData;
import ru.yandex.market.mboc.common.notifications.model.data.content.CategoriesWithoutManagerData;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.mbousers.models.MboUser;
import ru.yandex.market.mboc.common.services.offers.mapping.CategoryCheckMappingResult;
import ru.yandex.market.mboc.common.services.offers.mapping.CheckMappingResult;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static ru.yandex.market.mboc.common.utils.OfferTestUtils.defaultMappingName;

/**
 * All email templates should be tested.
 *
 * @author prediger
 */
@SuppressWarnings("checkstyle:magicNumber")
public class ThymeleafTemplateTest extends BaseIntegrationTestClass {

    @Autowired
    private TemplateEngine thymeleafTemplateEngine;

    @Test
    public void testCatManNewOffersEmailTemplate() {
        Notification notification =
            YamlTestUtil.readFromResources("email/catman/new_offers/notification.yml",
                Notification.class);
        Collection<NewOffersData.SupplierInfo> supplierInfos = ((NewOffersData) notification.getData())
            .getNewOffersBySuppliers()
            .values();

        Context context = new Context();
        context.setVariable("suppliers", supplierInfos);

        String processedTemplate = thymeleafTemplateEngine.process("CatManNewOffers", context);
        Assertions.assertThat(processedTemplate.replaceAll(" {2,}\\n", ""))
            .isEqualTo(YamlTestUtil
                .readFromResources("email/catman/new_offers/result-body.yml", String.class));
    }

    @Test
    public void testCatManNeedActionOffersEmailTemplate() {
        Notification notification =
            YamlTestUtil.readFromResources("email/catman/need_action/notification.yml",
                Notification.class);
        List<CategoryOffersNeedActionData.CategoryInfo> categoryInfos =
            ((CategoryOffersNeedActionData) notification.getData())
                .getCategoryInfos();

        Context context = new Context();
        context.setVariable("categories", categoryInfos);

        String processedTemplate = thymeleafTemplateEngine.process("CatManNeedActionOffers", context);
        Assertions.assertThat(processedTemplate.replaceAll(" {2,}\\n", ""))
            .isEqualTo(YamlTestUtil
                .readFromResources("email/catman/need_action/result-body.yml", String.class));
    }

    @Test
    public void testCategoriesWithoutManagerTemplate() {
        List<CategoriesWithoutManagerData.CategoryInfo> categoriesWithoutManager = ImmutableList.of(
            new CategoriesWithoutManagerData.CategoryInfo()
                .setId(1)
                .setName("Велосипеды")
                .setDepartment("Спорт")
                .setOffersCount(5),
            new CategoriesWithoutManagerData.CategoryInfo()
                .setId(2)
                .setName("Ноутбуки")
                .setDepartment("Техника")
                .setOffersCount(10),
            new CategoriesWithoutManagerData.CategoryInfo()
                .setId(3)
                .setName("Апельсины")
                .setDepartment("Фрукты")
                .setOffersCount(7)).stream()
            .sorted(Comparator.comparing(CategoriesWithoutManagerData.CategoryInfo::getOffersCount,
                Comparator.reverseOrder()))
            .collect(Collectors.toList());

        Context context = new Context();
        context.setVariable("categories", categoriesWithoutManager);

        String processedTemplate = thymeleafTemplateEngine.process("CategoriesWithoutManager", context);
        String expectedTemplate = YamlTestUtil.readFromResources(
            "email/content/categories-wo-manager-result.yml", String.class);
        Assertions.assertThat(processedTemplate).isEqualTo(expectedTemplate);
    }


    @Test
    public void testCheckMappingReportEmailTemplate() {
        CategoryCheckMappingResult result1 = new CategoryCheckMappingResult(11L,
            new Category().setCategoryId(11).setName("Тестовая категория 1"),
            Arrays.asList(createMappingResult(11), createMappingResult(11)),
            createUser("testuser", "Тестовый Юзер"));

        CategoryCheckMappingResult result2 = new CategoryCheckMappingResult(13L,
            new Category().setCategoryId(13).setName("Тестовая категория 2"),
            Arrays.asList(createMappingResult(13), createMappingResult(13)), null);

        Context context = new Context();
        context.setVariable("mboUrl", "http://mboUrl/");
        context.setVariable("mbocUrl", "http://mbocUrl/");
        context.setVariable("categoryResults",
            Arrays.asList(result1, result2));

        String processedTemplate = thymeleafTemplateEngine.process("CheckMappingReport", context);
        Assertions.assertThat(processedTemplate.replaceAll(" {2,}\\n", ""))
            .isEqualTo(YamlTestUtil
                .readFromResources("email/mapping/check-mapping-report-result.yml", String.class));
    }

    private MboUser createUser(String staffLogin, String staffName) {
        MboUser mboUser = new MboUser();
        mboUser.setStaffLogin(staffLogin);
        mboUser.setStaffFullname(staffName);
        return mboUser;
    }

    private CheckMappingResult createMappingResult(long categoryId) {
        return new CheckMappingResult(1L, 2L, categoryId, Offer.MappingType.APPROVED,
            OfferTestUtils.mapping(1), defaultMappingName(1), CheckMappingResult.Type.INVALID, Collections.singletonList(
            new CheckMappingResult.Cause(CheckMappingResult.CauseType.NOT_FOUND_MODEL, 13)));
    }
}
