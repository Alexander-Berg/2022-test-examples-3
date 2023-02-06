package ru.yandex.market.ir.autogeneration.common.db;

import com.googlecode.protobuf.format.JsonFormat;
import ru.yandex.market.mbo.http.ModelStorage;

public class TestModelData {
    public static final String PICTURE_CONTENT_URL_0 =
        "//avatars.mdst.yandex.net/get-mpic/4868/img_id2340357150036336256.jpeg/orig";
    public static final String PICTURE_CONTENT_URL_1 =
        "//avatars.mdst.yandex.net/get-mpic/4868/img_id7034390691237352312.jpeg/orig";
    public static final String PICTURE_CONTENT_URL_2 =
        "//avatars.mdst.yandex.net/get-mpic/4868/img_id7796943047537682165.jpeg/orig";

    public static final String MODEL_JSON =
        "{\n" +
        "  \"id\": 100100143829,\n" +
        "  \"category_id\": 91529,\n" +
        "  \"vendor_id\": 1006808,\n" +
        "  \"source_type\": \"VENDOR\",\n" +
        "  \"current_type\": \"VENDOR\",\n" +
        "  \"titles\": [\n" +
        "    {\n" +
        "      \"isoCode\": \"ru\",\n" +
        "      \"value\": \"Пробная модель\"\n" +
        "    }\n" +
        "  ],\n" +
        "  \"parameter_values\": [\n" +
        "    {\n" +
        "      \"param_id\": 7351729,\n" +
        "      \"type_id\": 4,\n" +
        "      \"str_value\": [\n" +
        "        {\n" +
        "          \"isoCode\": \"ru\",\n" +
        "          \"value\": \"" + PICTURE_CONTENT_URL_1 + "\"\n" +
        "        }\n" +
        "      ],\n" +
        "      \"xsl_name\": \"XL-Picture\",\n" +
        "      \"value_source\": \"VENDOR_OFFICE\",\n" +
        "      \"user_id\": 0,\n" +
        "      \"modification_date\": 1511950275549,\n" +
        "      \"value_type\": \"STRING\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"param_id\": 7351730,\n" +
        "      \"type_id\": 4,\n" +
        "      \"str_value\": [\n" +
        "        {\n" +
        "          \"isoCode\": \"ru\",\n" +
        "          \"value\": \"" + PICTURE_CONTENT_URL_2 + "\"\n" +
        "        }\n" +
        "      ],\n" +
        "      \"xsl_name\": \"XL-Picture_2\",\n" +
        "      \"value_source\": \"VENDOR_OFFICE\",\n" +
        "      \"user_id\": 0,\n" +
        "      \"modification_date\": 1511950276997,\n" +
        "      \"value_type\": \"STRING\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"param_id\": 7351735,\n" +
        "      \"type_id\": 2,\n" +
        "      \"numeric_value\": \"636\",\n" +
        "      \"xsl_name\": \"XLPictureSizeX\",\n" +
        "      \"value_source\": \"VENDOR_OFFICE\",\n" +
        "      \"user_id\": 0,\n" +
        "      \"modification_date\": 1511950275549,\n" +
        "      \"value_type\": \"NUMERIC\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"param_id\": 7351736,\n" +
        "      \"type_id\": 2,\n" +
        "      \"numeric_value\": \"636\",\n" +
        "      \"xsl_name\": \"XLPictureSizeX_2\",\n" +
        "      \"value_source\": \"VENDOR_OFFICE\",\n" +
        "      \"user_id\": 0,\n" +
        "      \"modification_date\": 1511950276997,\n" +
        "      \"value_type\": \"NUMERIC\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"param_id\": 7351741,\n" +
        "      \"type_id\": 2,\n" +
        "      \"numeric_value\": \"460\",\n" +
        "      \"xsl_name\": \"XLPictureSizeY\",\n" +
        "      \"value_source\": \"VENDOR_OFFICE\",\n" +
        "      \"user_id\": 0,\n" +
        "      \"modification_date\": 1511950275549,\n" +
        "      \"value_type\": \"NUMERIC\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"param_id\": 7351742,\n" +
        "      \"type_id\": 2,\n" +
        "      \"numeric_value\": \"460\",\n" +
        "      \"xsl_name\": \"XLPictureSizeY_2\",\n" +
        "      \"value_source\": \"VENDOR_OFFICE\",\n" +
        "      \"user_id\": 0,\n" +
        "      \"modification_date\": 1511950276997,\n" +
        "      \"value_type\": \"NUMERIC\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"param_id\": 7351712,\n" +
        "      \"type_id\": 2,\n" +
        "      \"numeric_value\": \"200\",\n" +
        "      \"xsl_name\": \"BigPicture_X\",\n" +
        "      \"value_source\": \"VENDOR_OFFICE\",\n" +
        "      \"user_id\": 0,\n" +
        "      \"modification_date\": 1511950273958,\n" +
        "      \"value_type\": \"NUMERIC\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"param_id\": 7351714,\n" +
        "      \"type_id\": 2,\n" +
        "      \"numeric_value\": \"148\",\n" +
        "      \"xsl_name\": \"BigPicture_Y\",\n" +
        "      \"value_source\": \"VENDOR_OFFICE\",\n" +
        "      \"user_id\": 0,\n" +
        "      \"modification_date\": 1511950273958,\n" +
        "      \"value_type\": \"NUMERIC\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"param_id\": 5085086,\n" +
        "      \"type_id\": 4,\n" +
        "      \"str_value\": [\n" +
        "        {\n" +
        "          \"isoCode\": \"ru\",\n" +
        "          \"value\": \"" + PICTURE_CONTENT_URL_0 + "\"\n" +
        "        }\n" +
        "      ],\n" +
        "      \"xsl_name\": \"BigPicture\",\n" +
        "      \"value_source\": \"VENDOR_OFFICE\",\n" +
        "      \"user_id\": 0,\n" +
        "      \"modification_date\": 1511950273958,\n" +
        "      \"value_type\": \"STRING\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"param_id\": 7351716,\n" +
        "      \"type_id\": 4,\n" +
        "      \"str_value\": [\n" +
        "        {\n" +
        "          \"isoCode\": \"ru\",\n" +
        "          \"value\": \"000.ru/a.jpg\"\n" +
        "        }\n" +
        "      ],\n" +
        "      \"xsl_name\": \"BigPictureUrl\",\n" +
        "      \"value_source\": \"VENDOR_OFFICE\",\n" +
        "      \"user_id\": 0,\n" +
        "      \"modification_date\": 1511950273958,\n" +
        "      \"value_type\": \"STRING\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"param_id\": 10470267,\n" +
        "      \"type_id\": 4,\n" +
        "      \"str_value\": [\n" +
        "        {\n" +
        "          \"isoCode\": \"ru\",\n" +
        "          \"value\": \"//avatars.mdst.yandex.net/get-mpic/4868/img_id8848571117914656112.jpeg/orig\"\n" +
        "        }\n" +
        "      ],\n" +
        "      \"xsl_name\": \"BigPicture_Orig\",\n" +
        "      \"value_source\": \"VENDOR_OFFICE\",\n" +
        "      \"user_id\": 0,\n" +
        "      \"modification_date\": 1511950273958,\n" +
        "      \"value_type\": \"STRING\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"param_id\": 7351771,\n" +
        "      \"type_id\": 4,\n" +
        "      \"str_value\": [\n" +
        "        {\n" +
        "          \"isoCode\": \"ru\",\n" +
        "          \"value\": \"Пробная модель\"\n" +
        "        }\n" +
        "      ],\n" +
        "      \"xsl_name\": \"name\",\n" +
        "      \"value_source\": \"VENDOR_OFFICE\",\n" +
        "      \"user_id\": 28027378,\n" +
        "      \"modification_date\": 1511950272559,\n" +
        "      \"value_type\": \"STRING\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"param_id\": 10470257,\n" +
        "      \"type_id\": 4,\n" +
        "      \"str_value\": [\n" +
        "        {\n" +
        "          \"isoCode\": \"ru\",\n" +
        "          \"value\": \"//avatars.mdst.yandex.net/get-mpic/4138/img_id2231959755988195399.jpeg/orig\"\n" +
        "        }\n" +
        "      ],\n" +
        "      \"xsl_name\": \"XLPictureOrig_2\",\n" +
        "      \"value_source\": \"VENDOR_OFFICE\",\n" +
        "      \"user_id\": 0,\n" +
        "      \"modification_date\": 1511950276997,\n" +
        "      \"value_type\": \"STRING\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"param_id\": 10470256,\n" +
        "      \"type_id\": 4,\n" +
        "      \"str_value\": [\n" +
        "        {\n" +
        "          \"isoCode\": \"ru\",\n" +
        "          \"value\": \"//avatars.mdst.yandex.net/get-mpic/4868/img_id3708928241497333420.jpeg/orig\"\n" +
        "        }\n" +
        "      ],\n" +
        "      \"xsl_name\": \"XLPictureOrig\",\n" +
        "      \"value_source\": \"VENDOR_OFFICE\",\n" +
        "      \"user_id\": 0,\n" +
        "      \"modification_date\": 1511950275549,\n" +
        "      \"value_type\": \"STRING\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"param_id\": 7351747,\n" +
        "      \"type_id\": 4,\n" +
        "      \"str_value\": [\n" +
        "        {\n" +
        "          \"isoCode\": \"ru\",\n" +
        "          \"value\": \"000.ru/a.jpg\"\n" +
        "        }\n" +
        "      ],\n" +
        "      \"xsl_name\": \"XLPictureUrl\",\n" +
        "      \"value_source\": \"VENDOR_OFFICE\",\n" +
        "      \"user_id\": 0,\n" +
        "      \"modification_date\": 1511950275549,\n" +
        "      \"value_type\": \"STRING\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"param_id\": 7351748,\n" +
        "      \"type_id\": 4,\n" +
        "      \"str_value\": [\n" +
        "        {\n" +
        "          \"isoCode\": \"ru\",\n" +
        "          \"value\": \"000.ru/b.jpg\"\n" +
        "        }\n" +
        "      ],\n" +
        "      \"xsl_name\": \"XLPictureUrl_2\",\n" +
        "      \"value_source\": \"VENDOR_OFFICE\",\n" +
        "      \"user_id\": 0,\n" +
        "      \"modification_date\": 1511950276997,\n" +
        "      \"value_type\": \"STRING\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"param_id\": 7893318,\n" +
        "      \"type_id\": 1,\n" +
        "      \"option_id\": 1006808,\n" +
        "      \"xsl_name\": \"vendor\",\n" +
        "      \"value_source\": \"VENDOR_OFFICE\",\n" +
        "      \"user_id\": 28027378,\n" +
        "      \"modification_date\": 1511950272559,\n" +
        "      \"value_type\": \"ENUM\"\n" +
        "    }\n" +
        "  ],\n" +
        "  \"published\": false,\n" +
        "  \"deleted\": false,\n" +
        "  \"modified_ts\": 1511950279879,\n" +
        "  \"modified_user_id\": 0,\n" +
        "  \"checked\": false,\n" +
        "  \"created_date\": 1511787265225,\n" +
        "  \"shop_count\": 0,\n" +
        "  \"doubtful\": false,\n" +
        "  \"titleApproved\": false,\n" +
        "  \"pictures\": [\n" +
        "    {\n" +
        "      \"xslName\": \"BigPicture\",\n" +
        "      \"url\": \"" + PICTURE_CONTENT_URL_0 + "\",\n" +
        "      \"width\": 200,\n" +
        "      \"height\": 148,\n" +
        "      \"url_source\": \"000.ru/a.jpg\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"xslName\": \"XL-Picture\",\n" +
        "      \"url\": \"" + PICTURE_CONTENT_URL_1 + "\",\n" +
        "      \"width\": 636,\n" +
        "      \"height\": 460,\n" +
        "      \"url_source\": \"000.ru/a.jpg\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"xslName\": \"XL-Picture_2\",\n" +
        "      \"url\": \"" + PICTURE_CONTENT_URL_2 + "\",\n" +
        "      \"width\": 636,\n" +
        "      \"height\": 460,\n" +
        "      \"url_source\": \"000.ru/b.jpg\"\n" +
        "    }\n" +
        "  ],\n" +
        "  \"micro_model_search\": \"\"\n" +
        "}\n";

    public static final ModelStorage.Model MODEL;
    static {
        try {
            ModelStorage.Model.Builder modelBuilder = ModelStorage.Model.newBuilder();
            JsonFormat.merge(MODEL_JSON, modelBuilder);
            MODEL = modelBuilder.build();
        } catch (JsonFormat.ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private TestModelData() {
    }
}
