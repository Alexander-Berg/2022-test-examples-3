package extracontrollerscontrollerother;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SimpleController2 {
    @RequestMapping("/api/return_int")
    public int makeCall(@RequestParam int param) {
        return param;
    }

    @RequestMapping("/api/return_void")
    public void makeVoid(@RequestParam int param) {
    }
}
