package ru.yandex.market.pers.grade.web.grade;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.common.framework.pager.Pager;
import ru.yandex.market.pers.grade.client.dto.GradePager;
import ru.yandex.market.pers.grade.client.dto.grade.GradeResponseDto;
import ru.yandex.market.pers.grade.client.dto.grade.ShopGradeResponseDto;
import ru.yandex.market.pers.grade.client.dto.partner.PartnerGradeStat;
import ru.yandex.market.pers.grade.client.model.Anonymity;
import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.db.DbGradeAdminService;
import ru.yandex.market.pers.grade.core.db.model.LastCommentType;
import ru.yandex.market.pers.grade.core.db.model.Sort;
import ru.yandex.market.pers.grade.core.db.model.SortType;
import ru.yandex.market.pers.grade.core.model.core.ShopGrade;
import ru.yandex.market.pers.grade.core.model.core.VerifiedType;
import ru.yandex.market.pers.grade.core.moderation.Object4Moderation;
import ru.yandex.market.pers.grade.ugc.api.dto.ShopListDto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.grade.core.model.core.BusinessIdEntityType.SHOP;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 22.01.2019
 */
public class GradeControllerPiShopTest extends GradeControllerBaseTest {
    private static final long UID = 813901384;

    @Autowired
    private DbGradeAdminService gradeAdminService;

    @Autowired
    private GradeCreator gradeCreator;

    private void prepareMatView() {
        pgJdbcTemplate.update("refresh materialized view mv_partner_grade");
    }

    @Test
    public void testGetSimpleShopGrades() {
        long shopId = 384719247;

        long[] gradeIds = {
            createTestGradeOk(UID, shopId, 2),
            createTestGradeOk(UID + 1, shopId, 4),
            createTestGrade(UID + 2, shopId, 4, ModState.UNMODERATED),
            createTestGrade(UID + 3, shopId, 3, ModState.REJECTED),
        };

        // sort desc
        prepareMatView();
        GradePager<ShopGradeResponseDto> shopGrades = getShopGrades(shopId,
            Sort.desc(SortType.ID),
            req -> req
        );

        assertEquals(2, shopGrades.getData().size());

        assertGradeSimple(shopGrades.getData().get(0), gradeIds[1], 4, UID + 1);
        assertGradeSimple(shopGrades.getData().get(1), gradeIds[0], 2, UID);

        // sort asc
        shopGrades = getShopGrades(shopId,
            Sort.asc(SortType.ID),
            req -> req
        );

        assertEquals(2, shopGrades.getData().size());

        assertGradeSimple(shopGrades.getData().get(0), gradeIds[0], 2, UID);
        assertGradeSimple(shopGrades.getData().get(1), gradeIds[1], 4, UID + 1);
    }

    @Test
    public void testGetSimpleShopGradesSortDateDesc() {
        long shopId = 384719247;

        long[] gradeIds = {
            createTestGradeOk(UID, shopId, 2),
            createTestGradeOk(UID + 1, shopId, 4),
            createTestGradeOk(UID + 2, shopId, 3),
        };

        pgJdbcTemplate.update("update GRADE \n" +
                "set CR_TIME = now() - interval '1' day\n" +
                "where id = ?",
            gradeIds[1]);
        pgJdbcTemplate.update("update GRADE \n" +
                "set CR_TIME = now() - interval '2' day\n" +
                "where id = ?",
            gradeIds[2]);

        prepareMatView();
        GradePager<ShopGradeResponseDto> shopGrades = getShopGrades(shopId,
            Sort.desc(SortType.DATE),
            req -> req
        );

        assertEquals(3, shopGrades.getData().size());

        assertGradeSimple(shopGrades.getData().get(0), gradeIds[0], 2, UID);
        assertGradeSimple(shopGrades.getData().get(1), gradeIds[1], 4, UID + 1);
        assertGradeSimple(shopGrades.getData().get(2), gradeIds[2], 3, UID + 2);
    }

