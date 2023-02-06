#include "database.h"

#include <kernel/daemon/config/daemon_config.h>

#include <library/cpp/archive/yarchive.h>
#include <library/cpp/resource/resource.h>

#include <rtline/util/algorithm/container.h>

#include <util/memory/blob.h>
#include <util/random/random.h>

namespace {
    static const unsigned char DriveTablesData[] = {
        #include "tables.inc"
    };

    class TTableDescription {
        R_READONLY(TString, Name);
        R_FIELD(bool, ImportData, true);
        R_FIELD(ui64, Limit, 0);
        R_FIELD(TString, OrderBy, "");
    public:
        TTableDescription(const TString& name)
            : Name(name)
        {}
    };

    static bool MatchFields = false;

    static const TArchiveReader DriveTablesDataReader(TBlob::NoCopy(DriveTablesData, sizeof(DriveTablesData)));

    static TVector<TTableDescription> DriveTables = {
        TTableDescription("account_tags"),
        TTableDescription("account_tags_history"),
        TTableDescription("car_model"),
        TTableDescription("car"),
        TTableDescription("autocode_fine"),
        TTableDescription("autocode_fine_photo"),
        TTableDescription("named_filters"),
        TTableDescription("named_filters_history"),
        TTableDescription("role_snapshot_propositions"),
        TTableDescription("promo_code_types"),
        TTableDescription("promo_codes_meta"),
        TTableDescription("promo_codes_usage"),
        TTableDescription("promo_code_types_history"),
        TTableDescription("promo_codes_meta_history").SetImportData(false),
        TTableDescription("promo_codes_usage_history").SetImportData(false),
        TTableDescription("maintenance_info"),
        TTableDescription("maintenance_info_history"),
        TTableDescription("chats"),
        TTableDescription("chat_views"),
        TTableDescription("chat_views_history"),
        TTableDescription("chats_history"),
        TTableDescription("chat_robot_media"),
        TTableDescription("chat_robot_media_history"),
        TTableDescription("messages_history"),
        TTableDescription("chat_last_event"),

        TTableDescription("support_request_categorization_history"),
        TTableDescription("support_categorization_tree_nodes_history"),
        TTableDescription("support_categorization_tree_nodes"),
        TTableDescription("support_categorization_tree_edges_history"),
        TTableDescription("support_categorization_tree_edges"),

        TTableDescription("chat_robot_state"),
        TTableDescription("chat_robot_state_history"),

        TTableDescription("chat_stickers"),
        TTableDescription("chat_stickers_history"),

        TTableDescription("car_document_assignment"),
        TTableDescription("car_document"),
        TTableDescription("user"),
        TTableDescription("user_devices"),
        TTableDescription("user_devices_history"),
        TTableDescription("distributing_block_event_stats"),
        TTableDescription("distributing_block_event_stats_history"),
        TTableDescription("drive_tag_actions"),
        TTableDescription("drive_landings"),
        TTableDescription("drive_landings_history"),
        TTableDescription("drive_landings_propositions"),
        TTableDescription("drive_roles"),
        TTableDescription("drive_roles_history"),

        TTableDescription("tags_description"),
        TTableDescription("tags_description_standart_history"),
        TTableDescription("tags_description_propositions").SetImportData(false),

        TTableDescription("rt_background_settings").SetImportData(false),
        TTableDescription("rt_background_settings_history").SetImportData(false),
        TTableDescription("rt_background_settings_propositions").SetImportData(false),
        TTableDescription("notifiers").SetImportData(false),
        TTableDescription("notifiers_history"),
        TTableDescription("notifiers_propositions").SetImportData(false),
        TTableDescription("rt_background_state").SetImportData(false),

        TTableDescription("drive_state_filters"),
        TTableDescription("drive_state_filters_history"),

        TTableDescription("localization"),
        TTableDescription("localization_history"),
        TTableDescription("localization_propositions").SetImportData(false),

        TTableDescription("drive_areas"),
        TTableDescription("drive_areas_history"),
        TTableDescription("area_tags"),
        TTableDescription("area_tags_history"),

        TTableDescription("datasync_queue"),

        TTableDescription("drive_images"),
        TTableDescription("drive_images_history"),

        TTableDescription("billing_account_description").SetImportData(true),
        TTableDescription("billing_account_description_history").SetImportData(false),
        TTableDescription("billing_account").SetImportData(false),
        TTableDescription("billing_account_history").SetImportData(false),
        TTableDescription("billing_user_accounts").SetImportData(false),
        TTableDescription("billing_user_accounts_history").SetImportData(false),
        TTableDescription("billing_accounts_parents").SetImportData(false),
        TTableDescription("billing_accounts_parents_history").SetImportData(false),
        TTableDescription("billing_tasks").SetImportData(false),
        TTableDescription("billing_tasks_history").SetImportData(false),
        TTableDescription("drive_refunds").SetImportData(false),

        TTableDescription("service_documents"),
        TTableDescription("service_documents_history"),
        TTableDescription("document_queue"),
        TTableDescription("document_queue_history"),

        TTableDescription("call_priority_user"),
        TTableDescription("car_tags"),
        TTableDescription("car_tag_propositions"),
        TTableDescription("car_tags_history"),
        TTableDescription("clearing_tasks").SetImportData(false),
        TTableDescription("compiled_rides"),
        TTableDescription("compiled_bills"),
        TTableDescription("compiled_refunds"),
        TTableDescription("promocode_account_links").SetImportData(false),
        TTableDescription("drive_actions_standart_history"),
        TTableDescription("drive_action_propositions"),
        TTableDescription("drive_area_propositions"),
        TTableDescription("drive_car_info_history"),
        TTableDescription("drive_car_document_history"),
        TTableDescription("drive_car_document_assignment_history"),
        TTableDescription("drive_user_data_history"),
        TTableDescription("drive_payments").SetImportData(false),
        TTableDescription("drive_offers"),
        TTableDescription("drive_role_actions"),
        TTableDescription("drive_role_roles"),
        TTableDescription("drive_role_actions_history"),
        TTableDescription("drive_role_roles_history"),
        TTableDescription("drive_settings"),
        TTableDescription("drive_settings_history"),
        TTableDescription("drive_settings_propositions"),
        TTableDescription("external_access_tokens"),
        TTableDescription("external_access_tokens_history"),
        TTableDescription("head_app_sessions"),
        TTableDescription("head_app_sessions_history"),
        TTableDescription("insurance_tasks"),
        TTableDescription("insurance_tasks_history"),
        TTableDescription("radar_geohash"),
        TTableDescription("refund_issues"),
        TTableDescription("refund_issues_history"),
        TTableDescription("short_sessions"),
        TTableDescription("short_sessions_history").SetImportData(false),
        TTableDescription("public_keys").SetImportData(false),
        TTableDescription("public_keys_history").SetImportData(false),
        TTableDescription("takeout_requests"),
        TTableDescription("trace_tags"),
        TTableDescription("trace_tags_history"),
        TTableDescription("trust_products"),
        TTableDescription("trust_products_history"),
        TTableDescription("user_document_photo"),
        TTableDescription("user_document_background_video"),
        TTableDescription("user_documents_checks"),
        TTableDescription("user_documents_checks_history"),
        TTableDescription("user_landings"),
        TTableDescription("user_logins"),
        TTableDescription("user_roles"),
        TTableDescription("user_roles_history"),
        TTableDescription("user_tags"),
        TTableDescription("user_tags_history"),
        TTableDescription("yang_assignments"),
        TTableDescription("zone"),
        TTableDescription("zone_history"),
    };

