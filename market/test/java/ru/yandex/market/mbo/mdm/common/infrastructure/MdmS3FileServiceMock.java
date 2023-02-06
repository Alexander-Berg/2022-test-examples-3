package ru.yandex.market.mbo.mdm.common.infrastructure;

import java.util.Optional;

/**
 * @author dmserebr
 * @date 16/06/2020
 */
public class MdmS3FileServiceMock implements MdmS3FileService {
    public static final String FILE_NAME_FORMAT = "file-%s-%s";

    @Override
    public Optional<String> uploadFileToS3(String filename, byte[] fileBytes, String fileTypeStr) {
        return Optional.of(String.format(FILE_NAME_FORMAT, fileTypeStr, filename));
    }
}
