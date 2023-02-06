#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/common/env.h>
#include <util/folder/dirut.h>
#include <market/idx/library/miconfig_to_options/options.h>
#include <market/idx/library/miconfig_to_options/ut/proto/required_in_submessage.pb.h>

using namespace testing;

const NGetoptPb::TGetoptPbSettings Settings{
    .DumpConfig= false
};

// Для проверки поля, которое идёт во вложенном сообщении и не имеет мапинга.
// Для него нужно прокидывать опцию во всех тестах
const char* RequiredSubMessageFiled = "--sub-general-proxy";
const char* value = "arnold";

TEST(IniConfig, ReadRequiredOptionWithoutMapping) {
    //Генерим пустой конфиг. Значения не важны. Важен сам факт наличия конфига.
    const auto configPath = TFsPath(GetOutputPath()) / "config.ini";
    TStringStream stream("");
    TOFStream configFile(configPath);
    configFile.Write(stream.Str());

    const char* argv[] = { "bin_path_mock", "--miconfig-path", configPath.c_str(), RequiredSubMessageFiled, value };
    int argc = std::size(argv);

    const TRISOptions& options = GetoptOrAbortWithMiconfig(argc, argv, Settings);

    EXPECT_THAT(options, Property(&TRISOptions::sub_general, Property(&TSubGeneral::proxy, StrEq(value))));
}
