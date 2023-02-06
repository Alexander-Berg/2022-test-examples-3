package ru.yandex.market.abo;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import ru.yandex.market.abo.web.AboDispatcherServlet;

import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author artemmz
 * @date 20.03.18.
 */
public class ControllerCommonTest extends AbstractControllerTest {
    private final ParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    private RequestMappingHandlerMapping handlerMapping;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AboDispatcherServlet aboDispatcherServlet;

    @BeforeEach
    void setUp() {
        handlerMapping = Objects.requireNonNull(
                aboDispatcherServlet.getWebApplicationContext()).getBean(RequestMappingHandlerMapping.class);
    }

    @Test
    void testGetMethods() {
        urlsForGetMethodsWithParamsAndValues().forEach(this::performUnchecked);
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
                    fail("5xx status on page " + url + " error " + response.getErrorMessage());
                }
                if (httpStatus == HttpStatus.FORBIDDEN) {
                    fail("something wrong with spring security on page " + url);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Disabled("for debug purposes")
    void testSingleGet() throws Exception {
        mockMvc.perform(get("/stop-word-hiding")).andExpect(status().isOk());
    }

    /**
     * берет все GET методы и добавляет к ним какие-нибудь рандомные параметры/url переменные
     */
    private Map<String, MultiValueMap<String, String>> urlsForGetMethodsWithParamsAndValues() {
        Map<String, MultiValueMap<String, String>> urlsWithParamsAndValues = new HashMap<>();
        handlerMapping.getHandlerMethods().forEach((reqInfo, handlerMethod) -> {
            String url;
            if (onlyGetMethods(reqInfo) && (url = getUrl(reqInfo)) != null) {
                if (url.startsWith("/tms")) {
                    return;
                }
                Method method = handlerMethod.getMethod();
                Class<?>[] parameterTypes = method.getParameterTypes();
                String[] parameterNames = nameDiscoverer.getParameterNames(method);

                for (int i = 0; i < parameterNames.length; i++) {
                    urlsWithParamsAndValues.computeIfAbsent(url, u -> new LinkedMultiValueMap<>())
                            .put(parameterNames[i], Collections.singletonList(resolveParamValue(parameterTypes[i])));
                }
            }
        });

        return urlsWithParamsAndValues;
    }

    private static String resolveParamValue(Class<?> parameterType) {
        if (Number.class.isAssignableFrom(parameterType)) {
            return String.valueOf(0);
        } else if (parameterType.isPrimitive()) { // по-простому: или boolean, или циферка (не верю в char параметры)
            return parameterType.equals(boolean.class) ? String.valueOf(true) : String.valueOf(0);
        } else if (Boolean.class.isAssignableFrom(parameterType)) {
            return String.valueOf(true);
        } else if (String.class.isAssignableFrom(parameterType)) {
            return "mvc-foobar";
        } else if (parameterType.isEnum()) {
            return String.valueOf(parameterType.getEnumConstants()[0]);
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
