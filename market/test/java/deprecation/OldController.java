package deprecation;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Deprecated
public class OldController {
    @GetMapping("get_smth")
    public int getSmth(int arg) {
        return arg;
    }
}
