#include <crypta/lib/native/yaml/yaml2json.h>

#include <library/cpp/json/json_writer.h>
#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(Yaml2Json) {
    using namespace NCrypta;

    Y_UNIT_TEST(Positive) {
        const TString& yamlStr = "name: Solar\n"
                                 "star_name: Sun\n"
                                 "planets:\n"
                                 "  - name: Mercury\n"
                                 "    radius: 2439.7\n"
                                 "  - name: Venus\n"
                                 "    radius: 6051.8\n"
                                 "  - name: Earth\n"
                                 "    radius: 6371\n"
                                 "    total_population: 7530000000\n"
                                 "  - name: Mars\n"
                                 "    radius: 3389.5\n"
                                 "    total_population: 1\n"
                                 "  - name: Jupiter\n"
                                 "  - name: Saturn\n"
                                 "  - name: Uranus\n"
                                 "  - name: Neptune\n"
                                 "local_names:\n"
                                 "  de: Sonnensystem\n"
                                 "  fr: Systeme solaire\n"
                                 "  es: Sistema solar";

        const TString& jsonStr = "{\n"
                                 "  \"local_names\":\n"
                                 "    {\n"
                                 "      \"de\":\"Sonnensystem\",\n"
                                 "      \"es\":\"Sistema solar\",\n"
                                 "      \"fr\":\"Systeme solaire\"\n"
                                 "    },\n"
                                 "  \"name\":\"Solar\",\n"
                                 "  \"planets\":\n"
                                 "    [\n"
                                 "      {\n"
                                 "        \"name\":\"Mercury\",\n"
                                 "        \"radius\":2439.7\n"
                                 "      },\n"
                                 "      {\n"
                                 "        \"name\":\"Venus\",\n"
                                 "        \"radius\":6051.8\n"
                                 "      },\n"
                                 "      {\n"
                                 "        \"name\":\"Earth\",\n"
                                 "        \"radius\":6371,\n"
                                 "        \"total_population\":7530000000\n"
                                 "      },\n"
                                 "      {\n"
                                 "        \"name\":\"Mars\",\n"
                                 "        \"radius\":3389.5,\n"
                                 "        \"total_population\":1\n"
                                 "      },\n"
                                 "      {\n"
                                 "        \"name\":\"Jupiter\"\n"
                                 "      },\n"
                                 "      {\n"
                                 "        \"name\":\"Saturn\"\n"
                                 "      },\n"
                                 "      {\n"
                                 "        \"name\":\"Uranus\"\n"
                                 "      },\n"
                                 "      {\n"
                                 "        \"name\":\"Neptune\"\n"
                                 "      }\n"
                                 "    ],\n"
                                 "  \"star_name\":\"Sun\"\n"
                                 "}";

        const auto& yamlNode = YAML::Load(yamlStr);
        const auto& jsonNode = Yaml2Json(yamlNode);

        UNIT_ASSERT_STRINGS_EQUAL(jsonStr, NJson::WriteJson(&jsonNode, true, true));
    }

    Y_UNIT_TEST(NonUniqueDictKey) {
        const TString& yamlStr = "name: Solar\n"
                                 "star_name: Sun\n"
                                 "local_names:\n"
                                 "  de: Sonnensystem eins\n"
                                 "  de: Sonnensystem zwei";

        UNIT_ASSERT_EXCEPTION_CONTAINS(Yaml2Json(YAML::Load(yamlStr)), yexception, "Duplicate key entry: de");
    }

    Y_UNIT_TEST(TypesPreserved) {
        const TString& yamlStr = "boolean: true\n"
                                 "float: 3.14\n"
                                 "int1: 42\n"
                                 "int2: 0x2A\n"
                                 "string1: \"42\"\n"
                                 "string2: \"0x2A\"\n"
                                 "string3: \"3.14\"\n"
                                 "string4: \"true\"\n";
        const TString& jsonStr = "{\n"
                                 "  \"boolean\":true,\n"
                                 "  \"float\":3.14,\n"
                                 "  \"int1\":42,\n"
                                 "  \"int2\":42,\n"
                                 "  \"string1\":\"42\",\n"
                                 "  \"string2\":\"0x2A\",\n"
                                 "  \"string3\":\"3.14\",\n"
                                 "  \"string4\":\"true\"\n"
                                 "}";

        const auto& yamlNode = YAML::Load(yamlStr);
        const auto& jsonNode = Yaml2Json(yamlNode);
        UNIT_ASSERT_STRINGS_EQUAL(jsonStr, NJson::WriteJson(&jsonNode, true, true));
    }
}
