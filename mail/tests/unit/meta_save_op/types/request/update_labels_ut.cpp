#include <gtest/gtest.h>
#include <mail/notsolitesrv/tests/unit/fakes/context.h>
#include <mail/notsolitesrv/src/meta_save_op/types/request.h>
#include <util/generic/algorithm.h>

using namespace testing;
using namespace NNotSoLiteSrv;
using namespace NNotSoLiteSrv::NMetaSaveOp;

namespace NNotSoLiteSrv::NMetaSaveOp {

bool operator==(const TLabel& lhs, const TLabel& rhs) {
    return std::tie(lhs.name, lhs.type) == std::tie(rhs.name, rhs.type);
}

bool operator<(const TLabel& lhs, const TLabel& rhs) {
    return std::tie(lhs.name, lhs.type) < std::tie(rhs.name, rhs.type);
}

void PrintTo(const TLabel& label, std::ostream* os) {
    *os << "{" << label.name << ", " << label.type << "}";
}

}

struct TUpdateLabelsInParamsTest: public Test {
    void SetUp() override {
        Ctx = GetContext();
    }

    struct TExpected {
        std::vector<TLabel> Labels;
        std::vector<TLid> Lids;
        std::vector<std::string> LabelSymbols;
    };

    void Check(bool isSpam, bool hasAttachments, bool sharedStid, TExpected expected) const {
        TParams params;
        UpdateLabelsInParams(Ctx, params, Hint, isSpam, hasAttachments, sharedStid);
        Sort(expected.Labels);
        Sort(expected.Lids);
        Sort(expected.LabelSymbols);
        Sort(params.labels);
        Sort(params.lids);
        Sort(params.label_symbols);
        EXPECT_EQ(expected.Labels, params.labels);
        EXPECT_EQ(expected.Lids, params.lids);
        EXPECT_EQ(expected.LabelSymbols, params.label_symbols);
    }

    TContextPtr Ctx;
    TXYandexHint Hint;
};

TEST(TUpdateLabelSymbolsFromMixedTest, Empty) {
    std::vector<std::string> symbols;
    UpdateLabelSymbolsFromMixed(symbols, 0ll);
    EXPECT_TRUE(symbols.empty());
}

TEST(TUpdateLabelSymbolsFromMixedTest, Obsoleted) {
    std::vector<std::string> symbols;
    UpdateLabelSymbolsFromMixed(symbols, 0x20000ll);
    EXPECT_TRUE(symbols.empty());
}

TEST(TUpdateLabelSymbolsFromMixedTest, SpamSeen) {
    std::vector<std::string> symbols;
    UpdateLabelSymbolsFromMixed(symbols, 0x804ll);
    EXPECT_EQ(symbols, std::vector<std::string>({"spam_label", "seen_label"}));
}

TEST(TUpdateLabelSymbolsFromMixedTest, AllSet) {
    std::vector<std::string> symbols;
    UpdateLabelSymbolsFromMixed(symbols, 0xfffffll);
    EXPECT_EQ(symbols,
        std::vector<std::string>({
            "attached_label",
            "spam_label",
            "postmaster_label",
            "recent_label",
            "draft_label",
            "deleted_label",
            "forwarded_label",
            "answered_label",
            "seen_label",
            "append_label"
        }));
}

TEST_F(TUpdateLabelsInParamsTest, Empty) {
    Check(false, false, false, {});
}

TEST_F(TUpdateLabelsInParamsTest, Flags) {
    Check(true, true, true, {{}, {}, {"spam_label", "attached_label", "mulcaShared_label"}});
}

TEST_F(TUpdateLabelsInParamsTest, ImapImplicitlyAddAppendSymbol) {
    Hint.imap = true;
    Check(false, false, false, {{}, {}, {"append_label"}});
}

TEST_F(TUpdateLabelsInParamsTest, PriorityHigh) {
    Hint.priority_high = true;
    Check(false, false, false, {{}, {}, {"important_label"}});
}

TEST_F(TUpdateLabelsInParamsTest, NoPriorityHighForSpam) {
    Hint.priority_high = true;
    Check(true, false, false, {{}, {}, {"spam_label"}});
}

TEST_F(TUpdateLabelsInParamsTest, MuteImplicitlyAddSeen) {
    Hint.label = {"symbol:mute_label"};
    Check(false, false, false, {{}, {}, {"mute_label", "seen_label"}});
}

TEST_F(TUpdateLabelsInParamsTest, SOLabels) {
    Hint.label = {"SystMetkaSO:trust_6", "SystMetkaSO:s_news"};
    Check(false, false, false, {{{"56", "so"}, {"47", "so"}}, {}, {}});
}

