#pragma once

#include <internal/common/context.h>
#include <internal/blackbox/interface.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

namespace settings::test {

struct MockBlackBox: blackbox::BlackBoxInterface {
     MOCK_METHOD(expected<std::string>, infoRequest, (ContextPtr, const std::string&), (const, override));
};

}
