package ru.yandex.direct.core.testing.data;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.math.RandomUtils;

import ru.yandex.direct.core.entity.organization.model.BannerPermalink;
import ru.yandex.direct.core.entity.organization.model.Organization;
import ru.yandex.direct.core.entity.organization.model.OrganizationStatusPublish;
import ru.yandex.direct.core.entity.organization.model.PermalinkAssignType;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.organizations.swagger.OrganizationApiInfo;

import static java.util.Collections.emptyList;
import static ru.yandex.direct.core.entity.organization.model.OrganizationStatusPublish.PUBLISHED;
import static ru.yandex.direct.core.entity.organization.model.OrganizationStatusPublish.UNKNOWN;

@ParametersAreNonnullByDefault
public class TestOrganizations {

    public static Organization defaultActiveOrganization(ClientId clientId) {
        return organization(clientId, PUBLISHED);
    }

    public static Organization defaultOrganization(ClientId clientId) {
        return organization(clientId, UNKNOWN);
    }

    private static Organization organization(ClientId clientId, OrganizationStatusPublish statusPublish) {
        return new Organization()
                .withClientId(clientId)
                .withPermalinkId(RandomUtils.nextLong())
                .withStatusPublish(statusPublish);
    }

    public static Organization copyOrganization(Organization organization) {
        return new Organization()
                .withClientId(organization.getClientId())
                .withPermalinkId(organization.getPermalinkId())
                .withChainId(organization.getChainId())
                .withStatusPublish(organization.getStatusPublish());
    }

    public static BannerPermalink createBannerPermalink(Long permalink, PermalinkAssignType assignType,
                                                        boolean isRejected) {
        return createBannerPermalink(permalink, assignType, isRejected, false);
    }

    public static BannerPermalink createBannerPermalink(Long permalink, PermalinkAssignType assignType,
                                                        boolean isRejected, boolean preferVCard) {
        return new BannerPermalink()
                .withPermalinkId(permalink)
                .withPermalinkAssignType(assignType)
                .withIsChangeToManualRejected(isRejected)
                .withPreferVCardOverPermalink(preferVCard);
    }

    public static OrganizationApiInfo defaultOrganizationApiInfo(Long permalinkId) {
        return organizationApiInfo(permalinkId);
    }

    private static OrganizationApiInfo organizationApiInfo(Long permalinkId) {
        OrganizationApiInfo organizationApiInfo = new OrganizationApiInfo();
        organizationApiInfo
                .withWorkIntervals(emptyList())
                .withPhones(emptyList())
                .withUrls(emptyList())
                .withCompanyName("---")
                .withPermalinkId(permalinkId)
                .withStatusPublish(UNKNOWN);
        return organizationApiInfo;
    }
}
