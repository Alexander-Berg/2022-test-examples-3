package ru.yandex.chemodan.app.psbilling.core.mocks;

import java.util.UUID;

import lombok.Getter;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.chemodan.app.psbilling.core.config.PsBillingTankerConfiguration;
import ru.yandex.chemodan.app.psbilling.core.dao.texts.TankerKeyDao;
import ru.yandex.chemodan.app.psbilling.core.entities.texts.TankerTranslationEntity;
import ru.yandex.chemodan.app.psbilling.core.texts.TankerTranslation;
import ru.yandex.chemodan.app.psbilling.core.texts.TextsManager;
import ru.yandex.inside.tanker.TankerClient;

import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;

@Configuration
@Import(PsBillingTankerConfiguration.class)
public class TextsManagerMockConfiguration {

    @Autowired
    @Getter
    private TextsManager mock;

    @Bean
    @Primary
    public TextsManager textsManager(TankerClient tankerClient, TankerKeyDao tankerKeyDao) {

        this.mock = Mockito.mock(TextsManager.class,
                Mockito.withSettings().spiedInstance(new TextsManager(tankerClient, tankerKeyDao)).defaultAnswer(CALLS_REAL_METHODS));
        return this.mock;
    }

    public void turnMockOn() {
        doNothing().when(mock).updateTranslations();
        doAnswer(x -> new TankerTranslation(x.getArgument(0),
                Cf.list(new TankerTranslationEntity(x.getArgument(0), "ru", "Тестовый ключ"))))
                .when(mock).findTranslation(Mockito.any());
        doAnswer(x -> ((ListF<UUID>) x.getArgument(0)).toMap(key -> Tuple2.tuple(
                key,
                new TankerTranslation(key,
                        Cf.list(new TankerTranslationEntity(key, "ru", "Тестовый ключ"))))))
                .when(mock).findTranslations(Mockito.any());
        doAnswer(x -> new TankerTranslation(UUID.randomUUID(),
                Cf.list(new TankerTranslationEntity(UUID.randomUUID(), "ru", "Тестовый ключ"))))
                .when(mock).findTranslation(Mockito.any(),Mockito.any(),Mockito.any());
        doAnswer(x -> new TankerTranslation(UUID.randomUUID(),
                Cf.list(new TankerTranslationEntity(UUID.randomUUID(), "ru", "Тестовый ключ"))))
                .when(mock).findPredefinedTankerTranslation(Mockito.any());
    }

    public void mockAnyFindTranslation(UUID tankerTranslationUid) {
        Mockito.when(mock.findTranslation(Mockito.eq(tankerTranslationUid)))
                .thenReturn(new TankerTranslation(tankerTranslationUid,
                        Cf.list(new TankerTranslationEntity(tankerTranslationUid, "ru", "Тестовый ключ"))));
    }

    public void mockFindTranslation(TankerTranslation translation) {
        Mockito.when(mock.findTranslation(Mockito.eq(translation.getKeyId()))).thenReturn(translation);
    }


    public void reset() {
        Mockito.reset(mock);
    }
}
