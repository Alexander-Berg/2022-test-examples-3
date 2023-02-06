#pragma once

#include <library/cpp/testing/unittest/registar.h>

#include <market/replenishment/algorithms/vicugna/test_util/world_builder.h>
#include <market/replenishment/algorithms/vicugna/util/rng.h>

using namespace NAlpaca;

class TWorldFixture : public NUnitTest::TBaseFixture {
public:
    TWorldFixture()
        : World(1, TDate(2000, 1, 1))
        , Builder(World)
    {}

    void BuildSimpleWorld();

    void LoadFromYson(const TString& dataPath);
    void LoadFromYsonRealTables(const TString& dataPath);

    TWorld World;
    TRng Rng;
    TTestWorldBuilder Builder;
};
