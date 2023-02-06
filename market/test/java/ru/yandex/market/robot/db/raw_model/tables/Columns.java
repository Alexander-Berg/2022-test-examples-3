package ru.yandex.market.robot.db.raw_model.tables;

/**
 * @author jkt on 19.12.17.
 */
public class Columns {

    public static final String CATEGORY_ID = "category_id";

    public static final String SOURCES = "sources";
    public static final String SETTINGS = "settings";
    public static final String TYPE = "type";

    public static final String SOURCE_ID = "source_id";
    public static final String REMOVE_UPPER_CASE = "remove_upper_case";
    public static final String REMOVE_CATEGORY_NAME = "remove_category_name";
    public static final String ENABLED = "enabled";
    public static final String STOP_WORDS = "stop_words";
    public static final String SPLIT_NUMBERS_LETTERS = "split_numbers_letters";
    public static final String CALCULATE_METRICS = "calculate_metrics";
    public static final String PARTNER_SHOP_ID = "partner_shop_id";

    public static final String ADD_TO_TITLE_STR = "add_to_title_str";
    public static final String XSL_NAME = "xsl_name";
    public static final String PROCESSORS = "processors";
    public static final String ALIASES = "aliases";
    public static final String ADD_TO_TITLE_ON_DUPLICATE = "add_to_title_on_duplicate";
    public static final String POSITION = "position";
    public static final String REMOVE_FROM_TITLE = "remove_from_title";
    public static final String PARAM_ID = "param_id";

    public static final String TYPE_INDEX = "type_index";
    public static final String USE_FORMALIZER = "use_formalizer";
    public static final String VENDOR_CODE_INDEX = "vendor_code_index";
    public static final String CLUSTERIZER_TYPE = "clusterizer_type";
    public static final String CLASSIFIER_WORDS = "classifier_words";
    public static final String CHECK_MODELS = "check_models";
    public static final String ID = "id";

    public static final String MODEL_ID = "model_id";
    public static final String NAME = "name";
    public static final String SUPPLEMENT = "supplement";
    public static final String REC_RAW_ID = "rec_raw_id";
    public static final String REC_MARKET_MODEL_ID = "rec_market_model_id";


    public static final String PICTURE_HASH = "picture_hash";
    public static final String SOURCE_URL = "source_url";
    public static final String STATUS = "status";
    public static final String URL = "url";
    public static final String DELETED = "deleted";
    public static final String FIRST_VERSION_NUMBER = "first_version_number";
    public static final String LAST_VERSION_NUMBER = "last_version_number";
    public static final String DOWNLOAD_TIMESTAMP = "download_timestamp";
    public static final String DOWNLOAD_STATUS = "download_status";
    public static final String DOWNLOAD_ERROR = "download_error";

    public static final String INTERNAL = "internal";
    public static final String VALUE = "value";
    public static final String UNIT = "unit";
    public static final String INDEX = "index";
    public static final String MARKET_PARAM_ID = "market_param_id";
    public static final String MARKET_VALUE = "market_value";

    public static final String MARKET_MODEL_ID = "market_model_id";
    public static final String MARKET_MODEL = "market_model";
    public static final String MARKET_MODIFICATION_ID = "market_modification_id";
    public static final String MARKET_MODIFICATION = "market_modification";
    public static final String CREATED = "created";
    public static final String MODEL_CREATED_DATE = "model_created_date";
    public static final String MODEL_PUBLISHED_DATE = "model_published_date";
    public static final String FIRST_PARAMS_COVERAGE = "first_params_coverage";
    public static final String LAST_PARAMS_COVERAGE = "last_params_coverage";
    public static final String PICTURES_COUNT = "pictures_count";

    public static final String VENDOR_ID = "vendor_id";
    public static final String VENDOR = "vendor";
    public static final String MARKET_CATEGORY_ID = "market_category_id";
    public static final String MAPPED_CATEGORY_ID = "mapped_category_id";
    public static final String MARKET_CATEGORY_NAME = "market_category_name";
    public static final String LAST_MODEL_UPDATE_TIME = "last_model_update_time";
    public static final String MARKET_CATEGORY_STATUS = "market_category_status";

    public static final String RAW_ID = "raw_id";
    public static final String CREATE_VERSION_NUMBER = "create_version_number";
    public static final String CREATE_VERSION_DATE = "create_version_date";
    public static final String FIRST_VERSION_DATE = "first_version_date";
    public static final String LAST_VERSION_DATE = "last_version_date";
    public static final String CATEGORY = "category";

    public static final String VENDOR_CODE = "vendor_code";
    public static final String DESCRIPTION = "description";
    public static final String ANNOUNCE_DATE = "announce_date";
    public static final String IN_STOCK_DATE = "in_stock_date";
    public static final String ACTUAL = "actual";
    public static final String FULL_NAME = "full_name";

    public static final String OFFERS_COUNT = "offers_count";
    public static final String MATCHED_COUNT = "matched_count";

    public static final String CREATED_ROW_DATE = "created_row_date";
}