    @Test
    public void testGetSimpleShopGradesSortGrade() {
        long shopId = 384719247;

        long[] gradeIds = {
            createTestGradeOk(UID, shopId, 2),
            createTestGradeOk(UID + 1, shopId, 4),
            createTestGradeOk(UID + 2, shopId, 3),
            createTestGradeOk(UID + 3, shopId, 3),
        };

        pgJdbcTemplate.update("update GRADE \n" +
                "set CR_TIME = now() - interval '1' day\n" +
                "where id = ?",
            gradeIds[1]);
        pgJdbcTemplate.update("update GRADE \n" +
                "set CR_TIME = now() - interval '3' day\n" +
                "where id = ?",
            gradeIds[2]);
        pgJdbcTemplate.update("update GRADE \n" +
                "set CR_TIME = now() - interval '2' day\n" +
                "where id = ?",
            gradeIds[3]);

        // sort desc
        prepareMatView();
        GradePager<ShopGradeResponseDto> shopGrades = getShopGrades(shopId,
            Sort.desc(SortType.GRADE),
            req -> req
        );

        assertEquals(4, shopGrades.getData().size());

        assertGradeSimple(shopGrades.getData().get(0), gradeIds[1], 4, UID + 1);
        assertGradeSimple(shopGrades.getData().get(1), gradeIds[3], 3, UID + 3);
        assertGradeSimple(shopGrades.getData().get(2), gradeIds[2], 3, UID + 2);
        assertGradeSimple(shopGrades.getData().get(3), gradeIds[0], 2, UID);

        // sort asc
        shopGrades = getShopGrades(shopId,
            Sort.asc(SortType.GRADE),
            req -> req
        );

        assertEquals(4, shopGrades.getData().size());

        assertGradeSimple(shopGrades.getData().get(0), gradeIds[0], 2, UID);
        assertGradeSimple(shopGrades.getData().get(1), gradeIds[3], 3, UID + 3);
        assertGradeSimple(shopGrades.getData().get(2), gradeIds[2], 3, UID + 2);
        assertGradeSimple(shopGrades.getData().get(3), gradeIds[1], 4, UID + 1);
    }

    @Test
    public void testGetFilterAvgVal() {
        long shopId = 384719247;

        long[] gradeIds = {
            createTestGradeOk(UID, shopId, 2),
            createTestGradeOk(UID + 1, shopId, 4),
            createTestGradeOk(UID + 2, shopId, 3),
            createTestGradeOk(UID + 3, shopId, 3),
        };

        prepareMatView();
        GradePager<ShopGradeResponseDto> shopGrades = getShopGrades(shopId,
            Sort.desc(SortType.ID),
            req -> req.param("gradeValue", String.valueOf(3))
        );

        assertEquals(2, shopGrades.getData().size());

        assertGradeSimple(shopGrades.getData().get(0), gradeIds[3], 3, UID + 3);
        assertGradeSimple(shopGrades.getData().get(1), gradeIds[2], 3, UID + 2);
    }

    @Test
    public void testGetAnonymous() {
        long shopId = 384719247;

        ShopGrade testGrade = buildTestGrade(UID, shopId, 2, ModState.APPROVED);
        testGrade.setAnonymous(null);

        ShopGrade testGrade2 = buildTestGrade(UID + 1, shopId, 2, ModState.APPROVED);
        testGrade2.setAnonymous(Anonymity.HIDE_NAME);

        ShopGrade testGrade3 = buildTestGrade(UID + 2, shopId, 2, ModState.APPROVED);
        testGrade3.setAnonymous(Anonymity.NONE);

        long[] gradeIds = {
            doSaveGrade(testGrade),
            doSaveGrade(testGrade2),
            doSaveGrade(testGrade3)
        };

        prepareMatView();
        GradePager<ShopGradeResponseDto> shopGrades = getShopGrades(shopId,
            Sort.asc(SortType.ID),
            req -> req.param("gradeValue", String.valueOf(2))
        );

        assertEquals(3, shopGrades.getData().size());

        assertGradeSimple(shopGrades.getData().get(0), gradeIds[0], 2, UID);
        assertGradeSimple(shopGrades.getData().get(1), gradeIds[1], 2, UID + 1);
        assertGradeSimple(shopGrades.getData().get(2), gradeIds[2], 2, UID + 2);

        assertEquals(Anonymity.NONE, shopGrades.getData().get(0).getAnonymity());
        assertEquals(Anonymity.HIDE_NAME, shopGrades.getData().get(1).getAnonymity());
        assertEquals(Anonymity.NONE, shopGrades.getData().get(2).getAnonymity());
    }

