import unittest

from crypta.graph.v1.python.v2.yuid_apps import appid_crc32


class TestCrc32(unittest.TestCase):
    def test_crc32(self):
        self.assertEquals(appid_crc32("mediam.music.player", "Android"), 404356791)
        self.assertEquals(appid_crc32("com.vkontakte.android", "IOS"), 3430459905)
        self.assertEquals(appid_crc32("com.cleanmaster.mguard", "SomeShop"), 3350695337)
