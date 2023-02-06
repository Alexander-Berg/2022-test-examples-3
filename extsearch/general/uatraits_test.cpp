#include "extsearch/video/vh/playlist_service/library/data_structures/protos/handle_by_uuid_structs.pb.h"
#include <library/cpp/testing/unittest/registar.h>
#include <extsearch/video/vh/playlist_service/library/common/util.h>

namespace NVH::NPlaylistSerivce {
    Y_UNIT_TEST_SUITE(UatraitsTest) {
        Y_UNIT_TEST(EmptyDeviceInfo) {
            NPlaylistService::TDeviceInfo device;
            device.SetUserAgent("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.174 YaBrowser/22.1.5.812 Yowser/2.5 Safari/537.36");
            NPlaylistService::PatchDeviceInfo(device);

            UNIT_ASSERT_VALUES_EQUAL(device.GetOSVersion(), "6.1");
            UNIT_ASSERT_VALUES_EQUAL(device.GetOperationSystem(), "Windows");
            UNIT_ASSERT_VALUES_EQUAL(device.GetBrowserType(), "YandexBrowser");
        }
        Y_UNIT_TEST(FullDeviceInfo) {
            NPlaylistService::TDeviceInfo device;
            device.SetOSVersion("10");
            device.SetOperationSystem("JOS");
            device.SetBrowserType("Firefox");
            device.SetUserAgent("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.174 YaBrowser/22.1.5.812 Yowser/2.5 Safari/537.36");
            NPlaylistService::PatchDeviceInfo(device);

            UNIT_ASSERT_VALUES_EQUAL(device.GetOSVersion(), "10");
            UNIT_ASSERT_VALUES_EQUAL(device.GetOperationSystem(), "JOS");
            UNIT_ASSERT_VALUES_EQUAL(device.GetBrowserType(), "Firefox");
        }

        Y_UNIT_TEST(EmptyUserAgent) {
            NPlaylistService::TDeviceInfo device;
            device.SetOSVersion("10");
            device.SetOperationSystem("JOS");
            device.SetBrowserType("Firefox");
            NPlaylistService::PatchDeviceInfo(device);

            UNIT_ASSERT_VALUES_EQUAL(device.GetOSVersion(), "10");
            UNIT_ASSERT_VALUES_EQUAL(device.GetOperationSystem(), "JOS");
            UNIT_ASSERT_VALUES_EQUAL(device.GetBrowserType(), "Firefox");
        }
    }
}