    @Test
    public void testGetFilterGradeId() {
        long shopId = 384719247;

        long[] gradeIds = {
            createTestGradeOk(UID, shopId, 2),
            createTestGradeOk(UID + 1, shopId, 4),
            createTestGradeOk(UID + 2, shopId, 3),
            createTestGradeOk(UID + 3, shopId, 3),
        };

        prepareMatView();
        GradePager<ShopGradeResponseDto> shopGrades = getShopGrades(shopId,
            Sort.desc(SortType.ID),
            req -> req.param("gradeId", String.valueOf(gradeIds[2]))
        );

        assertEquals(1, shopGrades.getData().size());

        assertGradeSimple(shopGrades.getData().get(0), gradeIds[2], 3, UID + 2);
    }

    @Test
    public void testGetFilterVerified() {
        long shopId = 384719247;

        long[] gradeIds = {
            createTestGradeOk(UID, shopId, 2),
            createTestGradeOk(UID + 1, shopId, 4),
            createTestGradeOk(UID + 2, shopId, 3),
            createTestGradeOk(UID + 3, shopId, 3),
        };

        makeVerified(gradeIds[0], true, false);
        makeVerified(gradeIds[1], false, true);
        makeVerified(gradeIds[2], true, true);
        makeVerified(gradeIds[3], false, false);

        // verified
        prepareMatView();
        GradePager<ShopGradeResponseDto> shopGrades = getShopGrades(shopId,
            Sort.asc(SortType.ID),
            req -> req.param("verified", "true")
        );

        assertEquals(3, shopGrades.getData().size());

        assertGradeSimple(shopGrades.getData().get(0), gradeIds[0], 2, UID);
        assertGradeSimple(shopGrades.getData().get(1), gradeIds[1], 4, UID + 1);
        assertGradeSimple(shopGrades.getData().get(2), gradeIds[2], 3, UID + 2);

        // not verified
        shopGrades = getShopGrades(shopId,
            Sort.asc(SortType.ID),
            req -> req.param("verified", "false")
        );
        assertEquals(1, shopGrades.getData().size());

        assertGradeSimple(shopGrades.getData().get(0), gradeIds[3], 3, UID + 3);
    }

    @Test
    public void testGetCommentType() {
        long shopId = 384719247;

        long[] gradeIds = {
            createTestGradeOk(UID, shopId, 2),
            createTestGradeOk(UID + 1, shopId, 4),
            createTestGradeOk(UID + 2, shopId, 3),
            createTestGradeOk(UID + 3, shopId, 3),
            createTestGradeOk(UID + 4, shopId, 3),
        };

        setCommentType(gradeIds[1], LastCommentType.SHOP);
        setCommentType(gradeIds[2], LastCommentType.USER);

        // link together 1+4 grades manually
        pgJdbcTemplate.update("update grade set fix_id = ? where id in (?,?)",
            gradeIds[1],
            gradeIds[1],
            gradeIds[4]
        );

        // user comment is last
        prepareMatView();
        GradePager<ShopGradeResponseDto> shopGrades = getShopGrades(shopId,
            Sort.desc(SortType.ID),
            req -> req.param("lastComment", LastCommentType.USER.getCode())
        );

        assertEquals(1, shopGrades.getData().size());

        assertGradeSimple(shopGrades.getData().get(0), gradeIds[2], 3, UID + 2);
        shopGrades.getData().forEach(g -> assertEquals(g.getId(), g.getFixId()));

        // shop comment is last
        shopGrades = getShopGrades(shopId,
            Sort.desc(SortType.ID),
            req -> req.param("lastComment", LastCommentType.SHOP.getCode())
        );

        // both actual and linked have last=shop type
        assertEquals(2, shopGrades.getData().size());

        assertGradeSimple(shopGrades.getData().get(0), gradeIds[4], 3, UID + 4);
        assertGradeSimple(shopGrades.getData().get(1), gradeIds[1], 4, UID + 1);
        shopGrades.getData().forEach(g->assertEquals(gradeIds[1], g.getFixId().longValue()));

        // none comment is last
        shopGrades = getShopGrades(shopId,
            Sort.desc(SortType.ID),
            req -> req.param("lastComment", LastCommentType.NONE.getCode())
        );

        assertEquals(2, shopGrades.getData().size());

        assertGradeSimple(shopGrades.getData().get(0), gradeIds[3], 3, UID + 3);
        assertGradeSimple(shopGrades.getData().get(1), gradeIds[0], 2, UID);
        shopGrades.getData().forEach(g -> assertEquals(g.getId(), g.getFixId()));
    }

