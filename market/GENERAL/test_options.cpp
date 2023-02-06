#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/common/env.h>
#include <util/folder/dirut.h>
#include <util/generic/guid.h>
#include <market/idx/library/miconfig_to_options/options.h>
#include <market/idx/library/miconfig_to_options/ut/proto/options.pb.h>

using namespace testing;

using TIniConfigData = THashMap<TString, THashMap<TString, TString>>;

const NGetoptPb::TGetoptPbSettings Settings{
    .DumpConfig= false
};

const char* MiConfigPathOption = "--miconfig-path";

void GenerateIniConfig(const TString& configPath, const TIniConfigData data) {
    TStringStream stream;
    for (const auto& [section, keyVal]: data) {
        stream << "[" << section << "]" << Endl;
        for (const auto& [key, val]: keyVal) {
            stream << key << "=" << val << Endl;
        }
        stream << Endl;
    }
    TOFStream configFile(configPath);
    configFile.Write(stream.Str());
}

TString MakePathForConfig(const TString& folderPrefix = "") {
    const auto guid = CreateGuidAsString();
    const auto outputPath = GetOutputPath();
    auto folderPath = TFsPath(outputPath) / guid;
    if (!folderPrefix.empty()) {
        folderPath = TFsPath(outputPath) / folderPrefix / guid;
    }
    NFs::MakeDirectoryRecursive(folderPath);
    return folderPath / "config.ini";
}

TEST(IniConfig, ReadIntOptions) {
    const auto configPath = MakePathForConfig();
    const TIniConfigData data = {
        {"general",
         {
             {"min_models_in_index", "2"},
             {"required_field", "some_value"}
         }}
    };
    GenerateIniConfig(configPath, data);

    const char* argv[] = { "bin_path_mock", MiConfigPathOption, configPath.data() };
    int argc = std::size(argv);

    const TOptions& options = GetoptOrAbortWithMiconfig(argc, argv, Settings);

    EXPECT_THAT(options, Property(&TOptions::min_models_in_index, Eq(2)))
        << "Miconfig has min_models_in_index=2 in general section";
}

TEST(IniConfig, ReadStringOptions) {
    const auto configPath = MakePathForConfig();
    const TIniConfigData data = {
        {"general",
         {
             {"ext", "//tmp"},
             {"required_field", "some_value"}
         }}
    };
    GenerateIniConfig(configPath, data);

    const char* argv[] = { "bin_path_mock", MiConfigPathOption, configPath.data() };
    int argc = std::size(argv);

    const TOptions& options = GetoptOrAbortWithMiconfig(argc, argv, Settings);

    EXPECT_THAT(options, Property(&TOptions::output, StrEq("//tmp")))
        << "Miconfig has ext=//tmp in general section";
}

TEST(IniConfig, ReadBoolOptions) {
    const auto configPath = MakePathForConfig();
    const TIniConfigData data = {
        {"general",
         {
             {"bool_field", "on"},
             {"required_field", "some_value"}
         }}
    };
    GenerateIniConfig(configPath, data);

    const char* argv[] = { "bin_path_mock", MiConfigPathOption, configPath.data() };
    int argc = std::size(argv);

    const TOptions& options = GetoptOrAbortWithMiconfig(argc, argv, Settings);

    EXPECT_THAT(options, Property(&TOptions::bool_field, Eq(true)));
}

TEST(IniConfig, ReadBoolOptionsAsCmdFlag) {
    const auto configPath = MakePathForConfig();
    const TIniConfigData data = {
        {"general",
         {
             {"required_field", "some_value"}
         }}
    };
    GenerateIniConfig(configPath, data);

    const char* argv[] = { "bin_path_mock", MiConfigPathOption, configPath.data(), "--bool-field" };
    int argc = std::size(argv);

    const TOptions& options = GetoptOrAbortWithMiconfig(argc, argv, Settings);

    EXPECT_THAT(options, Property(&TOptions::bool_field, Eq(true)));
}

TEST(IniConfig, ReadRepeatedStringOptions) {
    const auto configPath = MakePathForConfig();
    const TIniConfigData data = {
        {"misc",
         {
             {"filetered_build_mass_index_tasks", "one,two"},
         }},
        {"general",
         {
             {"required_field", "some_value"}
         }}
    };
    GenerateIniConfig(configPath, data);

    const char* argv[] = { "bin_path_mock", MiConfigPathOption, configPath.data() };
    int argc = std::size(argv);

    const TOptions& options = GetoptOrAbortWithMiconfig(argc, argv, Settings);

    const TString descMesg{"Miconfig has filetered_build_mass_index_tasks=one,two in misc section"};

    EXPECT_THAT(options, Property(&TOptions::filetered_build_mass_index_tasks, SizeIs(2)))
        << descMesg;

    EXPECT_THAT(options, Property(&TOptions::filetered_build_mass_index_tasks, Contains("one")))
        << descMesg;

    EXPECT_THAT(options, Property(&TOptions::filetered_build_mass_index_tasks, Contains("two")))
        << descMesg;
}

