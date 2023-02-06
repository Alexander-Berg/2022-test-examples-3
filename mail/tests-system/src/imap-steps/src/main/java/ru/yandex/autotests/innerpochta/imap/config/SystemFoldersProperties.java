package ru.yandex.autotests.innerpochta.imap.config;

import java.util.List;
import java.util.Properties;

import ru.yandex.autotests.innerpochta.imap.consts.folders.Folders;
import ru.yandex.qatools.properties.PropertyLoader;
import ru.yandex.qatools.properties.annotations.Property;
import ru.yandex.qatools.properties.annotations.Resource;
import ru.yandex.qatools.properties.annotations.With;
import ru.yandex.qatools.properties.providers.MapOrSyspropPathReplacerProvider;

import static com.google.common.collect.Lists.newArrayList;
import static com.sun.mail.imap.protocol.BASE64MailboxEncoder.encode;
import static ru.yandex.autotests.innerpochta.imap.config.ImapProperties.props;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 03.03.14
 * Time: 13:13
 */
@Resource.Classpath("folders/system_folders.${map.lang.value}.properties")
@With(MapOrSyspropPathReplacerProvider.class)
public class SystemFoldersProperties {
    private static SystemFoldersProperties instance;
    @Property("lang.value")
    private String lang = "undefined";
    @Property("inbox")
    private String inbox = "undefined";
    @Property("sent")
    private String sent = "undefined";
    @Property("deleted")
    private String deleted = "undefined";
    @Property("spam")
    private String spam = "undefined";
    @Property("drafts")
    private String drafts = "undefined";
    @Property("outgoing")
    private String outgoing = "undefined";

    public SystemFoldersProperties(String lang) {
        Properties map = new Properties();
        map.put("lang.value", lang);
        PropertyLoader.populate(this, map);
    }

    public SystemFoldersProperties() {
        this(props().getSystemFoldersLang());
    }

    public static SystemFoldersProperties systemFolders(String lang) {
        if (instance == null || !lang.equals(instance.getLang())) {
            instance = new SystemFoldersProperties(lang);
        }
        return instance;
    }

    public static SystemFoldersProperties systemFolders() {
        instance = new SystemFoldersProperties();
        return instance;
    }

    public String getInbox() {
        return encode(inbox);
    }

    public String getSent() {
        return encode(sent);
    }

    public String getDeleted() {
        return encode(deleted);
    }

    public String getSpam() {
        return encode(spam);
    }

    public String getDrafts() {
        return encode(drafts);
    }

    public String getOutgoing() {
        return encode(outgoing);
    }

    public String getLang() {
        return lang;
    }

    public List<String> getSystemFolders() {
        return newArrayList(
                getSent(),
                getDeleted(),
                getSpam(),
                getDrafts(),
                getOutgoing());
    }

    public List<String> getSystemFoldersWithInbox() {
        List<String> result = getSystemFolders();
        result.add(Folders.INBOX);
        return result;
    }
}
