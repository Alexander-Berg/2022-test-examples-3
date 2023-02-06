package ru.yandex.autotests.innerpochta.util.props;

import ru.yandex.qatools.properties.PropertyLoader;
import ru.yandex.qatools.properties.annotations.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static ru.yandex.autotests.innerpochta.util.props.UrlProps.urlProps;

@Resource.Classpath("experiments.properties")
public class ExperimentsProperties {

    private static ExperimentsProperties instance;

    public static ExperimentsProperties experimentsProperties() {
        if (instance == null) {
            instance = new ExperimentsProperties();
        }
        return instance;
    }

    private ExperimentsProperties() {
        PropertyLoader.populate(this);
    }

    public List<String> getExperiments() {
        List<String> experiments = new ArrayList<>();
        try (InputStream is =
                 getClass().getClassLoader().getResourceAsStream("experiments.properties")) {
            Properties properties = new Properties();
            properties.load(is);
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                String name = String.valueOf(entry.getKey());
                String value = String.valueOf(entry.getValue());
                if (isExperimentForProject(name)) {
                    experiments.add(value);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println(
                "Experiments can't be loaded. Error occurred while reading from file. More info: " + e.getMessage()
            );
        }
        return experiments;
    }

    private boolean isExperimentForProject(String key) {
        return (key.startsWith("exp." + urlProps().getProject().toLowerCase()));
    }
}
