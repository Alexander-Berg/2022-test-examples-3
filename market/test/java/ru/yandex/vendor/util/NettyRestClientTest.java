package ru.yandex.vendor.util;

import org.junit.Test;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import ru.yandex.common.util.collections.Pair;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.nio.charset.StandardCharsets;

import static java.util.Arrays.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class NettyRestClientTest {

    @Test
    public void testBodySerializedToJsonByDefault() {
        assertThat(bodyToString(asList(1,2,3,4)), is("[1,2,3,4]"));
    }

    @Test
    public void testBodySerializedToJsonWithHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Object body = new HttpEntity<>(asList(1,2,3,4), headers);
        assertThat(bodyToString(body), is("[1,2,3,4]"));
    }
    @Test
    public void testBodySerializedToXmlWithHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        Object body = new HttpEntity<>(asList(1,2,3,4), headers);
        assertThat(bodyToString(body), is("<ArrayList><item>1</item><item>2</item><item>3</item><item>4</item></ArrayList>"));
    }

    @Test
    public void testObjectSerializedToJsonWithHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Object body = new HttpEntity<>(new Pair<>("qwe", true), headers);
        assertThat(bodyToString(body), is("{\"first\":\"qwe\",\"second\":true}"));
    }
    @Test
    public void testObjectSerializedToXmlWithHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        Object body = new HttpEntity<>(new Pair<>("qwe", true), headers);
        assertThat(bodyToString(body), is("<Pair><first>qwe</first><second>true</second></Pair>"));
    }

    @Test
    public void testAnnotatedObjectSerializedToJsonWithHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Object body = new HttpEntity<>(new SomePoint<>(42, null), headers);
        assertThat(bodyToString(body), is("{\"a\":42,\"b\":null}"));
    }

    @Test
    public void testAnnotatedObjectSerializedToXmlWithHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        Object body = new HttpEntity<>(new SomePoint<>(42, null), headers);
        assertThat(bodyToString(body), is("<SomePoint><a>42</a><b/></SomePoint>"));
    }

    private static String bodyToString(Object body) {
        NettyRestClient.Config config = new NettyRestClient.Config();
        config.setServiceUrl("http://localhost:8080");
        NettyRestClient restClient = new NettyRestClient(config);
        byte[] posts = restClient.constructRequestBuilder("POST", new IRestClient.Request<>("/", Resource.class), body).build().getByteData();
        return posts == null ? "" : new String(posts, StandardCharsets.UTF_8);
    }

    @XmlRootElement
    public static class SomePoint<A,B> {

        private final A a;
        private final B b;

        SomePoint(A a, B b) {
            this.a = a;
            this.b = b;
        }

        @XmlElement
        public A getA() {
            return a;
        }

        @XmlElement
        public B getB() {
            return b;
        }
    }
}