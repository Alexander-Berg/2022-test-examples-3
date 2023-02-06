package ru.yandex.market.mini.testmjfortank.api;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import ru.yandex.mj.generated.server.api.CheckMyServiceApiDelegate;


@Component
public class CheckMyServiceApiService implements CheckMyServiceApiDelegate {
    @Override
    public ResponseEntity<String> checkMyServiceGet() {
        return ResponseEntity.ok("Everything is fine");
    }
}
