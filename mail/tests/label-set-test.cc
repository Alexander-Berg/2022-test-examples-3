#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <macs/label_set.h>
#include <macs/label_factory.h>
#include "throw-wmi-helper.h"

namespace {
using namespace ::testing;
using namespace ::macs;
using namespace ::std;

struct LabelSetTest : public Test {
    LabelSet labels;
    LabelSetTest() {
        fill();
    }

    void addLabel(const std::string& id, const std::string& name, const Label::Symbol& symbol) {
        LabelFactory factory;
        Label label = factory.lid(id).name(name).symbol(symbol);
        labels.insert(make_pair(label.lid(), label));
    }

    void fill() {
        addLabel("1", "answered", Label::Symbol::answered_label);
        addLabel("2", "seen", Label::Symbol::seen_label);
        addLabel("3", "user", Label::Symbol::none);
    }
};

TEST_F(LabelSetTest, getLabelByLid_forNonExistingLabel_throwsException) {
    ASSERT_THROW_SYS(labels.at("666"), macs::error::noSuchLabel,
                     "LabelSet::at: no lid '666': no such label");
}

TEST_F(LabelSetTest, getLabelByLid_forExistingLabel_returnsIt) {
    ASSERT_EQ("1", labels.at("1").lid());
}

TEST_F(LabelSetTest, getLabelByNameAndType_forNonExistingLabel_throwsException) {
    ASSERT_THROW_SYS(labels.at("sent", Label::Type::system), macs::error::noSuchLabel,
                     "LabelSet::at: no {name:'sent', type:'system'}: no such label");
}

TEST_F(LabelSetTest, getLabelByNameAndType_forExistingLabel_returnsIt) {
    ASSERT_EQ("3", labels.at("user", Label::Type::user).lid());
}

TEST_F(LabelSetTest, getLabelBySymbol_forNonExistingLabel_throwsException) {
    ASSERT_THROW_SYS(labels.at(Label::Symbol::draft_label),
                      macs::error::noSuchLabel,
                     "LabelSet::at: no symbol 'draft_label': no such label");
}

TEST_F(LabelSetTest, getLabelBySymbol_forExistingLabel_returnsIt) {
    ASSERT_EQ("2", labels.at(Label::Symbol::seen_label).lid());
}

TEST_F(LabelSetTest, getLabelLidByNameAndType_forNonExistingLabel_returnsEmptyString) {
    ASSERT_EQ(LabelSet::null(), labels.lid("sent", Label::Type::system));
}

TEST_F(LabelSetTest, getLabelLidByNameAndType_forExistingLabel_returnsIt) {
    ASSERT_EQ("3", labels.lid("user", Label::Type::user));
}

TEST_F(LabelSetTest, getLabelLidBySymbol_forNonExistingLabel_returnsEmptyString) {
    ASSERT_EQ(LabelSet::null(), labels.lid(Label::Symbol::draft_label));
}

TEST_F(LabelSetTest, getLabelLidBySymbol_forExistingLabel_returnsIt) {
    ASSERT_EQ("2", labels.lid(Label::Symbol::seen_label));
}

}
