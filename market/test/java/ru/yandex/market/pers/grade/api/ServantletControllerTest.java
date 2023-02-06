package ru.yandex.market.pers.grade.api;

import java.util.List;

import javax.servlet.http.Cookie;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.common.framework.core.AbstractCrudServantlet;
import ru.yandex.common.framework.http.HttpServRequest;
import ru.yandex.market.pers.grade.Const;
import ru.yandex.market.pers.grade.MockedPersGradeTest;
import ru.yandex.market.pers.grade.api.model.Vote;
import ru.yandex.market.pers.grade.client.model.GradeState;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.db.DbGradeService;
import ru.yandex.market.pers.grade.core.model.vote.GradeVoteKind;
import ru.yandex.market.pers.grade.core.model.vote.UserGradeVote;
import ru.yandex.market.pers.grade.db.ModReasonDbService;
import ru.yandex.market.pers.grade.vote.GradeVoteService;
import ru.yandex.market.util.ExecUtils;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 29.11.2021
 */
public class ServantletControllerTest extends MockedPersGradeTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GradeCreator gradeCreator;

    @Autowired
    private GradeVoteService gradeVoteService;

    @Autowired
    private DbGradeService gradeService;

    @Autowired
    private ModReasonDbService modReasonDbService;

    @Test
    public void testSaveVote() throws Exception {
        long gradeId = gradeCreator.createGrade(GradeCreator.constructModelGradeRnd());
        String response = invokeServantlet(get("/gradeVoting")
                .param(AbstractCrudServantlet.ACTION_PARAM_NAME, AbstractCrudServantlet.CREATE_ACTION_VALUE)
                .param(Const.GRADE_ID, String.valueOf(gradeId))
                .param(Const.GRADE_VOTE_PARAM, "1")
                .param(HttpServRequest.USER_ID_PARAM, "123")
                .cookie(new Cookie(Const.YANDEXUID, "yaya")),
            MockMvcResultMatchers.status().is2xxSuccessful()
        );

        assertEquals(fileToString("/servantlet/gradeVoting_save.xml"), response);

        List<UserGradeVote> votes = gradeVoteService.getVotesByUid(123);
        assertEquals(1, votes.size());
        assertEquals(GradeVoteKind.agree, votes.get(0).gradeVoteKind);
    }

    @Test
    public void testReadVotes() throws Exception {
        long gradeId = gradeCreator.createGrade(GradeCreator.constructModelGradeRnd());
        long gradeId2 = gradeCreator.createGrade(GradeCreator.constructModelGradeRnd());

        gradeVoteService.createVote(new Vote(gradeId, 1L, "none", 1));
        gradeVoteService.createVote(new Vote(gradeId2, 1L, "other", 0));
        gradeVoteService.createVote(new Vote(gradeId, 2L, "none", 1));

        String response = invokeServantlet(get("/gradeVoting")
                .param(AbstractCrudServantlet.ACTION_PARAM_NAME, AbstractCrudServantlet.REQUEST_ACTION_VALUE)
                .param(HttpServRequest.USER_ID_PARAM, "1")
                .cookie(new Cookie(Const.YANDEXUID, "yaya")),
            MockMvcResultMatchers.status().is2xxSuccessful()
        );

        assertEquals(fileToString("/servantlet/gradeVoting_read.xml")
                .replace("GRADE_1", String.valueOf(gradeId))
                .replace("GRADE_2", String.valueOf(gradeId2)).strip(),
            response);
    }

    @Test
    public void testModReasonDictionary() throws Exception {
        modReasonDbService.cleanCache();
        pgJdbcTemplate.update("insert into mod_rejection_reason(id, type, name, recomendation, active) " +
            "values (2, 1, 'Грубость', 'rec2', 1)");
        pgJdbcTemplate.update("insert into mod_rejection_reason(id, type, name, recomendation, active) " +
            "values (140, 1, 'не информативный', 'rec1', 1)");

        String response = invokeServantlet(get("/modReasonDictionary")
                .characterEncoding("UTF-8")
            .accept(MediaType.TEXT_XML_VALUE),
            MockMvcResultMatchers.status().is2xxSuccessful()
        );

        assertEquals(fileToString("/servantlet/modReasonDict.xml").strip(), response);
    }

    @Test
    public void testKillAll() throws Exception {
        long gradeId = gradeCreator.createModelGrade(42L, 1L);
        long gradeId2 = gradeCreator.createModelGrade(42L, 2L);
        long gradeId3 = gradeCreator.createModelGrade(43L, 1L);

        // remove by modelId
        String response = invokeServantlet(get("/killAll")
                .param(HttpServRequest.USER_ID_PARAM, "1")
                .param(Const.MODEL_ID, "42"),
            MockMvcResultMatchers.status().is2xxSuccessful()
        );

        assertEquals(fileToString("/servantlet/empty.xml").strip(), response);

        assertEquals(GradeState.DELETED, gradeService.getGrade(gradeId).getState());
        assertEquals(GradeState.LAST, gradeService.getGrade(gradeId2).getState());
        assertEquals(GradeState.LAST, gradeService.getGrade(gradeId3).getState());

        //remove by id
        invokeServantlet(get("/killAll")
                .param(AbstractCrudServantlet.ACTION_PARAM_NAME, AbstractCrudServantlet.REQUEST_ACTION_VALUE)
                .param(HttpServRequest.USER_ID_PARAM, "1")
                .param(Const.GRADE_ID, String.valueOf(gradeId3)),
            MockMvcResultMatchers.status().is2xxSuccessful()
        );

        assertEquals(GradeState.DELETED, gradeService.getGrade(gradeId).getState());
        assertEquals(GradeState.LAST, gradeService.getGrade(gradeId2).getState());
        assertEquals(GradeState.DELETED, gradeService.getGrade(gradeId3).getState());

        // remove by nothing
        invokeServantlet(get("/killAll")
                .param(AbstractCrudServantlet.ACTION_PARAM_NAME, AbstractCrudServantlet.REQUEST_ACTION_VALUE)
                .param(HttpServRequest.USER_ID_PARAM, "2"),
            MockMvcResultMatchers.status().is2xxSuccessful()
        );

        assertEquals(GradeState.DELETED, gradeService.getGrade(gradeId).getState());
        assertEquals(GradeState.LAST, gradeService.getGrade(gradeId2).getState());
        assertEquals(GradeState.DELETED, gradeService.getGrade(gradeId3).getState());
    }

    @Test
    public void testReadGrade() throws Exception {
        long gradeId = gradeCreator.createModelGrade(502L, 111L);
        long gradeId2 = gradeCreator.createModelGrade(502L, 112L);
        long gradeIdSh = gradeCreator.createShopGrade(111L, 502L);

        gradeVoteService.createVote(new Vote(gradeId, 1L, "none", 1));
        gradeVoteService.createVote(new Vote(gradeId2, 1L, "other", 0));
        gradeVoteService.createVote(new Vote(gradeId, 2L, "none", 1));

        // search by id
        String response = invokeServantlet(get("/myModelGrade")
                .param(HttpServRequest.USER_ID_PARAM, "111")
                .param(Const.GRADE_ID, String.valueOf(gradeId)),
            MockMvcResultMatchers.status().is2xxSuccessful()
        );

        assertEquals(fileToString("/servantlet/gradeReadById.xml")
                .replace("GRADE_1", String.valueOf(gradeId))
                .strip(),
            response.replaceAll("<grade.*</grade", "<grade></grade"));

        // search by model
        String responseByModel = invokeServantlet(get("/myModelGrade")
                .param(HttpServRequest.USER_ID_PARAM, "111")
                .param(Const.MODEL_ID, String.valueOf(502)),
            MockMvcResultMatchers.status().is2xxSuccessful()
        );

        assertEquals(fileToString("/servantlet/gradeReadByModelId.xml")
                .replace("GRADE_1", String.valueOf(gradeId))
                .strip(),
            responseByModel.replaceAll("<grade.*</grade", "<grade></grade"));

        // search by shop
        String responseByShop = invokeServantlet(get("/myGrade")
                .param(HttpServRequest.USER_ID_PARAM, "111")
                .param(Const.SHOP_ID, String.valueOf(502)),
            MockMvcResultMatchers.status().is2xxSuccessful()
        );

        assertEquals(fileToString("/servantlet/gradeReadByShopId.xml")
                .strip(),
            responseByShop.replaceAll("<grade.*</grade", "<grade></grade"));

        // invalid request
        String responseInvalid = invokeServantlet(get("/myGrade"),
            MockMvcResultMatchers.status().is2xxSuccessful()
        );

        assertEquals(fileToString("/servantlet/empty.xml").strip(), responseInvalid);

    }


    protected String invokeServantlet(MockHttpServletRequestBuilder requestBuilder, ResultMatcher expected) {
        try {
            return mockMvc.perform(requestBuilder
                .accept(MediaType.TEXT_XML_VALUE))
                .andDo(print())
                .andExpect(expected)
                .andReturn().getResponse().getContentAsString();
        } catch (Exception e) {
            throw ExecUtils.silentError(e);
        }
    }
}
