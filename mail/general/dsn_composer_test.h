#pragma once

#include <mail/library/dsn/composer.hpp>

#include <mail/yplatform/include/yplatform/application/config/loader.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

const std::string ENV_ID = "env_id";
const std::string ORIG_ENV_ID = "orig_env_id";
const std::string SENDER = "sender@yandex.ru";

template <dsn::composer::type_t ComposerType>
class TDsnComposer : public NTesting::TTest {
protected:
    void SetUp() override {
        std::string baseCfg =
            "dsn:\n"
            "    config_file: " + ArcadiaSourceRoot() + "/mail/library/dsn/conf/dsn.conf\n"
            "    origin: mailer-daemon@yandex.ru";

        yplatform::ptree ptree;
        utils::config::loader::from_str(baseCfg, ptree);
        Options.init(ptree.get_child("dsn"));
    }

    auto Compose(std::vector<dsn::rcpt> rcpts) const {
        dsn::composer composer(Options);
        return composer.compose(ComposerType, ENV_ID, ORIG_ENV_ID, SENDER, rcpts);
    }

protected:
    dsn::Options Options;
};
