package ru.yandex.market.notifier;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import ru.yandex.market.checkout.common.xml.ClassMappingXmlMessageConverter;
import ru.yandex.market.notifier.entity.Notification;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ResourceLoadUtil {

    @Autowired
    private ClassMappingXmlMessageConverter marketNotifierXmlMessageConverter;
    @Autowired
    private PathMatchingResourcePatternResolver resolver;

    static class StringInputMessage implements HttpInputMessage {
        final String str;

        public StringInputMessage(final String str) {
            this.str = str;
        }

        @Override
        public HttpHeaders getHeaders() {
            return null;
        }

        @Override
        public InputStream getBody() throws IOException {
            return IOUtils.toInputStream(str, "UTF-8");
        }
    }

    public List<Notification> getSampleNotifications() throws IOException {
        List<Notification> result = new ArrayList<>();
        for (Resource xml : getSamplesFiles()) {
            result.add((Notification) marketNotifierXmlMessageConverter.read(
                    Notification.class, new StringInputMessage(readStringFromResource(xml))));
        }
        return result;
    }

    private Resource[] getSamplesFiles() throws IOException {
        return resolver.getResources("classpath:samples/checkouter/*.xml");
    }

    private static String readStringFromResource(Resource xml) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(xml.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }
}
