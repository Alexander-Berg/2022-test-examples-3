#pragma once

#include "mocks.h"

#include <extsearch/geo/meta/report/decorators.h>

#include <util/generic/string.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>

void ExtractBody(TString& s);

struct TSearchContextHelper {
    TSearchContextHelper();

    template <typename MatcherType>
    void SetDocProperty(const MatcherType& propertyName, const TString& value) {
        ON_CALL(DocInfo, ReadDocProperty(propertyName, _, _))
            .WillByDefault(DoAll(SetArgPointee<2>(value),
                                 Return(true)));
    }

    TString GetResponse() const;
    TString GetCollectedData() const;

    ISearchContextExt* Ysc() {
        return &Decorator;
    }

public:
    TMockSearchContextExt Context;
    TSearchContextExtDecorator Decorator{&Context};
    TMockArchiveDocInfo DocInfo;
};
