package ru.yandex.market.fulfillment.stockstorage.controller;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.fulfillment.stockstorage.domain.entity.UnitId;
import ru.yandex.market.fulfillment.stockstorage.domain.exception.StocksUpdateFailedException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;


public class ControllerExceptionHandlerTest {

    ControllerExceptionHandler controllerExceptionHandler = new ControllerExceptionHandler();
    private static final String EXCEPTION_MESSAGE = "Exception message";

    private UnitId getStockUnitId() {
        return new UnitId("sku", 1L, 1);
    }

    Set<ConstraintViolation<?>> getConstraintsViolations() {
        Set<ConstraintViolation<?>> result = new HashSet<>();
        return result;
    }

    Map<UnitId, Exception> getFailedStocks() {
        Map<UnitId, Exception> failedStocks = new HashMap<>();
        failedStocks.put(getStockUnitId(), new Exception(EXCEPTION_MESSAGE));
        return failedStocks;
    }

    @Test
    public void StocksPushingFailedExceptionHandle() {
        ResponseEntity<String> responseEntity =
                controllerExceptionHandler.handle(new StocksUpdateFailedException(getFailedStocks()));
        assertNotNull(responseEntity);
        assertThat(responseEntity.toString(), containsString(getStockUnitId().toString()));
        assertThat(responseEntity.toString(), containsString(EXCEPTION_MESSAGE));
    }

    @Test
    public void ConstraintViolationExceptionHandle() {
        Map map = controllerExceptionHandler.handle(new ConstraintViolationException(getConstraintsViolations()));
        assertNotNull(map);
    }
}
