#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <tvm_guard/tvm_guard.h>
#include "test_tvm_module.h"


using namespace testing;


namespace tvm_guard {

void testInit(const std::string& config) {
    tvm_guard::init(yamlToPtree(config), std::make_shared<Tvm2Module>());
}

TEST(ParseActionTest, shouldParseStringSuccessfully) {
    EXPECT_EQ(fromString("accept", "name_action"), Action::accept);
    EXPECT_EQ(fromString("reject", "name_reject"), Action::reject);
}

TEST(ParseActionTest, shouldThrowAnExceptionOnStrangeString) {
    EXPECT_THROW(fromString("strange_string", "name_strange_string"), std::runtime_error);
}

TEST(ParseConfigTest, shoundNotThrowAnExceptionOnMissingBbEnv) {
    const std::string conf = R"(
        default: reject
        strong_uid_check: true
        clients:
        -   name: service1
            id: 1000
        rules: [ ]
    )";
    EXPECT_NO_THROW(testInit(conf));
}

TEST(ParseConfigTest, shoundThrowAnExceptionOnWrongBbEnv) {
    const std::string conf = R"(
        bb_env: wrong_blackbox
        default: reject
        strong_uid_check: true
        clients:
        -   name: service1
            id: 1000
        rules: [ ]
    )";
    EXPECT_THROW(testInit(conf), parse_error::BbEnv);
}

TEST(ParseConfigTest, shoundThrowAnExceptionOnMissingEnvAndFilledCheckByUserSection) {
    const std::string conf = R"(
        default: reject
        strong_uid_check: true
        clients:
        -   name: service1
            id: 1000
        rules:
        -   name: rule
            paths: ['/path']
            default_action: reject
            accept_by_service: []
            accept_by_user: ['service1']
    )";
    EXPECT_THROW(testInit(conf), parse_error::EmptyBbEnvButCheckByUserIsEnabled);
}

TEST(ParseConfigTest, shoundThrowAnExceptionOnWrongAction) {
    {
        const std::string conf = R"(
            bb_env: blackbox
            default: reject_wrong
            strong_uid_check: true
            clients:
            -   name: service1
                id: 1000
            rules: [ ]
        )";
        EXPECT_THROW(testInit(conf), parse_error::ActionParse);
    }

    {
        const std::string conf = R"(
            bb_env: blackbox
            clients:
            -   name: service1
                id: 1000
            rules: [ ]
        )";
        EXPECT_THROW(testInit(conf), parse_error::ActionParse);
    }
}

TEST(ParseConfigTest, shoundThrowAnExceptionOnWrongStrongUidCheck) {
    {
        const std::string conf = R"(
            bb_env: blackbox
            default: reject
            strong_uid_check: fuu
            clients:
            -   name: service1
                id: 1000
            rules: [ ]
        )";
        EXPECT_THROW(testInit(conf), parse_error::StrongUidCheck);
    }

    {
        const std::string conf = R"(
            bb_env: blackbox
            default: reject
            clients:
            -   name: service1
                id: 1000
            rules: [ ]
        )";
        EXPECT_THROW(testInit(conf), parse_error::StrongUidCheck);
    }
}

TEST(ParseConfigTest, shoundThrowAnExceptionOnWrongRootClientId) {
    const std::string conf = R"(
        bb_env: blackbox
        default: reject
        root_client_id: asdf
        strong_uid_check: true
        clients:
        -   name: service1
            id: 1000
        rules: [ ]
    )";
    EXPECT_THROW(testInit(conf), parse_error::RootClientId);
}

TEST(ParseConfigTest, shoundNotThrowAnExceptionOnMissingRootClientId) {
    const std::string conf = R"(
        bb_env: blackbox
        default: reject
        strong_uid_check: true
        clients:
        -   name: service1
            id: 1000
        rules: [ ]
    )";
    EXPECT_NO_THROW(testInit(conf));
}

TEST(ParseConfigTest, shoundThrowAnExceptionOnMissingClients) {
    const std::string conf = R"(
        bb_env: blackbox
        default: reject
        strong_uid_check: true
        root_client_id: 10
        rules: [ ]
    )";
    EXPECT_NO_THROW(testInit(conf));
}

TEST(ParseConfigTest, shoundNotThrowAnExceptionOnMissingRules) {
    const std::string conf = R"(
        bb_env: blackbox
        default: reject
        strong_uid_check: true
        root_client_id: 10
        clients: [ ]
    )";
    EXPECT_NO_THROW(testInit(conf));
}

TEST(ParseConfigTest, shoundThrowAnExceptionServicesNotListedInClientsSection) {
    {
        const std::string conf = R"(
            bb_env: blackbox
            default: reject
            strong_uid_check: true
            root_client_id: 10
            clients:
            -   name: service1
                id: 1000
            rules:
            -   name: rule
                paths: ['/path']
                default_action: reject
                accept_by_service: []
                accept_by_user: ['missing_service']

        )";
        EXPECT_THROW(testInit(conf), parse_error::MissingClient);
    }

    {
        const std::string conf = R"(
            bb_env: blackbox
            default: reject
            strong_uid_check: true
            root_client_id: 10
            clients:
            -   name: service1
                id: 1000
            rules:
            -   name: rule
                paths: ['/path']
                default_action: reject
                accept_by_service: ['missing_service']
                accept_by_user: []
        )";
        EXPECT_THROW(testInit(conf), parse_error::MissingClient);
    }
}

TEST(ParseConfigTest, shoundThrowAnExceptionInCaseOfServiceIsListedInBothSections) {
    const std::string conf = R"(
        bb_env: blackbox
        default: reject
        strong_uid_check: true
        root_client_id: 10
        clients:
        -   name: service1
            id: 1000
        rules:
        -   name: rule
            paths: ['/path']
            default_action: reject
            accept_by_service: ['service1']
            accept_by_user: ['service1']
    )";
    EXPECT_THROW(testInit(conf), parse_error::ClientInServiceAndUserSection);
}

TEST(ParseConfigTest, shoundThrowAnExceptionIfPathIsMentionedTwoOrMoreTimes) {
    {
        const std::string conf = R"(
            bb_env: blackbox
            default: reject
            strong_uid_check: true
            root_client_id: 10
            clients:
            -   name: service1
                id: 1000
            rules:
            -   name: rule
                paths: ['/path', '/path']
                default_action: reject
                accept_by_service: []
                accept_by_user: []

        )";
        EXPECT_THROW(testInit(conf), parse_error::MultiplePathMention);
    }

    {
        const std::string conf = R"(
            bb_env: blackbox
            default: reject
            strong_uid_check: true
            root_client_id: 10
            clients:
            -   name: service1
                id: 1000
            rules:
            -   name: rule1
                paths: ['/path']
                default_action: reject
                accept_by_service: []
                accept_by_user: []
            -   name: rule2
                paths: ['/path']
                default_action: reject
                accept_by_service: []
                accept_by_user: []
        )";
        EXPECT_THROW(testInit(conf), parse_error::MultiplePathMention);
    }
}

}
