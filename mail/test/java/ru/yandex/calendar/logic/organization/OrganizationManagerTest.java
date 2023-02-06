package ru.yandex.calendar.logic.organization;


import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.calendar.logic.beans.generated.Organization;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.random.Random2;

import static org.assertj.core.api.Assertions.assertThat;


public class OrganizationManagerTest extends AbstractConfTest {
    @Autowired
    private OrganizationDao organizationDao;
    @Autowired
    public OrganizationManager organizationManager;

    @Test
    public void getOrCreateOrganizationsByDirectoryIds() {
        Long newDirectoryId = Random2.R.nextLong();
        Long existingDirectoryId = Random2.R.nextLong();
        Organization existingOrganization = organizationDao
                .createOrganizationsByDirectoryIds(Cf.list(existingDirectoryId))
                .single();

        ListF<Organization> result =
                organizationManager.getOrCreateOrganizationsByDirectoryIds(Cf.list(newDirectoryId, existingDirectoryId));

        assertThat(result.size()).isEqualTo(2);

        Organization retrievedExistingOrganization =
                result.filter(o -> existingDirectoryId.equals(o.getDirectoryId())).single();
        Organization retrievedNewOrganization = result.filter(o -> newDirectoryId.equals(o.getDirectoryId())).single();
        assertThat(retrievedExistingOrganization.getId()).isEqualTo(existingOrganization.getId());

        ListF<Organization> newOrganizations = organizationDao.findOrganizationByDirectoryIds(Cf.list(newDirectoryId));
        assertThat(newOrganizations.size()).isEqualTo(1);
        assertThat(retrievedNewOrganization.getId()).isEqualTo(newOrganizations.single().getId());
    }
}