    TString GetDriveTableQuery(const TString& name) {
        return DriveTablesDataReader.ObjectByKey("/" + name)->ReadAll();
    }
}

NSQL::IDatabase::TPtr NDrive::CreateDatabase(const TString& configFile) {
    TAnyYandexConfig config;
    Y_ENSURE(config.Parse(configFile), "cannot read YandexConfig from " << configFile);
    auto root = config.GetRootSection();
    Y_ENSURE(root, "root section is missing");
    auto children = root->GetAllChildren();
    auto db = children.find("Database");
    Y_ENSURE(db != children.end(), "cannot find Database section");

    NStorage::TDatabaseConfig databaseConfig;
    databaseConfig.Init(db->second);
    return databaseConfig.ConstructDatabase();
}

void NDrive::ApplyMigrations(const NSQL::IDatabase& database, bool sqlite) {
    NResource::TResources resources;
    NResource::FindMatch("V", &resources);
    std::sort(resources.begin(), resources.end(), [](auto&& left, auto&& right) {
        return left.Key < right.Key;
    });
    for (size_t i = 0; i < resources.size(); ++i) {
        auto key = resources[i].Key;
        {
            auto index = FromString<size_t>(
                TStringBuf{key}.Before('_').After('V')
            );
            Y_ENSURE(index == i + 1, key << ": " << index << " == " << i << " + 1");
        }
        auto queries = resources[i].Data;
        Y_ENSURE(queries);
        if (sqlite) {
            SubstGlobal(queries, "ALTER TABLE IF EXISTS", "ALTER TABLE");
            SubstGlobal(queries, "COLUMN IF NOT EXISTS", "COLUMN");
        }
        auto tx = database.CreateTransaction();
        Y_ENSURE(tx);
        for (auto&& q : StringSplitter(queries).Split(';').SkipEmpty()) {
            auto query = TString{q};
            if (sqlite && query.Contains("nosqlite")) {
                INFO_LOG << key << ": skip query " << query << Endl;
                continue;
            }
            auto result = tx->Exec(query);
            Y_ENSURE(result);
            Y_ENSURE(result->IsSucceed(), key << ": " << query << " – " << tx->GetErrors().GetStringReport());
        }
        INFO_LOG << "executed " << key << Endl;
        Y_ENSURE(tx->Commit());
    }
}

