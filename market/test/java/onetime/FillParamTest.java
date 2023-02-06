//package onetime;
//
//import org.apache.commons.httpclient.HttpClient;
//import org.apache.commons.httpclient.methods.GetMethod;
//import org.junit.Test;
//import org.springframework.jdbc.core.JdbcTemplate;
//import ru.yandex.common.util.db.NamedDataSource;
//import ru.yandex.market.ir.http.Markup;
//import ru.yandex.market.ir.http.MarkupServiceStub;
//import ru.yandex.market.mbo.http.ModelStorage;
//
//import java.io.File;
//import java.io.InputStream;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Scanner;
//import java.util.Set;
//
///**
// * https://st.yandex-team.ru/ANGADMIN-12782
// *
// * @author Tatiana Goncharova <a href="mailto:tanlit@yandex-team.ru"></a>
// * @date 29.11.2016
// */
//public class FillParamTest {
//    private static final int REQUIRED_PARAM_COUNT = 4;
//
//    @Test
//    public void fillParamsTasks() throws Exception {
//        MarkupServiceStub markupService = new MarkupServiceStub();
//        markupService.setHost("http://csir1ht.yandex.ru:34536/");
//
//        Markup.LoadFillParamsRequest request = buildFillParamRequest(91148, 10);
////        Markup.LoadResponse response = markupService.loadFillParamsTasks(request); compilation err
//
////        System.out.println(response.getStatus());
//    }
//
//    private static Markup.LoadFillParamsRequest buildFillParamRequest(int categoryId, int taskCount)
//    throws Exception {
//        Markup.LoadFillParamsRequest.Builder result = Markup.LoadFillParamsRequest.newBuilder();
//
//        List<ModelStorage.Model> modelList = getModelsInCategory(categoryId);
//        Map<Long, Set<String>> existingModelsIds = readExistingModelIds(categoryId);
//        Set<Long> allowedParams = getAllowedParams(categoryId);
//
//        for (ModelStorage.Model model : modelList) {
//            if (!existingModelsIds.containsKey(model.getId())) {
//                continue;
//            }
//            List<Long> params = new ArrayList<>();
//            String image = null;
//            for (ModelStorage.ParameterValue pv : model.getParameterValuesList()) {
//                if (pv.getXslName().equals("XL-Picture")) {
//                    image = "http:" + pv.getStrValue(0).getValue();
//                } else {
//                    params.add(pv.getParamId());
//                }
//            }
//            if (image == null) {
//                continue;
//            }
//            Collections.shuffle(params);
//
//            List<Long> paramIds = new ArrayList<>();
//            for (long paramId : params) {
//                if (!allowedParams.contains(paramId)) {
//                    continue;
//                }
//
//                paramIds.add(paramId);
//
//                if (paramIds.size() >= REQUIRED_PARAM_COUNT) {
//                    break;
//                }
//            }
//            if (paramIds.size() < REQUIRED_PARAM_COUNT) {
//                continue;
//            }
//            result.addTask(Markup.FillParamsTask.newBuilder()
//                .setModelId(model.getId())
//                .setCategoryId(categoryId)
//                .addAllParamId(paramIds)
//            );
//            if (result.getTaskCount() == taskCount) {
//                break;
//            }
//        }
//        return result.build();
//    }
//
//    private static JdbcTemplate initJdbc() throws Exception {
//        NamedDataSource dataSource = new NamedDataSource();
//        dataSource.setMaxWait(300000);
//        dataSource.setMinEvictableIdleTimeMillis(3600000);
//        dataSource
//        .setNativeJdbcExtractor(new org.springframework.jdbc.support.nativejdbc.CommonsDbcpNativeJdbcExtractor());
//        dataSource.setDriverClassName("oracle.jdbc.driver.OracleDriver");
////        dataSource.setUrl("jdbc:oracle:thin:@markettestdbh-sas.yandex.ru:1521/scatdb");
//        dataSource.setUrl("jdbc:oracle:thin:@scatdb1f-vip.yandex.ru:1521/scatdb");
//        dataSource.setUsername("site_catalog");
//        dataSource.setPassword("99(=dev=mbo=)");
//        dataSource.setValidationQuery("select 1 from dual");
//
//        return new JdbcTemplate(dataSource);
//    }
//
//    private static Map<Long, Set<String>> readExistingModelIds(int categoryId) throws Exception {
///*
//select matched_id, url from (
//select matched_id, url, row_number() OVER
// (partition by matched_id ORDER by url) as row from offers where category_id = 91148 and matched_id > 0
//  and url <> ''
// ) t where row < 3
//         */
//        Map<Long, Set<String>> result = new HashMap<>();
//        Scanner sc = new Scanner(new File("/home/tanlit/" + categoryId + "_existing_models.csv"));
//        sc.nextLine();
//        while (sc.hasNextLine()) {
//            String[] s = sc.nextLine().split(",");
//            long modelId = Long.parseLong(s[0]);
//            String url = s[1].startsWith("http") ? s[1] : "http://" + s[1];
//
//            Set<String> urls = result.get(modelId);
//            if (urls == null) {
//                urls = new HashSet<>();
//                result.put(modelId, urls);
//            }
//            urls.add(url);
//        }
//        sc.close();
//        return result;
//    }
//
//    private static List<ModelStorage.Model> getModelsInCategory(int categoryId) throws Exception {
//
//        long createdThreshold = new SimpleDateFormat("dd/MM/yyyy").parse("01/01/2014").getTime();
//
//        List<ModelStorage.Model> modelList = new ArrayList<>();
//
//        HttpClient httpClient = createHttpClient();
//
//        // ssh -f -N -L 33715:cs-clusterizer02h.yandex.ru:33714 public01d.market.yandex.net
//        GetMethod getMethod = new GetMethod(
//            "http://127.0.0.1:33715/models/getCategoryModels?category_id=" + categoryId +
//                "&type=GURU&limit=15000&deleted=null"
//        );
//        httpClient.executeMethod(getMethod);
//        InputStream inputStream = getMethod.getResponseBodyAsStream();
//        ModelStorage.Model model;
//
//        while (null != (model = ModelStorage.Model.parseDelimitedFrom(inputStream)) && model.getId() != 0) {
//            if (!model.getDeleted() && model.getCreatedDate() > createdThreshold) {
//                modelList.add(model);
//            }
//        }
//
//        Collections.shuffle(modelList);
//        return modelList;
//    }
//
//    private static HttpClient createHttpClient() {
//        HttpClient result = new HttpClient();
//        result.getHttpConnectionManager().getParams().setConnectionTimeout(10000);
//        return result;
//    }
//
//    private static Set<Long> getAllowedParams(int categoryId) throws Exception {
//        JdbcTemplate siteCatalogJdbcTemplate = initJdbc();
//        Set<Long> result = new HashSet<>();
//        siteCatalogJdbcTemplate.query(
//            "select p.id, p.type, min_value, max_value, description, n.name, " +
//                "unit_id from market_content.parameter p " +
//                " inner join market_content.param_name n on n.id = p.id " +
//                " where p.category_hid = ? and p.override_param is null and use_for_guru = 1 " +
//                "and is_service = 0",
//            rs -> {
//                result.add(rs.getLong("id"));
//            },
//            categoryId
//        );
//        return result;
//    }
//
//}
