package ru.yandex.market.hrms.core.service.isrping;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.config.TestMockConfig;
import ru.yandex.market.hrms.core.domain.yt.YtTableDto;
import ru.yandex.market.hrms.core.service.ispring.IspringYtService;
import ru.yandex.market.hrms.core.service.ispring.enums.IspringYtTables;
import ru.yandex.market.hrms.core.service.isrping.stubs.IspringYqlRepoStub;

import static ru.yandex.market.tpl.common.util.StringFormatter.sf;

@DbUnitDataSet(schema = "public")
public class IspringYtServiceTest extends AbstractCoreTest {

    private static final String ANSWER_164097431 = "/inputs/ispring/yt_ispring_reserve_position_answer_164097431.json";
    private static final String ANSWER_164144260 = "/inputs/ispring/yt_ispring_reserve_position_answer_164144260.json";

    @Autowired
    private IspringYtService service;
    @Autowired
    private ApplicationContext context;

    private String readResource(String resourceName) {
        try {
            return IOUtils.toString(TestMockConfig.class.getResourceAsStream(resourceName),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(sf("Unknown resource: {}", resourceName));
        }
    }

    private void configureIspringYqlRepo(YtTableDtoBlank... blanks) {
        List<YtTableDto> records = Arrays.stream(blanks)
                .map(blank -> new YtTableDto(blank.id, blank.uid, readResource(blank.answerPath), blank.created))
                .toList();

        IspringYqlRepoStub repo = (IspringYqlRepoStub) context.getBean("ispringYqlRepo");
        repo.withData(IspringYtTables.RESERVE_POSITIONS.getPath(), records);
    }

    @Test
    @DbUnitDataSet(before = "IspringYtServiceTest.shouldLoadAndSaveIspringReservePositionForms.before.csv",
            after = "IspringYtServiceTest.shouldLoadAndSaveIspringReservePositionForms.after.csv")
    void shouldLoadAndSaveIspringReservePositionForms() throws IOException {
        configureIspringYqlRepo(
                new YtTableDtoBlank(164097431L, null, ANSWER_164097431, Instant.parse("2022-05-25T13:46:54Z")),
                new YtTableDtoBlank(164144260L, 899754545L, ANSWER_164144260, Instant.parse("2022-05-26T10:38:32Z"))
        );
        service.loadAndSaveIspringReservePositionForms();
    }

    record YtTableDtoBlank(
            long id,
            Long uid,
            String answerPath,
            Instant created
    ) {
    }
}
