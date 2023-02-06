package ru.yandex.market.hrms.core.service.outstaff.stubs;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import ru.yandex.market.hrms.core.domain.outstaff.repo.OutstaffYqlRepo;
import ru.yandex.market.hrms.core.domain.yt.YtTableDto;

@Service
public class OutstaffYqlRepoStub extends OutstaffYqlRepo {

    private final Map<String, List<YtTableDto>> entities = new HashMap<>();

    public OutstaffYqlRepoStub(NamedParameterJdbcTemplate yqlJdbcTemplate) {
        super(yqlJdbcTemplate, null);
    }

    public void withData(String ytTable, List<YtTableDto> returnValue) {
        entities.put(ytTable, returnValue);
    }

    @Override
    public @NotNull List<YtTableDto> getFormAnswers(@NotNull String yqlTable, long greaterThanId) {
        return entities.get(yqlTable);
    }

    @Override
    public List<YtTableDto> getFormAnswers(String yqlTable, Instant startDate) {
        return entities.get(yqlTable);
    }
}
