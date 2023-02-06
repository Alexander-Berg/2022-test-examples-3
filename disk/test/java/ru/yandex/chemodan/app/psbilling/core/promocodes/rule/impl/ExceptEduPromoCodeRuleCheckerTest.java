package ru.yandex.chemodan.app.psbilling.core.promocodes.rule.impl;

import java.util.UUID;

import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.promocodes.model.PromoCodeData;
import ru.yandex.chemodan.app.psbilling.core.promocodes.rule.PromoCodeRuleContext;
import ru.yandex.chemodan.directory.client.DirectoryClient;
import ru.yandex.chemodan.directory.client.DirectoryOrganizationFeaturesResponse;
import ru.yandex.chemodan.directory.client.OrganizationNotFoundException;
import ru.yandex.misc.test.Assert;

import static org.mockito.ArgumentMatchers.eq;

public class ExceptEduPromoCodeRuleCheckerTest {

    @Mock
    private DirectoryClient directoryClient;

    @Mock
    private Group group;

    @Before
    public void setUp() throws Exception {
        val groupExternalId = UUID.randomUUID().toString();

        MockitoAnnotations.initMocks(this);
        Mockito.when(group.getExternalId()).thenReturn(groupExternalId);
    }

    @Test
    public void testWithoutGroup() {
        val selector = new ExceptEduPromoCodeRuleChecker(directoryClient);

        Assert.assertThrows(
                () -> selector.check(Mockito.mock(PromoCodeData.class), Option.empty(), Option.empty(),
                        new PromoCodeRuleContext()),
                IllegalArgumentException.class
        );
    }

    @Test
    public void testOrgNotFound() {
        val externalId = group.getExternalId();

        val selector = new ExceptEduPromoCodeRuleChecker(directoryClient);

        Mockito.when(directoryClient.getOrganizationFeatures(eq(externalId)))
                .thenThrow(new OrganizationNotFoundException(externalId));

        Assert.assertThrows(
                () -> selector.check(Mockito.mock(PromoCodeData.class), Option.empty(), Option.of(group),
                        new PromoCodeRuleContext()),
                OrganizationNotFoundException.class
        );
    }

    @Test
    public void testOrgIsEdu() {
        val externalId = group.getExternalId();

        val selector = new ExceptEduPromoCodeRuleChecker(directoryClient);

        Mockito.when(directoryClient.getOrganizationFeatures(eq(externalId)))
                .thenReturn(new DirectoryOrganizationFeaturesResponse(true, true));

        val result = selector.check(Mockito.mock(PromoCodeData.class), Option.empty(), Option.of(group),
                new PromoCodeRuleContext());

        Assert.isFalse(result.isSuccess());
        Assert.some(result.getError());
    }

    @Test
    public void testOrgIsNotEdu() {
        val externalId = group.getExternalId();

        val selector = new ExceptEduPromoCodeRuleChecker(directoryClient);

        Mockito.when(directoryClient.getOrganizationFeatures(eq(externalId)))
                .thenReturn(new DirectoryOrganizationFeaturesResponse(false, true));

        val result = selector.check(Mockito.mock(PromoCodeData.class), Option.empty(), Option.of(group),
                new PromoCodeRuleContext());

        Assert.isTrue(result.isSuccess());
        Assert.none(result.getError());
    }
}
