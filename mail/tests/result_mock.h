#pragma once

#include <ozo/result.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

namespace ozo::tests {

using namespace testing;

struct pg_result_mock {
    MOCK_METHOD(ozo::oid_t, field_type, (int column), (const));
    MOCK_METHOD(ozo::impl::result_format, field_format, (int column), (const));
    MOCK_METHOD(const char*, get_value, (int row, int column), (const));
    MOCK_METHOD(std::size_t, get_length, (int row, int column), (const));
    MOCK_METHOD(bool, get_isnull, (int row, int column), (const));
    MOCK_METHOD(int, field_number, (const char* name), (const));
    MOCK_METHOD(int, nfields, (), (const));
    MOCK_METHOD(int, ntuples, (), (const));

    friend ozo::oid_t pq_field_type(const pg_result_mock& m, int column) {
        return m.field_type(column);
    }

    friend ozo::impl::result_format pq_field_format(const pg_result_mock& m, int column) {
        return m.field_format(column);
    }

    friend const char* pq_get_value(const pg_result_mock& m, int row, int column) {
        return m.get_value(row, column);
    }

    friend std::size_t pq_get_length(const pg_result_mock& m, int row, int column) {
        return m.get_length(row, column);
    }

    friend bool pq_get_isnull(const pg_result_mock& m, int row, int column) {
        return m.get_isnull(row, column);
    }

    friend int pq_field_number(const pg_result_mock& m, const char* name) {
        return m.field_number(name);
    }

    friend int pq_nfields(const pg_result_mock& m) {
        return m.nfields();
    }

    friend int pq_ntuples(const pg_result_mock& m) {
        return m.ntuples();
    }
};

} // namespace ozo::tests
