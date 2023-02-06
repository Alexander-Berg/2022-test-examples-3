package ru.yandex.market.ir.config;

import ru.yandex.market.gutgin.tms.pipeline.dcp.taskaction.databucket.AvatarImageDownloader;
import ru.yandex.market.gutgin.tms.pipeline.dcp.taskaction.databucket.PictureContents;

class AvatarImageDownloaderMock extends AvatarImageDownloader {
    @Override
    public Result downloadImage(String url) {
        return new Result(
                new PictureContents(new byte[]{1, 2, 3}, "testImageContentType", url),
                null
        );
    }
}
