#include "util/recognizer.h"

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <library/cpp/testing/unittest/tests_data.h>

int main(int argc, char* argv[]) {
    ::testing::FLAGS_gmock_catch_leaked_mocks = true;
    ::testing::InitGoogleMock(&argc, argv);

    // Initialize recognizer
    const auto testsData = GetWorkPath();
    const auto languageDict = testsData + "/queryrec.dict";
    const auto languageWeights = testsData + "/queryrec.weights";
    const auto encodingDict = testsData + "/dict.dict";
    NNotSoLiteSrv::NUtil::InitRecognizer(languageDict, languageWeights, encodingDict);

    return RUN_ALL_TESTS();
}
