package byteresponse;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author yuramalinov
 * @created 11.10.18
 */
@Controller
public class ByteResponseController {
    @GetMapping("some/path1")
    public byte[] responseEntityCall1(@RequestParam(name = "param") byte[] param) {
        return null;
    }

    @GetMapping("some/path2")
    public ResponseEntity<byte[]> responseEntityCall2(@RequestParam(name = "param") byte[] param) {
        return null;
    }
}
