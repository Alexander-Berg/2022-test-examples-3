package ru.yandex.chemodan.directory.client;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public class DirectoryOrganizationsResponseTest {

    @Test
    public void testDeserialize() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String body1 = "{ \"links\" : []}";
        String body2 = "{ \"links\" : {}}";
        String body3 = "{ \"links\" : {\"next\": \"1234\"}}";

        DirectoryOrganizationsResponse parsed =
                mapper.readValue(body1, DirectoryOrganizationsResponse.class);
        Assert.assertTrue(parsed.getLinks().isArray());
        Assert.assertFalse(parsed.getNext().isPresent());

        parsed = mapper.readValue(body2, DirectoryOrganizationsResponse.class);
        Assert.assertTrue(parsed.getLinks().isObject());
        Assert.assertFalse(parsed.getNext().isPresent());

        parsed = mapper.readValue(body3, DirectoryOrganizationsResponse.class);
        Assert.assertTrue(parsed.getLinks().isObject());
        Assert.assertEquals("1234", parsed.getNext().get());

    }
}
