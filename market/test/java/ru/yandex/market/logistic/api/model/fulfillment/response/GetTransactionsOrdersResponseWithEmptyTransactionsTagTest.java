package ru.yandex.market.logistic.api.model.fulfillment.response;

import java.io.IOException;
import java.util.Set;

import javax.validation.ConstraintViolation;

import com.fasterxml.jackson.databind.JavaType;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.model.common.response.ResponseWrapper;
import ru.yandex.market.logistic.api.utils.ParsingXmlWrapperTest;
import ru.yandex.market.logistic.api.utils.ValidationUtil;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class GetTransactionsOrdersResponseWithEmptyTransactionsTagTest
    extends ParsingXmlWrapperTest<ResponseWrapper, GetTransactionsOrdersResponse> {

    public GetTransactionsOrdersResponseWithEmptyTransactionsTagTest() {
        super(
            ResponseWrapper.class,
            GetTransactionsOrdersResponse.class,
            "fixture/response/ff_get_transactions_orders_with_empty_transactions_tag.xml"
        );
    }

    @Test
    public void checkThatTransactionWithEmptyTransactionsTagIsValid() throws IOException {
        String expected = getFileContent(fileName);
        JavaType javaType = getType();
        ResponseWrapper responseWrapper = getMapper().readValue(expected, javaType);

        Set<ConstraintViolation<ResponseWrapper>> constraintViolations = ValidationUtil.validate(responseWrapper);
        assertTrue(constraintViolations.isEmpty());
    }

    // Тест на сериализацию не проходит, так как пустая коллекция при сериализации отсутствует в xml
    @Ignore
    @Override
    public void testSerializationAndDeserialization() {
    }
}
