#include "context.h"
#include <yplatform/application/config/loader.h>
#include <library/cpp/resource/resource.h>
#include <util/generic/strbuf.h>

namespace {

constexpr char DEFAULT_CONFIG[]{"config/default.yml"};

} // namespace

NNotSoLiteSrv::TConfigPtr GetConfig(const TConfigParams& params) {
    auto data = NResource::Find(DEFAULT_CONFIG);
    TConfigParams configParams{
        { "skip_attach_with_content_id", "false" },
        { "max_parts",                   "1000"  },
        { "mdbsave_use_tvm",             "1" },
        { "merge_rules",                 "" },
        { "furita_use_tvm",              "1" },
        { "msearch_use_tvm",             "1" },
        { "msettings_use_tvm",           "1" },
        { "meta_save_op_drop_async",     "false" },
        { "trivial_subjects",            "" }
    };
    for (const auto& [k, v]: params) {
        configParams.insert_or_assign(k, v);
    }
    try {
        std::string strcfg{data.data(), data.size()};
        for (const auto& [k, v]: configParams) {
            std::string key = "@" + k + "@";
            for (;;) {
                auto keyStartPos = strcfg.find(key);
                if (keyStartPos == std::string::npos) {
                    break;
                }
                strcfg.replace(keyStartPos, key.length(), v);
            }
        }
        boost::property_tree::ptree pt;
        utils::config::loader::from_str(strcfg, pt);
        return std::make_shared<NNotSoLiteSrv::TConfig>(pt);
    } catch (const std::exception& e) {
        std::cerr << "Failed to parse config: " << e.what() << std::endl;
        throw;
    }
}
