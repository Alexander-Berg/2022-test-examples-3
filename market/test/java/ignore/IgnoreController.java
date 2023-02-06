package ignore;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import ru.yandex.market.springmvctots.annotations.TsIgnore;

/**
 * @author yuramalinov
 * @created 11.10.18
 */
@Controller
@TsIgnore
public class IgnoreController {
    @GetMapping("some/path")
    public SomeType responseEntityCall() {
        return null;
    }

    public static class SomeType {
        public String getDate() {
            return null;
        }
    }
}
