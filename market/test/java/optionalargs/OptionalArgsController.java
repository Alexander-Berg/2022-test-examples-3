package optionalargs;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author nikita-stenin
 * @created 11.10.18
 */
@Controller
class OptionalArgsController {
    @GetMapping("/api/args/optional")
    public String optional(Boolean visible) {
    return null;
  }

    @GetMapping("/api/args/requestParam")
    public String requestParam(
        @RequestParam(name = "required") String required,
        @RequestParam(name = "optional", required = false) String optional) {
        return null;
    }

    @GetMapping("/api/args/pathVariable/{required}/{optional}")
    public String pathVariable(
        @PathVariable(name = "required") String required,
        @PathVariable(name = "optional", required = false) String optional) {
        return null;
    }

    @PostMapping("/api/args/requestBody/required")
    public String requestBodyRequired(@RequestBody String required) {
        return null;
    }

    @PostMapping("/api/args/requestBody/optional")
    public String requestBodyOptional(@RequestBody(required = false) String optional) {
        return null;
    }

    @GetMapping("/api/args/sorting")
    public String sorting(@RequestParam(name = "three", required = false) String three,
        @RequestParam(name = "four", required = false) String four, @RequestParam(name = "one") String one,
        @RequestParam(name = "five", required = false) String five, @RequestParam(name = "two") String two) {
        return null;
    }
}