    @Test
    public void testGetFilterRegionId() {
        long shopId = 384719247;
        int regionIdBase = 682938;
        int regionIdOther = 28471293;

        long[] gradeIds = {
            createTestGradeRegion(UID, shopId, 2, regionIdBase),
            createTestGradeRegion(UID + 1, shopId, 4, regionIdOther),
            createTestGradeRegion(UID + 2, shopId, 3, regionIdBase),
            createTestGradeRegion(UID + 3, shopId, 3, regionIdBase),
        };

        // search base region
        prepareMatView();
        GradePager<ShopGradeResponseDto> shopGrades = getShopGrades(shopId,
            Sort.asc(SortType.ID),
            req -> req.param("regionId", String.valueOf(regionIdBase))
        );

        assertEquals(3, shopGrades.getData().size());

        assertGradeSimple(shopGrades.getData().get(0), gradeIds[0], 2, UID);
        assertGradeSimple(shopGrades.getData().get(1), gradeIds[2], 3, UID + 2);
        assertGradeSimple(shopGrades.getData().get(2), gradeIds[3], 3, UID + 3);

        // search other region
        shopGrades = getShopGrades(shopId,
            Sort.asc(SortType.ID),
            req -> req.param("regionId", String.valueOf(regionIdOther))
        );
        assertEquals(1, shopGrades.getData().size());

        assertGradeSimple(shopGrades.getData().get(0), gradeIds[1], 4, UID + 1);
    }

    @Test
    public void testGetFilterByClone() {
        long group_id = 237461;

        long shopId = 384719247;
        long shopIdClone = 7098347;
        long shopIdOther = 89671234;

        // create clones
        pgJdbcTemplate.update("insert into ext_shop_business_id(business_id, shop_id, type) values(?, ?, ?)", group_id, shopId, SHOP.getValue());
        pgJdbcTemplate.update("insert into ext_shop_business_id(business_id, shop_id, type) values(?, ?, ?)", group_id, shopIdClone, SHOP.getValue());

        long[] gradeIds = {
            createTestGradeOk(UID, shopId, 2),
            createTestGradeOk(UID + 1, shopIdOther, 4),
            createTestGradeOk(UID + 2, shopIdClone, 3),
            createTestGradeOk(UID + 3, shopId, 3),
        };

        // from base with clones
        prepareMatView();
        GradePager<ShopGradeResponseDto> shopGrades = getShopGrades(shopId,
            Sort.desc(SortType.ID),
            req -> req.param("withClones", "true")
        );

        assertEquals(3, shopGrades.getData().size());

        assertGradeSimple(shopGrades.getData().get(0), gradeIds[3], 3, UID + 3);
        assertGradeSimple(shopGrades.getData().get(1), gradeIds[2], 3, UID + 2);
        assertGradeSimple(shopGrades.getData().get(2), gradeIds[0], 2, UID);

        // from clone with clones
        shopGrades = getShopGrades(shopIdClone,
            Sort.desc(SortType.ID),
            req -> req.param("withClones", "true")
        );

        assertEquals(3, shopGrades.getData().size());

        assertGradeSimple(shopGrades.getData().get(0), gradeIds[3], 3, UID + 3);
        assertGradeSimple(shopGrades.getData().get(1), gradeIds[2], 3, UID + 2);
        assertGradeSimple(shopGrades.getData().get(2), gradeIds[0], 2, UID);

        // from clone without clones
        shopGrades = getShopGrades(shopIdClone,
            Sort.desc(SortType.ID),
            req -> req.param("withClones", "false")
        );

        assertEquals(1, shopGrades.getData().size());

        assertGradeSimple(shopGrades.getData().get(0), gradeIds[2], 3, UID + 2);

        // from shop without clones
        shopGrades = getShopGrades(shopIdOther,
            Sort.desc(SortType.ID),
            req -> req.param("withClones", "true")
        );

        assertEquals(1, shopGrades.getData().size());

        assertGradeSimple(shopGrades.getData().get(0), gradeIds[1], 4, UID + 1);
    }

