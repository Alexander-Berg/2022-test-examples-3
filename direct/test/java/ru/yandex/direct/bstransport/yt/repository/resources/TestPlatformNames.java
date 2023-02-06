package ru.yandex.direct.bstransport.yt.repository.resources;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.adv.direct.banner.resources.PlatformName;

@ParametersAreNonnullByDefault
public class TestPlatformNames {

    public static final PlatformName YANDEX_MAPS_RU = PlatformName.newBuilder().setName("maps")
            .setForms(PlatformName.Forms.newBuilder()
                    .setPostfix("на Картах")
                    .setPostfixYaShort("на Я.Картах")
                    .setPostfixYaFull("на Яндекс.Картах")
                    .setSimple("Карты")
                    .setSimpleYaShort("Я.Карты")
                    .setSimpleYaFull("Яндекс.Карты"))
            .build();

    public static final PlatformName GOOGLE_PLAY_RU = PlatformName.newBuilder().setName("googleplay")
            .setForms(PlatformName.Forms.newBuilder()
                    .setPostfix("в Google Play")
                    .setSimple("Google Play"))
            .build();

}
