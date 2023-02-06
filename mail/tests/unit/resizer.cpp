#include <src/logic/message_part_real/resizer.hpp>

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wold-style-cast"
#pragma clang diagnostic ignored "-Wsign-conversion"
#include <yamail/data/serialization/json_writer.h>
#pragma clang diagnostic pop

#include <mail/retriever/tests/unit/gtest.h>
#include <mail/retriever/tests/unit/gmock.h>

namespace retriever {

static bool operator ==(const ResizeClient::GenurlParams& lhs, const ResizeClient::GenurlParams& rhs) {
    return lhs.url == rhs.url
        && lhs.width == rhs.width
        && lhs.height == rhs.height
        && lhs.crop == rhs.crop
        && lhs.noautoorient == rhs.noautoorient;
}

static std::ostream& operator <<(std::ostream& stream, const ResizeClient::GenurlParams& value) {
    using yamail::data::serialization::toJson;
    return stream << toJson(value);
}

std::ostream& operator <<(std::ostream& stream, const ResizeClient::Image& value);

} // namespace retriever

namespace {

using namespace testing;
using namespace retriever;

struct ResizeClientMock : public ResizeClient {
    MOCK_METHOD(std::string, genurl, (const TaskContextPtr&, const GenurlParams&), (const, override));
    MOCK_METHOD(boost::optional<Image>, get, (const TaskContextPtr&, std::string), (const, override));
};

struct ResizerTest : public Test {
    const std::string imageUrl = "image_url";
    const std::string resizeUrl = "resize_url";
    ResizeClientMock resizeClient;
    TaskContextPtr context;
    ResizeClient::GenurlParams params;

    ResizerTest() {
        params.url = imageUrl;
        params.width = std::numeric_limits<std::uint32_t>::max();
        params.height = std::numeric_limits<std::uint32_t>::max();
    }
};

TEST_F(ResizerTest, call_genurl_should_return_url) {
    EXPECT_CALL(resizeClient, genurl(context, params)).WillOnce(Return(resizeUrl));
    const auto result = Resizer().genurl(context, imageUrl, resizeClient);
    EXPECT_EQ(result, "resize_url");
}

TEST_F(ResizerTest, set_exif_rotate_false_then_call_genurl_should_call_resize_client_genurl_with_noautoorient_true_and_return_url) {
    params.noautoorient = true;
    EXPECT_CALL(resizeClient, genurl(context, params)).WillOnce(Return(resizeUrl));
    const auto result = Resizer().exifRotate(false).genurl(context, imageUrl, resizeClient);
    EXPECT_EQ(result, "resize_url");
}

TEST_F(ResizerTest, set_crop_rect_true_then_call_genurl_should_call_resize_client_genurl_with_crop_true_and_return_url) {
    params.crop = true;
    EXPECT_CALL(resizeClient, genurl(context, params)).WillOnce(Return(resizeUrl));
    const auto result = Resizer().cropRect(true).genurl(context, imageUrl, resizeClient);
    EXPECT_EQ(result, "resize_url");
}

TEST_F(ResizerTest, set_attachment_then_call_genurl_should_return_url_with_attachment) {
    EXPECT_CALL(resizeClient, genurl(context, params)).WillOnce(Return(resizeUrl));
    const auto result = Resizer().attachment("image.jpeg").genurl(context, imageUrl, resizeClient);
    EXPECT_EQ(result, "resize_url&attachment=image.jpeg");
}

TEST_F(ResizerTest, set_from_base64_false_then_call_genurl_should_call_resize_client_genurl_without_from_base64) {
    EXPECT_CALL(resizeClient, genurl(context, params)).WillOnce(Return(resizeUrl));
    const auto result = Resizer().fromBase64(false).genurl(context, imageUrl, resizeClient);
    EXPECT_EQ(result, "resize_url");
}

TEST_F(ResizerTest, set_from_base64_true_then_call_genurl_should_call_resize_client_genurl_with_from_base64) {
    EXPECT_CALL(resizeClient, genurl(context, params)).WillOnce(Return(resizeUrl));
    const auto result = Resizer().fromBase64(true).genurl(context, imageUrl, resizeClient);
    EXPECT_EQ(result, "resize_url&frombase64");
}

TEST_F(ResizerTest, set_size_then_call_genurl_should_call_resize_client_genurl_with_width_and_height_and_return_url) {
    params.width = 100;
    params.height = 200;
    EXPECT_CALL(resizeClient, genurl(context, params)).WillOnce(Return(resizeUrl));
    const auto result = Resizer().size(ImageSize {100, 200}).genurl(context, imageUrl, resizeClient);
    EXPECT_EQ(result, "resize_url");
}

} // namespace
