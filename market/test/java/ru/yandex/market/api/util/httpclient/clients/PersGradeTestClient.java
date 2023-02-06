package ru.yandex.market.api.util.httpclient.clients;

import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * @author dimkarp93
 */
@Service
public class PersGradeTestClient extends AbstractFixedConfigurationTestClient {
    public PersGradeTestClient() {
        super("PersGrade");
    }

    public void addModelOpinion(long uid,
                                byte[] request,
                                String filename) {
        configure(x -> x.post()
            .serverMethod("api/grade/user/UID/" + uid + "/model/publish")
            .body(body -> Arrays.equals(request, body), "body matched")
        )
            .ok().body(filename);
    }

    public void getModelOpinion(long uid,
                                long modelId,
                                String filename) {
        configure(x -> x.get()
            .serverMethod("myModelGrade")
            .param("_user_id", String.valueOf(uid))
            .param("modelid", String.valueOf(modelId))
        )
            .ok().body(filename);
    }

}
