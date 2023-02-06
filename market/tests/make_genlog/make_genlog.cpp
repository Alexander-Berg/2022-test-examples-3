#include <market/library/snappy-protostream/proto_snappy_stream.h>
#include <market/library/fixed_point_number/fixed_point_number.h>
#include <market/idx/library/proto_helpers/offers_data.h>
#include <market/proto/common/common.pb.h>
#include <market/proto/indexer/GenerationLog.pb.h>

#include <library/cpp/json/json_reader.h>
#include <library/cpp/json/json_value.h>

#include <mapreduce/yt/interface/client.h>

#include <stdexcept>
#include <iostream>

#include <util/string/vector.h>
#include <util/stream/file.h>
#include <util/string/hex.h>
#include <util/string/split.h>


using GLRecord = MarketIndexer::GenerationLog::Record;


/* For binary_price && binary_oldprice parsing */
void FillPriceExpression(NMarketIndexer::Common::PriceExpression* qprice,
                         const TString& priceExpression)
{
    const std::size_t PriceExpressionFieldsNumber = 5;

    /*                                0    1       2        3   4
     * FORMAT of priceExpression: "PRICE RATE PLUS_PERCENT ID REF_ID"
     */

    TVector<TString> expression;
    StringSplitter(priceExpression.data()).Split(' ').SkipEmpty().Collect(&expression);
    if (expression.size() != PriceExpressionFieldsNumber)
        ythrow yexception() << "Price Expression has wrong number of fields";

    // price
    qprice->set_price(TFixedPointNumber(TFixedPointNumber::DefaultPrecision,
                      ::FromString<double>(expression[0])).AsRaw());

    /*
     * Avoid serialization of values which are the same as default ones
     * Write only non-default values for rate, plus, currency_id, ref_currency_id
     */

    // rate
    const TString& rate = expression[1];
    if (rate != qprice->rate())
        qprice->set_rate(rate);

    // plus
    const double plus_percent = ::FromString<double>(expression[2]);
    if (plus_percent != qprice->plus())
        qprice->set_plus(plus_percent);

    // currency_id
    const TString& currency_id = expression[3];
    if (currency_id != qprice->id())
        qprice->set_id(currency_id);

    // reference currency id
    const TString& currency_ref_id = expression[4];
    if (currency_ref_id != qprice->ref_id())
        qprice->set_ref_id(currency_ref_id);
}


using namespace NJson;