    @Test
    public void testSimplePaging() {
        long shopId = 384719247;

        int pageSize = 4;
        int firstPage = 1;
        int secondPage = 2;

        long[] gradeIds = {
            createTestGradeOk(UID, shopId, 2),
            createTestGradeOk(UID + 1, shopId, 4),
            createTestGradeOk(UID + 2, shopId, 3),
            createTestGradeOk(UID + 3, shopId, 3),
            createTestGradeOk(UID + 4, shopId, 2),
            createTestGradeOk(UID + 5, shopId, 4),
        };

        prepareMatView();
        GradePager<ShopGradeResponseDto> shopGrades = getShopGrades(shopId,
            Sort.asc(SortType.ID),
            new Pager(firstPage, pageSize),
            req -> req
        );

        assertEquals(4, shopGrades.getData().size());
        assertEquals(6, shopGrades.getPager().getCount());

        assertGradeSimple(shopGrades.getData().get(0), gradeIds[0], 2, UID);
        assertGradeSimple(shopGrades.getData().get(1), gradeIds[1], 4, UID + 1);
        assertGradeSimple(shopGrades.getData().get(2), gradeIds[2], 3, UID + 2);
        assertGradeSimple(shopGrades.getData().get(3), gradeIds[3], 3, UID + 3);

        shopGrades = getShopGrades(shopId,
            Sort.asc(SortType.ID),
            new Pager(secondPage, pageSize),
            req -> req
        );

        assertEquals(2, shopGrades.getData().size());
        assertEquals(6, shopGrades.getPager().getCount());

        assertGradeSimple(shopGrades.getData().get(0), gradeIds[4], 2, UID + 4);
        assertGradeSimple(shopGrades.getData().get(1), gradeIds[5], 4, UID + 5);
    }

    @Test
    public void testComplexPaging() {
        long shopId = 384719247;

        int pageSize = 4;
        int firstPage = 1;
        int secondPage = 2;

        long[] gradeIds = {
            createTestGradeOk(UID, shopId, 2),
            createTestGradeOk(UID + 1, shopId, 4),
            createTestGradeOk(UID + 2, shopId, 3),
            createTestGradeOk(UID + 3, shopId, 3),
            createTestGradeOk(UID + 4, shopId, 2),
            createTestGradeOk(UID + 5, shopId, 1),
            createTestGradeOk(UID + 6, shopId, 3),
            createTestGradeOk(UID + 7, shopId, 2),
            createTestGradeOk(UID + 8, shopId, 3),
            createTestGradeOk(UID + 9, shopId, 3),
            createTestGradeOk(UID + 10, shopId, 4),
        };

        prepareMatView();
        GradePager<ShopGradeResponseDto> shopGrades = getShopGrades(shopId,
            Sort.desc(SortType.ID),
            new Pager(firstPage, pageSize),
            req -> req.param("gradeValue", "3")
        );

        assertEquals(4, shopGrades.getData().size());
        assertEquals(5, shopGrades.getPager().getCount());

        assertGradeSimple(shopGrades.getData().get(0), gradeIds[9], 3, UID + 9);
        assertGradeSimple(shopGrades.getData().get(1), gradeIds[8], 3, UID + 8);
        assertGradeSimple(shopGrades.getData().get(2), gradeIds[6], 3, UID + 6);
        assertGradeSimple(shopGrades.getData().get(3), gradeIds[3], 3, UID + 3);

        shopGrades = getShopGrades(shopId,
            Sort.desc(SortType.ID),
            new Pager(secondPage, pageSize),
            req -> req.param("gradeValue", "3")
        );

        assertEquals(1, shopGrades.getData().size());
        assertEquals(5, shopGrades.getPager().getCount());

        assertGradeSimple(shopGrades.getData().get(0), gradeIds[2], 3, UID + 2);
    }