TEST(IniConfig, ReadRepeatedEnumOptions) {
    const auto configPath = MakePathForConfig();
    const TIniConfigData data = {
        {"genlog_dumper",
         {{"optional_types", "WARE_MD5,QVAT"}}
        },
        {"general",
         {
             {"required_field", "some_value"}
         }}
    };
    GenerateIniConfig(configPath, data);

    const char* argv[] = { "bin_path_mock", MiConfigPathOption, configPath.data() };
    int argc = std::size(argv);

    const TOptions& options = GetoptOrAbortWithMiconfig(argc, argv, Settings);

    const TString descMesg{"Miconfig has optional_types=WARE_MD5,QVAT in genlog_dumper section"};

    EXPECT_THAT(options, Property(&TOptions::dumper_optional_types, SizeIs(2)))
        << descMesg;

    EXPECT_THAT(options, Property(&TOptions::dumper_optional_types, Contains(WARE_MD5)))
        << descMesg;

    EXPECT_THAT(options, Property(&TOptions::dumper_optional_types, Contains(QVAT)))
        << descMesg;
}

TEST(IniConfig, ReadSubMessage) {
    const auto configPath = MakePathForConfig();
    const TIniConfigData data = {
        {"general",
         {
             {"target", "production"},
             {"required_field", "some_value"}
         }}
    };
    GenerateIniConfig(configPath, data);

    const char* argv[] = { "bin_path_mock", MiConfigPathOption, configPath.data() };
    int argc = std::size(argv);

    const TOptions& options = GetoptOrAbortWithMiconfig(argc, argv, Settings);

    EXPECT_THAT(options, Property(&TOptions::general, Property(&TGeneral::target, StrEq("production"))));
}


TEST(IniConfig, BadConfigPath) {
    const char* argv[] = { "bin_path_mock", MiConfigPathOption, "bad_config_path.ini" };
    int argc = std::size(argv);

    TOptions options;
    EXPECT_ANY_THROW({
        options = GetoptOrAbortWithMiconfig(argc, argv, Settings);
    });
}

TEST(IniConfig, ReadTwoConfigsMessage) {
    const auto configPathFirst = MakePathForConfig("first");
    const TIniConfigData dataFirst = {
        {"general",
         {
             {"target", "production"},
             {"required_field", "some_value"}
         }}
    };
    GenerateIniConfig(configPathFirst, dataFirst);

    const auto configPathSecond = MakePathForConfig("second");
    const TIniConfigData dataSecond = {
        {"general",
         {
             {"target", "testing"},
         }}
    };
    GenerateIniConfig(configPathSecond, dataSecond);

    const char* argv[] = { "bin_path_mock",
        MiConfigPathOption, configPathFirst.data(),
        MiConfigPathOption, configPathSecond.data()
    };
    int argc = std::size(argv);

    const TOptions& options = GetoptOrAbortWithMiconfig(argc, argv, Settings);

    EXPECT_THAT(options, Property(&TOptions::general, Property(&TGeneral::target, StrEq("testing"))));
}

TEST(IniConfig, ReadSimpleOptions) {
    const char* argv[] = { "bin_path_mock", "--output", "//tmp", "--required-field", "1" };
    int argc = std::size(argv);

    const TOptions& options = GetoptOrAbortWithMiconfig(argc, argv, Settings);

    EXPECT_THAT(options, Property(&TOptions::output, StrEq("//tmp")));
}

TEST(IniConfig, OverrideOptionsByConfig) {
    const auto configPath = MakePathForConfig();
    const TIniConfigData data = {
        {"general",
         {
             {"min_models_in_index", "2"},
             {"required_field", "some_value"}
         }}
    };
    GenerateIniConfig(configPath, data);

    const char* argv[] = { "bin_path_mock", MiConfigPathOption, configPath.data(), "--min-models-in-index", "3" };
    int argc = std::size(argv);

    const TOptions& options = GetoptOrAbortWithMiconfig(argc, argv, Settings);

    EXPECT_THAT(options, Property(&TOptions::min_models_in_index, Eq(2)));
}

TEST(IniConfig, OptionWithoutMapping) {
    const char* argv[] = { "bin_path_mock", "--simple-number", "1",  "--required-field", "1"};
    int argc = std::size(argv);

    const TOptions& options = GetoptOrAbortWithMiconfig(argc, argv, Settings);

    EXPECT_THAT(options, Property(&TOptions::simple_number, Eq(1)));
}

TEST(IniConfig, WithNotExistsOptins) {
    const char* argv[] = { "bin_path_mock", "--bad-option", "--required-field", "1" };
    int argc = std::size(argv);

    TOptions options;
    EXPECT_ANY_THROW({
        options = GetoptOrAbortWithMiconfig(argc, argv, Settings);
    });
}

TEST(IniConfig, WithoutRequiredOption) {
    const char* argv[] = { "bin_path_mock", "--simple-number", "1" };
    int argc = std::size(argv);

    TOptions options;
    EXPECT_ANY_THROW({
        options = GetoptOrAbortWithMiconfig(argc, argv, Settings);
    });
}

TEST(IniConfig, RequiredOptionFromConfigWithoutCmd) {
    const auto configPath = MakePathForConfig();
    const TIniConfigData data = {
        {"general",
         {
             {"required_field", "some_value"}
         }}
    };
    GenerateIniConfig(configPath, data);

    const char* argv[] = { "bin_path_mock", MiConfigPathOption, configPath.data() };
    int argc = std::size(argv);


    const TOptions& options = GetoptOrAbortWithMiconfig(argc, argv, Settings);
    EXPECT_THAT(options, Property(&TOptions::required_field, StrEq("some_value")));
}

