package requesturls;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yuramalinov
 * @created 02.10.18
 */
@RestController
@RequestMapping("base-url")
public class RequestUrlsController {
    @RequestMapping("/api")
    public String callWithNamedParam(@RequestParam int param, @RequestParam("named") int namedParam) {
        return null;
    }

    @RequestMapping(value = "/api/{id}", method = RequestMethod.POST)
    public String callWithSinglePathVariable(@PathVariable("id") int id, @RequestParam String name) {
        return null;
    }

    @RequestMapping(value = "/api/{a}/{b}", method = RequestMethod.POST)
    public String callWithTwoPathVariables(@PathVariable("a") int a, @PathVariable("b") String bVariable) {
        return null;
    }

    @GetMapping("/api/get")
    public String simpleGetCall() {
        return null;
    }

    @PostMapping("/api/post")
    public String simplePostCall() {
        return null;
    }

    @DeleteMapping("/api/delete")
    public String simpleDeleteCall() {
        return null;
    }

    @PatchMapping("/api/patch")
    public String simplePatchCall() {
        return null;
    }

    @PutMapping("/api/put")
    public String simplePutCall() {
        return null;
    }
}
