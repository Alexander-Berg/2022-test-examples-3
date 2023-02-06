package ru.yandex.direct.core.entity.campaign.service.validation.type.bean;

import java.util.Collections;
import java.util.List;

import com.google.common.net.InetAddresses;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.direct.common.net.NetAcl;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MAX_DISABLED_IPS_COUNT;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.invalidIpFormat;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.ipCantBeFromInternalNetwork;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.ipCantBeFromPrivateNetwork;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.maxCollectionSize;
import static ru.yandex.direct.validation.defect.CollectionDefects.notEmptyCollection;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class CommonCampaignBeanDisabledIpsValidatorTest {

    private static final String INTERNAL_IP = "12.12.12.12";
    private static final String PRIVATE_IP = "192.168.1.1";
    private NetAcl netAcl;

    @Before
    public void fillNetAcl() {
        netAcl = Mockito.mock(NetAcl.class);
        Mockito.when(netAcl.isInternalIp(InetAddresses.forString(INTERNAL_IP))).thenReturn(true);
    }

    @Test
    public void testIpIsCorrect() {
        ValidationResult<List<String>, Defect> vr = validateIps(List.of("209.85.233.94"));
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void testIpIsCorrect_OneZero() {
        ValidationResult<List<String>, Defect> vr = validateIps(List.of("209.85.233.0"));
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void testIpIsCorrect_TwoZero() {
        ValidationResult<List<String>, Defect> vr = validateIps(List.of("209.85.00.94"));
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void testIpIsCorrect_ThreeZero() {
        ValidationResult<List<String>, Defect> vr = validateIps(List.of("209.000.233.94"));
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void testIpIsInvalid_FourZero() {
        String invalidIp = "0000.85.233.94";
        ValidationResult<List<String>, Defect> vr = validateIps(List.of(invalidIp));
        assertThat(vr, hasDefectWithDefinition(validationError(path(index(0)), invalidIpFormat(invalidIp))));
    }

    @Test
    public void testIpIsInvalid_LeadingZero() {
        String invalidIp = "209.85.233.094";
        ValidationResult<List<String>, Defect> vr = validateIps(List.of(invalidIp));
        assertThat(vr, hasDefectWithDefinition(validationError(path(index(0)), invalidIpFormat(invalidIp))));
    }

    @Test
    public void testIpIsInvalid_FiveGroups() {
        String invalidIp = "1.2.3.4.5";
        ValidationResult<List<String>, Defect> vr = validateIps(List.of(invalidIp));
        assertThat(vr, hasDefectWithDefinition(validationError(path(index(0)), invalidIpFormat(invalidIp))));
    }

    @Test
    public void testIpIsInvalid_ThreeGroups() {
        String invalidIp = "1.2.3";
        ValidationResult<List<String>, Defect> vr = validateIps(List.of(invalidIp));
        assertThat(vr, hasDefectWithDefinition(validationError(path(index(0)), invalidIpFormat(invalidIp))));
    }

    @Test
    public void testIpIsInvalid_UnknownSymbol() {
        String invalidIp = "1.2.3.a";
        ValidationResult<List<String>, Defect> vr = validateIps(List.of(invalidIp));
        assertThat(vr, hasDefectWithDefinition(validationError(path(index(0)), invalidIpFormat(invalidIp))));
    }

    @Test
    public void testMoreThanMaxListSize() {
        ValidationResult<List<String>, Defect> vr = validateIps(
                Collections.nCopies(MAX_DISABLED_IPS_COUNT + 1, "192.168.1.1"));
        assertThat(vr, hasDefectWithDefinition(validationError(path(), maxCollectionSize(MAX_DISABLED_IPS_COUNT))));
    }

    @Test
    public void testIpIsNotValid() {
        String invalidIp = "hello";
        ValidationResult<List<String>, Defect> vr = validateIps(List.of(invalidIp));
        assertThat(vr, hasDefectWithDefinition(validationError(path(index(0)), invalidIpFormat(invalidIp))));
    }

    @Test
    public void testIpIsPrivate() {
        ValidationResult<List<String>, Defect> vr = validateIps(List.of(PRIVATE_IP));
        assertThat(vr, hasDefectWithDefinition(validationError(path(index(0)),
                ipCantBeFromPrivateNetwork(PRIVATE_IP))));
    }

    @Test
    public void testInternal() {
        ValidationResult<List<String>, Defect> vr = validateIps(List.of(INTERNAL_IP));
        assertThat(vr, hasDefectWithDefinition(validationError(path(index(0)), ipCantBeFromInternalNetwork(INTERNAL_IP))));
    }

    @Test
    public void testEmpty() {
        ValidationResult<List<String>, Defect> vr = validateIps(emptyList());
        assertThat(vr, hasDefectWithDefinition(validationError(path(), notEmptyCollection())));
    }

    private ValidationResult<List<String>, Defect> validateIps(List<String> ips) {
        return CommonCampaignBeanValidator.validateDisabledIps(ips, netAcl);
    }
}
