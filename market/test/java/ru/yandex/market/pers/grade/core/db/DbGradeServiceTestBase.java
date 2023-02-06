package ru.yandex.market.pers.grade.core.db;

import java.util.ArrayList;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.framework.filter.QueryFilter;
import ru.yandex.common.framework.filter.SimpleQueryFilter;
import ru.yandex.common.framework.user.UserInfo;
import ru.yandex.common.framework.user.UserInfoField;
import ru.yandex.market.pers.grade.client.model.GradeState;
import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.MockedTest;
import ru.yandex.market.pers.grade.core.model.core.AbstractGrade;
import ru.yandex.market.pers.grade.core.model.core.ModelGrade;
import ru.yandex.market.pers.grade.core.model.core.ShopGrade;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 17.01.2019
 */
public abstract class DbGradeServiceTestBase extends MockedTest {
    public static final SimpleQueryFilter.SimpleSelector<Integer> WITHOUT_KILLED_SELECTOR =
        new SimpleQueryFilter.SimpleSelector<Integer>("state", GradeState.DELETED.value()) {
            public String getOperator() {
                return "<>";
            }
        };

    protected static final Long UID = 1L;
    protected static final Long SHOP_ID = 2L;
    protected static final Long SHOP_ID_1 = 3L;
    protected static final Long MODEL_ID = 4L;
    protected static final Long MODEL_ID_1 = 5L;
    protected static final String TEST_YANDEXUID = "234098203749";
    protected static final String TEST_SESSIONUID = "234098203749";

    protected static boolean same(AbstractGrade thisGrade, AbstractGrade thatGrade) {
        if (thisGrade.getModState() != thatGrade.getModState()) {
            return false;
        }
        if (thisGrade.getResourceId() != null ? !thisGrade.getResourceId().equals(thatGrade.getResourceId()) : thatGrade
            .getResourceId() != null) {
            return false;
        }
        if (thisGrade.getState() != thatGrade.getState()) {
            return false;
        }
        if (thisGrade.getText() != null ? !thisGrade.getText().equals(thatGrade.getText()) : thatGrade
            .getText() != null) {
            return false;
        }
        return true;
    }

    protected static boolean oneOf(AbstractGrade thisGrade, AbstractGrade... thatGrades) {
        boolean result = false;
        for (AbstractGrade g : thatGrades) {
            result = result || same(thisGrade, g);
        }
        return result;
    }

    @Autowired
    protected DbGradeService gradeService;
    @Autowired
    protected PartnerShopGradeService partnerShopGradeService;
    @Autowired
    protected GradeCreator gradeCreator;

    protected static final UserInfo userInfo = new UserInfo() {
        @Override
        public String getValue(UserInfoField field) {
            return null;
        }

        @Override
        public long getUserId() {
            return UID;
        }

        @Override
        public String getLogin() {
            return "Ivan Ivanov";
        }
    };

    protected static final String TEST_HTTPHEADERS = "Host: n4p.ru.redtram.com\n" +
        "User-Agent: Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.1) Gecko/20061204 Firefox/2.0.0.1\n" +
        "Accept: */*\n" +
        "Accept-Language: en-us,en;q=0.5\n" +
        "Accept-Encoding: gzip,deflate\n" +
        "Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7\n" +
        "Cookie: GX=DQAAAHsAAACkZGqO4xWvKlx16VqU3AcRofcBLNDFa0hRYXwmD6z_gYpR0eZnorPHme1r-2SWIP4cTQKTKow2WYABw-3KqGB3v2EvQA_2ljRxN5mUh_t_bMZzja7JfruPSC1uehqjhVB9ETZTIg9UQarsCaMv1VUINkP-xFv8LIVzFLOiOn1HfA; gmailchat=vladimir.gorovoy@gmail.com/440138; GMAIL_AT=7bc7506b1d308852-11072447ad3; SID=DQAAAHoAAABUaO-EQHUh7im81O9ZXyxkpi-96U5yTlNQ15YMQTFI92iTaxvcMtLDTN14igfsn8grrpSe_MQMWpIjh_9Wr8pN3lP3zqghSu77-zVMgEPA6DjFBj6BMweRJGLS6Y2_yK0yZNveGr1xWQzH5mT81y7_jDD2wW7yJoF-slnKLd4a6Q; rememberme=true; PREF=ID=cec347b671f0e946:TB=2:TM=1158662967:LM=1164904953:L=0n45aBwI:DV=AA:GM=1:S=gg41TS5dXvIWaw5L; GMAIL_HELP=hosted:0; S=gmail=bbITG52mrny1VNe1XGFZzw:gmail_yj=jgWOyMMudmwQLFUfEGkaUw:gmproxy=tARlcgmQeB4:gmproxy_yj=AWef9ylnOr0:gmproxy_yj_sub=FQymr2mlvtE; TZ=-180\n" +
        "Keep-Alive: 300\n" +
        "Connection: keep-alive\n" +
        "Referer: http://www.newsru.com/sport/30jan2007/derl.html";
    protected final String TEST_CONTEXT = "http://www.ya.ru/";

