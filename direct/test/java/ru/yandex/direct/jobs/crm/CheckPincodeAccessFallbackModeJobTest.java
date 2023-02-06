package ru.yandex.direct.jobs.crm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.juggler.JugglerStatus;
import ru.yandex.direct.rbac.PpcRbac;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static ru.yandex.direct.core.entity.ppcproperty.model.PpcPropertyEnum.ENABLE_LIMITED_SUPPORT_READ_ALL_CLIENTS_META;

class CheckPincodeAccessFallbackModeJobTest {

    @Mock
    PpcRbac ppcRbac;

    @Mock
    PpcPropertiesSupport ppcPropertiesSupport;

    @Spy
    @InjectMocks
    CheckPincodeAccessFallbackModeJob job;

    @BeforeEach
    void setUp() {
        openMocks(this);
    }

    @Test
    void checkJugglerStatus_whenFallbackEnabled() {
        String meta = "test meta";
        String description = String.format("Fallback access mode for limited support enabled. Info: %s", meta);

        when(ppcRbac.canLimitedSupportReadAllClients()).thenReturn(true);
        when(ppcPropertiesSupport.get(ENABLE_LIMITED_SUPPORT_READ_ALL_CLIENTS_META.getName()))
                .thenReturn(meta);

        checkJugglerStatus(JugglerStatus.CRIT, description);
    }

    @Test
    void checkJugglerStatus_whenFallbackDisabled() {
        String description = "Fallback access mode for limited support disabled";

        when(ppcRbac.canLimitedSupportReadAllClients()).thenReturn(false);

        checkJugglerStatus(JugglerStatus.OK, description);
    }

    private void checkJugglerStatus(JugglerStatus status, String description) {
        job.execute();
        verify(job).setJugglerStatus(status, description);
    }
}
