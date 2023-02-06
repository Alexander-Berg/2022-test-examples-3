package ru.yandex.market.acw;

import java.util.HexFormat;
import java.util.UUID;

import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ru.yandex.market.acw.api.Image;
import ru.yandex.market.acw.api.Text;
import ru.yandex.market.acw.utils.HashUtils;

public class Utils {

    @Test
    @Disabled
    void convertBytesToHumanReadable() throws InvalidProtocolBufferException {
        printImage("00000000  0A 06 08 0E 15 0F 0B 06");
        printText("00000000  0A 02 1A 00 10 02");
    }


    @Test
    @Disabled
    void convertTextToHash() {
        String gutginTextFormat = "title = Мобильная баскетбольная стойка DFC 80х58см п/э KIDSD2\n";

        UUID uuid = HashUtils.convertTextToMD5UUID(gutginTextFormat);
        System.out.println("uuid(acw-text-hash):");
        System.out.println(uuid);
    }

    private void printImage(String imageCacheData) throws InvalidProtocolBufferException {
        String formattedHex = imageCacheData.substring(10).replace(" ", "");
        byte[] bytes = HexFormat.of().parseHex(formattedHex);
        Image.ImageVerdictResult imageVerdictResult = Image.ImageVerdictResult.parseFrom(bytes);
        System.out.println("ImageVerdictResult:");
        System.out.println(imageVerdictResult);

    }

    private void printText(String textCacheData) throws InvalidProtocolBufferException {
        String formattedHex = textCacheData.substring(10).replace(" ", "");
        byte[] bytes = HexFormat.of().parseHex(formattedHex);
        Text.TextVerdictResult textVerdictResult = Text.TextVerdictResult.parseFrom(bytes);
        System.out.println("TextVerdictResult:");
        System.out.println(textVerdictResult);
    }
}
