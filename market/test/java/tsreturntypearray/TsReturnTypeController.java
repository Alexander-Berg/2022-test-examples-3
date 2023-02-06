package tsreturntypearray;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

import ru.yandex.market.springmvctots.annotations.TsReturnType;

/**
 * @author nikita-stenin
 * @created 11.10.18
 */
@Controller
class TsReturnTypeController {
    @TsReturnType(ComplexObject[].class)
    @PostMapping("/api/json")
    public String request() {
        return null;
    }

    public static class ComplexObject {
        public List<String> getComplexField() {
            return null;
        }
    }
}
