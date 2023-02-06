package ru.yandex.market.pers.tms.export.abo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import ru.yandex.common.framework.user.UserInfoField;
import ru.yandex.common.framework.user.UserInfoService;
import ru.yandex.common.framework.user.blackbox.BlackBoxUserInfo;
import ru.yandex.market.pers.grade.core.util.StaffClient;
import ru.yandex.market.pers.tms.MockedPersTmsTest;

import static org.mockito.Mockito.when;

/**
 * @author bahus
 */
public class ModerationExportUidPreprocessorTest extends MockedPersTmsTest {

    private final long passportUid = 1L;
    private final String passportLogin = "login";

    @Autowired
    private StaffClient staffMock;

    @Autowired
    @Qualifier("blackBoxUserService")
    private UserInfoService userInfoMock;

    private ModerationExportUidPreprocessor preprocessor;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Before
    public void setUp() {
        preprocessor = new ModerationExportUidPreprocessor(userInfoMock, pgJdbcTemplate, pgJdbcTemplate, staffMock);
    }

    @Test
    public void testBlankShot() {
        // blank shot, nothing moderated
        preprocessor.preprocess();
        Mockito.verify(userInfoMock, Mockito.never()).getUserInfo(passportUid);
    }

    @Test
    public void testEmptyPassportResponse() {
        // adding something moderated
        addComplaint(passportUid);

        // no mapping, empty passport response
        when(userInfoMock.getUserInfo(passportUid)).thenReturn(null);
        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage("ModerationExport contains moderator(s), unknown by BlackBox");

        preprocessor.preprocess();

        Mockito.verify(userInfoMock, Mockito.only()).getUserInfo(passportUid);
        Mockito.verify(staffMock, Mockito.never()).getPersonUidByExternalLoginAccurate(Mockito.any());
    }

    @Test
    public void testEmptyStaffResponse() {
        BlackBoxUserInfo userInfo = new BlackBoxUserInfo(passportUid);
        userInfo.addField(UserInfoField.LOGIN, passportLogin);

        // adding something moderated
        addComplaint(passportUid);

        // no mapping, some passport response, empty staff response
        when(userInfoMock.getUserInfo(passportUid)).thenReturn(userInfo);
        when(staffMock.getPersonUidByExternalLoginAccurate(passportLogin)).thenReturn(Optional.empty());
        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage("ModerationExport contains moderator(s) without Staff UID");

        preprocessor.preprocess();

        Mockito.verify(userInfoMock, Mockito.only()).getUserInfo(passportUid);
        Mockito.verify(staffMock, Mockito.only()).getPersonUidByExternalLoginAccurate(passportLogin);
    }

    @Test
    public void testHappyPath() {
        BlackBoxUserInfo userInfo = new BlackBoxUserInfo(passportUid);
        userInfo.addField(UserInfoField.LOGIN, passportLogin);
        long staffUid = 11L;

        // adding something moderated
        addComplaint(passportUid);

        // no mapping, some passport response, some staff response and mapping as result
        Assert.assertTrue(getMapping().isEmpty());
        when(userInfoMock.getUserInfo(passportUid)).thenReturn(userInfo);
        when(staffMock.getPersonUidByExternalLoginAccurate(passportLogin)).thenReturn(Optional.of(staffUid));

        preprocessor.preprocess();

        Mockito.verify(userInfoMock, Mockito.only()).getUserInfo(passportUid);
        Mockito.verify(staffMock, Mockito.only()).getPersonUidByExternalLoginAccurate(passportLogin);
        Assert.assertEquals(Map.of(passportUid, staffUid), getMapping());

        // reset mock invocations count
        Mockito.reset(userInfoMock);
        Mockito.reset(staffMock);

        // existing mapping
        Assert.assertEquals(Map.of(passportUid, staffUid), getMapping());
        preprocessor.preprocess();
        Mockito.verify(userInfoMock, Mockito.never()).getUserInfo(passportUid);
    }

    private void addComplaint(long passportId) {
        pgJdbcTemplate.update("INSERT INTO GRADE_COMPLAINT " +
            "(MODERATOR_ID, MOD_TIME, STATE, TYPE, REASON_ID, SOURCE_ID, ID) " +
            "VALUES (?, now() - interval '1' day, 1, 1, 1, ?, ?)", passportId, passportId, passportId);
    }

    private Map<Long, Long> getMapping() {
        return pgJdbcTemplate.query("SELECT * FROM MODERATOR_ID_MAPPING", new ResultSetExtractor<Map<Long, Long>>() {
            @Override
            public Map<Long, Long> extractData(ResultSet rs) throws SQLException, DataAccessException {
                Map<Long, Long> result = new HashMap<>();
                while (rs.next()) {
                    result.put(rs.getLong("PASSPORT_UID"), rs.getLong("STAFF_UID"));
                }
                return result;
            }
        });
    }
}
