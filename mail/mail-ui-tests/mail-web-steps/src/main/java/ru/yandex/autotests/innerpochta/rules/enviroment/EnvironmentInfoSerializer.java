package ru.yandex.autotests.innerpochta.rules.enviroment;

import lombok.extern.slf4j.Slf4j;
import ru.yandex.qatools.commons.model.Environment;
import ru.yandex.qatools.commons.model.ObjectFactory;
import ru.yandex.qatools.commons.model.Parameter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Set;

import static java.io.File.separator;

/**
 * @author a-zoshchuk
 */
@Slf4j
public class EnvironmentInfoSerializer {

    private static final Environment env = new ObjectFactory().createEnvironment().withName("Run environment");

    private static final EnvironmentInfoSerializer INSTANCE = new EnvironmentInfoSerializer();

    private final Set<Parameter> parameters = new HashSet<>();

    private EnvironmentInfoSerializer() {
        addShutdownHookFor();
    }

    public static EnvironmentInfoSerializer getInstance() {
        return INSTANCE;
    }

    public void addEnvParameter(Parameter param) {
        parameters.add(param);
    }

    private void addShutdownHookFor() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::writeToFile));
    }

    private void writeToFile() {
        JAXBElement<Environment> jaxbElement = new ObjectFactory().createEnvironment(env);

        parameters.forEach(env::withParameter);

        try {
            JAXBContext context = JAXBContext.newInstance(Environment.class);
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            String allureResultsPath = System.getProperty("allure.results.directory", "target/allure-results");
            m.marshal(jaxbElement, new FileOutputStream(allureResultsPath + separator + "environment.xml"));

        } catch (JAXBException e) {
            log.error("Can't create jaxb context", e);
        } catch (FileNotFoundException e) {
            log.error("Can't write to environment file", e);
        }
    }
}
