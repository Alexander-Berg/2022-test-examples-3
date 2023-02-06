package ru.yandex.autotests.innerpochta.util.experiments;

import com.google.common.base.Joiner;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import ru.yandex.autotests.innerpochta.util.Utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static com.google.common.collect.ImmutableMap.of;

/**
 * @author marchart
 */
public class ExperimentsBuilder {

    private String EXPERIMENT_NUM = Utils.getRandomNumber(99999, 10000) + ",0,0";
    private String baseTemplate;
    private String fileData;
    private String result;
    private Map<String, String> encodedResult;

    public static ExperimentsBuilder experimentsBuilder() {
        return new ExperimentsBuilder();
    }

    ExperimentsBuilder() {
        try {
            baseTemplate = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("json/expTemplates/baseTemplate.json")
            );
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Не удалось считать данные из файла baseTemplate.json");
        }
    }

    public Map<String, String> withBaseTemplate(String params) {
        setFileData(params);
        setEncodedResult();

        return encodedResult;
    }

    public Map<String, String> withDefaultConditions(String... params) {
        setFileData("default");
        String featureList = Joiner.on(",").join(params);
        fileData = fileData.replace("{}", "{" + featureList + "}");
        setEncodedResult();

        return encodedResult;
    }

    private void setFileData(String fileName) {
        try {
            fileData = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("json/expTemplates/" + fileName + ".json")
            );
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Не удалось считать данные из файла " + fileName + ".json");
        }
    }

    private void setExpFlags() {
        fileData = fileData.replaceAll("\"", "\\\\\"");
        result = baseTemplate.replace("[]", "[\"" + fileData + "\"]");
        result = StringUtils.deleteWhitespace(result);
    }

    private void setEncodedResult() {
        setExpFlags();
        String encodedResult = Base64.getEncoder().encodeToString(result.getBytes(StandardCharsets.UTF_8));
        this.encodedResult = of(EXPERIMENT_NUM, encodedResult);
    }
}
