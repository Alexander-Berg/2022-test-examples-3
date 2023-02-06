#include "insert_methods_tester.h"

#include <search/grouping/name.h>
#include <search/web/core/rule.h>

#include <library/cpp/testing/unittest/registar.h>

namespace {
    class TTestRule: public IRearrangeRule {
    public:
        class TTestRuleContext: public IRearrangeRuleContext {
            void DoRearrangeAfterFetch(TRearrangeParams& rearrangeParams) override {
                if (!rearrangeParams.Current || !IsDeepMainGrouping(rearrangeParams.Current)) {
                    return;
                }
                UNIT_ASSERT(TestFunction);
                TestFunction(*this, rearrangeParams);
            }

            void DoGetGroupingsDependency(THashMap<TString, TVector<TString>>& dep) override {
                dep[NSearchGrouping::NName::d].push_back(NSearchGrouping::NName::ugc_db);
                dep[NSearchGrouping::NName::d].push_back(NSearchGrouping::NName::e);
            }
        };

        IRearrangeRuleContext* DoConstructContext() const override {
            return new TTestRuleContext();
        }

        static IRearrangeRule* CreateRule(const TString&, const TConstructRearrangeEnv&) {
            return new TTestRule();
        }

        static TUgcInsertMethodsTester::TTestFunction TestFunction;
    };

    TUgcInsertMethodsTester::TTestFunction TTestRule::TestFunction;

    REGISTER_REARRANGE_RULE(UgcInserterTestRule, TTestRule::CreateRule);
} // anonymous namespace

TUgcInsertMethodsTester::TUgcInsertMethodsTester()
    : Search(NRearrUT::TMetaSearchMock::ConfigFromConfJson("/test_rule_conf.json", true))
    , Ctx(Search)
{
    CreateEmptyGrouping(NSearchGrouping::NName::d);
    CreateEmptyGrouping(NSearchGrouping::NName::e);

    Ctx.FillGroupingJson("{Groups: [{MetaDocs: [{DocId: '0'}]},{MetaDocs: [{DocID: '1'}]} ]}");
}

void TUgcInsertMethodsTester::CreateEmptyGrouping(const TString& groupingName) {
    TMetaGroupingId grouping = Ctx.AddGrouping(NSc::TValue(), groupingName, GM_DEEP);
    UNIT_ASSERT(grouping && grouping->second);
    // Turns out InitGrouping sometimes drops parameters from a non-main grouping.
    // This prevents that.
    Ctx.GetGrouping(groupingName).FixCount();
}

TUgcInsertMethodsTester::TWithTestFunction::TWithTestFunction(TTestFunction f, bool checkCalled)
    : CheckCalled(checkCalled)
{
    auto func = [f, this](IRearrangeRuleContext& context, IMetaRearrangeContext::TRearrangeParams& rearrangeParams) {
        TestFunctionCalled = true;
        f(context, rearrangeParams);
    };
    TTestRule::TestFunction = std::move(func);
    UNIT_ASSERT(TTestRule::TestFunction);
}

TUgcInsertMethodsTester::TWithTestFunction::~TWithTestFunction() {
    TTestFunction f;
    TTestRule::TestFunction.swap(f);
    if (CheckCalled && !TestFunctionCalled) {
        UNIT_FAIL_NONFATAL_IMPL("call assertion", "test function expected to be called");
    }
}
