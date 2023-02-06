package ru.yandex.direct.http.smart;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.asynchttpclient.Response;
import org.asynchttpclient.request.body.multipart.PartBase;
import org.asynchttpclient.request.body.multipart.StringPart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.http.smart.annotations.Id;
import ru.yandex.direct.http.smart.core.Call;
import ru.yandex.direct.http.smart.core.Smart;
import ru.yandex.direct.http.smart.http.Body;
import ru.yandex.direct.http.smart.http.Field;
import ru.yandex.direct.http.smart.http.FieldMap;
import ru.yandex.direct.http.smart.http.FormUrlEncoded;
import ru.yandex.direct.http.smart.http.GET;
import ru.yandex.direct.http.smart.http.HEAD;
import ru.yandex.direct.http.smart.http.Header;
import ru.yandex.direct.http.smart.http.HeaderMap;
import ru.yandex.direct.http.smart.http.Headers;
import ru.yandex.direct.http.smart.http.Multipart;
import ru.yandex.direct.http.smart.http.POST;
import ru.yandex.direct.http.smart.http.Part;
import ru.yandex.direct.http.smart.http.PartMap;
import ru.yandex.direct.http.smart.http.Path;
import ru.yandex.direct.http.smart.http.Query;
import ru.yandex.direct.http.smart.http.QueryMap;
import ru.yandex.direct.http.smart.http.QueryName;
import ru.yandex.direct.http.smart.http.Url;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.mockito.Mockito.mock;

public class ServiceMethodBuilderNegativeTest {
    public Smart.Builder builder;

    @BeforeEach
    public void setUp() {
        builder = Smart.builder().withBaseUrl("https://ya.ru")
                .withProfileName("test")
                .withParallelFetcherFactory(mock(ParallelFetcherFactory.class));
    }

    interface TwoMethods {
        @GET("/")
        @POST("/")
        Call<String> get();
    }

    @Test
    public void twoMethods() {
        Assertions.assertThatThrownBy(() -> {
            builder.build().create(TwoMethods.class).get();
        }).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Only one HTTP method is allowed");
    }

    interface ReplaceBlockInPath {
        @GET("/?{key}")
        Call<String> get();
    }

    @Test
    public void replaceBlockInPath() {
        Assertions.assertThatThrownBy(() -> {
            builder.build().create(ReplaceBlockInPath.class).get();
        }).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("must not have replace block");
    }

    interface TwoEncodings {
        @Multipart
        @FormUrlEncoded
        Call<String> get1();

        @FormUrlEncoded
        @Multipart
        Call<String> get2();
    }

    @Test
    public void twoEncodingsMultipartFirst() {
        Assertions.assertThatThrownBy(() -> {
            builder.build().create(TwoEncodings.class).get1();
        }).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Only one encoding annotation is allowed");
    }

    @Test
    public void twoEncodingsFormUrlEncodedFirst() {
        Assertions.assertThatThrownBy(() -> {
            builder.build().create(TwoEncodings.class).get2();
        }).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Only one encoding annotation is allowed");
    }

    interface InvalidHeaders {
        @GET("/")
        @Headers({})
        Call<String> empty();

        @GET("/")
        @Headers({"header"})
        Call<String> invalidFormat();

        @GET("/")
        @Headers({"Content-Type: asdf"})
        Call<String> invalidContentType();

        @GET("/")
        Call<String> headerListWithoutType(@Header("header") List headers);

        @GET("/")
        Call<String> headerMapNotMap(@HeaderMap String string);

        @GET("/")
        Call<String> headerMapWithoutType(@HeaderMap Map map);

        @GET("/")
        Call<String> headerMapNotStringKey(@HeaderMap Map<Object, Object> map);
    }

    @Test
    public void emptyHeaders() {
        Assertions.assertThatThrownBy(() -> {
            builder.build().create(InvalidHeaders.class).empty();
        }).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("@Headers annotation is empty");
    }

