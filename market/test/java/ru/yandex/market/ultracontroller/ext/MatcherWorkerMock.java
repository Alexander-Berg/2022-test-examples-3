package ru.yandex.market.ultracontroller.ext;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.googlecode.protobuf.format.JsonFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Ignore;
import org.springframework.beans.factory.InitializingBean;
import ru.yandex.market.ir.http.Matcher;
import ru.yandex.market.ultracontroller.utils.RequestLogEntity;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Ignore
public class MatcherWorkerMock  extends MatcherWorker implements InitializingBean {
    private static final String MODEL_ID_FIELD = "model_id";
    private static final String MATCHER_ANSWER_FIELD = "matcher_answer";
    private static final Logger log = LogManager.getLogger();
    private final Map<Integer,Matcher.MatchResult> modelIdToMatchResultMap = new HashMap<>();

    private void readMatcherAnswersFile(String fileName) {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(fileName))) {
            JsonElement element = JsonParser.parseReader(reader);
            Matcher.MatchResult.Builder builder = Matcher.MatchResult.newBuilder();
            for (JsonElement jsonElement : element.getAsJsonArray()) {
                builder.clear();
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                int modelId = jsonObject.get(MODEL_ID_FIELD).getAsInt();
                String str = jsonObject.get(MATCHER_ANSWER_FIELD).getAsJsonObject().toString();
                JsonFormat.merge(str, builder);
                Matcher.MatchResult matchResult = builder.build();
                modelIdToMatchResultMap.put(modelId, matchResult);
            }
        } catch (IOException e) {
            log.error("Error while reading matcher_answers json file.");
        }
    }

    @Override
    public void afterPropertiesSet() {
        String matcherAnswersFileName = new File(
            getClass().getResource("/proto_json/matcher_answers.json").getFile()
        ).toString();
        readMatcherAnswersFile(matcherAnswersFileName);
    }

    public int processQuery(List<Matcher.Offer> offers, RequestLogEntity logEntity, MatcherRes[] results) {
        List<Matcher.MatchResult> resultList = new ArrayList<>();
        offers.forEach(offer -> {
            int modelId = offer.getModelId();
            if (!modelIdToMatchResultMap.containsKey(modelId)) {
                throw new RuntimeException("Unknown modelId = " + modelId);
            }
            resultList.add(modelIdToMatchResultMap.get(modelId));
            for (int i = 0; i < resultList.size(); i++) {
                processResult(offers, resultList.get(i), results, i);
            }
        });
        return resultList.size();
    }
}
