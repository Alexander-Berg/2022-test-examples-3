#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <tvm_guard/tvm_guard.h>

#include <mail/tvm_guard/tvm_api/helpers.h>
#include <yplatform/application/config/yaml_to_ptree.h>

using namespace testing;


namespace tvm_guard {

boost::property_tree::ptree yamlToPtree(const std::string& yaml) {
    boost::property_tree::ptree node;
    utils::config::yaml_to_ptree::convert_str(yaml, node);
    return node;
}

TString mockFileReading(const std::string&) {
    return TString("secret");
}

TEST(ParseTvmApiTicketParserWrapper, shouldThrowAnExceptionOnMissingExpectedParam) {
    std::string conf = R"(

    )";
    EXPECT_THROW(parseSettings(yamlToPtree(conf), mockFileReading), Exception);

    conf = R"(
        service_id: 123
    )";
    EXPECT_THROW(parseSettings(yamlToPtree(conf), mockFileReading), Exception);

    conf = R"(
        service_id: 123
        host: mytvm
    )";
    EXPECT_THROW(parseSettings(yamlToPtree(conf), mockFileReading), Exception);

    conf = R"(
        service_id: 123
        port: 123
    )";
    EXPECT_THROW(parseSettings(yamlToPtree(conf), mockFileReading), Exception);
}

TEST(ParseTvmApiTicketParserWrapper, shouldNotParseTargetServicesWithoutNameAndIdOrSecretFile) {
    std::string conf = R"(
        service_id: 123
        host: mytvm
        port: 123
        target_services:
        -   name: blackbox
    )";
    EXPECT_THROW(parseSettings(yamlToPtree(conf), mockFileReading), Exception);

    conf = R"(
        service_id: 123
        host: mytvm
        port: 123
        target_services:
        -   id: 1
    )";
    EXPECT_THROW(parseSettings(yamlToPtree(conf), mockFileReading), Exception);

    conf = R"(
        service_id: 123
        host: mytvm
        port: 123
        target_services:
        -   id: 1
            name: blackbox
    )";
    EXPECT_THROW(parseSettings(yamlToPtree(conf), mockFileReading), Exception);

    conf = R"(
        service_id: 123
        host: mytvm
        port: 123
        secret_file: path_to_secret_file
        target_services:
        -   id: 1
            name: blackbox
    )";
    EXPECT_NO_THROW(parseSettings(yamlToPtree(conf), mockFileReading));
}

TEST(ParseTvmApiTicketParserWrapper, shouldParseConfigForSelfChecking) {
    const std::string conf = R"(
        service_id: 123
        host: mytvm
        port: 1234
    )";

    NTvmAuth::NTvmApi::TClientSettings clientSettings = parseSettings(yamlToPtree(conf), mockFileReading);

    EXPECT_EQ(clientSettings.SelfTvmId, 123u);
    EXPECT_EQ(clientSettings.TvmHost, "mytvm");
    EXPECT_EQ(clientSettings.TvmPort, 1234u);
}

TEST(ParseTvmApiTicketParserWrapper, shouldParseConfigForFetchingTickets) {
    const std::string conf = R"(
        service_id: 123
        host: mytvm
        port: 1234
        secret_file: path_to_secret_file
        disk_cache_dir: './'
        target_services:
        -   id: 1
            name: blackbox
    )";

    NTvmAuth::NTvmApi::TClientSettings clientSettings = parseSettings(yamlToPtree(conf), mockFileReading);

    EXPECT_EQ(clientSettings.SelfTvmId, 123u);
    EXPECT_EQ(clientSettings.TvmHost, "mytvm");
    EXPECT_EQ(clientSettings.TvmPort, 1234u);
    EXPECT_EQ(clientSettings.DiskCacheDir, "./");
    EXPECT_THAT(clientSettings.FetchServiceTicketsForDsts, UnorderedElementsAre(NTvmAuth::NTvmApi::TClientSettings::TDst(1)));
}

TEST(ParseTvmApiTicketParserWrapper, shouldParseConfigForCheckingUserTickets) {
    const std::string conf = R"(
        bb_env: blackbox-stress
        service_id: 123
        host: mytvm
        port: 1234
    )";

    NTvmAuth::NTvmApi::TClientSettings clientSettings = parseSettings(yamlToPtree(conf), mockFileReading);

    EXPECT_EQ(clientSettings.SelfTvmId, 123u);
    EXPECT_EQ(clientSettings.TvmHost, "mytvm");
    EXPECT_EQ(clientSettings.TvmPort, 1234u);
    EXPECT_EQ(clientSettings.CheckUserTicketsWithBbEnv, NTvmAuth::EBlackboxEnv::Stress);
}

TEST(ParseTvmApiTicketParserWrapper, shouldParseConfigForCheckingUserTicketsAndFetchingServiceTicketsToAnotherServices) {
    const std::string conf = R"(
        bb_env: blackbox-stress
        service_id: 123
        host: mytvm
        port: 1234
        secret_file: path_to_secret_file
        target_services:
        -   id: 1
            name: blackbox
        -   id: 2
            name: whitebox
    )";

    NTvmAuth::NTvmApi::TClientSettings clientSettings = parseSettings(yamlToPtree(conf), mockFileReading);

    EXPECT_EQ(clientSettings.SelfTvmId, 123u);
    EXPECT_EQ(clientSettings.TvmHost, "mytvm");
    EXPECT_EQ(clientSettings.TvmPort, 1234u);
    EXPECT_EQ(clientSettings.CheckUserTicketsWithBbEnv, NTvmAuth::EBlackboxEnv::Stress);
    EXPECT_THAT(clientSettings.FetchServiceTicketsForDsts, UnorderedElementsAre(
                    NTvmAuth::NTvmApi::TClientSettings::TDst(1),
                    NTvmAuth::NTvmApi::TClientSettings::TDst(2)
                    )
    );
}

}
