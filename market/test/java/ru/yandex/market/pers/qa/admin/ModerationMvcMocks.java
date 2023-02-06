package ru.yandex.market.pers.qa.admin;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.pers.test.common.AbstractMvcMocks;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@Service
public class ModerationMvcMocks extends AbstractMvcMocks {

    public String moderateComplaintBanComment(Long complaintId, ResultMatcher resultMatcher) throws Exception {
        return invokeAndRetrieveResponse(
            post("/api/complaint/" + complaintId + "/moderate?banComment=true")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher);
    }

    public String moderateComplaintPublishComment(Long complaintId, ResultMatcher resultMatcher) throws Exception {
        return invokeAndRetrieveResponse(
            post("/api/complaint/" + complaintId + "/moderate?publishComment=true")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher);
    }
}
