package ru.yandex.market.tpl.common.web.exception;

import javax.validation.Valid;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@Slf4j
public class ValidatorController {

    @PostMapping(value = "/validation", produces = {"application/json; charset=UTF-8"})
    public ParentDto validate(@RequestBody @Valid ParentDto dto) {
        log.info("TEST");
        return dto;
    }
}
