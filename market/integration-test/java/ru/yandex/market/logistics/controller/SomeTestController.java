package ru.yandex.market.logistics.controller;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tests")
@ParametersAreNonnullByDefault
public class SomeTestController {

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    @PostMapping("/save")
    public Long saveDto(@RequestBody TestDto dto) {
        return dto.id;
    }

    @GetMapping("entity/by-id/{id}")
    public TestDto getById(@PathVariable("id") long id) {
        return new TestDto(id);
    }

    static class TestDto {
        long id;

        TestDto(long id) {
            this.id = id;
        }
    }
}
