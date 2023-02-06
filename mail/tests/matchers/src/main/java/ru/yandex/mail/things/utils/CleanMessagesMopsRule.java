package ru.yandex.mail.things.utils;

import ru.yandex.mail.common.credentials.UserCredentials;
import ru.yandex.mail.common.rules.BeforeAfterOptionalRule;
import ru.yandex.mail.tests.hound.Folders;
import ru.yandex.mail.tests.hound.HoundApi;
import ru.yandex.mail.tests.hound.HoundProperties;
import ru.yandex.mail.tests.hound.HoundResponses;
import ru.yandex.mail.tests.hound.generated.FolderSymbol;
import ru.yandex.mail.tests.mops.Mops;
import ru.yandex.mail.tests.mops.MopsResponses;
import ru.yandex.mail.tests.mops.source.FidSource;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.ArrayList;
import java.util.List;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;
import static ru.yandex.mail.common.properties.CoreProperties.props;
import static ru.yandex.mail.things.matchers.IsThereMessagesMatcher.hasMsgsIn;
import static ru.yandex.mail.things.matchers.WithWaitFor.withWaitFor;
import static org.hamcrest.Matchers.*;

public class CleanMessagesMopsRule extends BeforeAfterOptionalRule<CleanMessagesMopsRule> {
    private List<FolderSymbol> symbolsToClean = new ArrayList<>();
    private UserCredentials rule;
    private boolean cleanAllFolders = false;
    private boolean shouldWait = false;
    private static final long WAIT_TIME = MINUTES.toMillis(10);

    public static CleanMessagesMopsRule with(UserCredentials rule) {
        return new CleanMessagesMopsRule(rule);
    }

    public CleanMessagesMopsRule(UserCredentials rule) {
        this.rule = rule;
    }

    public CleanMessagesMopsRule allfolders() {
        cleanAllFolders = true;
        return this;
    }

    public CleanMessagesMopsRule shouldWait(boolean v) {
        this.shouldWait = v;
        return this;
    }

    public CleanMessagesMopsRule inbox() {
        symbolsToClean.add(FolderSymbol.INBOX);
        return this;
    }

    public CleanMessagesMopsRule outbox() {
        symbolsToClean.add(FolderSymbol.OUTBOX);
        return this;
    }

    public CleanMessagesMopsRule draft() {
        symbolsToClean.add(FolderSymbol.DRAFT);
        return this;
    }

    public CleanMessagesMopsRule deleted() {
        symbolsToClean.add(FolderSymbol.TRASH);
        return this;
    }

    @Step("[RULE]: Очищаем папки")
    @Override
    public void call() {
        try {
            Folders f = Folders.folders(
                    HoundApi.apiHound(
                            HoundProperties.properties()
                                    .houndUri(),
                            props().getCurrentRequestId()
                    )
                            .folders()
                            .withUid(rule.account().uid())
                            .post(shouldBe(HoundResponses.ok200()))
            );

            List<String> fids;
            if (cleanAllFolders) {
                fids = f.fids();
            } else {
                fids = new ArrayList<>();

                for (FolderSymbol symbol : symbolsToClean) {
                    fids.add(f.fid(symbol));
                }
            }

            for (String fid : fids) {
                Mops.purge(rule, new FidSource(fid)).post(shouldBe(MopsResponses.ok()));
            }

            if (shouldWait) {
                for (String fid : fids) {
                    assertThat(rule, withWaitFor(not(hasMsgsIn(1, fid)), WAIT_TIME));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Невозможно выполнить очистку папки: " + e.getMessage(), e);
        }
    }
}
