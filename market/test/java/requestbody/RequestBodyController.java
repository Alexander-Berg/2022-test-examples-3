package requestbody;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author nikita-stenin
 * @created 11.10.18
 */
@Controller
@RequestMapping("base-url")
class RequestBodyController {
    @PostMapping("/api/json")
    public String requestJson(@RequestBody String body) {
        return null;
    }
}