    @Test
    public void testDateFilters() {
        long shopId = 384719247;

        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

        createTestGradeOk(UID + 1, shopId, 2, parse(dateFormat, "30.01.2022"));
        createTestGradeOk(UID + 2, shopId, 4, parse(dateFormat, "31.01.2022"));

        prepareMatView();

        checkGrades(shopId, null, null, List.of("31.01.2022", "30.01.2022"), dateFormat);
        checkGrades(shopId, "30.01.2022", null, List.of("31.01.2022", "30.01.2022"), dateFormat);
        checkGrades(shopId, "30.01.2022", null, List.of("31.01.2022", "30.01.2022"), dateFormat);
        checkGrades(shopId, null, "31.01.2022", List.of("31.01.2022", "30.01.2022"), dateFormat);
        checkGrades(shopId, "31.01.2022", null, List.of("31.01.2022"), dateFormat);
        checkGrades(shopId, "30.01.2022", "30.01.2022", List.of("30.01.2022"), dateFormat);
        checkGrades(shopId, "30.01.2021", "31.01.2021", List.of(), dateFormat);

    }

    @Test
    public void testInvalidDateFilters() {
        long shopId = 384719247;

        checkBadRequest(shopId, "32.01.2022", null);
        checkBadRequest(shopId, "hello!", null);
        checkBadRequest(shopId, "01.01.2022 03:00:00", null);

        checkBadRequest(shopId, null, "32.01.2022");
        checkBadRequest(shopId, null, "bye!");
        checkBadRequest(shopId, null, "01.01.2022 03:00:00");

        checkBadRequest(shopId, "32.01.2022", "02.02.2022");
    }

    @Test
    public void testFull() {
        long shopId = 384719247;

        prepareMatView();
        GradePager<ShopGradeResponseDto> shopGrades = getShopGrades(shopId,
            Sort.asc(SortType.ID),
            req -> req.param("withUnpublished", "true")
                .param("withClones", "true")
                .param("lastComment", "SHOP")
        );
    }

    @Test
    public void testClonesSingle() throws Exception {
        long shopId = 384719247;

        ShopListDto result = objectMapper.readValue(
            invokeAndRetrieveResponse(
                get("/api/grade/shop/" + shopId + "/clones")
                    .accept(MediaType.APPLICATION_JSON),
                status().is2xxSuccessful()
            ),
            ShopListDto.class);

        assertEquals(1, result.getShopIds().size());
        assertEquals(shopId, result.getShopIds().get(0).longValue());
    }

    @Test
    public void testClonesMultiple() throws Exception {
        long shopId = 384719247;
        long shopIdClone = 635673;
        long groupId = 3772;

        // create clones
        pgJdbcTemplate.update("insert into ext_shop_business_id(business_id, shop_id, type) values(?, ?, ?)", groupId, shopId, SHOP.getValue());
        pgJdbcTemplate.update("insert into ext_shop_business_id(business_id, shop_id, type) values(?, ?, ?)", groupId, shopIdClone, SHOP.getValue());

        ShopListDto result = objectMapper.readValue(
            invokeAndRetrieveResponse(
                get("/api/grade/shop/" + shopId + "/clones")
                    .accept(MediaType.APPLICATION_JSON),
                status().is2xxSuccessful()
            ),
            ShopListDto.class);

        assertEquals(2, result.getShopIds().size());
        assertTrue(result.getShopIds().containsAll(Arrays.asList(shopId, shopIdClone)));
    }

    @Test
    public void testStats() {
        long shopId = 384719247;

        prepareMatView();
        assertStats(shopId, 0, 0);

        long[] gradeIds = {
            createTestGradeOk(UID, shopId, 2),
            createTestGradeOk(UID + 1, shopId, 4),
            createTestGradeOk(UID + 2, shopId, 3),
        };

        prepareMatView();
        // check cache is used
        assertStats(shopId, 0, 0);

        gradeCacher.cleanForShopId(shopId);
        assertStats(shopId, 3, 3);

        pgJdbcTemplate.update("insert into grade_comment_marker(grade_id, last_comment_by) values (?, ?)",
            gradeIds[0], LastCommentType.SHOP.getId());

        prepareMatView();
        gradeCacher.cleanForShopId(shopId);
        assertStats(shopId, 3, 2);

        pgJdbcTemplate.update("insert into grade_comment_marker(grade_id, last_comment_by) values (?, ?)",
            gradeIds[1], LastCommentType.USER.getId());

        prepareMatView();
        gradeCacher.cleanForShopId(shopId);
        assertStats(shopId, 3, 1);

        // just to check, that it handles none value as expected
        pgJdbcTemplate.update("insert into grade_comment_marker(grade_id, last_comment_by) values (?, ?)",
            gradeIds[2], LastCommentType.NONE.getId());

        prepareMatView();
        gradeCacher.cleanForShopId(shopId);
        assertStats(shopId, 3, 0);
    }


