#include "json_comparer.h"

#include <library/cpp/iterator/zip.h>


bool ContainingJson(const NSc::TValue& actual, const NSc::TValue& expected) {
    if (actual.IsArray() and expected.IsArray()) {
        if (actual.GetArray().size() != expected.GetArray().size()) {
            ythrow yexception()
                << "Assertation error: "
                << "actual.size(" << actual.GetArray().size() << ") "
                << "not equals to "
                << "expected.size(" << expected.GetArray().size() << ") "
                << Endl
                << "actual: " << actual.ToJson() << Endl
                << "expected: " << expected.ToJson() << Endl
                << Endl;

        }
        for (const auto& [actualItem, expectedItem]: Zip(actual.GetArray(), expected.GetArray())) {
            if (not ContainingJson(actualItem, expectedItem)) {
                return false;
            }
        }
    } else if(actual.IsDict() and expected.IsDict()) {
        const auto& actualDict = actual.GetDict();
        for (const auto& [key, value]: expected.GetDict()) {
            // comparing only presented keys
            if (actualDict.contains(key) and actualDict.at(key) != value) {
                ythrow yexception()
                    << "Assertation error: "
                    << "actual[" << key << "](" << actualDict.at(key).ToJson() << ") "
                    << " not equals to "
                    << "expected[" << key << "](" << value.ToJson() << ")"
                    << Endl
                    << "actual: " << actual.ToJson() << Endl
                    << "expected: " << expected.ToJson() << Endl
                    << Endl;
            }
        }
    } else if(actual.GetType() != expected.GetType()) {
        ythrow yexception()
            << "Assertation error: "
            << "objects has different type "
            << "actual(" << ToString(actual.GetType()) << "), "
            << "expected(" << ToString(expected.GetType()) << ")"
            << Endl
            << "actual: " << actual.ToJson() << Endl
            << "expected: " << expected.ToJson() << Endl
            << Endl;
    } else {
        if (actual != expected) {
        ythrow yexception()
            << "Assertation error: "
            << "actual(" << actual.ToJson() << "),"
            << " not equals to "
            << "expected(" << expected.ToJson() << ")"
            << Endl;
        }
    }
    return true;
}