TEST_F(TUpdateLabelsInParamsTest, SOLabelsPrefixIsCaseInsensitive) {
    Hint.label = {"systmetkaso:trust_6", "sYSTmETKAso:s_news"};
    Check(false, false, false, {{{"56", "so"}, {"47", "so"}}, {}, {}});
}

TEST_F(TUpdateLabelsInParamsTest, SOLabelsNameIsCaseSensitive) {
    Hint.label = {"SystMetkaSO:trust_6", "SystMetkaSO:s_News"};
    Check(false, false, false, {{{"56", "so"}}, {}, {}});
}

TEST_F(TUpdateLabelsInParamsTest, SOLabelsSpamIsNotHamon) {
    Hint.label = {"SystMetkaSO:hamon"};
    Check(true, false, false, {{}, {}, {"spam_label"}});
}

TEST_F(TUpdateLabelsInParamsTest, ConvertHamonTypeToHamonLabel) {
    Hint.label = {"SystMetkaSO:hamon"};
    Check(false, false, false, {{}, {}, {"hamon_label"}});
}

TEST_F(TUpdateLabelsInParamsTest, DomainLabels) {
    Hint.label = {"domain_vtnrf0vkcom"};
    Check(false, false, false, {{{"vtnrf0vkcom", "social"}}, {}, {}});
}

TEST_F(TUpdateLabelsInParamsTest, Symbols) {
    Hint.label = {"symbol:spam_label" ,"symbol:seen_label"};
    Check(false, false, false, {{}, {}, {"spam_label", "seen_label"}});
}

TEST_F(TUpdateLabelsInParamsTest, SkipDuplicatedSymbols) {
    Hint.label = {"symbol:spam_label" ,"symbol:seen_label", "symbol:spam_label"};
    Check(false, false, false, {{}, {}, {"spam_label", "seen_label"}});
}

TEST_F(TUpdateLabelsInParamsTest, UseSymbolsFromMixed) {
    Hint.label = {"symbol:spam_label" ,"symbol:seen_label", "symbol:spam_label"};
    Hint.mixed = 0x10004ll; // append + spam
    Check(false, false, false, {{}, {}, {"spam_label", "seen_label", "append_label"}});
}

TEST_F(TUpdateLabelsInParamsTest, SymbolsIsCaseSensitive) {
    Hint.label = {"symbol:spam_label" ,"symbol:SPAM_label"};
    Check(false, false, false, {{}, {}, {"spam_label", "SPAM_label"}});
}

TEST_F(TUpdateLabelsInParamsTest, JustLabelsAreSystem) {
    Hint.label = {"label1", "label2"};
    Check(false, false, false, {{{"label1", "system"}, {"label2", "system"}}, {}, {}});
}

TEST_F(TUpdateLabelsInParamsTest, UserLabels) {
    Hint.userlabel = {"label1", "label2"};
    Check(false, false, false, {{{"label1", "user"}, {"label2", "user"}}, {}, {}});
}

TEST_F(TUpdateLabelsInParamsTest, ImapLabels) {
    Hint.imaplabel = {"label1", "label2"};
    Check(false, false, false, {{{"label1", "imap"}, {"label2", "imap"}}, {}, {}});
}

TEST_F(TUpdateLabelsInParamsTest, SkipDuplicatedLabelsOfSameType) {
    Hint.imaplabel = {"label1", "label2", "label1"};
    Hint.userlabel = {"label1", "label2", "label1"};
    Hint.label = {"label1", "label2", "symbol:hamon_label", "label1", "SystMetkaSO:hamon"};
    Check(false, false, false,
        {
            {
                {"label1", "imap"}, {"label2", "imap"},
                {"label1", "user"}, {"label2", "user"},
                {"label1", "system"}, {"label2", "system"}
            },
            {},
            {"hamon_label"}
        });
}

TEST_F(TUpdateLabelsInParamsTest, EmptyLidValues) {
    Hint.lid.emplace_back("");
    Check(false, false, false, {});

    Hint.lid.emplace_back("17");
    Hint.lid.emplace_back("");
    Check(false, false, false, {{}, {"17"}, {}});
}

TEST_F(TUpdateLabelsInParamsTest, Lids) {
    Hint.lid = {"17", "1"};
    Check(false, false, false, {{}, {"1", "17"}, {}});
}

TEST_F(TUpdateLabelsInParamsTest, SkipNonNumericLids) {
    Hint.lid = {"17", "abyrvalg", "1"};
    Check(false, false, false, {{}, {"1", "17"}, {}});
}

TEST_F(TUpdateLabelsInParamsTest, SkipDuplicatedLids) {
    Hint.lid = {"17", "1", "17", "1"};
    Check(false, false, false, {{}, {"1", "17"}, {}});
}