    private void checkBadRequest(long shopId, String dateFrom, String dateTo) {
        invokeAndRetrieveResponse(
                addDateFilters(get("/api/grade/pi/shop/" + shopId), dateFrom, dateTo)
                        .accept(MediaType.APPLICATION_JSON),
                status().isBadRequest());
    }

    private void checkGrades(long shopId, String dateFrom, String dateTo, List<String> expectedDates,
                             DateFormat dateFormat) {
        GradePager<ShopGradeResponseDto> shopGrades = getShopGrades(shopId,
                Sort.desc(SortType.ID),
                req -> addDateFilters(req, dateFrom, dateTo)
        );

        assertEquals(shopGrades.getData().size(), expectedDates.size());

        List<String> dates = shopGrades.getData().stream()
                .map(GradeResponseDto::getCreationDate)
                .map(dateFormat::format)
                .collect(Collectors.toList());
        assertEquals(expectedDates, dates);
    }

    private void assertStats(long shopId, long count, long needReaction) {
        PartnerGradeStat stats = getStats(shopId);
        assertEquals(count, stats.getTotalCount());
        assertEquals(needReaction, stats.getNeedReactionCount());
    }


    private void makeVerified(long gradeId, boolean isCpa, boolean isVerified) {
        verifiedGradeService.changeVerified(List.of(gradeId), isVerified, VerifiedType.ANTIFRAUD, 0L);
        verifiedGradeService.setCpaInDB(List.of(gradeId), isCpa);
    }

    private void setCommentType(long gradeId, LastCommentType commentType) {
        pgJdbcTemplate.update(
            "insert into grade_comment_marker(grade_id, last_comment_by) \n" +
                "values(?, ?)",
            gradeId,
            commentType.getId()
        );
    }

    private void setModReason(long gradeId, ModState modState, Integer reasonId) {
        final long moderator = 394617364134L;

        Object4Moderation obj = Object4Moderation.moderated(
            gradeId,
            modState,
            reasonId != null ? reasonId.longValue() : null
        );

        gradeAdminService.moderate(Collections.singletonList(obj), moderator);
    }

    private void assertGradeSimple(ShopGradeResponseDto grade,
                                   long gradeId,
                                   int avgGrade,
                                   long uid) {
        assertGradeSimple(grade, gradeId, avgGrade, uid, buildTestText(uid));
    }

    private void assertGradeSimple(ShopGradeResponseDto grade,
                                   long gradeId,
                                   int avgGrade,
                                   long uid,
                                   String text) {
        assertEquals(gradeId, grade.getId().longValue());
        assertEquals(uid, grade.getUser().getPassportUid().longValue());
        assertEquals(avgGrade, grade.getAverageGrade().intValue());
        assertEquals(text, grade.getText());
    }

    private void assertGradeComplex(ShopGradeResponseDto grade,
                                    long gradeId,
                                    int avgGrade,
                                    long uid,
                                    String text,
                                    String pro,
                                    String contra) {
        assertGradeSimple(grade, gradeId, avgGrade, uid, text);
        assertEquals(pro, grade.getPro());
        assertEquals(contra, grade.getContra());
    }

    private long createTestGradeRegion(long uid, long shopId, int avgGrade, int regionId) {
        ShopGrade grade = buildTestGrade(uid, shopId, avgGrade, ModState.APPROVED);
        grade.setRegionId(regionId);
        return doSaveGrade(grade);
    }

    private long createTestGradeOk(long uid, long shopId, int avgGrade, Date createDate) {
        ShopGrade shopGrade = buildTestGrade(uid, shopId, avgGrade, ModState.APPROVED);
        shopGrade.setCreated(createDate);
        return doSaveGrade(shopGrade);
    }