int main(int argc, char** argv)
{
    if ((argc != 2) && (argc != 5))
        return 1;

    NYT::TTableWriterPtr<GLRecord> ytWriter;
    NYT::IClientPtr ytClient;
    if (argc == 5) {
        const char* ytProxy = argv[2];
        const char* ytTokenPath = argv[3];
        const char* tableName = argv[4];
        ytClient = NYT::CreateClient(
            TString(ytProxy),
            NYT::TCreateClientOptions()
                .TokenPath(TString(ytTokenPath))
        );
        ytClient->CreateTable<GLRecord>(tableName, {}, NYT::TCreateOptions().Force(true).Recursive(true));
        ytWriter = ytClient->CreateTableWriter<GLRecord>(tableName);
    }

    TJsonValue feedLog;
    {
        TUnbufferedFileInput input((TString(argv[1])));
        if (!ReadJsonTree(&input, &feedLog))
            throw std::runtime_error("Error parsing input");
    }

    NMarket::TSnappyProtoWriter writer("/dev/stdout", TStringBuf("GLOG"));

    for (const TJsonValue& jsonRecord : feedLog.GetArray()) {
        GLRecord record;
        record.set_feed_id(jsonRecord.Has("feed_id") ? jsonRecord["feed_id"].GetInteger() : 100);
        record.set_shop_id(jsonRecord.Has("shop_id") ? jsonRecord["shop_id"].GetInteger() : 100);
        if (jsonRecord.Has("supplier_id")) {
            record.set_supplier_id(jsonRecord["supplier_id"].GetUInteger());
        }
        if (jsonRecord.Has("supplier_type")) {
            record.set_supplier_type(jsonRecord["supplier_type"].GetUInteger());
        }
        if (jsonRecord.Has("offer_id"))
            record.set_offer_id(jsonRecord["offer_id"].GetString());

        record.set_shop_name(jsonRecord["shop_name"].GetString());
        if (jsonRecord.Has("int_regions"))
            for (auto& paramValue: jsonRecord["int_regions"].GetArray()) {
                record.add_int_regions(paramValue.GetUInteger());
            }
        record.set_priority_regions(jsonRecord["priority_regions"].GetString());
        if (jsonRecord.Has("int_geo_regions"))
            for (auto& paramValue: jsonRecord["int_geo_regions"].GetArray()) {
                record.add_int_geo_regions(paramValue.GetUInteger());
            }
        record.set_binary_ware_md5(HexDecode(jsonRecord["binary_ware_md5"].GetString()));
        record.set_url(jsonRecord["url"].GetString());
        record.set_title(jsonRecord["title"].GetString());
        record.set_description(jsonRecord["description"].GetString());
        record.set_shop_category_id(jsonRecord["shop_category_id"].GetString());
        record.set_picture_url(jsonRecord["picture_url"].GetString());
        record.set_model_id(jsonRecord.Has("model_id") ? jsonRecord["model_id"].GetUInteger() : 0);
        record.set_picture_url(jsonRecord["picture_url"].GetString());
        record.set_url(jsonRecord["url"].GetString());
        record.set_classifier_magic_id(jsonRecord["classifier_magic_id"].GetString());
        record.set_vendor_id(jsonRecord["vendor_id"].GetInteger());
        record.set_downloadable(jsonRecord["downloadable"].GetBoolean());
        record.set_flags(jsonRecord["flags"].GetUInteger());

        record.set_vendor_param_id(1111);
        record.set_hasfortitleortype(false);
        record.set_hasofferparams(true);
        record.set_is_express(jsonRecord["is_express"].GetBoolean());
        record.set_warehouse_id(jsonRecord["warehouse_id"].GetUInteger());

        if (jsonRecord.Has("cluster_id"))
            record.set_cluster_id(jsonRecord["cluster_id"].GetUInteger());

        record.set_category_id(jsonRecord.Has("category_id") ? jsonRecord["category_id"].GetUInteger() : 0);

        if (jsonRecord.Has("binary_price"))
            FillPriceExpression(record.mutable_binary_price(),
                                jsonRecord["binary_price"].GetString());
        if (jsonRecord.Has("binary_oldprice"))
            FillPriceExpression(record.mutable_binary_oldprice(),
                                jsonRecord["binary_oldprice"].GetString());
        if (jsonRecord.Has("binary_white_oldprice"))
            FillPriceExpression(record.mutable_binary_white_oldprice(),
                                jsonRecord["binary_white_oldprice"].GetString());
        if (jsonRecord.Has("binary_blue_oldprice"))
            FillPriceExpression(record.mutable_binary_blue_oldprice(),
                                jsonRecord["binary_blue_oldprice"].GetString());
        if (jsonRecord.Has("binary_allowed_oldprice"))
            FillPriceExpression(record.mutable_binary_allowed_oldprice(),
                                jsonRecord["binary_allowed_oldprice"].GetString());
        if (jsonRecord.Has("binary_unverified_oldprice"))
            FillPriceExpression(record.mutable_binary_unverified_oldprice(),
                                jsonRecord["binary_unverified_oldprice"].GetString());
        if (jsonRecord.Has("binary_min_price"))
            FillPriceExpression(record.mutable_binary_min_price(),
                                jsonRecord["binary_min_price"].GetString());

        if (jsonRecord.Has("outlets_data")) {
            for (const auto& value: jsonRecord["outlets_data"].GetArray()) {
                record.add_outlets_data(value.GetUInteger());
            }
        }

        if (jsonRecord.Has("mbo_params")) {
            for (const auto& jsonParam: jsonRecord["mbo_params"].GetArray()) {
                auto* param = record.add_mbo_params();
                param->set_id(jsonParam["id"].GetUInteger());
                for (auto& paramValue: jsonParam["values"].GetArray()) {
                    param->add_values(paramValue.GetUInteger());
                }
            }
        }

        if (jsonRecord.Has("numeric_params")) {
            for (const auto& jsonParam: jsonRecord["numeric_params"].GetArray()) {
                auto* param = record.add_numeric_params();
                param->set_id(jsonParam["id"].GetUInteger());
                for (auto& paramValue: jsonParam["ranges"].GetArray()) {
                    param->add_ranges(paramValue.GetDouble());
                }
            }
        }

        if (jsonRecord.Has("downloadable")) {
            record.set_downloadable(jsonRecord["downloadable"].GetBoolean());
        }

        if (jsonRecord.Has("promo_type")) {
            record.set_promo_type(jsonRecord["promo_type"].GetUInteger());
        }

        if (jsonRecord.Has("delivery_flag")) {
            record.set_delivery_flag(jsonRecord["delivery_flag"].GetBoolean());
        }

        if (jsonRecord.Has("cpa")) {
            record.set_cpa(jsonRecord["cpa"].GetUInteger());
        }

        if (jsonRecord.Has("ff_light")) {
            record.set_ff_light(jsonRecord["ff_light"].GetBoolean());
        }

        if (jsonRecord.Has("is_blue_offer")) {
            record.set_is_blue_offer(jsonRecord["is_blue_offer"].GetBoolean());
        }

        if (jsonRecord.Has("is_buyboxes")) {
            record.set_is_buyboxes(jsonRecord["is_buyboxes"].GetBoolean());
        }

        if (jsonRecord.Has("has_gone")) {
            record.set_has_gone(jsonRecord["has_gone"].GetBoolean());
        }

        if (jsonRecord.Has("binary_promos_md5_base64")) {
            for (const auto& md5: jsonRecord["binary_promos_md5_base64"].GetArray()) {
                record.add_binary_promos_md5_base64(md5.GetString());
            }
        }

        if (jsonRecord.Has("market_sku")) {
            record.set_market_sku(jsonRecord["market_sku"].GetUInteger());
        }


        if (jsonRecord.Has("recs")) {
            record.set_recs(jsonRecord["recs"].GetString());
        }

        if (jsonRecord.Has("is_recommended_by_vendor")) {
            record.set_is_recommended_by_vendor(jsonRecord["is_recommended_by_vendor"].GetBoolean());
        }

        if (jsonRecord.Has("disabled_by_price_limit")) {
            record.set_disabled_by_price_limit(jsonRecord["disabled_by_price_limit"].GetBoolean());
        }

        if (jsonRecord.Has("disabled_flags")) {
            record.set_disabled_flags(jsonRecord["disabled_flags"].GetInteger());
        }

        if (jsonRecord.Has("delivery_bucket_ids")) {
            for (const auto& jsonParam: jsonRecord["delivery_bucket_ids"].GetArray()) {
                record.add_delivery_bucket_ids(jsonParam.GetInteger());
            }
        }

        if (jsonRecord.Has("is_dsbs")) {
            record.set_is_dsbs(jsonRecord["is_dsbs"].GetBoolean());
        }

        if (jsonRecord.Has("type")) {
            record.set_type(jsonRecord["type"].GetInteger());
        }

        if (jsonRecord.Has("disabled_by_dynamic")) {
            record.set_disabled_by_dynamic(jsonRecord["disabled_by_dynamic"].GetBoolean());
        }

        if (jsonRecord.Has("contex_info")) {
            const auto& jsonContexInfo = jsonRecord["contex_info"];
            auto* contexInfo = record.mutable_contex_info();
            contexInfo->set_experiment_id(jsonContexInfo["experiment_id"].GetString());
            if (jsonContexInfo.Has("original_msku_id")) {
                contexInfo->set_original_msku_id(jsonContexInfo["original_msku_id"].GetUInteger());
            }
            if (jsonContexInfo.Has("experimental_msku_id")) {
                contexInfo->set_experimental_msku_id(jsonContexInfo["experimental_msku_id"].GetUInteger());
            }
        }

        if (ytWriter) {
            ytWriter->AddRow(record);
        } else {
            writer.Write(record);
        }
    }

    return 0;
}
