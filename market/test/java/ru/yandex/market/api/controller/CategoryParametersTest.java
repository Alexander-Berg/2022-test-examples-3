package ru.yandex.market.api.controller;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

import ru.yandex.market.api.MockRequestBuilder;
import ru.yandex.market.api.error.ValidationError;
import ru.yandex.market.api.integration.ContainerTestBase;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class CategoryParametersTest extends ContainerTestBase {
    @Test
    public void shouldValidateShortExcludeHids_request(){
        HttpServletRequest request = MockRequestBuilder.start()
                .param("exclude_hids", "1,2,3,4,5,6,7,8,9,10")
                .build();

        assertTrue(new CategoryParameters.ExcludeHidsParser().get(request).isOk());
    }

    @Test
    public void shouldNotValidateLongExcludeHids_request() {
        HttpServletRequest request = MockRequestBuilder.start()
                .param("exclude_hids", "1,2,3,4,5,6,7,8,9,10,11")
                .build();

        assertTrue(new CategoryParameters.ExcludeHidsParser().get(request).hasError());
    }

    @Test
    public void shouldValidateShortExcludeHids_object(){
        ValidationError error = CategoryParameters.validateExcludedHids(Arrays.asList(-1, -2, -3, -4, -5, -6, -7, -8, -9, -10));

        assertThat(error, is(nullValue(ValidationError.class)));
    }

    @Test
    public void shouldNotValidateLongExcludeHids_object() {
        ValidationError error = CategoryParameters.validateExcludedHids(Arrays.asList(-1, -2, -3, -4, -5, -6, -7, -8, -9, -10, -11));

        assertThat(error, instanceOf(ValidationError.class));
    }
}