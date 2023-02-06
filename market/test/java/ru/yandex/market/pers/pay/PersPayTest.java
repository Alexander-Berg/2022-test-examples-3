package ru.yandex.market.pers.pay;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.google.common.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.pers.author.client.PersAuthorClient;
import ru.yandex.market.pers.author.client.api.dto.AgitationDto;
import ru.yandex.market.pers.author.client.api.dto.pager.DtoList;
import ru.yandex.market.pers.author.client.api.model.AgitationType;
import ru.yandex.market.pers.test.common.AbstractPersWebTest;
import ru.yandex.market.pers.test.common.PersTestMocksHolder;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.author.client.api.PersAuthorApiConstants.AGIT_PAID_KEY;

@Import({
    PersPayTestConfiguration.class,
})
@ExtendWith(SpringExtension.class)
@TestPropertySource("classpath:/test-application.properties")
public class PersPayTest extends AbstractPersWebTest {
    public static final ResultMatcher isOk = status().is2xxSuccessful();
    public static final ResultMatcher error400 = status().is4xxClientError();

    @Autowired
    protected MockMvc mockMvc;

    @Qualifier("payJdbcTemplate")
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Qualifier("payDataSource")
    @Autowired
    private DataSource dataSource;

    @Autowired
    @Qualifier("memCacheMock")
    private Cache<String, Object> cache;

    @Autowired
    protected PersAuthorClient persAuthorClient;

    @BeforeEach
    public void cleanDatabase() {
        applySqlScript(dataSource, "truncate.sql");
        invalidateCache();
        resetMocks();
    }

    public void resetMocks() {
        PersTestMocksHolder.resetMocks();
    }

    protected void invalidateCache() {
        cache.invalidateAll();
    }

    protected void mockPersAuthorAgitations(long user_id, List<Long> entityIds) {
        List<AgitationDto> dtos = entityIds.stream()
            .map(entityId -> new AgitationDto(user_id, AgitationType.MODEL_GRADE.value(), entityId, Map.of(AGIT_PAID_KEY, "1")))
            .collect(Collectors.toList());
        when(persAuthorClient.getExistedUserAgitationsByUid(eq(user_id), eq(AgitationType.MODEL_GRADE), anyList()))
            .thenReturn(dtos);
    }
}
