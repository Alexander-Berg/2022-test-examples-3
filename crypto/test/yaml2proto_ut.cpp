#include <crypta/lib/native/yaml/yaml2proto.h>
#include <crypta/lib/native/yaml/test/star_system.pb.h>

#include <library/cpp/testing/unittest/registar.h>

#include <util/system/env.h>

Y_UNIT_TEST_SUITE(Yaml2Proto) {
    using namespace NCrypta;

    const TString ENV_VALUE("env_value");

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
                                 "  es: Sistema solar\n"
                                 "unused: unused";

        SetEnv("ENV_VAR", ENV_VALUE);

        const auto& starSystem = Yaml2Proto<TStarSystem>(yamlStr);

        UNIT_ASSERT_STRINGS_EQUAL("Solar", starSystem.GetName());
        UNIT_ASSERT_STRINGS_EQUAL("Sun", starSystem.GetStarName());
        UNIT_ASSERT_EQUAL(8, starSystem.GetPlanets().size());

        const auto& earth = starSystem.GetPlanets(2);
        UNIT_ASSERT_STRINGS_EQUAL("Earth", earth.GetName());
        UNIT_ASSERT_DOUBLES_EQUAL(6371, earth.GetRadius(), 0.001);
        UNIT_ASSERT_EQUAL(7530000000, earth.GetTotalPopulation());

        const auto& mars = starSystem.GetPlanets(3);
        UNIT_ASSERT_STRINGS_EQUAL("Mars", mars.GetName());
        UNIT_ASSERT_DOUBLES_EQUAL(3389.5, mars.GetRadius(), 0.001);
        UNIT_ASSERT_EQUAL(1, mars.GetTotalPopulation());

        const auto& localNames = starSystem.GetLocalNames();
        UNIT_ASSERT_EQUAL(3, localNames.size());
        UNIT_ASSERT_STRINGS_EQUAL("Sonnensystem", localNames.at("de"));
        UNIT_ASSERT_STRINGS_EQUAL("Systeme solaire", localNames.at("fr"));
        UNIT_ASSERT_STRINGS_EQUAL("Sistema solar", localNames.at("es"));

        UNIT_ASSERT_STRINGS_EQUAL(ENV_VALUE, starSystem.GetEnvVariable());
    }

    Y_UNIT_TEST(MissingRequiredField) {
        const TString& yamlStr = "name: Solar";

        UNIT_ASSERT_EXCEPTION_CONTAINS(Yaml2Proto<TStarSystem>(yamlStr), yexception, "YAML has no field for required field star_name.");
    }

    Y_UNIT_TEST(NonUniqueDictKey) {
        const TString& yamlStr = "name: Solar\n"
                                 "star_name: Sun\n"
                                 "local_names:\n"
                                 "  de: Sonnensystem eins\n"
                                 "  de: Sonnensystem zwei";

        UNIT_ASSERT_EXCEPTION_CONTAINS(Yaml2Proto<TStarSystem>(yamlStr), yexception, "Duplicate key entry: de");
    }

    Y_UNIT_TEST(OptionalNestedMessageIsNotAppearing) {
        const TString& starName = "Proxima Centauri";
        const TString& yamlStr = "star_name: \"Proxima Centauri\"";

        const auto& starSystem = Yaml2Proto<TExoplanetSystem>(yamlStr);
        UNIT_ASSERT_EQUAL(starName, starSystem.GetStarName());
        UNIT_ASSERT_C(!starSystem.HasPlanet(), starSystem.AsJSON());

        TStringStream ss;
        starSystem.Save(&ss);
        TExoplanetSystem deserialized;
        deserialized.Load(&ss);
        UNIT_ASSERT_EQUAL(starName, deserialized.GetStarName());
        UNIT_ASSERT_C(!deserialized.HasPlanet(), deserialized.AsJSON());
    }

    Y_UNIT_TEST(FieldInNestedMessageIsSetFromEnvWithoutCrash) {
        const TString exoplanetName = "Proxima Centauri b";
        SetEnv("EXOPLANET_NAME", exoplanetName);

        const TString& yamlStr = "star_name: \"Proxima Centauri\"\n"
                                 "planet:\n"
                                 "  mass: 0.123\n"
                                 "  period_days: 11.186\n"
                                 "  temperature: 3042";

        const auto& starSystem = Yaml2Proto<TExoplanetSystem>(yamlStr);
        UNIT_ASSERT_EQUAL("Proxima Centauri", starSystem.GetStarName());
        UNIT_ASSERT_EQUAL(exoplanetName, starSystem.GetPlanet().GetName());
    }

    Y_UNIT_TEST(RequiredEnvFieldInNestedMessageIsMissing) {
        SetEnv("EXOPLANET_NAME", "");
        const TString& yamlStr = "star_name: \"Barnard's Star\"\n"
                                 "planet:\n"
                                 "  mass: 3.23\n"
                                 "  period_days: 232.8\n"
                                 "  temperature: 105";

        UNIT_ASSERT_EXCEPTION_CONTAINS(Yaml2Proto<TExoplanetSystem>(yamlStr), yexception, "YAML has no field for required field name.");
    }
}
