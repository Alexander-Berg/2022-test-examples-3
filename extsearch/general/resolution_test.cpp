#include <library/cpp/testing/unittest/registar.h>
#include <extsearch/video/vh/playlist_service/library/common/util.h>

/*
RESOLUTION ARRAY:
480x852
576x1024
720x1280
1080x1920
*/
namespace NVH::NPlaylistSerivce {
    Y_UNIT_TEST_SUITE(ResolitionsTests) {
        Y_UNIT_TEST(ChooseResolutionsTest) {
            auto InOutResolution = [](const std::pair<ui64, ui64> input, const std::pair<ui64, ui64> output) {
                const auto [h, w]= NPlaylistService::ChooseResolution(input.first, input.second);
                UNIT_ASSERT_VALUES_EQUAL(h, output.first);
                UNIT_ASSERT_VALUES_EQUAL(w, output.second);
            };
            InOutResolution({9999, 9999}, {1080, 1920});
            InOutResolution({0, 0}, {480, 852});
            InOutResolution({720, 1280}, {720, 1280});
            InOutResolution({720, 1024}, {576, 1024});
            InOutResolution({576 + 1, 1024 + 1}, {576, 1024});
        }
        Y_UNIT_TEST(FrameUrlCommonTest) {
            constexpr TStringBuf url = "https://avatars.mds.yandex.net/get-vh/5610734/111/orig";
            const auto& result = NPlaylistService::CreateFirstFrameUrl(url, 0, 0, true);
            UNIT_ASSERT_VALUES_EQUAL(result, "https://avatars.mds.yandex.net/get-vh/5610734/111");
        }
        Y_UNIT_TEST(FrameUrlForYabs) {
            constexpr TStringBuf url = "https://avatars.mds.yandex.net/get-vh/5610734/111/orig";
            const auto& result = NPlaylistService::CreateFirstFrameUrl(url, 0, 0, false);
            UNIT_ASSERT_VALUES_EQUAL(result, "https://avatars.mds.yandex.net/get-vh/5610734/111/orig");
        }
        Y_UNIT_TEST(FrameUrlWithResolutionTest) {
            constexpr TStringBuf url = "https://avatars.mds.yandex.net/get-vh/5610734/111/orig";
            const auto& result = NPlaylistService::CreateFirstFrameUrl(url, 1081, 1921, true);
            UNIT_ASSERT_VALUES_EQUAL(result, "https://avatars.mds.yandex.net/get-vh/5610734/111/1080x1920");
        }
        Y_UNIT_TEST(FrameUrlWithMinResolutionTest) {
            constexpr TStringBuf url = "https://avatars.mds.yandex.net/get-vh/5610734/111/orig";
            const auto& result = NPlaylistService::CreateFirstFrameUrl(url, 1, 1, true);
            UNIT_ASSERT_VALUES_EQUAL(result, "https://avatars.mds.yandex.net/get-vh/5610734/111/480x852");
        }
    }
}
