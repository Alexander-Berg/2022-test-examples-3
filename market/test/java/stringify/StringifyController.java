package stringify;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import ru.yandex.market.springmvctots.annotations.TsBody;
import ru.yandex.market.springmvctots.annotations.TsStringify;

/**
 * @author nikita-stenin
 * @created 11.10.18
 */
@Controller
class StringifyController {
    @PostMapping("/api/json")
    public String request(@TsBody @TsStringify @RequestParam("theBody") ComplexObject body,
                          @RequestParam List<MultipartFile> files) {
        return null;
    }

    public static class ComplexObject {
        public List<String> getComplexField() {
            return null;
        }
    }
}
