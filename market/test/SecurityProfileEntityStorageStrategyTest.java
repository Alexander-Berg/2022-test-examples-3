package ru.yandex.market.jmf.logic.def.test;

import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.jmf.configuration.api.ConfigurationService;
import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.entity.test.assertions.EntityCollectionAssert;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.MetadataService;
import ru.yandex.market.jmf.security.impl.SecurityProfileEntityStorageStrategy;

@SpringJUnitConfig(classes = InternalLogicDefaultTestConfiguration.class)
public class SecurityProfileEntityStorageStrategyTest {

    @Inject
    private SecurityProfileEntityStorageStrategy entityStorageStrategy;
    @Inject
    private EntityStorageService entityStorageService;
    @Inject
    private MetadataService metadataService;
    @Inject
    private ConfigurationService configurationService;

    private Boolean useNewSecurityValue;

    @BeforeEach
    public void setUp() {
        this.useNewSecurityValue = configurationService.getValue("useNewSecurity");
    }

    @AfterEach
    public void tearDown() {
        configurationService.setValue("useNewSecurity", useNewSecurityValue);
    }

    @ParameterizedTest
    @CsvSource({
            "true",
            "false"
    })
    public void getAllTest(boolean useNewSecurity) {
        configurationService.setValue("useNewSecurity", useNewSecurity);

        var securityProfileTest = metadataService.getMetaclassOrError(Fqn.of("securityProfileTest"));
        var securityProfiles = entityStorageService.list(
                Query.of(Fqn.of("securityProfile"))
                        .withFilters(Filters.eq(
                                "definedFor",
                                securityProfileTest
                        ))
        );

        Set<String> expectedProfiles = useNewSecurity
                ? Set.of("@default", "profileWithRelativeRole", "hardcodedProfile")
                : Set.of("profileWithRelativeRole", "hardcodedProfile");
        int expectedProfilesCount = expectedProfiles.size();

        EntityCollectionAssert.assertThat(securityProfiles)
                .hasSize(expectedProfilesCount)
                .allMatch(x -> expectedProfiles.contains(x.getGid()));
    }

    @Test
    public void possibilityTest() {
        var securityProfile = metadataService.getMetaclassOrError(Fqn.of("securityProfile"));
        var attachment = metadataService.getMetaclassOrError(Fqn.of("attachment"));
        var bo = metadataService.getMetaclassOrError(Fqn.of("bo"));

        Assertions.assertTrue(entityStorageStrategy.isPossible(securityProfile));
        Assertions.assertFalse(entityStorageStrategy.isPossible(attachment));
        Assertions.assertFalse(entityStorageStrategy.isPossible(bo));
    }
}
