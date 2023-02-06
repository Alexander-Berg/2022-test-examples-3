package ru.yandex.market.pers.tms.export;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author imelnikov
 */
public class UserInfoDumperTest {
    @Autowired
    private UserInfoDumper userInfoDumper;

    @Test
    @Ignore
    public void testUserAvatars() throws Exception {
        ExportProcessor.writeLocalFile(userInfoDumper, "user-avatar.txt");
        Files.delete(Paths.get("user-avatar.txt"));
    }

    @Test
    public void testJsonUserInfo() {
        String result = UserInfoDumper.createJsonString(3336908, 2);
        String expectedResult = "{" +
            "\"user\": {" +
            "\"id\": 3336908," +
            "\"grades\": 2" +
            "}" +
            "}\n";
        Assert.assertEquals(expectedResult, result);
    }
}
