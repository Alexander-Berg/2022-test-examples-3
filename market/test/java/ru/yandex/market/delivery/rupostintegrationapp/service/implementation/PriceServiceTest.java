package ru.yandex.market.delivery.rupostintegrationapp.service.implementation;


import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.delivery.rupostintegrationapp.exception.RussianPostIntegrationAppException;
import ru.yandex.market.delivery.russianposttracker.RussianPostOperationHistoryClient;
import ru.yandex.market.delivery.russianposttracker.russianpostsingletracking.wsdl.FinanceParameters;
import ru.yandex.market.delivery.russianposttracker.russianpostsingletracking.wsdl.OperationHistoryRecord;
import ru.yandex.market.delivery.russianposttracker.russianpostsingletracking.wsdl.OperationParameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PriceServiceTest {
    private static final String SHIPMENT_IDENTIFIER = "SOME123CODE";
    @Mock
    private RussianPostOperationHistoryClient client;

    private PriceService service;

    @BeforeEach
    void init() {
        service = new PriceService(client, null);
    }

    @Test
    public void getPriceDifferentRecords() throws DatatypeConfigurationException {
        when(client.getOrderHistory(Mockito.anyString())).thenReturn(getMockRecords());
        var response = service.getOrderPrice(SHIPMENT_IDENTIFIER);
        assertEquals(response, BigDecimal.valueOf(18000, 2));
    }

    @Test
    public void getPriceNoRecords() {
        when(client.getOrderHistory(Mockito.anyString())).thenReturn(List.of());
        Assertions.assertThrows(RussianPostIntegrationAppException.class,
                () -> service.getOrderPrice(SHIPMENT_IDENTIFIER));
    }

    @Test
    public void getPriceInvalidRecord() throws DatatypeConfigurationException {
        when(client.getOrderHistory(Mockito.anyString())).thenReturn(List.of(getInvalidRecord()));
        Assertions.assertThrows(RussianPostIntegrationAppException.class,
                () -> service.getOrderPrice(SHIPMENT_IDENTIFIER));
    }


    private List<OperationHistoryRecord> getMockRecords() throws DatatypeConfigurationException {
        OperationHistoryRecord record = new OperationHistoryRecord();
        var finParams = new FinanceParameters();
        var opParams = new OperationParameters();
        finParams.setMassRate(BigInteger.valueOf(10000));
        opParams.setOperDate(DatatypeFactory.newInstance()
                .newXMLGregorianCalendar(2022, 1, 1, 1, 1, 1, 1, 1));
        record.setFinanceParameters(finParams);
        record.setOperationParameters(opParams);

        OperationHistoryRecord record2 = new OperationHistoryRecord();
        var finParams2 = new FinanceParameters();
        finParams2.setMassRate(BigInteger.valueOf(18000));
        var opParams2 = new OperationParameters();
        opParams2.setOperDate(DatatypeFactory.newInstance()
                .newXMLGregorianCalendar(2022, 2, 1, 1, 1, 1, 2, 1));
        record2.setFinanceParameters(finParams2);
        record2.setOperationParameters(opParams2);

        return List.of(record, record2);
    }

    private OperationHistoryRecord getInvalidRecord() throws DatatypeConfigurationException {
        OperationHistoryRecord record = new OperationHistoryRecord();
        var finParams = new FinanceParameters();
        var opParams = new OperationParameters();
        //no massRateSet
        opParams.setOperDate(DatatypeFactory.newInstance()
                .newXMLGregorianCalendar(2022, 1, 1, 1, 1, 1, 1, 1));
        record.setFinanceParameters(finParams);
        record.setOperationParameters(opParams);
        return record;
    }
}
