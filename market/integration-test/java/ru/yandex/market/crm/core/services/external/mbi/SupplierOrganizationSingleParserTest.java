package ru.yandex.market.crm.core.services.external.mbi;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

public class SupplierOrganizationSingleParserTest {
    private SupplierOrganizationSingleParser parser = new SupplierOrganizationSingleParser();

    private String oneSupplier = "[\n" +
            "  {\n" +
            "    \"prepayRequestId\": \"23628\",\n" +
            "    \"supplierId\": \"10265019\",\n" +
            "    \"type\": \"1\",\n" +
            "    \"ogrn\": \"1107746275900\",\n" +
            "    \"name\": \"ООО \\\"БлуРнпнСапплаер\\\"\",\n" +
            "    \"juridicalAddress\": \"127081, г. Москва, улица Чермянская, дом 3, строение 2, помещение 3\",\n" +
            "    \"factAddress\": \"127081, г. Москва, улица Чермянская, дом 3, строение 2, помещение 3.a\",\n" +
            "    \"supplierName\": \"BlueRnpn\",\n" +
            "    \"supplierDomain\": \"bluernpn.ru\",\n" +
            "    \"createdAt\": \"2018-04-26 14:10:25.0\",\n" +
            "    \"regnumName\": \"ОГРН\",\n" +
            "    \"contactPhone\": \"+7 4968775522\",\n" +
            "    \"shopPhoneNumber\": \"+7 4957897567\",\n" +
            "    \"inn\": \"7715805253\"\n" +
            "  }\n" +
            "]";

    private String fewSuppliers = "[\n" +
            "  {\n" +
            "    \"prepayRequestId\": \"23628\",\n" +
            "    \"supplierId\": \"10265019\",\n" +
            "    \"type\": \"1\",\n" +
            "    \"ogrn\": \"1107746275900\",\n" +
            "    \"name\": \"ООО \\\"БлуРнпнСапплаер\\\"\",\n" +
            "    \"juridicalAddress\": \"127081, г. Москва, улица Чермянская, дом 3, строение 2, помещение 3\",\n" +
            "    \"factAddress\": \"127081, г. Москва, улица Чермянская, дом 3, строение 2, помещение 3.a\",\n" +
            "    \"supplierName\": \"BlueRnpn\",\n" +
            "    \"supplierDomain\": \"bluernpn.ru\",\n" +
            "    \"createdAt\": \"2018-04-26 14:10:25.0\",\n" +
            "    \"regnumName\": \"ОГРН\",\n" +
            "    \"contactPhone\": \"+7 4968775522\",\n" +
            "    \"shopPhoneNumber\": \"+7 4957897567\",\n" +
            "    \"inn\": \"7715805253\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"prepayRequestId\": \"23628\",\n" +
            "    \"supplierId\": \"10265019\",\n" +
            "    \"type\": \"1\",\n" +
            "    \"ogrn\": \"1107746275911\",\n" +
            "    \"name\": \"ООО \\\"БлуРнпнСапплаер!!!!!\\\"\",\n" +
            "    \"juridicalAddress\": \"127081, г. !Москва, улица Чермянская, дом 3, строение 2, помещение 3\",\n" +
            "    \"factAddress\": \"127081, г. Москва!, улица Чермянская, дом 3, строение 2, помещение 3.a\",\n" +
            "    \"supplierName\": \"BlueRnpn\",\n" +
            "    \"supplierDomain\": \"bluernpn.ru\",\n" +
            "    \"createdAt\": \"2018-04-26 14:10:25.0\",\n" +
            "    \"regnumName\": \"ОГРН\",\n" +
            "    \"contactPhone\": \"+7 4968775555\",\n" +
            "    \"shopPhoneNumber\": \"+7 4957897567\",\n" +
            "    \"inn\": \"7715805253\"\n" +
            "  }\n" +
            "]";

    private String noSuppliers = "[]";

    @Test
    public void testFewSuppliers() throws IOException {
        byte[] bytes = fewSuppliers.getBytes();
        Organization organization = parser.parse(bytes);

        Organization expectedOrg = new Organization();
        expectedOrg.setOgrn("1107746275900");
        expectedOrg.setAddress("127081, г. Москва, улица Чермянская, дом 3, строение 2, помещение 3");
        expectedOrg.setPostalAddress("127081, г. Москва, улица Чермянская, дом 3, строение 2, помещение 3.a");
        expectedOrg.setName("ООО \"БлуРнпнСапплаер\"");
        expectedOrg.setContactPhone("+7 4968775522");
        Assert.assertTrue(equals(expectedOrg, organization));
    }

    @Test
    public void testNoSuppliers() throws IOException {
        byte[] bytes = noSuppliers.getBytes();
        Organization organization = parser.parse(bytes);

        Assert.assertNull(organization);
    }

    @Test
    public void testOneSupplier() throws IOException {
        byte[] bytes = oneSupplier.getBytes();
        Organization organization = parser.parse(bytes);

        Organization expectedOrg = new Organization();
        expectedOrg.setOgrn("1107746275900");
        expectedOrg.setAddress("127081, г. Москва, улица Чермянская, дом 3, строение 2, помещение 3");
        expectedOrg.setPostalAddress("127081, г. Москва, улица Чермянская, дом 3, строение 2, помещение 3.a");
        expectedOrg.setName("ООО \"БлуРнпнСапплаер\"");
        expectedOrg.setContactPhone("+7 4968775522");
        Assert.assertTrue(equals(expectedOrg, organization));
    }

    private boolean equals(Organization organization1, Organization organization2) {
        return organization1.getName().equals(organization2.getName()) &&
                organization1.getOgrn().equals(organization2.getOgrn()) &&
                organization1.getContactPhone().equals(organization2.getContactPhone()) &&
                organization1.getAddress().equals(organization2.getAddress()) &&
                organization1.getPostalAddress().equals(organization2.getPostalAddress());
    }
}