    @Test
    public void invalidFormat() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidHeaders.class).invalidFormat();
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@Headers value must be in the form \"Name: Value\"");
    }

    @Test
    public void invalidContentType() {
        Assertions.assertThatThrownBy(() -> {
            builder.build().create(InvalidHeaders.class).invalidContentType();
        }).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Malformed content type");
    }

    @Test
    public void headerListWithoutType() {
        Assertions.assertThatThrownBy(() -> {
            builder.build().create(InvalidHeaders.class).headerListWithoutType(emptyList());
        }).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("must include generic type");
    }

    @Test
    public void headerMapNotMap() {
        Assertions.assertThatThrownBy(() -> {
            builder.build().create(InvalidHeaders.class).headerMapNotMap("val");
        }).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("@HeaderMap parameter type must be Map");
    }

    @Test
    public void headerMapWithoutType() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidHeaders.class).headerMapWithoutType(emptyMap());
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Map must include generic types (e.g., Map<String, String>)");
    }

    @Test
    public void headerMapNotStringKey() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidHeaders.class).headerMapNotStringKey(emptyMap());
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@HeaderMap keys must be of type String");
    }

    interface HeadNotVoid {
        @HEAD("/")
        Call<String> get();
    }

    @Test
    public void headNotVoid() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(HeadNotVoid.class).get();
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HEAD method must use Void as response type");
    }

    interface InvalidResponseType {
        @HEAD("/")
        Call<Response> get();
    }

    @Test
    public void invalidResponseType() {
        Assertions.assertThatThrownBy(() -> {
            builder.build().create(InvalidResponseType.class).get();
        }).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("is not a valid response body type");
    }

    interface WithoutMethod {
        Call<String> get();
    }

    @Test
    public void withoutMethod() {
        Assertions.assertThatThrownBy(() -> {
            builder.build().create(WithoutMethod.class).get();
        }).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("HTTP method annotation is required");
    }

    interface EncodingWithoutBody {
        @GET("/")
        @Multipart
        Call<String> multipart();

        @GET("/")
        @FormUrlEncoded
        Call<String> formUrlEncoded();
    }

    @Test
    public void multipartWithoutBody() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(EncodingWithoutBody.class).multipart();
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Multipart can only be specified on HTTP methods with request body");
    }

    @Test
    public void formUrlEncodedWithoutBody() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(EncodingWithoutBody.class).formUrlEncoded();
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FormUrlEncoded can only be specified on HTTP methods with request body");
    }

    interface UnresolvableType {
        @GET("/")
        Call<String> get(@Query("key") List<?> lst);
    }

    @Test
    public void unresolvableType() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(UnresolvableType.class).get(Collections.emptyList());
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Parameter type must not include a type variable or wildcard");
    }

    interface ParameterWithoutAnnotation {
        @GET("/")
        Call<String> get(String param);
    }

    @Test
    public void parameterWithoutAnnotation() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(ParameterWithoutAnnotation.class).get("");
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No Smart annotation found");
    }

    interface InvalidUrl {
        @GET
        Call<String> multipleUrls(@Url String url1, @Url String url2);

        @GET
        Call<String> urlAfterQuery(@Query("query") String query, @Url String url);

        @GET("/")
        Call<String> urlAndRelativePath(@Url String url1);

        @GET("/{ddd}")
        Call<String> pathWithoutPlaceholder(@Path("path") String path);

        @GET
        Call<String> invalidUrlType(@Url Integer url);
    }

    @Test
    public void multipleUrls() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidUrl.class).multipleUrls("1", "2");
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Multiple @Url method annotations found");
    }

    @Test
    public void urlAfterQuery() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidUrl.class).urlAfterQuery("1", "2");
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("A @Url parameter must not come after a @Query");
    }

    @Test
    public void urlAndRelativePath() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidUrl.class).urlAndRelativePath("1");
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@Url cannot be used with @GET URL");
    }

    @Test
    public void pathWithoutPlaceholder() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidUrl.class).pathWithoutPlaceholder("1");
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("URL \"/{ddd}\" does not contain \"{path}\". (parameter #1)");
    }

    @Test
    public void invalidUrlType() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidUrl.class).invalidUrlType(1);
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@Url must be org.asynchttpclient.uri.Uri, String or java.net.URI type.");
    }


    interface InvalidPath {
        @GET
        Call<String> pathAfterQuery(@Query("query") String query, @Path("path") String path);

        @GET
        Call<String> urlAndPath(@Url String url, @Path("path") String path);

        @GET
        Call<String> pathWithoutRelativeUrl(@Path("path") String path);

        @GET("/{path}")
        Call<String> pathAndUrl(@Path("path") String path, @Url String url);

        @GET("/")
        Call<String> pathWithoutParam(@Path("hey!") String thing);
    }

    @Test
    public void pathAfterQuery() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidPath.class).pathAfterQuery("1", "2");
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("A @Path parameter must not come after a @Query");
    }

    @Test
    public void urlAndPath() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidPath.class).urlAndPath("1", "2");
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@Path parameters may not be used with @Url");
    }

    @Test
    public void pathWithoutRelativeUrl() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidPath.class).pathWithoutRelativeUrl("1");
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@Path can only be used with relative url on @GET");
    }

    @Test
    public void pathAndUrl() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidPath.class).pathAndUrl("1", "2");
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@Path parameters may not be used with @Url");
    }

    @Test
    public void pathWithoutParam() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidPath.class).pathWithoutParam("1");
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@Path parameter name must match");
    }


    interface MultipleAnnotations {
        @GET("/{path}")
        Call<String> pathAfterQuery(@Path("path") @Query("query") String query);
    }

    @Test
    public void multipleAnnotations() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(MultipleAnnotations.class).pathAfterQuery("1");
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Multiple Smart annotations found, only one allowed");
    }

    interface WithoutUrl {
        @GET
        Call<String> get();
    }

    @Test
    public void withoutUrl() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(WithoutUrl.class).get();
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing either @GET URL or @Url parameter");
    }

    interface GetWithBody {
        @GET("/")
        Call<String> get(@Body String body);
    }

    @Test
    public void getWithBody() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(GetWithBody.class).get("1");
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Non-body HTTP method cannot contain @Body");
    }

    interface FormWithoutField {
        @POST("/")
        @FormUrlEncoded
        Call<String> get();
    }


    @Test
    public void formWithoutField() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(FormWithoutField.class).get();
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Form-encoded method must contain at least one @Field");
    }

    interface MultipartWithoutPart {
        @POST("/")
        @Multipart
        Call<String> get();
    }

    @Test
    public void multipartWithoutPart() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(MultipartWithoutPart.class).get();
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Multipart method must contain at least one @Part");
    }

    interface InvalidQuery {
        @GET("/")
        Call<String> query(@Query("query") List lst);

        @GET("/")
        Call<String> queryName(@QueryName List lst);

        @GET("/")
        Call<String> queryMapNotMap(@QueryMap String str);

        @GET("/")
        Call<String> queryMapWithoutTypes(@QueryMap Map map);

        @GET("/")
        Call<String> queryMapKeyNotString(@QueryMap Map<Object, Object> map);
    }

    @Test
    public void queryListWithoutType() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidQuery.class).query(Collections.emptyList());
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must include generic type");
    }

    @Test
    public void queryNameListWithoutType() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidQuery.class).queryName(Collections.emptyList());
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must include generic type");
    }

    @Test
    public void queryMapNotMap() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidQuery.class).queryMapNotMap("");
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@QueryMap parameter type must be Map");
    }

    @Test
    public void queryMapWithoutTypes() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidQuery.class).queryMapWithoutTypes(emptyMap());
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Map must include generic types (e.g., Map<String, String>)");
    }

    @Test
    public void queryMapKeyNotString() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidQuery.class).queryMapKeyNotString(emptyMap());
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@QueryMap keys must be of type String");
    }

    interface InvalidField {
        @POST("/")
        Call<String> fieldNotForm(@Field("key") String val);

        @POST("/")
        @FormUrlEncoded
        Call<String> fieldlListWithoutType(@Field("key") List lst);

        @POST("/")
        Call<String> fieldMapNotForm(@FieldMap Map<String, String> map);

        @POST("/")
        @FormUrlEncoded
        Call<String> fieldMapNotMap(@FieldMap String str);

        @POST("/")
        @FormUrlEncoded
        Call<String> fieldMapWithoutType(@FieldMap Map str);

        @POST("/")
        @FormUrlEncoded
        Call<String> fieldMapNotStringKey(@FieldMap Map<Object, Object> map);
    }

    @Test
    public void fieldNotForm() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidField.class).fieldNotForm("");
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@Field parameters can only be used with form encoding");
    }

    @Test
    public void fieldlListWithoutType() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidField.class).fieldlListWithoutType(Collections.emptyList());
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must include generic type");
    }

    @Test
    public void fieldMapNotForm() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidField.class).fieldMapNotForm(emptyMap());
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@FieldMap parameters can only be used with form encoding");
    }

    @Test
    public void fieldMapNotMap() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidField.class).fieldMapNotMap("");
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@FieldMap parameter type must be Map");
    }

    @Test
    public void fieldMapWithoutType() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidField.class).fieldMapWithoutType(emptyMap());
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Map must include generic types (e.g., Map<String, String>) (parameter #1)");
    }

    @Test
    public void fieldMapNotStringKey() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidField.class).fieldMapNotStringKey(emptyMap());
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@FieldMap keys must be of type String");
    }

    interface InvalidBody {
        @POST("/")
        Call<String> multipleBody(@Body String param1, @Body String param2);

        @POST("/")
        @Multipart
        Call<String> multipartBody(@Body String param);

        @POST("/")
        @FormUrlEncoded
        Call<String> formBody(@Body String param);
    }

    @Test
    public void multipleBody() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidBody.class).multipleBody("1", "2");
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Multiple @Body method annotations found");
    }

    @Test
    public void multipartBody() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidBody.class).multipartBody("1");
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@Body parameters cannot be used with form or multi-part encoding");
    }

    @Test
    public void formBody() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidBody.class).formBody("1");
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@Body parameters cannot be used with form or multi-part encoding");
    }

    interface InvalidMultipart {
        @POST("/")
        Call<String> partWithoutMultipart(@Part("part") String param);

        @POST("/")
        @Multipart
        Call<String> partListWithoutType(@Part List param);

        @POST("/")
        @Multipart
        Call<String> partListOfNotParts(@Part List<String> param);

        @POST("/")
        @Multipart
        Call<String> partArrayOfNotParts(@Part String... param);

        @POST("/")
        @Multipart
        Call<String> partWithoutName(@Part String param);

        @POST("/")
        @Multipart
        Call<String> partListWithoutTypeWithName(@Part("part") List param);

        @POST("/")
        @Multipart
        Call<String> partListWithNamePartType(@Part("part") PartBase param);

        @POST("/")
        @Multipart
        Call<String> partListWithNamePartListType(@Part("part") List<PartBase> param);

        @POST("/")
        @Multipart
        Call<String> partListWithNamePartArrayType(@Part("part") PartBase... param);
    }

    @Test
    public void partWithoutMultipart() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidMultipart.class).partWithoutMultipart("1");
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@Part parameters can only be used with multipart encoding");
    }

    @Test
    public void partListWithoutType() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidMultipart.class).partListWithoutType(emptyList());
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must include generic type");
    }

    @Test
    public void partListOfNotParts() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidMultipart.class).partListOfNotParts(emptyList());
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "@Part annotation must supply a name or use org.asynchttpclient.request.body.multipart.Part " +
                                "parameter type");
    }

    @Test
    public void partArrayOfNotParts() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidMultipart.class).partArrayOfNotParts("");
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "@Part annotation must supply a name or use org.asynchttpclient.request.body.multipart.Part " +
                                "parameter type");
    }

    @Test
    public void partWithoutName() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidMultipart.class).partWithoutName("");
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "@Part annotation must supply a name or use org.asynchttpclient.request.body.multipart.Part " +
                                "parameter type");
    }

    @Test
    public void partListWithoutTypeWithName() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidMultipart.class).partListWithoutTypeWithName(emptyList());
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must include generic type");
    }

    @Test
    public void partListWithNamePartType() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidMultipart.class).partListWithNamePartType(new StringPart("k", "v"));
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "@Part parameters using the org.asynchttpclient.request.body.multipart."
                                + "Part must not include a part name in the annotation");
    }

    @Test
    public void partListWithNamePartListType() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidMultipart.class).partListWithNamePartListType(emptyList());
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "@Part parameters using the org.asynchttpclient.request.body.multipart."
                                + "Part must not include a part name in the annotation");
    }

    @Test
    public void partListWithNamePartArrayType() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidMultipart.class).partListWithNamePartArrayType(new StringPart("k",
                            "v"));
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "@Part parameters using the org.asynchttpclient.request.body.multipart."
                                + "Part must not include a part name in the annotation");
    }

    interface InvalidMultipartMap {
        @POST("/")
        Call<String> partMapWithoutMultipart(@PartMap Map<String, String> param);

        @POST("/")
        @Multipart
        Call<String> notMap(@PartMap String param);

        @POST("/")
        @Multipart
        Call<String> withoutType(@PartMap Map param);

        @POST("/")
        @Multipart
        Call<String> keyNotString(@PartMap Map<Object, PartBase> param);

        @POST("/")
        @Multipart
        Call<String> valuePart(@PartMap Map<String, PartBase> param);
    }

    @Test
    public void partMapWithoutMultipart() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidMultipartMap.class).partMapWithoutMultipart(emptyMap());
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@PartMap parameters can only be used with multipart encoding");
    }

    @Test
    public void partMapNotMap() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidMultipartMap.class).notMap("");
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@PartMap parameter type must be Map.");
    }

    @Test
    public void partMapWithoutType() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidMultipartMap.class).withoutType(emptyMap());
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Map must include generic types (e.g., Map<String, String>)");
    }

    @Test
    public void partMapKeyNotString() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidMultipartMap.class).keyNotString(emptyMap());
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@PartMap keys must be of type String");
    }

    @Test
    public void partMapValueNotPart() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidMultipartMap.class).valuePart(emptyMap());
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "@PartMap values cannot be org.asynchttpclient.request.body.multipart.Part. "
                                + "Use @Part List<Part> or a different value type instead. (parameter #1)");
    }

    interface InvalidId {
        @GET("/")
        Call<String> invalidIdType(@Id long id);

        @GET("/")
        Call<String> multipleIds(@Id Long id1, @Id Long id2);

    }

    @Test
    public void invalidIdType() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidId.class).invalidIdType(1L);
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "@Id parameter must be java.lang.Long");
    }

    @Test
    public void multipleIds() {
        Assertions.assertThatThrownBy(() -> {
                    builder.build().create(InvalidId.class).multipleIds(1L, 2L);
                }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Multiple @Id method annotations found");
    }
}
