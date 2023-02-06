#pragma once

#include "address.h"

#include <extsearch/geo/kernel/localeutils/packedlocale.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/xml/document/xml-document.h>

#include <util/generic/vector.h>
#include <util/generic/string.h>

namespace NPa = ::yandex::maps::proto::search::address;

void UnitAssertStringVectorsEqual(const TVector<TString>& actual, const TVector<TString>& expected);

#define ASSERT_STRING_VECTORS_EQUAL(actual, expected)                                                                        \
    do {                                                                                                                     \
        const TVector<TString> actualVector(actual);                                                                         \
        const TVector<TString> expectedVector(expected);                                                                     \
        UNIT_ASSERT_VALUES_EQUAL_C(actualVector.size(), expectedVector.size(), "vectors have different size");               \
        for (size_t i = 0; i < actualVector.size(); ++i) {                                                                   \
            UNIT_ASSERT_VALUES_EQUAL_C(actualVector[i], expectedVector[i], "elements with index " << i << " are different"); \
        }                                                                                                                    \
    } while (false)

#define ASSERT_EQUAL_XAL(actual, expected) \
    ASSERT_STRING_VECTORS_EQUAL(SplitXAL(actual), SplitXAL(expected))

TString Address2String(const NGeosearch::NAddress::TAddress& address, NGeosearch::NLocaleUtils::TPackedLocale locale,
                       const TString& houseName = "",
                       const TString& addressLine = "");
TString Node2String(const NXml::TConstNode& node);

TVector<TString> SplitXAL(const TString& data);
TString GetTag(const TString& tag, const TString& data);

TVector<TString> ReadChunks(IInputStream& in);
TString SerializeAddress(const NPa::Address& a);
