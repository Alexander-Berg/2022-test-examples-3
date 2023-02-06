package ru.yandex.market.crm.platform.mappers;

import com.google.common.collect.Iterables;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.crm.platform.models.ExternalCertificate;
import ru.yandex.market.crm.util.ResourceHelpers;

public class ExternalCertificateMapperTest {

    ExternalCertificate result;

    @Test
    public void buyerEmail() {
        Assert.assertEquals("mr.buyer@sputnik.ru", result.getBuyer().getEmail());
    }

    @Test
    public void buyerMuid() {
        Assert.assertEquals(1928374655l, result.getBuyer().getMuid());
    }

    @Test
    public void buyerName() {
        Assert.assertEquals("Имя Ихних О", result.getBuyer().getName());
    }

    @Test
    public void buyerPhone() {
        Assert.assertEquals("+79123456789", result.getBuyer().getPhone());
    }

    @Test
    public void createdAt() {
        Assert.assertEquals(1542973940364l, result.getCreatedAt());
    }

    @Test
    public void expiredAt() {
        Assert.assertEquals(1543060340364l, result.getExpiryAt());
    }

    @Test
    public void id() {
        Assert.assertEquals(1, result.getId());
    }

    @Test
    public void msku() {
        Assert.assertEquals(5849164778757068800l, result.getMsku());
    }

    @Test
    public void offerName() {
        Assert.assertEquals("PODAROCHNIY OFFER", result.getOfferName());
    }

    @Test
    public void price() {
        Assert.assertEquals(255590l, result.getPrice());
    }

    @Test
    public void receiptId() {
        Assert.assertEquals(310536l, result.getReceiptId());
    }

    @Before
    public void setUp() {
        byte[] resource = ResourceHelpers.getResource("ExternalCertificate.json");
        result = Iterables.getFirst(new ExternalCertificateMapper().apply(resource), null);
    }

    @Test
    public void shopSku() {
        Assert.assertEquals("|asfuiwgaeibsbvuw", result.getShopSku());
    }

    @Test
    public void status() {
        Assert.assertEquals(ExternalCertificate.Status.RESERVED, result.getStatus());
    }

    @Test
    public void supplierId() {
        Assert.assertEquals(564738l, result.getSupplierId());
    }

    @Test
    public void uniqueId() {
        Assert.assertEquals(" [> J?xL~oS+xgM", result.getUniqueId());
    }

    @Test
    public void updatedAt() {
        Assert.assertEquals(1542973940365l, result.getUpdatedAt());
    }

    @Test
    public void wareMd5() {
        Assert.assertEquals("-_40VqaS9BpXO1qaTtweBA", result.getWareMd5());
    }

    @Test
    public void warehouseId() {
        Assert.assertEquals(1, result.getWarehouseId());
    }
}
