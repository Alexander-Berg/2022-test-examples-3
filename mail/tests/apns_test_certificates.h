#pragma once

#include <yxiva/core/platforms.h>
#include <string>

namespace yxiva::web::webui::test_certificates {

using std::string;

struct current
{
    static const string pem;
    static const string p12;
    static const yxiva::apns::p8_token p8;
};

struct backup
{
    static const string pem;
    static const string p12;
    static const yxiva::apns::p8_token p8;
};

struct fresh
{
    static const string pem;
    static const string p12;
    static const yxiva::apns::p8_token p8;
};

}
