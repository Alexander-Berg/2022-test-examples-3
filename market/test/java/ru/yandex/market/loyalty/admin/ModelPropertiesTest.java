package ru.yandex.market.loyalty.admin;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.web.bind.annotation.RestController;

import ru.yandex.market.loyalty.test.SourceScanner;
import ru.yandex.market.loyalty.test.TestUtils;

public class ModelPropertiesTest {
    private static final String LOYALTY_PACKAGE = "ru.yandex.market.loyalty";

    //todo: надо бы все эти поля поправить, по идее
    private static final Set<String> EXCLUDED_FIELDS = Set.of(
            "ru.yandex.market.loyalty.api.model.ItemCashbackRequest.isdownloadable",
            "ru.yandex.market.loyalty.api.model.coin.creation.SingleCoinCreationRequest.isauth",
            "ru.yandex.market.loyalty.core.model.Experiments.raw",
            "ru.yandex.market.loyalty.core.model.GenericParam.value",
            "ru.yandex.market.loyalty.core.model.StaffUsersRecord.istaxi",
            "ru.yandex.market.loyalty.core.model.attachment.Field.valuesupplier",
            "ru.yandex.market.loyalty.core.model.attachment.Field.width",
            "ru.yandex.market.loyalty.core.model.attachment.Field.valueclass",
            "ru.yandex.market.loyalty.core.model.attachment.Field.name",
            "ru.yandex.market.loyalty.core.model.budgeting.AddedBudgetAudit.isreservebudget",
            "ru.yandex.market.loyalty.core.model.budgeting.Expectation.globaldefaultvalue",
            "ru.yandex.market.loyalty.core.model.budgeting.converter.DecimalConverter.expectation",
            "ru.yandex.market.loyalty.core.model.bundle.PromoBundleDefinition.useanaplanid",
            "ru.yandex.market.loyalty.core.model.bundle.PromoBundleDiscount.bundledefinition",
            "ru.yandex.market.loyalty.core.model.bundle.PromoBundleItemDefinition.itemkey",
            "ru.yandex.market.loyalty.core.model.bundle.PromoBundleItemDiscount.bundleitemdefinition",
            "ru.yandex.market.loyalty.core.model.coin.CoinSearchRequest.filters",
            "ru.yandex.market.loyalty.core.model.coin.CoreCoinCreationReason$AdminInfo.hasanalyticname",
            "ru.yandex.market.loyalty.core.model.coin.UserInfo.isyaplus",
            "ru.yandex.market.loyalty.core.model.coin.UserInfo.additionalphones",
            "ru.yandex.market.loyalty.core.model.coin.UserInfo.additionalemails",
            "ru.yandex.market.loyalty.core.model.trigger.event.data.OrderTermination.isantifraudcancel",
            "ru.yandex.market.loyalty.core.model.flash.FlashPromoItemDescription.hasnullfeedid",
            "ru.yandex.market.loyalty.core.model.flash.history.FlashHistoryContext.description",
            "ru.yandex.market.loyalty.core.model.order.Item.isdownloadable",
            "ru.yandex.market.loyalty.core.model.promo.PromoParameterName.tag",
            "ru.yandex.market.loyalty.core.model.trigger.TriggerCoinData.corecointype",
            "ru.yandex.market.loyalty.core.model.trigger.TriggerCoinData.nominal",
            "ru.yandex.market.loyalty.core.model.trigger.event.data.TriggerEventData.isexperiment",
            "ru.yandex.market.loyalty.admin.controller.dto.PagedRequest.from",
            "ru.yandex.market.loyalty.admin.controller.AdminCoinResponse.shortterm",
            "ru.yandex.market.loyalty.core.model.action.PromoActionFactory.beanfactory",
            "ru.yandex.market.loyalty.core.model.action.StaticPerkAdditionAction.staticperkservice",
            "ru.yandex.market.loyalty.core.model.action.StaticPerkRevocationAction.staticperkservice",
            "ru.yandex.market.loyalty.core.model.accounting.SmartAccount.accountgetter"
    );

    @Test
    public void fieldGettersAndSettersNamesTest() {
        var packagesToCheck = Set.of(
                "ru.yandex.market.loyalty.core.model",
                "ru.yandex.market.loyalty.client.model",
                "ru.yandex.market.loyalty.api.model"
        );

        var classes = packagesToCheck.stream()
                .flatMap(SourceScanner::findAllClasses)
                .filter(c -> Modifier.isPublic(c.getModifiers()));
        var controllerClasses = SourceScanner.findClassesByAnnotation(LOYALTY_PACKAGE, RestController.class)
                .flatMap(controller -> Arrays.stream(controller.getMethods())
                        .flatMap(method -> {
                            var subtypes = getAllFieldsClasses(method.getGenericReturnType());
                            Arrays.stream(method.getGenericParameterTypes())
                                    .map(ModelPropertiesTest::getAllFieldsClasses)
                                    .forEach(subtypes::addAll);

                            return subtypes.stream();
                        })
                );

        var brokenFields = Stream.of(classes, controllerClasses).flatMap(s -> s)
                .distinct()
                .filter(clazz -> !clazz.getName().contains("Builder")
                        && !clazz.getName().contains("Test")
                        && !clazz.isEnum()
                )
                .flatMap(clazz -> TestUtils.checkClassPropertyNamesIsRight(clazz).stream())
                .filter(field -> !EXCLUDED_FIELDS.contains(field))
                .collect(Collectors.joining("\n"));

        Assert.assertTrue("These fields may produce (de)serialization bugs:\n" + brokenFields,
                brokenFields.isEmpty());
    }

    private static List<Class<?>> getAllFieldsClasses(Type type) {
        var result = new ArrayList<Class<?>>();

        if (type instanceof ParameterizedType) {
            Arrays.stream(((ParameterizedType) type).getActualTypeArguments())
                    .forEach(innerType -> result.addAll(getAllFieldsClasses(innerType)));
            return result;
        }
        if (!type.getTypeName().contains(LOYALTY_PACKAGE)) {
            return result;
        }

        var clazz = (Class<?>) type;

        result.add(clazz);

        Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .map(Field::getGenericType)
                .forEach(fieldType -> result.addAll(getAllFieldsClasses(fieldType)));

        return result;
    }
}
