package ru.yandex.market.mboc.app;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.mboc.app.security.SecuredRolesAdvice;
import ru.yandex.market.mboc.common.services.category.CategoryRepository;
import ru.yandex.market.mboc.common.services.category.CategoryTree;
import ru.yandex.market.mboc.common.services.category.DatabaseCategoryCachingService;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.users.User;
import ru.yandex.market.mboc.common.users.UserRepository;
import ru.yandex.market.mboc.common.users.UserRoles;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Helper utils for writing web tests. Which do test json conversions.
 * <p>
 * Helps with user creation/auth management. Also has quick helpers to convert to and from java objects
 * - this way it's easier to assert.
 *
 * @author yuramalinov
 * @created 22.07.19
 */
public abstract class BaseWebIntegrationTestClass extends BaseAppIntegrationTestClass {
    static {
        Configuration.setDefaults(new Configuration.Defaults() {
            private final JsonProvider jsonProvider = new JacksonJsonProvider();
            private final MappingProvider mappingProvider = new JacksonMappingProvider();

            @Override
            public JsonProvider jsonProvider() {
                return jsonProvider;
            }

            @Override
            public MappingProvider mappingProvider() {
                return mappingProvider;
            }

            @Override
            public Set<Option> options() {
                return EnumSet.noneOf(Option.class);
            }
        });
    }

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected ObjectMapper objectMapper;
    protected User testUser;
    @Autowired
    private SecuredRolesAdvice securedRolesAdvice;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private DatabaseCategoryCachingService databaseCategoryCachingService;

    @Before
    public void initCategoryTree() {
        categoryRepository.insert(
            new Category()
                .setCategoryId(CategoryTree.ROOT_CATEGORY_ID)
                .setName("root")
                .setParentCategoryId(CategoryTree.NO_ROOT_ID)
                .setPublished(true)
        );
    }

    @Before
    @After
    public void invalidateCategoryCache() {
        databaseCategoryCachingService.invalidateAll();
    }

    @Before
    public void resetRolesCache() {
        securedRolesAdvice.reset();
    }

    @Before
    public void setupUser() {
        testUser = new User("test")
            .setRoles(new HashSet<>(UserRoles.ROLES_DESCRIPTION.keySet()));
        userRepository.insert(testUser);
    }

    protected void setRoles(String... roles) {
        testUser.setRoles(roles);
        userRepository.update(testUser);
    }

    protected MvcResult getJson(String url, Object... params) throws Exception {
        return mvc.perform(get(String.format(url, params)).header("Authorization", "test"))
            .andExpect(status().isOk())
            .andReturn();
    }

    protected MvcResult get404(String url, Object... params) throws Exception {
        return mvc.perform(get(String.format(url, params)).header("Authorization", "test"))
            .andExpect(status().is4xxClientError())
            .andReturn();
    }

    protected MvcResult postJson(String url, Object body) throws Exception {
        return mvc.perform(
            post(url)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(objectMapper.writeValueAsBytes(body))
                .header("Authorization", "test"))
            .andExpect(status().is2xxSuccessful())
            .andReturn();
    }

    protected MvcResult post400Json(String url, Object body) throws Exception {
        return mvc.perform(
            post(url)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(objectMapper.writeValueAsBytes(body))
                .header("Authorization", "test"))
            .andExpect(status().is4xxClientError())
            .andReturn();
    }

    protected MvcResult putJson(String url, Object body) throws Exception {
        return mvc.perform(
            put(url)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(objectMapper.writeValueAsString(body))
                .header("Authorization", "test"))
            .andExpect(status().is2xxSuccessful())
            .andReturn();
    }

    protected MvcResult deleteUrl(String url, Object... params) throws Exception {
        return mvc.perform(delete(String.format(url, params)).header("Authorization", "test"))
            .andExpect(status().isOk())
            .andReturn();
    }

    protected MvcResult delete400Url(String url, Object... params) throws Exception {
        return mvc.perform(delete(String.format(url, params)).header("Authorization", "test"))
            .andExpect(status().is4xxClientError())
            .andReturn();
    }

    protected <T> T readJson(String content, Class<T> cls) throws IOException {
        return objectMapper.readValue(content, cls);
    }

    protected <T> T readJson(MvcResult content, Class<T> cls) throws IOException {
        return objectMapper.readValue(content.getResponse().getContentAsString(), cls);
    }

    protected <T> T readJson(String content, TypeReference<T> cls) throws IOException {
        return objectMapper.readValue(content, cls);
    }

    protected <T> T readJson(MvcResult result, TypeReference<T> cls) throws IOException {
        return readJson(result.getResponse().getContentAsString(), cls);
    }
}
