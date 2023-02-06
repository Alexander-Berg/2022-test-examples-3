#include <market/idx/feeds/qparser/inc/parser_config.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/folder/path.h>
#include <util/folder/tempdir.h>
#include <util/stream/file.h>

using namespace NMarket;

namespace {

void WriteConfig(const TString& dir, const TString& file, const TString& config) {
    TOFStream(JoinFsPaths(dir, file)).Write(config);
}

void WriteConfig(
    const TTempDir& tempDir,
    const TString& file,
    const TString& config,
    TVector<TFsPath>& avaliableConfigs
) {
    WriteConfig(tempDir.Path(), file, config);
    avaliableConfigs.emplace_back(JoinFsPaths(tempDir.Path(), file));
}

} // namespace

static const TString CONFIG(R"wrap(
{
    "string": "value",
    "signed_int": -10,
    "unisigned_int": 10,
    "param0": {
        "param1": "value"
    }
}
)wrap");

TEST(TestConfigs, SimpleTest) {
    TTempDir tempDir;
    WriteConfig(tempDir.Path(), "common.json", CONFIG);
    TVector<TFsPath> confAvaliableConfigs;
    tempDir.Path().List(confAvaliableConfigs);
    TParserConfig parserConfig(confAvaliableConfigs);
    UNIT_ASSERT_VALUES_EQUAL(parserConfig.Get<double>("unisigned_int"), 10.0);
    UNIT_ASSERT_VALUES_EQUAL(parserConfig.Get<TString>("param0.param1"), "value");
}

TEST(TestConfigs, GetUnknownParam) {
    TTempDir tempDir;
    WriteConfig(tempDir.Path(), "common.json", CONFIG);
    TVector<TFsPath> confAvaliableConfigs;
    tempDir.Path().List(confAvaliableConfigs);
    TParserConfig parserConfig(confAvaliableConfigs);
    UNIT_ASSERT_EXCEPTION(parserConfig.Get<int>("unknown_param"), TParserConfig::TBadOptionName);
    UNIT_ASSERT_EXCEPTION(parserConfig.Get<ui32>("param0.unknown_param"), TParserConfig::TBadOptionName);
}

TEST(TestConfigs, GetParamWithWrongType) {
    TTempDir tempDir;
    WriteConfig(tempDir.Path(), "common.json", CONFIG);
    TVector<TFsPath> confAvaliableConfigs;
    tempDir.Path().List(confAvaliableConfigs);
    TParserConfig parserConfig(confAvaliableConfigs);
    UNIT_ASSERT_EXCEPTION(parserConfig.Get<int>("string"), TParserConfig::TBadOptionType);
    UNIT_ASSERT_EXCEPTION(parserConfig.Get<ui32>("signed_int"), TParserConfig::TBadOptionType);
}

TEST(TestConfigs, CheckConfigOverwrite) {
    const TString overwriteConfig(R"wrap(
    {
        "new_param": 100,
        "signed_int": 0,
        "param0": {
            "param1": "new_value"
        }
    }
    )wrap");
    TTempDir tempDir;
    TVector<TFsPath> confAvaliableConfigs;
    WriteConfig(tempDir, "common.json", CONFIG, confAvaliableConfigs);
    WriteConfig(tempDir, "overwrite.json", overwriteConfig, confAvaliableConfigs);
    TParserConfig parserConfig(confAvaliableConfigs);
    UNIT_ASSERT_VALUES_EQUAL(parserConfig.Get<TString>("string"), "value");
    UNIT_ASSERT_VALUES_EQUAL(parserConfig.Get<int>("signed_int"), 0);
    UNIT_ASSERT_VALUES_EQUAL(parserConfig.Get<TString>("param0.param1"), "new_value");
    UNIT_ASSERT_VALUES_EQUAL(parserConfig.Get<int>("new_param"), 100);
}

TEST(TestConfigs, ParserRealConfigsCommon) {
    const TFsPath etcDir = TFsPath(ArcadiaSourceRoot()) / "market/idx/feeds/qparser/etc";
    TVector<TFsPath> commonConfigs;
    etcDir.List(commonConfigs);
    for (const auto& configPath : commonConfigs) {
        if (!configPath.IsFile() || configPath.GetName().StartsWith("."))
            continue;
        UNIT_ASSERT_NO_EXCEPTION_C(TParserConfig{configPath}, configPath.GetPath());
    }
}

TEST(TestConfigs, ParserRealConfigsConfAvaliable) {
    const TFsPath confAvaliableDir = TFsPath(ArcadiaSourceRoot()) / "market/idx/feeds/qparser/etc/conf-available";
    TVector<TFsPath> confAvaliableConfigs;
    confAvaliableDir.List(confAvaliableConfigs);
    for (const auto& configPath : confAvaliableConfigs) {
        if (!configPath.IsFile() || configPath.GetName().StartsWith("."))
            continue;
        UNIT_ASSERT_NO_EXCEPTION_C(TParserConfig{configPath}, configPath.GetPath());
    }
}
