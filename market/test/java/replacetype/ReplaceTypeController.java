package replacetype;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReplaceTypeController {
    @GetMapping("/api/getIt!")
    public SomeTooSmartType getIt() {
        return null;
    }

    public interface SomeTooSmartType {
    }
}
