package ru.yandex.market.mbisfintegration.salesforce;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.mbisfintegration.core.error.InternalException;
import ru.yandex.market.mbisfintegration.generated.sf.model.SObject;
import ru.yandex.market.mbisfintegration.generated.sf.model.SaveResult;
import ru.yandex.market.mbisfintegration.generated.sf.model.Soap;
import ru.yandex.market.mbisfintegration.generated.sf.model.UpsertResult;
import ru.yandex.market.mbisfintegration.salesforce.impl.AbstractSfService;

import static java.util.Collections.nCopies;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.mbisfintegration.salesforce.SfService.MODIFY_OBJECTS_PER_CALL_LIMIT;
import static ru.yandex.market.mbisfintegration.salesforce.SfService.RETRIEVE_IDS_PER_CALL_LIMIT;
import static ru.yandex.market.mbisfintegration.salesforce.SfServiceTest.SfServiceStub.FIELDS;

public class SfServiceTest {
    private static final String ID = "theId";
    private static final String FAIL_ID = "theFailId";

    private static final SObject S_OBJECT = new SObject().withId(ID);

    @Mock
    SoapHolder soapHolder;

    @Mock
    Soap soap;

    SfService<SObject> sfService;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        Mockito.when(soap.retrieve(anyString(), anyString(), anyList()))
                .thenReturn(List.of(S_OBJECT));
        Mockito.when(soapHolder.getSoap()).thenReturn(soap);
        sfService = new SfServiceStub(soapHolder);
    }

    @Test
    void retrieveSingleTest() throws Exception {
        assertThat(sfService.retrieve(ID)).contains(S_OBJECT);
        verify(soap, times(1)).retrieve(FIELDS, SObjectType.LEAD.getId(), List.of(ID));
    }

    @Test
    void retrieveMultipleTest() throws Exception {
        assertThat(sfService.retrieve(List.of(ID))).containsExactly(S_OBJECT);
        verify(soap, times(1)).retrieve(FIELDS, SObjectType.LEAD.getId(), List.of(ID));
    }

    @Test
    void upsertTest() throws Exception {
        mockSuccessSave();
        assertThat(sfService.upsert(ID, List.of(S_OBJECT))).hasSize(1).containsExactly(ID);
        verify(soap, times(1)).upsert(ID, List.of(S_OBJECT));

        mockFailedSave();
        assertThatExceptionOfType(InternalException.class)
                .isThrownBy(() -> sfService.upsert(ID, List.of(S_OBJECT)))
                .withMessage("Failed to upsert Lead(s)");
    }

    @Test
    void updateTest() throws Exception {
        mockSuccessSave();
        assertThat(sfService.update(List.of(S_OBJECT))).hasSize(1).containsExactly(ID);
        verify(soap, times(1)).update(List.of(S_OBJECT));

        mockFailedSave();
        assertThatExceptionOfType(InternalException.class)
                .isThrownBy(() -> sfService.update(List.of(S_OBJECT)))
                .withMessage("Failed to save (create/update) Lead(s)");
    }

    @Test
    void createTest() throws Exception {
        mockSuccessSave();
        assertThat(sfService.create(List.of(S_OBJECT))).hasSize(1).containsExactly(ID);
        verify(soap, times(1)).create(List.of(S_OBJECT));

        mockFailedSave();
        assertThatExceptionOfType(InternalException.class)
                .isThrownBy(() -> sfService.create(List.of(S_OBJECT)))
                .withMessage("Failed to save (create/update) Lead(s)");
    }

    @Test
    void shouldCheckPerCallLimits() throws Exception {
        var ids = nCopies(RETRIEVE_IDS_PER_CALL_LIMIT + 1, ID);
        assertThatExceptionOfType(SalesForceException.class).isThrownBy(() -> sfService.retrieve(ids));
        var sObjects = nCopies(MODIFY_OBJECTS_PER_CALL_LIMIT + 1, S_OBJECT);
        assertThatExceptionOfType(SalesForceException.class).isThrownBy(() -> sfService.upsert(FIELDS, sObjects));
        assertThatExceptionOfType(SalesForceException.class).isThrownBy(() -> sfService.update(sObjects));
        assertThatExceptionOfType(SalesForceException.class).isThrownBy(() -> sfService.create(sObjects));
        verify(soap, times(0)).retrieve(anyString(), anyString(), anyList());
        verify(soap, times(0)).upsert(anyString(), anyList());
        verify(soap, times(0)).update(anyList());
        verify(soap, times(0)).create(anyList());
    }

    private void mockSuccessSave() throws Exception {
        Mockito.when(soap.upsert(anyString(), anyList()))
                .thenReturn(List.of(new UpsertResult().withSuccess(true).withId(ID)));
        Mockito.when(soap.update(anyList()))
                .thenReturn(List.of(new SaveResult().withSuccess(true).withId(ID)));
        Mockito.when(soap.create(anyList()))
                .thenReturn(List.of(new SaveResult().withSuccess(true).withId(ID)));
    }

    private void mockFailedSave() throws Exception {
        Mockito.when(soap.upsert(anyString(), anyList()))
                .thenReturn(List.of(
                        new UpsertResult().withSuccess(true).withId(ID),
                        new UpsertResult().withSuccess(false).withId(FAIL_ID)
                ));
        Mockito.when(soap.update(anyList()))
                .thenReturn(List.of(
                        new SaveResult().withSuccess(true).withId(ID),
                        new SaveResult().withSuccess(false).withId(FAIL_ID)
                ));
        Mockito.when(soap.create(anyList()))
                .thenReturn(List.of(
                        new SaveResult().withSuccess(true).withId(ID),
                        new SaveResult().withSuccess(false).withId(FAIL_ID)
                ));
    }

    public static class SfServiceStub extends AbstractSfService<SObject> {

        public static final String FIELDS = "Id";

        protected SfServiceStub(SoapHolder soapHolder) {
            super(soapHolder);
        }

        @Override
        public SObjectType getSObjectType() {
            return SObjectType.LEAD;
        }

        @Override
        protected String getSObjectFields() {
            return FIELDS;
        }
    }
}