void NDrive::CreateStructure(const NSQL::IDatabase& database) {
    for (auto&& table : DriveTables) {
        CreateTable(database, table.GetName());
    }
}

void NDrive::CreateTable(const NSQL::IDatabase& database, const TString& table) {
    bool committed = false;
    for (size_t i = 0; i < 10; ++i) {
        auto tx = database.CreateTransaction();
        CreateTable(tx, table);
        committed = tx->Commit();
        if (committed) {
            break;
        } else {
            Sleep(TDuration::Seconds(RandomNumber<ui32>(10)));
        }
    }
    Y_ENSURE(committed);
}

void NDrive::CreateTable(const NSQL::ITransaction::TPtr tx, const TString& table) {
    {
        INFO_LOG << "creating table " << table << Endl;
        Y_ENSURE(tx);
        auto query = GetDriveTableQuery(table);
        Y_ENSURE(query, table);
        auto result = tx->Exec(query);
        Y_ENSURE(result);
        Y_ENSURE(result->IsSucceed(), tx->GetErrors().GetStringReport());
        INFO_LOG << "created table " << table << Endl;
    }
}

void NDrive::DropTable(const NSQL::IDatabase& database, const TString& table) {
    auto tx = database.CreateTransaction();
    auto queryResult = tx->Exec("DROP TABLE IF EXISTS " + table);
    Y_ENSURE(queryResult);
    Y_ENSURE(queryResult->IsSucceed(), tx->GetErrors().GetStringReport());
    Y_ENSURE(tx->Commit(), tx->GetErrors().GetStringReport());
}

void NDrive::TransferData(const NSQL::IDatabase& from, const NSQL::IDatabase& to, TInstant historySince, bool matchFields) {
    MatchFields = matchFields;
    if (MatchFields) {
        INFO_LOG << "Match fields before transfer is enabled" << Endl;
    }
    for (auto&& table : DriveTables) {
        if (table.GetImportData()) {
            TransferData(from, to, table.GetName(), historySince, table.GetOrderBy(), table.GetLimit());
        }
    }
}

void NDrive::TransferData(const NSQL::IDatabase& from, const NSQL::IDatabase& to, const TString& table, TInstant historySince, const TString& orderBy, ui64 limit) {
    TSet<TString> fields;
    if (MatchFields) {
        auto schemeFields = to.GetFieldsNames(table);
        if (schemeFields.empty()) {
            INFO_LOG << "cannot get fields names from destination for " << table << Endl;
        }
        auto availableFields = from.GetFieldsNames(table);
        TSet<TString> availableFieldsNames;
        for (auto&& info : availableFields) {
            availableFieldsNames.insert(info.GetId());
        }
        if (availableFields.empty()) {
            INFO_LOG << "cannot get fields names from source for " << table << Endl;
        }
        for (auto&& schemeFieldInfo : schemeFields) {
            auto&& id = schemeFieldInfo.GetId();
            if (availableFieldsNames.contains(id)) {
                fields.emplace(id);
            }
        }
    }
    if (fields.empty()) {
        if (MatchFields) {
            INFO_LOG << "cannot match fields, using fallback \'SELECT *\'" << Endl;
        }
        fields.emplace("*");
    }

    INFO_LOG << "transferring table " << table << Endl;
    auto input = from.CreateTransaction(true);
    Y_ENSURE(input);
    auto reader = from.GetTable(table.Quote());
    Y_ENSURE(reader);

    auto output = to.CreateTransaction(false);
    Y_ENSURE(output);
    auto writer = to.GetTable(table.Quote());
    Y_ENSURE(writer);

    TString condition;
    TRecordsSet records;
    if (table.EndsWith("_history")) {
        condition = TString("history_timestamp >= ") + ToString(historySince.Seconds());
    }

    auto read = reader->GetRows(condition, records, input, MakeVector(fields), orderBy, limit);
    Y_ENSURE(read);
    Y_ENSURE(read->IsSucceed());

    for (auto&& record : records) {
        auto written = writer->AddRow(record, output);
        Y_ENSURE(written);
        Y_ENSURE(written->IsSucceed(), "failed on " << record.SerializeToJson().GetStringRobust());
    }

    Y_ENSURE(output->Commit());
    Y_ENSURE(input->Rollback());
    INFO_LOG << "transferred table " << table << Endl;
}
