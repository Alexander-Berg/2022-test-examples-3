package convert.args_to_object;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author nikita-stenin
 * @created 11.01.19
 */
@Controller
public class ConvertArgsToObjectController {
    @GetMapping("some/path")
    public String someMethod(
        @RequestParam(name = "a") int a, @RequestParam(name = "b") int b,
        @RequestParam(name = "c", required = false) int c, @RequestParam(name = "d", required = false) int d) {
        return null;
    }
}
