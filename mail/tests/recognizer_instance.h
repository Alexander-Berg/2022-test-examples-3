#pragma once

#ifdef ARCADIA_BUILD
#include <library/cpp/testing/unittest/tests_data.h>
#endif

inline const Recognizer::Wrapper& getRecognizer() {
#ifdef ARCADIA_BUILD
    static const auto createInstance = [] {
        const auto testsData = GetWorkPath();
        const auto languageDict = testsData + "/queryrec.dict";
        const auto languageWeights = testsData + "/queryrec.weights";
        const auto encodingDict = testsData + "/dict.dict";
        return Recognizer::create(languageDict.c_str(), languageWeights.c_str(), encodingDict.c_str());
    };

    static auto instance = createInstance();
#else
    static auto instance = Recognizer::create();
#endif
    return *instance;
}
