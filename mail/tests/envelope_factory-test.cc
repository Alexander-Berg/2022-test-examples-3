#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <macs/envelope_factory.h>
#include "compare_envelopes.h"
#include <type_traits>

namespace {

using namespace ::testing;
using namespace ::macs;

class EnvelopeDataReference : public EnvelopeDataInterface<EnvelopeDataReference> {
public:
    EnvelopeDataReference(const EnvelopeData& data) : data_(&data) {}
private:
    friend class EnvelopeDataInterface<EnvelopeDataReference>;
    const EnvelopeData& data() const { return *data_; }
    const EnvelopeData* data_ = nullptr;
};

struct EnvelopeFactoryTest : public Test {
    EnvelopeFactory dirtyFactory() {
        EnvelopeFactory f;
        f.mid("1234567890")
            .fid("1")
            .threadId("1234567890")
            .revision(100)
            .date(0)
            .receiveDate(0)
            .from("volozh@yandex-team.ru")
            .replyTo("volozh@yandex-team.ru")
            .subject("We will not to force the Arcadia usage")
            .cc("pg@yandex-team.ru")
            .bcc("imperator@yandex-team.ru")
            .to("staff@yandex-team.ru")
            .uidl("16565312")
            .imapId("134293094763")
            .size(4096)
            .stid("167562358721369821370921")
            .firstline(std::string(512, '8'))
            .addLabelIDs({ "1", "2", "3", "4", "5", "6", "7", "8"})
            .addType(1).addType(2).addType(3).addType(4)
            .inReplyTo("inReplyTo")
            .references("references")
            .rfcId("some_ugly_string@with_many_symbols")
//        data.attachments = {};
            .threadCount(10)
            .attachmentsCount(100)
            .attachmentsFullSize(2048)
            .newCount(12)
            .extraData("extra");
        return f;
    }
};

TEST_F(EnvelopeFactoryTest, release_noCallsToAddType_noTypes) {
    macs::EnvelopeFactory factory;

    ASSERT_THAT(factory.release().types(), ElementsAre());
}

TEST_F(EnvelopeFactoryTest, release_addTypeCalledTwiceWithDifferentTypes_hasTwoTypes) {
    macs::EnvelopeFactory factory;

    factory.addType(1).addType(2);

    ASSERT_THAT(factory.release().types(), ElementsAre(1, 2));
}

TEST_F(EnvelopeFactoryTest, release_addTypeCalledTwiceWithSameType_hasOneType) {
    macs::EnvelopeFactory factory;

    factory.addType(1).addType(1);

    ASSERT_THAT(factory.release().types(), ElementsAre(1));
}

TEST_F(EnvelopeFactoryTest, release_addLabelIDCalledTwiceWithSameId_hasOneLabel) {
    macs::EnvelopeFactory factory;

    factory.addLabelID("1").addLabelID("1");

    ASSERT_THAT(factory.release().labels(), ElementsAre("1"));
}

TEST_F(EnvelopeFactoryTest, release_addLabelIDCalledTwiceWithDifferentIds_hasTwoLabels) {
    macs::EnvelopeFactory factory;

    factory.addLabelID("1").addLabelID("2");

    ASSERT_THAT(factory.release().labels(), ElementsAre("1", "2"));
}

TEST_F(EnvelopeFactoryTest, release_addLabelIDCalledWithDifferentIds_hasSortedLabels) {
    macs::EnvelopeFactory factory;

    factory.addLabelID("3").addLabelID("1").addLabelID("2");

    ASSERT_THAT(factory.release().labels(), ElementsAre("1", "2", "3"));
}

TEST_F(EnvelopeFactoryTest, release_resetsFactoryData_toDefaultState) {
    auto factory = dirtyFactory();
    factory.release();
    ASSERT_EQ(EnvelopeDataReference(EnvelopeData::default_), factory.product());
}

TEST_F(EnvelopeFactoryTest, release_withFactoryData_returnsEnvelopeWithFactoryData) {
    ASSERT_EQ(dirtyFactory().release(), dirtyFactory().product());
}

TEST_F(EnvelopeFactoryTest, reset_resetsFactoryData_toDefaultState) {
    auto factory = dirtyFactory();
    factory.reset();
    ASSERT_EQ(EnvelopeDataReference(EnvelopeData::default_), factory.product());
}

TEST_F(EnvelopeFactoryTest, constructor_default_setsDataToDefaultState) {
    auto factory = EnvelopeFactory{};
    factory.release();
    ASSERT_EQ(EnvelopeDataReference(EnvelopeData::default_), factory.product());
}

TEST_F(EnvelopeFactoryTest, constructor_withEnvelope_setsDataToEnvelopeCopy) {
    auto factory = EnvelopeFactory{dirtyFactory().release()};
    ASSERT_EQ(dirtyFactory().product(), factory.product());
}

TEST_F(EnvelopeFactoryTest, envelopeFactory_moveConstructible_throwsNothing) {
    static_assert(std::is_nothrow_move_constructible<macs::EnvelopeFactory>::value,
            "macs::EnvelopeFactory must no throw while move");
}

TEST_F(EnvelopeFactoryTest, envelopeFactory_nonCopyConstructible_throwsNothing) {
    static_assert(!std::is_copy_constructible<macs::EnvelopeFactory>::value,
            "macs::EnvelopeFactory expected do not be copyable");
}

}


