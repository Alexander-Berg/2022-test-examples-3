#include <crypta/lib/native/resource_service/parsers/yaml_config_parser/test/message.pb.h>
#include <crypta/lib/native/resource_service/parsers/yaml_config_parser/yaml_config_parser.h>

#include <library/cpp/resource/resource.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/generic/ymath.h>

using namespace NCrypta;

Y_UNIT_TEST_SUITE(TConfigParser) {
    NProtobufJson::TJson2ProtoConfig GetConfig() {
        auto config = GetDefaultYaml2ProtoConfig();
        config.AllowUnknownFields = false;
        return config;
    }


    Y_UNIT_TEST(Parse) {
        static const TString yaml = "int32: 123\n"
                                    "string: string\n";

        const auto& result = NResourceService::TYamlConfigParser<TMessage>::Parse(yaml, GetConfig());

        UNIT_ASSERT_EQUAL(123, result.GetInt32());
        UNIT_ASSERT_STRINGS_EQUAL("string", result.GetString());
    }
}
