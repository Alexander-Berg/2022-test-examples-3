#include <mail/message_types/lib/message_types.h>
#include <mail/notsolitesrv/src/config/msearch.h>
#include <yplatform/application/config/yaml_to_ptree.h>
#include <util/generic/yexception.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <memory>

namespace {

using namespace testing;

struct TTestMSearchConfig : Test {
    using TConfig = NNotSoLiteSrv::NConfig::TMSearch;
    using TConfigPtr = std::shared_ptr<TConfig>;

    TConfigPtr MakeConfig(std::string strcfg) {
        boost::property_tree::ptree pt;
        utils::config::yaml_to_ptree::convert_str(strcfg, pt);
        return std::make_shared<TConfig>(pt.get_child("msearch"));
    }
};

TEST_F(TTestMSearchConfig, for_valid_message_type_names_config_must_contains_proper_codes) {
    auto config = MakeConfig(R"(
        msearch:
            message_types:
            -   notification
            -   news
            -   correspond
    )");

    EXPECT_THAT(config->MessageTypes, UnorderedElementsAre(
        NMail::MT_NOTIFICATION,
        NMail::MT_NEWS,
        NMail::MT_CORRESPOND
    ));
}

TEST_F(TTestMSearchConfig, for_invalid_message_type_name_config_creation_throws) {
    EXPECT_THROW(MakeConfig(R"(
        msearch:
            message_types:
            -   notification
            -   some_unknown_name
            -   correspond
        )"),
        yexception
    );
}

} // namespace
