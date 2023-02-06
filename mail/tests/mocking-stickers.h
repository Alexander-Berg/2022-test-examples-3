#pragma once

#include <gmock/gmock.h>
#include <macs/stickers_repository.h>



struct MockStickerRepository : public macs::StickersRepository {
    MOCK_METHOD(void, asyncCreateReplyLater, (const macs::Mid&, const macs::Fid&, std::time_t date, const std::optional<macs::Tab::Type>&, macs::OnReplyLaterSticker), (const, override));
    MOCK_METHOD(void, asyncGetReplyLaterList, (macs::OnReplyLaterStickers), (const, override));
    MOCK_METHOD(void, asyncRemoveReplyLater, (const macs::Mid& mid, macs::OnUpdate), (const, override));
    MOCK_METHOD(void, asyncRemoveIncorrectReplyLaterStickers, (macs::OnIncorrectStickersMids), (const, override));
};
