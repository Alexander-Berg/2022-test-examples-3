package ru.yandex.market.gutgin.tms.mocks;

import ru.yandex.market.gutgin.tms.datafile.excel.parser.GenericFileParser;
import ru.yandex.market.gutgin.tms.service.FileParseServiceNG;
import ru.yandex.market.partner.content.common.db.jooq.enums.FileType;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.FileDataProcessRequest;

import java.util.HashMap;
import java.util.Map;

public class FileParseServiceNGMock implements FileParseServiceNG {
    private final Map<Long, Response> responseMap = new HashMap<>();

    @Override
    public void addParser(FileType fileType, GenericFileParser fileParser) {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public <T> Response<T> parseFile(FileDataProcessRequest dataProcessRequest, long processId) {
        return responseMap.get(processId);
    }

    public void putResponse(long processId, Response response) {
        responseMap.put(processId, response);
    }

    public void clear() {
        responseMap.clear();
    }
}
