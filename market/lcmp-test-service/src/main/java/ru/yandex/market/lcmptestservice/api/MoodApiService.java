package ru.yandex.market.lcmptestservice.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.protobuf.InvalidProtocolBufferException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import ru.yandex.market.exp3configs.lcmptestservice.general.GneralSettings;
import ru.yandex.market.experiments3.client.Exp3MatcherCallException;
import ru.yandex.market.experiments3.client.Experiments3Client;
import ru.yandex.mj.generated.server.api.MoodApiDelegate;


@Component
public class MoodApiService implements MoodApiDelegate {
    @Autowired
    private Experiments3Client experiments3Client;

    @Override
    public ResponseEntity<String> moodGet() {
        GneralSettings.Settings settings;
        try {
            settings = experiments3Client.getConfig(GneralSettings.Settings.class);
        } catch (Exp3MatcherCallException | InvalidProtocolBufferException | JsonProcessingException e) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok(settings.getMood());
    }
}
