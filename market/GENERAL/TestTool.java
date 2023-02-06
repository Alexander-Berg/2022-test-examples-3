package ru.yandex.mbo.tool.jira.MBO21843;

import com.beust.jcommander.Parameter;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.market.mbo.db.TovarTreeService;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.tree.ExportTovarTree;
import ru.yandex.mbo.tool.jira.utils.CommandLineTool;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @see <a href ="https://github.yandex-team.ru/market-java/mbo/blob/master/scripts/nanny/README.md">readme</a>
 */
@Component
public class TestTool extends CommandLineTool {
    private static final Logger log = LogManager.getLogger();

    @Parameter(names = "--hid", required = true)
    private long hid;

    @Parameter(names = "--file-path", description = "Path to file on nanny instance", required = false)
    private String filePath;

    @Value("${uc.yt.tovar-tree.table}")
    private String ytTovarTreeTablePath;

    @Resource(name = "siteCatalogJdbcTemplate")
    protected JdbcTemplate siteCatalogJdbcTemplate;

    @Resource
    private TovarTreeService tovarTreeService;

    @Resource
    private Yt yt;

    @Override
    protected void start() throws Exception {
        log.info("TestTool was started with parameters: hid={}", hid);

        testService();
        testJdbcTemplate();
        testYt();
        testReadFromFile();

        log.info("TestTool was finished");
    }

    private void testService() {
        TovarCategory category = tovarTreeService.getCategoryByHid(hid);
        if (category == null) {
            log.info("Category with hid {} not found", hid);
        } else {
            log.info("Category {} was found by hid {}", category.getName(), hid);
        }
    }

    private void testJdbcTemplate() {
        int siteCatalogSize = siteCatalogJdbcTemplate.queryForObject("select count(*) from MODEL", Integer.class);

        log.info("Site catalog current size is {}", siteCatalogSize);
    }

    private void testYt() {
        YPath ytPath = YPath.simple(ytTovarTreeTablePath);

        List<ExportTovarTree.TovarCategory> categories = new ArrayList<>();
        yt.tables().read(ytPath, YTableEntryTypes.YSON, entry -> {
            try {
                categories.add(ExportTovarTree.TovarCategory.parseFrom(entry.getBytes("data")));
            } catch (InvalidProtocolBufferException e) {
                log.error("Failed to read tovar tree: {}", e.getMessage());
            }
        });

        log.info("Found {} categories in tovar category tree", categories.size());
    }

    private void testReadFromFile() throws IOException {
        if (filePath != null) {
            Files.lines(Paths.get(filePath), StandardCharsets.UTF_8).forEach(log::info);
        }
    }
}