    private Date parse(DateFormat dateFormat, String date) {
        try {
            return dateFormat.parse(date);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid date format: " + date);
        }
    }

    private long createTestGradeOk(long uid, long shopId, int avgGrade) {
        return doSaveGrade(buildTestGrade(uid, shopId, avgGrade, ModState.APPROVED));
    }

    private long createTestGrade(long uid, long shopId, int avgGrade, ModState modState) {
        return doSaveGrade(buildTestGrade(uid, shopId, avgGrade, modState));
    }

    private long createTestGrade(long uid,
                                 long shopId,
                                 int avgGrade,
                                 ModState modState,
                                 String text,
                                 String pro,
                                 String contra) {
        return doSaveGrade(buildTestGrade(uid, shopId, avgGrade, modState, text, pro, contra));
    }

    private long createTestGrade(long uid, long shopId, int avgGrade, ModState modState, Integer modReasonId) {
        long gradeId = doSaveGrade(buildTestGrade(uid, shopId, avgGrade, modState));
        if (modReasonId != null) {
            setModReason(gradeId, modState, modReasonId);
        }
        return gradeId;
    }

    @NotNull
    private ShopGrade buildTestGrade(long uid, long shopId, int avgGrade, ModState modState) {
        ShopGrade testGrade = buildTestShopGrade(uid, shopId, buildTestText(uid), avgGrade);
        testGrade.setModState(modState);
        return testGrade;
    }

    @NotNull
    private ShopGrade buildTestGrade(long uid,
                                     long shopId,
                                     int avgGrade,
                                     ModState modState,
                                     String text,
                                     String pro,
                                     String contra) {
        ShopGrade testGrade = buildTestGrade(uid, shopId, avgGrade, modState);
        testGrade.setText(text);
        testGrade.setPro(pro);
        testGrade.setContra(contra);
        return testGrade;
    }

    private ShopGrade buildTestShopGrade(long uid, long shopId, String text, int averageGrade) {
        ShopGrade grade = GradeCreator.constructShopGrade(shopId, uid);
        grade.setText(text);
        grade.setAverageGrade(averageGrade);
        return grade;
    }

    private long doSaveGrade(ShopGrade testGrade) {
        return gradeCreator.createGrade(testGrade);
    }

    private String buildTestText(long uid) {
        return "test grade " + uid;
    }

    private GradePager<ShopGradeResponseDto> getShopGrades(long shopId,
                                                           Sort sort,
                                                           Function<MockHttpServletRequestBuilder, MockHttpServletRequestBuilder> more) {
        return getShopGrades(shopId, sort, new Pager(1, 10), more);
    }

    private MockHttpServletRequestBuilder addDateFilters(MockHttpServletRequestBuilder builder,
                                                         String dateFrom, String dateTo) {
        if (dateFrom != null) {
            builder = builder.param("dateFrom", dateFrom);
        }
        if (dateTo != null) {
            builder = builder.param("dateTo", dateTo);
        }
        return builder;
    }

    private GradePager<ShopGradeResponseDto> getShopGrades(long shopId,
                                                           Sort sort,
                                                           Pager pager,
                                                           Function<MockHttpServletRequestBuilder, MockHttpServletRequestBuilder> more) {
        try {
            return objectMapper.readValue(
                invokeAndRetrieveResponse(
                    more.apply(
                        get("/api/grade/pi/shop/" + shopId)
                            .param("page_num", String.valueOf(pager.getPageNum()))
                            .param("page_size", String.valueOf(pager.getPageSize()))
                            .param("sortBy", sort.getSortType().getCode())
                            .param("sortOrder", sort.getOrder())
                            .accept(MediaType.APPLICATION_JSON)
                    ),
                    status().is2xxSuccessful()),
                new TypeReference<GradePager<ShopGradeResponseDto>>() {
                });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private PartnerGradeStat getStats(long shopId) {
        try {
            return objectMapper.readValue(
                invokeAndRetrieveResponse(
                    get("/api/grade/pi/shop/" + shopId + "/stat")
                        .accept(MediaType.APPLICATION_JSON),
                    status().is2xxSuccessful()),
                new TypeReference<PartnerGradeStat>() {
                });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
