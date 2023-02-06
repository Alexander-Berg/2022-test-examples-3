package ru.yandex.market.deepdive;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.deepdive.configuration.IntegrationTestConfiguration;

@ExtendWith(SpringExtension.class)
@Slf4j
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = IntegrationTestConfiguration.class)
@AutoConfigureMockMvc(secure = false)
@TestPropertySource(locations = "classpath:application-integrationtest.properties")
public abstract class AbstractTest {
    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String readFromFile(String fileName) {
        StringBuilder result = new StringBuilder();
        ClassPathResource resource = new ClassPathResource(fileName);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            while (reader.ready()) {
                result.append((char) reader.read());
            }
        } catch (IOException e) {
            log.error("Error while reading from file.", e);
        }
        return result.toString();
    }
}
