#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <macs/label_factory.h>
#include <macs/label_set.h>
#include <internal/envelope/convert.h>
#include <algorithm>
#include <map>
#include <iterator>
#include "base-convert-test.h"
#include "envelope-row.h"

namespace macs {
std::ostream & operator << ( std::ostream & s, const AttachmentDescriptor & v ) {
    s << v.m_hid << ", " << v.m_contentType << ", " << v.m_fileName << ", " <<  v.m_size;
    return s;
}

bool operator == (const AttachmentDescriptor & l, const AttachmentDescriptor & r) {
    return l.m_hid == r.m_hid && l.m_contentType == r.m_contentType
            && l.m_fileName == r.m_fileName && l.m_size == r.m_size;
}

} // namespace macs

namespace {

using namespace testing;

class ConvertEnvelopeTest : public tests::BaseConvertTest<macs::pg::reflection::Envelope> {
protected:
    static void fill(Reflection& data) {
        tests::fillDefaultEnvelopeRowData(data);
    }

    ConvertEnvelopeTest() {
        modifyData(fill);
    }

    using LT = macs::Label::Type;
    macs::Label getLabel (const std::string & lid, const std::string &name, const LT& type) {
        return macs::LabelFactory().lid(lid).name(name).type(type);
    }
    void setLabel(const macs::Lid& lid, const std::string &name, const LT& type) {
        userLabels[lid] = getLabel (lid, name, type);
    }
    macs::Envelope convert() {
        return macs::pg::makeEnvelope(userLabels, data());
    }

private:
    macs::LabelSet userLabels;
};


TEST_F(ConvertEnvelopeTest, EnvelopeConverter) {
    macs::Envelope e = convert();
    EXPECT_EQ(e.mid(), "12345");
    EXPECT_EQ(e.fid(), "1");
    EXPECT_EQ(e.threadId(), "2");
    EXPECT_EQ(e.imapId(), "111");
    EXPECT_EQ(e.receiveDate(), 1);
    EXPECT_EQ(e.size(), 1024ul);
    EXPECT_EQ(e.subject(), "Into the cave");
    EXPECT_EQ(e.firstline(), "Here must be dragons...");
    EXPECT_EQ(e.date(), 22);
    EXPECT_EQ(e.rfcId(), "<NM62F0599D400D232BCozon_prod_mid1@news.ozon.ru>");
    EXPECT_EQ(e.inReplyTo(), "<1814792997.8636801336707460201.JavaMail.web@wbid002cnc.rim.net>");
    EXPECT_EQ(e.extraData(), "Here can be any shit");
    EXPECT_EQ(e.revision(), macs::Revision(5));
}

TEST_F(ConvertEnvelopeTest, rowWithLids_adds_lables) {
    setLabel("5", "user5", LT::user);
    setLabel("7", "system", LT::system);
    setLabel("9", "9", LT::spamDefense);
    modifyData([] (Reflection &data) {
        data.lids = {5, 7, 9};
    });
    macs::Envelope e = convert();
    EXPECT_THAT( e.labels(), AllOf( Contains("5"), Contains("7"), Contains("9")) );
}

TEST_F(ConvertEnvelopeTest, rowWithUnknowLids_convert_throwsException) {
    modifyData([] (Reflection &data) {
        data.lids = {11};
    });
    EXPECT_THROW(convert(), macs::system_error);
}

TEST_F(ConvertEnvelopeTest, rowWithUnknowLids_convert_throwsSystemErrorWithNoSuchLabelCode) {
    modifyData([] (Reflection &data) {
        data.lids = {11};
    });
    try {
        convert();
    } catch (const macs::system_error& e) {
        EXPECT_EQ(e.code(), macs::error::noSuchLabel);
    }
}

TEST_F(ConvertEnvelopeTest, rowWithSoLabelContainsNonNumericNameLids_convert_throwsException) {
    setLabel("11", "zzz", LT::spamDefense);
    modifyData([] (Reflection &data) {
        data.lids = {11};
    });
    EXPECT_THROW(convert(),
            std::runtime_error);
}

TEST_F(ConvertEnvelopeTest, rowWithSoLabelsLids_sets_types) {
    setLabel("11", "1", LT::spamDefense);
    setLabel("12", "10", LT::spamDefense);
    setLabel("13", "24", LT::spamDefense);
    modifyData([] (Reflection &data) {
        data.lids = {11, 12, 13};
    });
    macs::Envelope e = convert();
    EXPECT_THAT(e.types(), ElementsAre(1,10,24));
}

TEST_F(ConvertEnvelopeTest, rowNoSoLabelsTypes_doesNotSet_types) {
    macs::Envelope e = convert();
    EXPECT_TRUE( e.types().empty() );
}

TEST_F(ConvertEnvelopeTest, rowWithAttributePostmaster_adds_fakePostmasterLabel) {
    modifyData([] (Reflection &data) {
        data.attributes = {"postmaster", "spam"};
    });
    macs::Envelope e = convert();
    EXPECT_THAT( e.labels(), Contains("FAKE_POSTMASTER_LBL") );
}

TEST_F(ConvertEnvelopeTest, rowWithoutAttributePostmaster_doesNotAdd_fakePostmasterLabel) {
    modifyData([] (Reflection &data) {
        data.attributes = {"mulca-shared", "spam"};
    });
    macs::Envelope e = convert();
    EXPECT_THAT( e.labels(), Not(Contains("FAKE_POSTMASTER_LBL")) );
}

TEST_F(ConvertEnvelopeTest, rowWithAttributeSpam_adds_fakeSpamLabel) {
    modifyData([] (Reflection &data) {
        data.attributes = {"postmaster", "spam"};
    });
    macs::Envelope e = convert();
    EXPECT_THAT( e.labels(), Contains("FAKE_SPAM_LBL") );
}

TEST_F(ConvertEnvelopeTest, rowWithoutAttributeSpam_doesNotAdd_fakeSpamLabel) {
    modifyData([] (Reflection &data) {
        data.attributes = {};
    });
    macs::Envelope e = convert();
    EXPECT_THAT( e.labels(), Not(Contains("FAKE_SPAM_LBL")) );
}

TEST_F(ConvertEnvelopeTest, rowWithAttributeMalcaShared_adds_fakeSpamLabel) {
    modifyData([] (Reflection &data) {
        data.attributes = {"mulca-shared", "spam"};
    });
    macs::Envelope e = convert();
    EXPECT_THAT( e.labels(), Contains("FAKE_MULCA_SHARED_LBL") );
}

TEST_F(ConvertEnvelopeTest, rowWithoutAttributeMulcaShared_doesNotAdd_fakeSpamLabel) {
    modifyData([] (Reflection &data) {
        data.attributes = {};
    });
    macs::Envelope e = convert();
    EXPECT_THAT( e.labels(), Not(Contains("FAKE_MULCA_SHARED_LBL")) );
}

TEST_F(ConvertEnvelopeTest, rowWithAttributeAppend_adds_fakeSpamLabel) {
    modifyData([] (Reflection &data) {
        data.attributes = {"append", "spam"};
    });
    macs::Envelope e = convert();
    EXPECT_THAT( e.labels(), Contains("FAKE_APPEND_LBL") );
}

TEST_F(ConvertEnvelopeTest, rowWithoutAttributeAppend_doesNotAdd_fakeSpamLabel) {
    modifyData([] (Reflection &data) {
        data.attributes = {};
    });
    macs::Envelope e = convert();
    EXPECT_THAT( e.labels(), Not(Contains("FAKE_APPEND_LBL")) );
}

TEST_F(ConvertEnvelopeTest, rowWithAttributeCopy_adds_fakeSpamLabel) {
    modifyData([] (Reflection &data) {
        data.attributes = {"copy", "spam"};
    });
    macs::Envelope e = convert();
    EXPECT_THAT( e.labels(), Contains("FAKE_COPY_LBL") );
}

TEST_F(ConvertEnvelopeTest, rowWithoutAttributeCopy_doesNotAdd_fakeSpamLabel) {
    modifyData([] (Reflection &data) {
        data.attributes = {};
    });
    macs::Envelope e = convert();
    EXPECT_THAT( e.labels(), Not(Contains("FAKE_COPY_LBL")) );
}

TEST_F(ConvertEnvelopeTest, rowWithOldFormatStid_setsStid_withoutPrefix) {
    modifyData([] (Reflection &data) {
        data.st_id = "mulca:2:23234.62776296.3123456844257369311306655180";
    });
    macs::Envelope e = convert();
    EXPECT_EQ( e.stid(), "23234.62776296.3123456844257369311306655180" );
}

TEST_F(ConvertEnvelopeTest, rowWithNewFormatStid_setsStid_withoutPrefix) {
    modifyData([] (Reflection &data) {
        data.st_id = "23234.62776296.3123456844257369311306655180";
    });
    macs::Envelope e = convert();
    EXPECT_EQ( e.stid(), "23234.62776296.3123456844257369311306655180" );
}

TEST_F(ConvertEnvelopeTest, rowWithFid_doesNotAdd_fidIntoLabels) {
    modifyData([] (Reflection &data) {
        data.fid = 12345;
    });
    macs::Envelope e = convert();
    EXPECT_THAT( e.labels(), Not(Contains("12345")) );
}

TEST_F(ConvertEnvelopeTest, rowWithSeenFalse_doesNotAdd_fakeSeenLabel) {
    modifyData([] (Reflection &data) {
        data.seen = false;
    });
    macs::Envelope e = convert();
    EXPECT_THAT( e.labels(), Not(Contains("FAKE_SEEN_LBL")) );
}

TEST_F(ConvertEnvelopeTest, rowWithSeenTrue_adds_fakeSeenLabel) {
    modifyData([] (Reflection &data) {
        data.seen = true;
    });
    macs::Envelope e = convert();
    EXPECT_THAT( e.labels(), Contains("FAKE_SEEN_LBL") );
}

TEST_F(ConvertEnvelopeTest, rowWithRecentFalse_doesNotAdd_fakeRecentLabel) {
    modifyData([] (Reflection &data) {
        data.recent = false;
    });
    macs::Envelope e = convert();
    EXPECT_THAT( e.labels(), Not(Contains("FAKE_RECENT_LBL")) );
}

TEST_F(ConvertEnvelopeTest, rowWithRecentTrue_adds_fakeResentLabel) {
    modifyData([] (Reflection &data) {
        data.recent = true;
    });
    macs::Envelope e = convert();
    EXPECT_THAT( e.labels(), Contains("FAKE_RECENT_LBL") );
}

TEST_F(ConvertEnvelopeTest, rowWithDeletedFalse_doesNotAdd_fakeDeletedLabel) {
    modifyData([] (Reflection &data) {
        data.deleted = false;
    });
    macs::Envelope e = convert();
    EXPECT_THAT( e.labels(), Not(Contains("FAKE_DELETED_LBL")) );
}

TEST_F(ConvertEnvelopeTest, rowWithDeletedTrue_adds_fakeDeletedLabel) {
    modifyData([] (Reflection &data) {
        data.deleted = true;
    });
    macs::Envelope e = convert();
    EXPECT_THAT( e.labels(), Contains("FAKE_DELETED_LBL") );
}

typedef macs::AttachmentDescriptor Attach;
typedef std::vector<Attach> AttachVector;

TEST_F(ConvertEnvelopeTest, rowWithNoAttaches_doesNonAdd_fakeAttachedLabel) {
    modifyData([] (Reflection &data) {
        data.attaches.clear();
    });
    macs::Envelope e = convert();
    EXPECT_THAT( e.labels(), Not(Contains("FAKE_ATTACHED_LBL")) );
}

TEST_F(ConvertEnvelopeTest, rowWithAttaches_adds_attachEntries) {
    using MA = macs::pg::MessageAttach::Reflection;
    modifyData([] (Reflection &data) {
        data.attaches = {
            MA {"1.2", "image/jpeg", "373731 - Fighters SNK.jpg", 186312},
            MA {"1.3", "image/jpeg", "662903 -  Mokusa.jpg", 337991},
            MA {"1.4", "image/jpeg", "king_of_fighters.jpg", 303938},
        };
    });
    AttachVector atts;
    atts.push_back(Attach("1.2", "image/jpeg", "373731 - Fighters SNK.jpg",186312));
    atts.push_back(Attach("1.3", "image/jpeg", "662903 -  Mokusa.jpg",337991));
    atts.push_back(Attach("1.4", "image/jpeg", "king_of_fighters.jpg",303938));

    macs::Envelope e = convert();
    EXPECT_EQ(e.attachmentsCount(), 3ul);
    EXPECT_EQ(e.attachmentsFullSize(), 186312ul + 337991 + 303938);
    EXPECT_EQ(e.attachments(), atts);
}

TEST_F(ConvertEnvelopeTest, rowWithAttaches_adds_fakeAttachedLabel) {
    using MA = macs::pg::MessageAttach::Reflection;
    modifyData([] (Reflection &data) {
        data.attaches = {
            MA {"1.2", "image/jpeg", "373731 - Fighters SNK.jpg", 186312},
            MA {"1.3", "image/jpeg", "662903 -  Mokusa.jpg", 337991},
            MA {"1.4", "image/jpeg", "king_of_fighters.jpg", 303938},
        };
    });
    macs::Envelope e = convert();
    EXPECT_THAT( e.labels(), Contains("FAKE_ATTACHED_LBL") );
}

TEST_F(ConvertEnvelopeTest, rowWithRecipients_adds_recipientsEntries) {
    using R = macs::pg::Recipient::Reflection;
    modifyData([] (Reflection &data) {
        data.recipients = {
            R {"from", "Pinterest", "pinbot@pinterest.com"},
            R {"to", "", "me@mydomain.ru"},
            R {"to", "Альфред Нобель", "nobel@huyahoo.ru"},
            R {"cc", "Алена Дутф", "lenaLena@mymail.ua"},
            R {"bcc", "John Doe", "doe-john@crypt.hl"},
            R {"sender", "Birius Slack", "birius@slack.com"},
        };
    });
    macs::Envelope e = convert();
    EXPECT_EQ(e.from(), "Pinterest <pinbot@pinterest.com>");
    EXPECT_EQ(e.to(), "me@mydomain.ru,Альфред Нобель <nobel@huyahoo.ru>");
    EXPECT_EQ(e.cc(), "Алена Дутф <lenaLena@mymail.ua>");
    EXPECT_EQ(e.bcc(), "John Doe <doe-john@crypt.hl>");
    EXPECT_EQ(e.sender(), "Birius Slack <birius@slack.com>");
}

} // namespace
