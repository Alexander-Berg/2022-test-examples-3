package responseentity;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author yuramalinov
 * @created 11.10.18
 */
@Controller
public class ResponseEntityController {
    @GetMapping("some/path1")
    public ResponseEntity<Result> responseEntityCall1() {
        return null;
    }

    @GetMapping("some/path2")
    public ResponseEntity responseEntityCall2() {
        return null;
    }

    public static class Result {
        public String getData() {
            return null;
        }
    }
}
