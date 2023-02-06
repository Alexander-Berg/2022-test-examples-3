package ru.yandex.direct.teststeps.swagger;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import springfox.documentation.annotations.ApiIgnore;

import ru.yandex.direct.common.spring.DevelopmentComponent;

/**
 * Приложение стартует в "/" пути, а test-steps добавляется на сервере в nginx
 * учим локально поднятый jetty ходить в корень, если передан путь test-steps
 */
@ApiIgnore
@RequestMapping("/test-steps")
@DevelopmentComponent
public class RedirectController {
    @RequestMapping("/**")
    public ModelAndView redirectApi(ModelMap model, HttpServletRequest req) {
        String newUrl = StringUtils.removeStart(req.getRequestURI(), "/test-steps");
        return new ModelAndView("forward:" + newUrl, model);
    }
}
