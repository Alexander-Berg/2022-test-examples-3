package formdata;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author nikita-stenin
 * @created 11.10.18
 */
@Controller
@RequestMapping("base-url")
class FormDataController {
    @PostMapping("/api/file/upload")
    public String uploadFile(@RequestParam(name = "file") MultipartFile file) {
        return null;
    }

    @PostMapping("/api/files/upload")
    public String uploadFiles(@RequestParam(name = "file1") MultipartFile file1,
            @RequestParam(name = "file2") MultipartFile file2) {
        return null;
    }
}
