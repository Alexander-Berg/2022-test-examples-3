package ru.yandex.market.loyalty.back.controller;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;

import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author artemmz
 */
public class FuzzApiTest extends MarketLoyaltyBackMockedDbTestBase {
    private static final Random RND = new Random();
    private final ParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    private RequestMappingHandlerMapping handlerMapping;


    @Autowired
    private WebApplicationContext applicationContext;

    @Before
    public void setUp() throws Exception {
        handlerMapping = Objects.requireNonNull(applicationContext).getBean("requestMappingHandlerMapping",
                RequestMappingHandlerMapping.class);
    }

    @Test
    public void testGetMethods() {
        urlsForGetMethodsWithParamsAndValues().forEach(this::performUnchecked);
    }

    @Test
    @Ignore("for debug purposes")
    public void testSingleGet() throws Exception {
        mockMvc.perform(get("/perk/list/0")).andExpect(status().isOk());
    }

    /**
     * Поскольку разделить параметры метода на pathVariable и query param проблематично,
     * кладем значения параметров и в url переменные, и в query параметры
     */
    private void performUnchecked(String url, MultiValueMap<String, String> paramNamesWithValues) {
        try {
            mockMvc.perform(
                    get(url, paramNamesWithValues.toSingleValueMap().values().toArray()).params(paramNamesWithValues)
            ).andExpect(result -> {
                MockHttpServletResponse response = result.getResponse();
                HttpStatus httpStatus = HttpStatus.valueOf(response.getStatus());
                if (httpStatus.is5xxServerError()) {
                    fail("5xx status on page " + url + " error " + response.getErrorMessage() + " params: " + paramNamesWithValues);
                }
                if (httpStatus == HttpStatus.FORBIDDEN) {
                    fail("something wrong with spring security on page " + url);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * берет все GET методы и добавляет к ним какие-нибудь рандомные параметры/url переменные
     */
    private Map<String, MultiValueMap<String, String>> urlsForGetMethodsWithParamsAndValues() {
        Map<String, MultiValueMap<String, String>> urlsWithParamsAndValues = new HashMap<>();
        handlerMapping.getHandlerMethods().forEach((reqInfo, handlerMethod) -> {
            String url;
            if (onlyGetMethods(reqInfo) && (url = getUrl(reqInfo)) != null) {
                Method method = handlerMethod.getMethod();
                Class<?>[] parameterTypes = method.getParameterTypes();
                String[] parameterNames = nameDiscoverer.getParameterNames(method);

                for (int i = 0; i < Objects.requireNonNull(parameterNames).length; i++) {
                    urlsWithParamsAndValues.computeIfAbsent(url, u -> new LinkedMultiValueMap<>())
                            .put(parameterNames[i], Collections.singletonList(resolveParamValue(parameterTypes[i])));
                }
            }
        });

        return urlsWithParamsAndValues;
    }

    private static String resolveParamValue(Class<?> parameterType) {
        boolean someBool = RND.nextBoolean();
        String someString = RandomStringUtils.randomAlphabetic(1, 10);

        if (Number.class.isAssignableFrom(parameterType)) {
            return String.valueOf(0);
        } else if (parameterType.isPrimitive()) { // по-простому: или boolean, или циферка (не верю в char параметры)
            return parameterType.equals(boolean.class) ? String.valueOf(RND.nextBoolean()) : String.valueOf(0);
        } else if (Boolean.class.isAssignableFrom(parameterType)) {
            return String.valueOf(someBool);
        } else if (String.class.isAssignableFrom(parameterType)) {
            return someString;
        } else if (parameterType.isEnum()) {
            Object[] enumConstants = parameterType.getEnumConstants();
            return String.valueOf(enumConstants[RND.nextInt(enumConstants.length)]);
        }
        return "";
    }

    private static String getUrl(RequestMappingInfo reqInfo) {
        return reqInfo.getPatternsCondition().getPatterns().stream().findFirst().orElse(null);
    }

    private static boolean onlyGetMethods(RequestMappingInfo reqInfo) {
        return reqInfo.getMethodsCondition().getMethods().stream().allMatch(method -> method.equals(RequestMethod.GET));
    }
}
