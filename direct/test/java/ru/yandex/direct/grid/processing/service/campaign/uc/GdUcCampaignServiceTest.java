package ru.yandex.direct.grid.processing.service.campaign.uc;

import java.util.Set;

import junitparams.JUnitParamsRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.bidmodifiers.service.validation.typesupport.BidModifierTestHelper;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.forecast.GdDeviceType;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class GdUcCampaignServiceTest {

    @Test
    public void extractNoMobileFromContainer() {
        var data = new UcCampaignDataContainer();
        data.setBidModifierMobile(BidModifierTestHelper.getZeroMobile());
        var result = GdUcCampaignService.extractDeviceTypesFromDeviceBidModifiers(data);
        assertThat(result, equalTo(Set.of(GdDeviceType.TABLET, GdDeviceType.DESKTOP)));
    }

    @Test
    public void extractNoDesktopFromContainer() {
        var data = new UcCampaignDataContainer();
        data.setBidModifierDesktop(BidModifierTestHelper.getZeroDesktop());
        var result = GdUcCampaignService.extractDeviceTypesFromDeviceBidModifiers(data);
        assertThat(result, equalTo(Set.of(GdDeviceType.PHONE)));
    }

    @Test
    public void extractNoDesktopOnlyFromContainer() {
        var data = new UcCampaignDataContainer();
        data.setBidModifierDesktopOnly(BidModifierTestHelper.getZeroDesktopOnly());
        var result = GdUcCampaignService.extractDeviceTypesFromDeviceBidModifiers(data);
        assertThat(result, equalTo(Set.of(GdDeviceType.PHONE, GdDeviceType.TABLET)));
    }

    @Test
    public void extractNoTabletFromContainer() {
        var data = new UcCampaignDataContainer();
        data.setBidModifierTablet(BidModifierTestHelper.getZeroTablet());
        var result = GdUcCampaignService.extractDeviceTypesFromDeviceBidModifiers(data);
        assertThat(result, equalTo(Set.of(GdDeviceType.PHONE, GdDeviceType.DESKTOP)));
    }

    @Test
    public void extractNoTabletNoPhoneFromContainer() {
        var data = new UcCampaignDataContainer();
        data.setBidModifierTablet(BidModifierTestHelper.getZeroTablet());
        data.setBidModifierMobile(BidModifierTestHelper.getZeroMobile());
        var result = GdUcCampaignService.extractDeviceTypesFromDeviceBidModifiers(data);
        assertThat(result, equalTo(Set.of(GdDeviceType.DESKTOP)));
    }

    @Test
    public void extractNoDesktopNoPhoneFromContainer() {
        var data = new UcCampaignDataContainer();
        data.setBidModifierDesktopOnly(BidModifierTestHelper.getZeroDesktopOnly());
        data.setBidModifierMobile(BidModifierTestHelper.getZeroMobile());
        var result = GdUcCampaignService.extractDeviceTypesFromDeviceBidModifiers(data);
        assertThat(result, equalTo(Set.of(GdDeviceType.TABLET)));
    }

    @Test
    public void extractALLFromContainer() {
        var data = new UcCampaignDataContainer();
        var result = GdUcCampaignService.extractDeviceTypesFromDeviceBidModifiers(data);
        assertThat(result, equalTo(Set.of(GdDeviceType.ALL)));
    }
}
