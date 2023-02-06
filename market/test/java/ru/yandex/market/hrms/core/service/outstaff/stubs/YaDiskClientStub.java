package ru.yandex.market.hrms.core.service.outstaff.stubs;

import java.util.Optional;

import ru.yandex.market.hrms.core.service.outstaff.client.YaDiskClient;
import ru.yandex.market.hrms.core.service.outstaff.dto.YaDiskResponseDto;

public class YaDiskClientStub extends YaDiskClient {

    private String link;
    private int statusCode;
    private byte[] photo;

    public void withDownloadLink(int statusCode, String link) {
        this.statusCode = statusCode;
        this.link = link;
    }

    public void withPhoto(byte[] photo) {
        this.photo = photo;
    }

    @Override
    public YaDiskResponseDto getFileDirectDownloadLink(String publishedPhotoLink) {
        return new YaDiskResponseDto(statusCode, null, link, null, false, null, null, null);
    }

    @Override
    public Optional<byte[]> downloadFile(String directUrl) {
        return photo != null
                ? Optional.of(photo)
                : Optional.empty();
    }
}