    public AbstractGrade createShopLoad() throws Exception {
        return createShopLoad(SHOP_ID);
    }

    public AbstractGrade createModelLoad(long modelId) {
        AbstractGrade result = createTestModelGradePassport(modelId);
        gradeCreator.createGrade(result);
        return result;
    }

    public AbstractGrade createModelLoadWithoutPassport(long modelId) {
        AbstractGrade result = createTestModelGradeWithoutPassport(modelId);
        gradeCreator.createGradeUnlogin(result, TEST_SESSIONUID);
        return result;
    }

    public AbstractGrade createShopLoad(long shopId) {
        AbstractGrade result = createTestShopGrade(shopId);
        gradeCreator.createGrade(result);
        return result;
    }

    protected AbstractGrade createShopGradeWithouthLoadLongHeaders() {
        final ShopGrade testGrade = createTestShopGradeWithoutPassport();
        createGrade(testGrade);
        return testGrade;
    }

    protected AbstractGrade createModelGradeWithouthLoadLongHeaders(long modelId) {
        final ModelGrade testGrade = createTestModelGradeWithoutPassport(modelId);
        createGrade(testGrade);
        return testGrade;
    }

    protected void createGrade(AbstractGrade testGrade) {
        gradeCreator.createGradeUnlogin(testGrade, TEST_SESSIONUID);
    }

    protected static SimpleQueryFilter createAuthorFilterForShopGrades() {
        return createAuthorAndShopFilter(null);
    }

    protected static SimpleQueryFilter createNoAuthorAndShopFilter(Long shopId) {
        final SimpleQueryFilter filter = new SimpleQueryFilter();
        filter.addSorter("state");
        filter.addSelector("author_id", null);
        if (shopId != null) {
            filter.addSelector("resource_id", shopId);
        }
        filter.addSelector(WITHOUT_KILLED_SELECTOR);
        return filter;
    }

    protected static SimpleQueryFilter createModelAndNoAuthorFilter(Long modelId) {
        final SimpleQueryFilter filter = new SimpleQueryFilter();
        filter.addSorter("state");
        filter.addSelector("resource_id", modelId);
        filter.addSelector("author_id", null);
        filter.addSelector(WITHOUT_KILLED_SELECTOR);
        return filter;
    }

    protected static SimpleQueryFilter createAuthorFilter() {
        return createAuthorFilter(UID);
    }

    protected static SimpleQueryFilter createAuthorFilter(Long author_id) {
        final SimpleQueryFilter filter = new SimpleQueryFilter();
        filter.addSorter("state");
        filter.addSelector("author_id", author_id);
        filter.addSelector(WITHOUT_KILLED_SELECTOR);
        return filter;
    }

