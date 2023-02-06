#pragma once

/**
 * @file util.h
 * @brief Provide set of utils for tests.
 * @author ilinvalery
 * © Yandex LLC.
 */

#include <util/generic/string.h>
#include <library/cpp/testing/unittest/tests_data.h>

TString GetPathToFile(const TString& filename) {
    return GetWorkPath() + "/" + filename;
}
