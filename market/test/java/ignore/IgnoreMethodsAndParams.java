package ignore;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import ru.yandex.market.springmvctots.annotations.TsIgnore;

/**
 * @author yuramalinov
 * @created 11.10.18
 */
@Controller
public class IgnoreMethodsAndParams {
    @GetMapping("some/goodMethod")
    public SomeType goodMethod() {
        return null;
    }

    @TsIgnore
    @GetMapping("some/ignoredMethod")
    public NotUsedType ignoredMethod() {
        return null;
    }

    @GetMapping("some/methodWithoutTsParams")
    public String methodWithoutTsParams(@TsIgnore NotUsedType ignoredParam) {
        return null;
    }

    @GetMapping("some/ignorePojo")
    public IgnorePojo ignore() {
        return null;
    }

    @GetMapping("some/ignorePojo2")
    public IgnorePojo2 ignore2() {
        return null;
    }

    public static class SomeType {
        public String getDate() {
            return null;
        }
    }

    public static class NotUsedType {
        public String getDate() {
            return null;
        }
    }

    public static class IgnorePojo {

        public String getStr1() {
            return null;
        }

        @TsIgnore
        public String getStr2() {
            return null;
        }

        @JsonIgnore
        public String getStr3() {
            return null;
        }
    }

    @TsIgnore
    public static class IgnorePojo2 {
        public String getStr1() {
            return null;
        }
    }
}
