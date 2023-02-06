package ru.yandex.autotests.direct.cmd.common;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.tags.NoReleaseTag;
import ru.yandex.qatools.Tag;

import java.io.IOException;

@Aqua.Test
@Tag(NoReleaseTag.YES)
public class DepsTest {

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9593")
    public void viewDependencies() {
//        run("pwd");
        run("mvn", "-X", "dependency:tree");
//        run("/bin/bash ls -la");
//        run("/bin/bash set");
//        run("/bin/bash type mvn");
//        run("/bin/bash ls -la /usb/bin");
//        run("/bin/bash ls -la /usb/local/bin");
    }

    private void run(String... commands) {
        try {
            ProcessBuilder pb = new ProcessBuilder(".");
            pb.inheritIO();
            pb.command(commands);
            Process process = pb.start();
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
