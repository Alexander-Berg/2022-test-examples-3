package ru.yandex.market.cashier.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.market.cashier.AbstractApplicationTest;
import ru.yandex.market.cashier.trust.api.Product;

import static org.junit.jupiter.api.Assertions.*;

public class TrustProductServiceTest extends AbstractApplicationTest {

    @Autowired
    private TrustProductService service;

    @Test
    void existsProduct() {
        String token = "TrustProductServiceTest";
        Product product = product("existsProduct");

        assertFalse(service.existsProduct(token, product));
        service.create(token,product);
        assertTrue(service.existsProduct(token, product));
    }

    @Test
    void create() {
        String token = "TrustProductServiceTest";
        Product product = product("create");

        service.create(token,product);
    }

    @Test
    @Transactional(readOnly = true)
    void testTransactionForEdit(){
        String token = "TrustProductServiceTest";
        Product product = product("testTransactionForEdit");

        //при вызове транзакции на изменение из под ридонли транзакции должна быть ошибка
        assertThrows(IllegalTransactionStateException.class, () -> service.create(token,product));
    }

    @Test
    @Transactional
    void testReadonlyNestedCall(){
        String token = "TrustProductServiceTest";
        Product product = product("testReadonlyNestedCall");

        //при вызове ридонли метода из метода на изменение все должно отработать
        assertFalse(service.existsProduct(token, product));
    }


    private static Product product(String productId){
        return new Product(productId,"name",1L, 11);
    }
}
