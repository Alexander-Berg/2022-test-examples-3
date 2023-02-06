#pragma once

#include <search/web/util/ut_mocks/meta_mock.h>

#include <functional>

// Class that starts environment to call
// ugc inserter methods like in ugc rearrange rule
class TUgcInsertMethodsTester {
public:
    using TTestFunction = std::function<void(IRearrangeRuleContext&, IMetaRearrangeContext::TRearrangeParams&)>;

    // Class that installs functor that will be called
    // in context of test rearrage rule like ugc
    class TWithTestFunction {
    public:
        explicit TWithTestFunction(TTestFunction f, bool checkCalled = true);
        ~TWithTestFunction();

    private:
        bool TestFunctionCalled = false;
        bool CheckCalled = true;
    };

public:
    TUgcInsertMethodsTester();

    // Installs test function and run rearrange
    template <class TFunc>
    void TestWithFunction(TFunc f) {
        TWithTestFunction withTestFunction(std::move(f));
        Ctx.AfterFetch();
    }

    void CreateEmptyGrouping(const TString& groupingName);

public:
    NRearrUT::TMetaSearchMock Search;
    NRearrUT::TMetaSearchContextMock Ctx;
};
