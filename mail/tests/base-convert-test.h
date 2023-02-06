#ifndef MACS_PG_TESTS_BASE_CONVERT_TEST_H
#define MACS_PG_TESTS_BASE_CONVERT_TEST_H

#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <yamail/data/serialization/json_writer.h>

namespace tests {

template <class ReflectionT>
class BaseConvertTest : public testing::Test {
protected:
    using Reflection = ReflectionT;

    void modifyData(const std::function<void (Reflection &data)> modify) {
        modify(data_);
    }

    const Reflection& data() const {
        return data_;
    }

private:
    Reflection data_;
};

} // namespace tests

#endif // MACS_PG_TESTS_BASE_CONVERT_TEST_H
