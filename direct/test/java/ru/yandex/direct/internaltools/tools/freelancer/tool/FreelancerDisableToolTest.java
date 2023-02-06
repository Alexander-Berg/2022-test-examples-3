package ru.yandex.direct.internaltools.tools.freelancer.tool;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.freelancer.service.FreelancerRegisterService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.internaltools.core.exception.InternalToolValidationException;
import ru.yandex.direct.internaltools.tools.freelancer.model.FreelancerDisableParameters;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class FreelancerDisableToolTest {

    private static final long OPERATOR_UID = 2L;
    private static final long FREELANCER_ID = 1L;
    private FreelancerDisableTool freelancerDisableTool;
    private FreelancerRegisterService mockFreelancerRegisterService;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        Result mockedSuccessfulResult = mock(Result.class);
        when(mockedSuccessfulResult.isSuccessful()).thenReturn(true);

        mockFreelancerRegisterService = mock(FreelancerRegisterService.class);
        when(mockFreelancerRegisterService.disableFreelancer(anyLong(), anyLong()))
                .thenReturn(mockedSuccessfulResult);
        when(mockFreelancerRegisterService.enableFreelancer(anyLong(), anyLong()))
                .thenReturn(mockedSuccessfulResult);

        freelancerDisableTool = new FreelancerDisableTool(mockFreelancerRegisterService);
    }

    @Test
    public void disable_success() {
        FreelancerDisableParameters parameters = new FreelancerDisableParameters();
        parameters
                .setClientId(FREELANCER_ID)
                .setEnabled(false)
                .setOperator(new User().withUid(OPERATOR_UID));
        freelancerDisableTool.process(parameters);

        verify(mockFreelancerRegisterService).disableFreelancer(eq(FREELANCER_ID), eq(OPERATOR_UID));
        verifyNoMoreInteractions(mockFreelancerRegisterService);
    }

    @Test
    public void enable_success() {
        FreelancerDisableParameters parameters = new FreelancerDisableParameters();
        parameters
                .setClientId(FREELANCER_ID)
                .setEnabled(true)
                .setOperator(new User().withUid(OPERATOR_UID));
        freelancerDisableTool.process(parameters);

        verify(mockFreelancerRegisterService).enableFreelancer(eq(FREELANCER_ID), eq(OPERATOR_UID));
        verifyNoMoreInteractions(mockFreelancerRegisterService);
    }

    @Test
    public void disable_error() {
        FreelancerDisableParameters parameters = new FreelancerDisableParameters();
        parameters
                .setClientId(FREELANCER_ID)
                .setEnabled(false)
                .setOperator(new User().withUid(OPERATOR_UID));


        Result brokenResult = mock(Result.class);
        when(brokenResult.getValidationResult())
                .thenReturn(mock(ValidationResult.class));
        //noinspection unchecked
        when(mockFreelancerRegisterService.disableFreelancer(anyLong(), anyLong()))
                .thenReturn(brokenResult);

        assertThatThrownBy(() -> freelancerDisableTool.process(parameters))
                .isInstanceOf(InternalToolValidationException.class);
    }
}
