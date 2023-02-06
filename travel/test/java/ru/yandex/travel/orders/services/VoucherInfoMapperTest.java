package ru.yandex.travel.orders.services;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ru.yandex.travel.orders.commons.proto.ESnippet;
import ru.yandex.travel.orders.entities.Voucher;

import static org.assertj.core.api.Assertions.assertThat;

public class VoucherInfoMapperTest {
    public void testGetVouchersInfoFor() {
        var voucher = new Voucher();
        var voucherId = UUID.randomUUID();
        voucher.setId(voucherId);
        var voucherName = "document.pdf";
        voucher.setVoucherName(voucherName);
        var voucherUrl = "http://secret-url.example.com/";
        voucher.setVoucherUrl(voucherUrl);

        var snippets = List.of(ESnippet.S_PRIVATE_INFO);
        List<ESnippet> emptySnippets = new ArrayList<>();

        var voucherMapper = new VoucherInfoMapper();
        var publicdata = voucherMapper.getVouchersInfoFor(List.of(voucher), emptySnippets).get(0);
        var privatedata = voucherMapper.getVouchersInfoFor(List.of(voucher), snippets).get(0);

        assertThat(publicdata).isNotEqualTo(privatedata);

        assertThat(publicdata.getUrl()).isEmpty();

        assertThat(privatedata.getUrl()).isEqualTo(voucherUrl);

        assertThat(publicdata.getId()).isEqualTo(privatedata.getId()).isEqualTo(voucherId.toString());
        assertThat(publicdata.getName()).isEqualTo(privatedata.getName()).isEqualTo(voucherName);
    }
}
