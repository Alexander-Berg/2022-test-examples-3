package ru.yandex.market.logistics.tarifficator.admin.mdsfile;

import org.junit.jupiter.api.DisplayName;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.tarifficator.base.AbstractDownloadMdsFileTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@DisplayName("Скачивание mds-файла через админку")
public class AdminDownloadMdsFileTest extends AbstractDownloadMdsFileTest {
    @Override
    protected ResultActions downloadFile(long id) throws Exception {
        return mockMvc.perform(get("/admin/files/" + id + "/download"));
    }
}
