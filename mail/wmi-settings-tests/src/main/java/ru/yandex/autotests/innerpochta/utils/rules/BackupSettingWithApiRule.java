package ru.yandex.autotests.innerpochta.utils.rules;

import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.LogManager;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import ru.yandex.autotests.innerpochta.utils.oper.GetAll;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Oper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.String.format;
import static java.net.URLEncoder.encode;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static ru.yandex.autotests.innerpochta.utils.oper.UpdateParams.updateParams;
import static ru.yandex.autotests.innerpochta.utils.oper.UpdateProfile.updateProfile;
import static ru.yandex.autotests.innerpochta.wmi.core.filter.log.LoggerFilterBuilder.log;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.EmptyObj.empty;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsApiObj.settings;
import static ru.yandex.autotests.innerpochta.utils.SettingsUtils.getSettingValue;
/**
 * Created with IntelliJ IDEA.
 * User: lanwen
 * Date: 25.03.13
 * Time: 21:32
 */
public class BackupSettingWithApiRule extends TestWatcher {
    private String uid;
    private String settingName;

    private String backupValue;
    private Oper<?> updateOper;

    public BackupSettingWithApiRule backup(String settingName) {
        this.settingName = settingName;
        return this;
    }

    private BackupSettingWithApiRule(String uid) {
        this.uid = uid;
    }

    private BackupSettingWithApiRule setUpdateOper(Oper<?> updateOper) {
        this.updateOper = updateOper;
        return this;
    }

    public static BackupSettingWithApiRule profile(String uid) {
        return new BackupSettingWithApiRule(uid)
                .setUpdateOper(
                        updateProfile(empty())
                                .log(log().onlyIfError())
                );
    }

    public static BackupSettingWithApiRule params(String uid) {
        return new BackupSettingWithApiRule(uid)
                .setUpdateOper(
                        updateParams(empty())
                                .log(log().onlyIfError())
                );
    }

    @Override
    protected void starting(Description description) {
        if (description.getAnnotation(DoNotBackupAny.class) != null) {
            return;
        }
        
        backupValue = getSettingValue(GetAll.getAll(settings(uid).askValidator().json())
                .get().via(new DefaultHttpClient())
                .log(log().onlyIfError())
                .statusCodeShouldBe(HttpStatus.OK_200)
                .toString() ,settingName);
    }

    @Override
    protected void finished(Description description) {
        if (description.getAnnotation(DoNotBackupAny.class) != null) {
            return;
        }
        
        if (backupValue == null) {
            return;
        }
        LogManager.getLogger(this.getClass()).debug(
                format("Reverting to saved value... %s -> %s", settingName,
                        defaultIfEmpty(backupValue, "<empty>"))
        );
        updateOper
                .params(settings(uid).set(true, settingName, encode(backupValue)))
                .post().via(new DefaultHttpClient());
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface DoNotBackupAny {
        
    }
}
