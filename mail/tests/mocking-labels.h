#ifndef __MOCKING_LABELS_H
#define __MOCKING_LABELS_H
#include <gmock/gmock.h>
#include <macs/labels_repository.h>
#include <boost/shared_ptr.hpp>

#ifdef __clang__
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Winconsistent-missing-override"
#endif

struct MockLabelsRepository: public macs::LabelsRepository {
    MOCK_METHOD(void, syncGetLabels, (macs::OnLabels h), (const, override));
    MOCK_METHOD(void, syncCreateLabel, (const std::string&, const std::string&, const macs::Label::Type&,
                         macs::OnUpdateLabel), (const, override));
    MOCK_METHOD(void, syncCreateLabel, (const macs::Label::Symbol &, macs::OnUpdateLabel), (const, override));
    MOCK_METHOD(void, syncModifyLabel, (const macs::Label&, macs::OnUpdateLabel), (const, override));
    MOCK_METHOD(void, syncEraseLabel, (const std::string&, macs::OnUpdate), (const, override));
    MOCK_METHOD(void, syncClearLabel, (const macs::Label&, macs::OnUpdateMessages), (const, override));
    MOCK_METHOD(void, syncGetThreadsCount, (const std::string&, macs::OnCountReceive), (const, override));
    MOCK_METHOD(void, syncGetOrCreateLabel, (const std::string&, const std::string&, const macs::Label::Type&,
                         macs::OnUpdateLabel), (const, override));
    MOCK_METHOD(void, syncGetOrCreateLabel, (const macs::Label::Symbol &, macs::OnUpdateLabel), (const, override));

    macs::LabelFactory factory(void) const {
        return getLabelfactory();
    }

    macs::LabelFactory label(const std::string& id,
                             const std::string& name = "",
                             const std::string& color = "") {
        return factory().lid(id).name(name).color(color);
    }

    macs::LabelFactory withType(macs::LabelFactory factory, const macs::Label::Type& type) {
        return factory.type(type);
    }

    macs::LabelFactory system(macs::LabelFactory factory) {
        return withType(factory, macs::Label::Type::system);
    }
};

struct LabelsRepositoryTest: public testing::Test {
    typedef testing::StrictMock<MockLabelsRepository> Repository;
    std::shared_ptr<Repository> labelsPtr;
    Repository &labels;

    typedef decltype(testing::InvokeArgument<0>(macs::error_code(), macs::LabelSet())) LabelsInvoker;
    typedef std::vector<macs::Label> TestData;

    LabelsRepositoryTest() : labelsPtr(new Repository), labels(*labelsPtr) {}

    static LabelsInvoker GiveLabels(const TestData& args) {
        macs::LabelSet ret;
        for( const auto & i : args ) {
            ret[i.lid()] = i;
        }
        return testing::InvokeArgument<0>(macs::error_code(), ret);
    }

    static LabelsInvoker GiveLabels(const macs::Label& l1) {
        return GiveLabels(TestData{l1});
    }

    static LabelsInvoker GiveLabels(const macs::Label& l1,
                                    const macs::Label& l2) {
        return GiveLabels(TestData{l1, l2});
    }

    static LabelsInvoker GiveLabels(const macs::Label& l1,
                                    const macs::Label& l2,
                                    const macs::Label& l3) {
        return GiveLabels(TestData{l1, l2, l3});
    }
};

struct LabelMatcher {
    LabelMatcher(const std::string& name)
        : name(name), checkColor(false), checkLid(false)
    {}

    bool operator()(const macs::Label& label) const {
        return label.name() == name
            && (!checkLid || label.lid() == lid)
            && (!checkColor || label.color() == color);
    }

    LabelMatcher& withColor(const std::string& color) {
        checkColor = true;
        this->color = color;
        return *this;
    }

    LabelMatcher& withLid(const std::string& lid) {
        checkLid = true;
        this->lid = lid;
        return *this;
    }

    std::string name, color, lid;
    bool checkColor, checkLid;
};

typedef testing::internal::TrulyMatcher<LabelMatcher> TrulyLabelMatcher;
typedef testing::PolymorphicMatcher<TrulyLabelMatcher> PolyLabelMatcher;

inline PolyLabelMatcher matchLabel(const macs::Label& label) {
    return testing::Truly(LabelMatcher(label.name())
                          .withColor(label.color())
                          .withLid(label.lid()));
}

#ifdef __clang__
#pragma clang diagnostic pop
#endif

#endif
