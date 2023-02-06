package ru.yandex.market.tpl.core.domain.region;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.region.RegionType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author sekulebyakin
 */
@ExtendWith(MockitoExtension.class)
public class RegionDaoBatchTest {

    @InjectMocks
    private RegionDao regionDao;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @BeforeEach
    void setup() {
        doAnswer(invocation -> invocation.getArgument(0, TransactionCallback.class).doInTransaction(null))
                .when(transactionTemplate).execute(any());
    }

    @Test
    void updateRegionsBatchTest() {
        var expectedBatchesCount = 13; // RegionDao#MAX_BATCH_SIZE = 10000
        var regions = generateRegions(122000);
        regionDao.updateRegions(regions);
        var paramsCaptor = ArgumentCaptor.forClass(Map[].class);
        verify(jdbcTemplate, times(1)).update(eq("TRUNCATE TABLE region"));
        verify(namedParameterJdbcTemplate, times(expectedBatchesCount)).batchUpdate(any(), paramsCaptor.capture());
        var paramsList = paramsCaptor.getAllValues();
        assertThat(paramsList).hasSize(expectedBatchesCount);
        paramsList.forEach(params -> assertThat(params.length).isLessThanOrEqualTo(10000));
    }

    private List<TplRegion> generateRegions(int count) {
        var list = new ArrayList<TplRegion>(count);
        for (int i = 0; i < count; i++) {
            list.add(TplRegion.builder()
                    .id(i)
                    .parentId(i + 1)
                    .name("name"+i)
                    .englishName("engName"+i)
                    .type(RegionType.REGION)
                    .tzOffset(i)
                    .chiefRegionId(i+2)
                    .latitude(55.23456)
                    .longitude(35.23456)
                    .build()
            );
        }
        return list;
    }

}
