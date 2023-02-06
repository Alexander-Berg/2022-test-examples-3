package ru.yandex.autotests.market.stat.console;

import java.time.LocalDateTime;
import java.time.ZoneId;

import ru.yandex.autotests.market.console.command.CommandRunFailException;
import ru.yandex.autotests.market.stat.beans.Job;
import ru.yandex.autotests.market.common.attacher.Attacher;
import ru.yandex.autotests.market.tmsconsole.TmsConsole;
import ru.yandex.qatools.allure.annotations.Step;

/**
 * Created by entarrion on 06.04.15.
 */
public abstract class AbstractTmsConsole<T extends Job> implements ITmsConsole<T> {
    protected TmsConsole tmsConsole;

    @Step("Run command {0}")
    public LocalDateTime runJob(T job) {
        try {
            return  LocalDateTime.ofInstant(tmsConsole.runJob(job.getName()).toInstant(), ZoneId.systemDefault());
        } catch (CommandRunFailException e) {
            if (e.getMessage().contains("already exists with this identification")) {
                Attacher.attach("Job " + job.getName()+ " just triggered!");
            } else {
                Attacher.attach("Run job exception", e.getMessage());
            }
            return LocalDateTime.now();
        }
    }

    @Step("Stop command {0}")
    public void stopJob(T job) {
        try {
            tmsConsole.stopJob(job.getName());
        } catch (CommandRunFailException e) {
            if (e.getMessage().contains("Command run finished with errors")) {
                Attacher.attach("Stop job exception", e.getMessage());
                tmsConsole.stopJob(job.getName());
            }
        }


    }

    @Step("Resume default triggers for {0}")
    public void resumeJob(T job) {
        try {
            tmsConsole.resumeJob(job.getName());
        } catch (CommandRunFailException e) {
            if (e.getMessage().contains("Command run finished with errors")) {
                Attacher.attach("Resume job exception", e.getMessage());
                tmsConsole.resumeJob(job.getName());
            }
        }
    }

    @Step("Print default triggers for service")
    public void printDefaultTriggersXmlFromService() {
        String defaultTriggersXml = tmsConsole.getDefaultTriggersXml();
        Attacher.attach("Default triggers", defaultTriggersXml);
    }
}
