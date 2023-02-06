#pragma once

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/sendbernar/core/include/metadata.h>

namespace sendbernar::tests {

struct MockMetadata: public Metadata {
    MOCK_METHOD(macs::Envelope, getById, (const macs::Mid&), (const, override));
    MOCK_METHOD(macs::MidVec, getByMessageId, (const std::string&, const macs::FidVec&), (const, override));
    MOCK_METHOD(std::string, getMessageId, (const macs::Mid&), (const, override));
    MOCK_METHOD(macs::Fid, fidBySymbol, (const macs::Folder::Symbol&), (const, override));
    MOCK_METHOD(mail_getter::MessageAccessWithWindatParams, getMessageAccessWithWindatParams, (const macs::Mid&), (const, override));
    MOCK_METHOD(mail_getter::MessageAccessParams, getMessageAccessParams, (const macs::Mid&), (const, override));
    MOCK_METHOD(void, deleteDraft, (const macs::Mid&), (const, override));
    MOCK_METHOD(macs::Label, labelBySymbol, (const macs::Label::Symbol&), (const, override));
    MOCK_METHOD((std::pair<macs::error_code, macs::MidsWithMimes>), getMimes, (const macs::MidVec& mids), (const, override));
    MOCK_METHOD(yamail::expected<macs::settings::ProfilePtr>, getSettingsProfile, (macs::settings::SettingsList), (const, override));

    MOCK_METHOD(std::vector<macs::Envelope>, getEnvelopesWithNoAnswer, (const std::string&, const macs::Lid&), (const, override));
    MOCK_METHOD(void, unmarkEnvelopes, (const macs::Mid&, const macs::Labels&), (const, override));
    MOCK_METHOD(void, markEnvelopes, (const macs::MidVec&, const macs::Labels&), (const, override));
    MOCK_METHOD(void, moveMessage, (const macs::Fid&, const macs::Mid&), (const, override));

    MOCK_METHOD(std::shared_ptr<Metadata>, clone, (boost::asio::yield_context), (const, override));
};

}
