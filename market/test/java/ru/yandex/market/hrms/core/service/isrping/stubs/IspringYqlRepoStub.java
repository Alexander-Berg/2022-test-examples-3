package ru.yandex.market.hrms.core.service.isrping.stubs;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.hrms.core.domain.ispring.repo.IspringYqlRepo;
import ru.yandex.market.hrms.core.domain.yt.YtTableDto;

public class IspringYqlRepoStub extends IspringYqlRepo {

    private final Map<String, List<YtTableDto>> content = new HashMap<>();

    public IspringYqlRepoStub(NamedParameterJdbcTemplate yqlJdbcTemplate) {
        super(yqlJdbcTemplate);
    }

    public void withData(String path, List<YtTableDto> records) {
        content.put(path, records);
    }

    @Override
    public List<YtTableDto> getIspringReservePositionFormAnswersFrom(String path, Instant from) {
        return content.get(path).stream()
                .filter(dto -> dto.getCreated().compareTo(from) >= 0)
                .toList();
    }
}
