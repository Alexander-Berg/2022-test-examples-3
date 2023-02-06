package ru.yandex.market.billing.imports.partner;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import ru.yandex.market.core.orginfo.model.OrganizationType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тест для {@link PartnerLegalInfoYtDao}
 */
class PartnerLegalInfoYtDaoTest {
    private static final PartnerLegalInfo PARTNER_LEGAL_INFO = PartnerLegalInfo.builder()
            .setDatasourceId(1L)
            .setRequestId(3L)
            .setOrganizationType(OrganizationType.IP)
            .setInn("123456789qwerty")
            .setAccountNum("1234567890123456")
            .setBik("BIKXXXX")
            .setOrgName("OrgName")
            .setSellerClientId(2L)
            .setUpdatedAt(LocalDateTime.of(2021, 9, 9, 9, 9, 9))
            .setOgrn("OGRN")
            .setJurAddress("JurAddress 1B")
            .setCorrAccNum("1234567890123456")
            .setKpp("KPP")
            .setStartDate(LocalDateTime.of(2021, 8, 8, 8, 8, 8, 8000))
            .build();

    @Test
    void test() throws SQLException {
        ResultSet rs = prepareResultSet();
        PartnerLegalInfo partnerLegalInfo = PartnerLegalInfoYtDao.ROW_MAPPER.mapRow(rs, 1);
        assertEquals(PARTNER_LEGAL_INFO.getDatasourceId(), partnerLegalInfo.getDatasourceId());
        assertEquals(PARTNER_LEGAL_INFO.getRequestId(), partnerLegalInfo.getRequestId());
        assertEquals(PARTNER_LEGAL_INFO.getOrganizationType(), partnerLegalInfo.getOrganizationType());
        assertEquals(PARTNER_LEGAL_INFO.getInn(), partnerLegalInfo.getInn());
        assertEquals(PARTNER_LEGAL_INFO.getAccountNum(), partnerLegalInfo.getAccountNum());
        assertEquals(PARTNER_LEGAL_INFO.getBik(), partnerLegalInfo.getBik());
        assertEquals(PARTNER_LEGAL_INFO.getOrgName(), partnerLegalInfo.getOrgName());
        assertEquals(PARTNER_LEGAL_INFO.getSellerClientId(), partnerLegalInfo.getSellerClientId());
        assertEquals(PARTNER_LEGAL_INFO.getUpdatedAt(), partnerLegalInfo.getUpdatedAt());
        assertEquals(PARTNER_LEGAL_INFO.getOgrn(), partnerLegalInfo.getOgrn());
        assertEquals(PARTNER_LEGAL_INFO.getJurAddress(), partnerLegalInfo.getJurAddress());
        assertEquals(PARTNER_LEGAL_INFO.getCorrAccNum(), partnerLegalInfo.getCorrAccNum());
        assertEquals(PARTNER_LEGAL_INFO.getKpp(), partnerLegalInfo.getKpp());
        assertEquals(PARTNER_LEGAL_INFO.getStartDate(), partnerLegalInfo.getStartDate());
    }

    private ResultSet prepareResultSet() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("datasource_id")).thenReturn("1");
        when(rs.getLong("request_id")).thenReturn(3L);
        when(rs.getInt("org_type")).thenReturn(4);
        when(rs.getString("inn")).thenReturn("123456789qwerty");
        when(rs.getString("account_num")).thenReturn("1234567890123456");
        when(rs.getString("bik")).thenReturn("BIKXXXX");
        when(rs.getString("org_name")).thenReturn("OrgName");
        when(rs.getLong("seller_client_id")).thenReturn(2L);
        when(rs.getString("updated_at")).thenReturn("2021-09-09 09:09:09");
        when(rs.getString("ogrn")).thenReturn("OGRN");
        when(rs.getString("jur_address")).thenReturn("JurAddress 1B");
        when(rs.getString("corr_acc_num")).thenReturn("1234567890123456");
        when(rs.getString("kpp")).thenReturn("KPP");
        when(rs.getString("start_date")).thenReturn("2021-08-08 08:08:08.000008");

        return rs;
    }
}
