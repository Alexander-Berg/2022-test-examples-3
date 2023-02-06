package ru.yandex.market.deepdive.domain.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import ru.yandex.market.deepdive.domain.controller.dto.HttpCodeRequestDto;

@RestController
@RequestMapping(value = "api/code")
@RequiredArgsConstructor
public class TestHttpReturnCodeController {
    @PutMapping()
    @ResponseBody
    public ResponseEntity<HttpCodeRequestDto> getTestHttpCode(
            @Validated @RequestBody HttpCodeRequestDto codeRequestDto
    ) {
        return ResponseEntity.status(codeRequestDto.getCode()).body(codeRequestDto);
    }
}
