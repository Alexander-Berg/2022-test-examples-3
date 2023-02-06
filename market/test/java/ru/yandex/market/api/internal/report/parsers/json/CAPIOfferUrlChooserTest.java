package ru.yandex.market.api.internal.report.parsers.json;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.sec.client.internal.TariffRepository;
import ru.yandex.market.api.server.sec.client.internal.Tariffs;

/**
 * @author dimkarp93
 */
public class CAPIOfferUrlChooserTest extends UnitTestBase {
    @Test
    public void vendorWithoutCPConCPALinkAndNoRawUrl_useCartLink() {
        Client cl = new Client();
        cl.setTariff(TariffRepository.get(Tariffs.BASE_VENDOR));
        cl.setShowShopUrl(false);
        CAPIOfferUrlChooser chooser = new CAPIOfferUrlChooser(false, () -> cl);

        Assert.assertEquals("c", chooser.choose("a", "b", "c", true));
    }

    @Test
    public void vendorWithCPConCPALinkAndNoRawUrl_useEncryptedLink() {
        Client cl = new Client();
        cl.setTariff(TariffRepository.get(Tariffs.BASE_VENDOR));
        cl.setShowShopUrl(true);
        CAPIOfferUrlChooser chooser = new CAPIOfferUrlChooser(false, () -> cl);

        Assert.assertEquals("a", chooser.choose("a", "b", "c", true));
    }

    @Test
    public void vendorWithoutCPConCPALinkAndRawUrl_useDirectLink() {
        Client cl = new Client();
        cl.setTariff(TariffRepository.get(Tariffs.BASE_VENDOR));
        cl.setShowShopUrl(false);
        CAPIOfferUrlChooser chooser = new CAPIOfferUrlChooser(true, () -> cl);

        Assert.assertEquals("b", chooser.choose("a", "b", "c", true));
    }


    @Test
    public void vendorWithCPConCPALinkAndRawUrl_useDirectLink() {
        Client cl = new Client();
        cl.setTariff(TariffRepository.get(Tariffs.BASE_VENDOR));
        cl.setShowShopUrl(true);
        CAPIOfferUrlChooser chooser = new CAPIOfferUrlChooser(true, () -> cl);

        Assert.assertEquals("b", chooser.choose("a", "b", "c", true));
    }

    @Test
    public void vendorAndCpcOfferAndRawUrl_useDirectLink() {
        Client cl = new Client();
        cl.setTariff(TariffRepository.get(Tariffs.BASE_VENDOR));
        cl.setShowShopUrl(true);
        CAPIOfferUrlChooser chooser = new CAPIOfferUrlChooser(true, () -> cl);

        Assert.assertEquals("b", chooser.choose("a", "b", "c", false));
    }

    @Test
    public void vendorAndCpcOfferAndNoRawUrl_useDirectLink() {
        Client cl = new Client();
        cl.setTariff(TariffRepository.get(Tariffs.BASE_VENDOR));
        cl.setShowShopUrl(true);
        CAPIOfferUrlChooser chooser = new CAPIOfferUrlChooser(false, () -> cl);

        Assert.assertEquals("a", chooser.choose("a", "b", "c", false));
    }

    @Test
    public void usualPartnerAndRawUrl_useDirectLink() {
        Client cl = new Client();
        CAPIOfferUrlChooser chooser = new CAPIOfferUrlChooser(true, () -> cl);

        Assert.assertEquals("b", chooser.choose("a", "b", "c", false));
    }

    @Test
    public void usualPartnerOfferAndNoRawUrl_useDirectLink() {
        Client cl = new Client();
        CAPIOfferUrlChooser chooser = new CAPIOfferUrlChooser(false, () -> cl);

        Assert.assertEquals("a", chooser.choose("a", "b", "c", false));
    }
}
