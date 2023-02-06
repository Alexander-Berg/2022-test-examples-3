package ru.yandex.common.framework.user;

import junit.framework.TestCase;

import ru.yandex.common.framework.user.blackbox.BlackBoxUserInfo;
import ru.yandex.common.framework.user.blackbox.BlackBoxUserInfoParser;

/**
 * Author: Olga Bolshakova (obolshakova@yandex-team.ru)
 * Date: 17.06.2010
 */
public class BlackBoxUserInfoParserTest extends TestCase {

    private final BlackBoxUserInfoParser parser = new BlackBoxUserInfoParser();

    public void testSimple() throws Exception {
        final String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "    <doc>\n" +
                "    <uid hosted=\"0\" domid=\"\" domain=\"\">23075721</uid>\n" +
                "    <karma confirmed=\"0\">0</karma>\n" +
                "    <dbfield id=\"account_info.fio.uid\">Bolshakova Olga</dbfield>\n" +
                "    <dbfield id=\"accounts.login.uid\">olgab-87</dbfield>\n" +
                "    </doc>";
        final BlackBoxUserInfo userInfo = parser.parse(xml);
        assertEquals("olgab-87", userInfo.getLogin());
        assertEquals(23075721L, userInfo.getUserId());
        assertNull(userInfo.getValue(UserInfoField.PARAM_DISPLAY_NAME));
        assertNull(userInfo.getValue(UserInfoField.PARAM_PUBLIC_NAME));
    }

    public void testPublicName() throws Exception {
        final String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "    <doc>\n" +
                "    <uid hosted=\"0\" domid=\"\" domain=\"\">23075721</uid>\n" +
                "    <karma confirmed=\"0\">0</karma>\n" +
                "    <dbfield id=\"accounts.login.uid\">olgab-87</dbfield>\n" +
                "    <display_name>\n" +
                "        <name>Simple name</name>\n" +
                "        <public_name>Simple N.</public_name>\n" +
                "    </display_name>" +
                "    </doc>";
        final BlackBoxUserInfo userInfo = parser.parse(xml);
        assertEquals("olgab-87", userInfo.getLogin());
        assertEquals(23075721L, userInfo.getUserId());
        assertEquals("Simple name", userInfo.getValue(UserInfoField.PARAM_DISPLAY_NAME));
        assertEquals("Simple N.", userInfo.getValue(UserInfoField.PARAM_PUBLIC_NAME));
    }

    public void testPublicNameInvalid() throws Exception {
        // just to declare fallback in this case
        final String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "    <doc>\n" +
                "    <uid hosted=\"0\" domid=\"\" domain=\"\">23075721</uid>\n" +
                "    <karma confirmed=\"0\">0</karma>\n" +
                "    <dbfield id=\"accounts.login.uid\">olgab-87</dbfield>\n" +
                "    <display_name>\n" +
                "        <name><unexpected_node>Value</unexpected_node><node2>Value2</node2></name>\n" +
                "    </display_name>" +
                "    </doc>";
        final BlackBoxUserInfo userInfo = parser.parse(xml);
        assertEquals("olgab-87", userInfo.getLogin());
        assertEquals(23075721L, userInfo.getUserId());

        // not expected to happen ever
        assertEquals("ValueValue2", userInfo.getValue(UserInfoField.PARAM_DISPLAY_NAME));
        // case when public name is not received
        assertNull(userInfo.getValue(UserInfoField.PARAM_PUBLIC_NAME));
    }

    public void testEmpty() throws Exception {
        final String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<doc>\n" +
                "<uid hosted=\"0\" domid=\"\" domain=\"\"></uid>\n" +
                "<karma confirmed=\"0\">0</karma>\n" +
                "</doc>";
        final BlackBoxUserInfo userInfo = parser.parse(xml);

        assertNull(userInfo);
    }

    public void testError() throws Exception {
        final String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<doc>\n" +
                "<exception id=\"21\">ACCESS_DENIED</exception>\n" +
                "<error>BlackBox error: more useful text</error>\n" +
                "</doc>";

        try {
            parser.parse(xml);
        } catch (Exception e) {
            assertEquals("Failed to parse blackbox response", e.getMessage());
            assertEquals("Blackbox response failed - ACCESS_DENIED. BlackBox error: more useful text",
                    e.getCause().getMessage());
        }
    }
}
