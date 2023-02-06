package ru.yandex.market.mbo.gwt.server.security.development;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import ru.yandex.common.gwt.shared.User;


import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class DevelopmentBlackBoxManagerTest {

    private static final Integer UID = 1234;
    private static final String UNAME = "JackSparrow";

    DevelopmentBlackboxManager blackboxManager;

    @Before
    public void init() {
        blackboxManager = new DevelopmentBlackboxManager();
    }

    @Test
    public void testSuccess() throws Exception {
        User user = blackboxManager.getUserFromDocument(getUserInfoDocument(UID, UNAME));
        assertThat(user.getUid()).isEqualTo(UID.intValue());
        assertThat(user.getLogin()).isEqualTo(UNAME);
    }

    @Test
    public void testFailOnUidIsAbsent() throws Exception {
        User user = blackboxManager.getUserFromDocument(getUserInfoDocument(null, UNAME));
        assertThat(user).isNull();
    }

    @Test
    public void testFailOnUnameIsAbsent() throws Exception {
        User user = blackboxManager.getUserFromDocument(getUserInfoDocument(UID, null));
        assertThat(user).isNull();
    }

    private Document getUserInfoDocument(Integer uid, String regname) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(
            (
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<doc>\n" +
                    (uid == null ? "" : "<uid hosted=\"0\">" + uid + "</uid>\n") +
                    (regname == null ? "" : "<regname>" + regname + "</regname>\n") +
                    "</doc>\n"
            ).getBytes()
        ));
    }
}


