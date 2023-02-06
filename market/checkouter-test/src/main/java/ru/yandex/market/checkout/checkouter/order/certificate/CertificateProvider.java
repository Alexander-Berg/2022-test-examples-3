package ru.yandex.market.checkout.checkouter.order.certificate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;

import ru.yandex.market.checkout.checkouter.order.VatType;

import static ru.yandex.market.checkout.test.providers.OrderItemProvider.DEFAULT_WARE_MD5;

/**
 * @author : poluektov
 * date: 12.09.2018.
 */
public final class CertificateProvider {

    private CertificateProvider() {
    }

    public static ExternalCertificateUser getDefaultBuyer() {
        ExternalCertificateUser buyer = new ExternalCertificateUser();

        buyer.setPhone("+79123456789");
        buyer.setEmail("mr.buyer@sputnik.ru");
        buyer.setMuid(1928374655L);
        buyer.setName("Имя Ихних О");

        return buyer;
    }

    public static ExternalCertificateUser getDefaultReceiver() {
        ExternalCertificateUser receiver = new ExternalCertificateUser();

        receiver.setMuid(9182736455L);
        receiver.setRegionId(2L);

        return receiver;
    }

    public static ExternalCertificate getDefaultCertificate() {
        ExternalCertificate request = new ExternalCertificate();
        request.setUniqueId(RandomStringUtils.randomAscii(15));
        request.setMsku(Long.toString(RandomUtils.nextLong()));
        request.setShopSku("|asfuiwgaeibsbvuw");
        request.setOfferName("PODAROCHNIY OFFER");
        request.setPrice(BigDecimal.valueOf(2555.90));
        request.setWarehouseId(1);
        request.setVat(VatType.VAT_18_118);
        request.setSupplierId(564738L);
        request.setWareMd5(DEFAULT_WARE_MD5);

        request.setReceiver(getDefaultReceiver());
        request.setBuyer(getDefaultBuyer());
        return request;
    }

    public static ExternalCertificatePayment getCertificatePayment(ExternalCertificate certificate) {
        ExternalCertificatePayment payment = new ExternalCertificatePayment();
        payment.setExternalPaymentId(RandomStringUtils.randomAscii(15));
        payment.setExternalPaymentDate(Instant.now().minus(1, ChronoUnit.DAYS));
        payment.setPrice(certificate.getPrice().add(BigDecimal.valueOf(0.1)));
        return payment;
    }
}
