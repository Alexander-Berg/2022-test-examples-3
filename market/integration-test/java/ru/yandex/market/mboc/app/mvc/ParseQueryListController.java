package ru.yandex.market.mboc.app.mvc;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ru.yandex.market.mboc.app.security.SecuredRolesIgnore;

@RestController
@SecuredRolesIgnore(reason = "Test controller")
public class ParseQueryListController {

    /**
     * Метод нужен, чтобы корректно распарсить список айдишников.
     * Используется в тестах {@link ParseQueryListControllerTest#parseIds()}.
     */
    @GetMapping("/integration-test-api/parse-ids")
    public String parseIds(@RequestParam("ids") List<Long> ids) {
        return "Count: " + ids.size();
    }
}
