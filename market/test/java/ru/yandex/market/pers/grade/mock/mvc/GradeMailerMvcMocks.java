package ru.yandex.market.pers.grade.mock.mvc;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import ru.yandex.market.pers.grade.client.dto.mailer.MailerModelGrade;
import ru.yandex.market.pers.grade.client.dto.mailer.MailerShopGrade;
import ru.yandex.market.pers.test.common.AbstractMvcMocks;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 14.04.2021
 */
@Service
public class GradeMailerMvcMocks extends AbstractMvcMocks {

    public List<MailerModelGrade> getModelGrade(long gradeId) {
        return parseValue(invokeAndRetrieveResponse(
            get("/api/grade/mailer/model")
                .param("gradeId", String.valueOf(gradeId))
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()), new TypeReference<List<MailerModelGrade>>() {
        });
    }

    public List<MailerShopGrade> getShopGrade(long gradeId, Boolean onlyPublic) {
        return parseValue(invokeAndRetrieveResponse(
            get("/api/grade/mailer/shop")
                .param("gradeId", String.valueOf(gradeId))
                .param("onlyPublic", onlyPublic == null ? null : String.valueOf(onlyPublic))
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()), new TypeReference<List<MailerShopGrade>>() {
        });
    }

    public Boolean checkShopGrade(long userId, long shopId) {
        return parseValue(invokeAndRetrieveResponse(
            get("/api/grade/mailer/shop/exists")
                .param("userId", String.valueOf(userId))
                .param("shopId", String.valueOf(shopId))
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()), Boolean.class);
    }

    public Boolean checkVoteGrade(long voteId) {
        return parseValue(invokeAndRetrieveResponse(
            get("/api/grade/mailer/vote/exists")
                .param("voteId", String.valueOf(voteId))
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()), Boolean.class);
    }

}