    protected static SimpleQueryFilter createAuthorAndShopFilter(Long shopId) {
        final SimpleQueryFilter filter = new SimpleQueryFilter();
        filter.addSorter("state");
        String SHOP_NAME_SORTER_SQL =
            "(select upper(name) from ext_mbi_datasource m where m.shop_id = resource_id)";
        filter.addSorter(SHOP_NAME_SORTER_SQL);
        filter.addSelector("author_id", UID);
        if (shopId != null) {
            filter.addSelector("resource_id", shopId);
        }
        filter.addSelector(WITHOUT_KILLED_SELECTOR);
        return filter;
    }

    protected static SimpleQueryFilter createAuthorNewFilter() {
        SimpleQueryFilter filter = createAuthorFilterForShopGrades();
        filter.addSelector("state", GradeState.LAST.value());
        return filter;
    }

    protected static QueryFilter createSessionUidFilter() {
        final SimpleQueryFilter filter = new SimpleQueryFilter();
        filter.addSorter("state");
        // filter.addSorter("resource_id_id");
        String SHOP_NAME_SORTER_SQL =
            "(select upper(name) from ext_mbi_datasource m where m.shop_id = resource_id)";
        filter.addSorter(SHOP_NAME_SORTER_SQL);
        filter.addSelector("author_id", null);
        filter.addSelector("yandexuid", TEST_SESSIONUID);
        filter.addSelector(WITHOUT_KILLED_SELECTOR);
        return filter;
    }

    protected String getUniqueSessionId() {
        String baseString = TEST_YANDEXUID + System.nanoTime();
        return DigestUtils.md5Hex(baseString);
    }

    protected String createText(final int length) {
        final StringBuffer text = new StringBuffer();
        // System.out.println("\uC383");
        for (int i = 0; i < length; i++) {
            // text.append("\u5639");
            text.append('�');
            // text.append("\0490");
            // text.append("\uC999");
            // text.append("?");
        }
        System.out.println(text.length());
        System.out.println(text);
        return text.toString();
    }

    protected ShopGrade createTestShopGrade() {
        return createTestShopGrade(UUID.randomUUID().toString());
    }

    protected ShopGrade createTestShopGrade(long shopId) {
        return createTestShopGrade(shopId, UUID.randomUUID().toString());
    }

    protected ShopGrade createTestShopGrade(final String text) {
        return createTestShopGrade(SHOP_ID, text);
    }

    protected ShopGrade createTestShopGrade(long shopId, String text) {
        return createTestShopGradePassport(shopId, text);
    }

    protected ShopGrade createTestShopGrade(long uid, long shopId, String text, int averageGrade) {
        ShopGrade grade = GradeCreator.constructShopGrade(shopId, uid);
        grade.setText(text);
        grade.setAverageGrade(averageGrade);
        return grade;
    }

    protected ShopGrade createTestShopGradePassport(long shopId, String text) {
        ShopGrade grade = GradeCreator.constructShopGrade(shopId, UID);
        grade.setText(text);
        grade.setGr0(-2);
        return grade;
    }

    protected ShopGrade createTestShopGradeWithoutPassport() {
        ShopGrade grade = GradeCreator.constructShopGrade(SHOP_ID, null);
        grade.setGr0(-2);
        return grade;
    }

    protected ModelGrade createTestModelGradePassport(long modelId) {
        ModelGrade grade = GradeCreator.constructModelGrade(modelId, UID);
        grade.setText(UUID.randomUUID().toString());
        grade.setModState(ModState.APPROVED);
        grade.setGr0(-2);
        return grade;
    }

    protected ModelGrade createTestModelGradePassportNoText(long modelId) {
        ModelGrade grade = GradeCreator.constructModelGrade(modelId, UID);
        grade.setText(null);
        grade.setPro(null);
        grade.setContra(null);
        grade.setPhotos(new ArrayList<>());
        grade.setModState(ModState.APPROVED);
        grade.setGr0(-2);
        return grade;
    }

    protected ModelGrade createTestModelGradeWithoutPassport(long modelId) {
        ModelGrade grade = GradeCreator.constructModelGrade(modelId, null);
        grade.setText(UUID.randomUUID().toString());
        grade.setModState(ModState.APPROVED);
        grade.setGr0(-2);
        return grade;
    }
}
