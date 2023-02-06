package ru.yandex.calendar.logic.organization;


import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.calendar.logic.beans.GenericBeanDao;
import ru.yandex.calendar.logic.beans.generated.Organization;
import ru.yandex.calendar.logic.beans.generated.OrganizationHelper;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.random.Random2;

import static org.assertj.core.api.Assertions.assertThat;


public class OrganizationDaoTest extends AbstractConfTest {
    @Autowired
    private OrganizationDao organizationDao;
    @Autowired
    private GenericBeanDao genericBeanDao;

    @Test
    public void createOrganizationsByDirectoryIdsForNewOrganization() {
        long directoryId = Random2.R.nextLong();

        ListF<Organization> result = organizationDao.createOrganizationsByDirectoryIds(Cf.list(directoryId));

        assertThat(result.size()).isEqualTo(1);
        Organization mappedOrganization = result.first();
        assertThat(mappedOrganization.getDirectoryId()).isEqualTo(directoryId);
        Organization actualOrganization = genericBeanDao.loadBeanById(
                OrganizationHelper.INSTANCE,
                result.first().getId());
        assertThat(mappedOrganization.getId()).isEqualTo(actualOrganization.getId());
    }

    @Test
    public void createOrganizationsByDirectoryIdsForExistingOrganization() {
        long directoryId = Random2.R.nextLong();
        Organization existingOrganization = new Organization();
        existingOrganization.setDirectoryId(directoryId);
        genericBeanDao.insertBean(existingOrganization);

        ListF<Organization> result = organizationDao.createOrganizationsByDirectoryIds(Cf.list(directoryId));

        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void createOrganizationsByDirectoryIdsForSeveralOrganizations() {
        ListF<Long> newDirectoryIds = Cf.repeat(Random2.R::nextLong, 3);
        ListF<Long> existingDirectoryIds = Cf.repeat(Random2.R::nextLong, 3);
        existingDirectoryIds.forEach(
                directoryId -> {
                    Organization existingOrganization = new Organization();
                    existingOrganization.setDirectoryId(directoryId);
                    genericBeanDao.insertBean(existingOrganization);
                });

        ListF<Organization> result = organizationDao.createOrganizationsByDirectoryIds(
                newDirectoryIds.plus(existingDirectoryIds));

        assertThat(result.size()).isEqualTo(newDirectoryIds.size());
        ListF<Long> actualOrganizationDirectoryIds = genericBeanDao.loadBeansById(
                OrganizationHelper.INSTANCE,
                result.map(Organization::getId))
                .sortedBy(Organization::getId)
                .map(Organization::getDirectoryId);
        assertThat(result.sortedBy(Organization::getId).map(Organization::getDirectoryId))
                .isEqualTo(actualOrganizationDirectoryIds);
    }
}
