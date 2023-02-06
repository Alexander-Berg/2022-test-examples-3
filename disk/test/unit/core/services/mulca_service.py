from hamcrest import assert_that, calling, raises
from parameterized import parameterized

from mpfs.common.errors import MalformedAvatarsStidError
from mpfs.core.services.mulca_service import Mulca
from test.unit.base import NoDBTestCase


class MulcaServiceTestCase(NoDBTestCase):

    @parameterized.expand([
        ('get', 'ava:disk:6660:abcdefg', 'http://avatars.mdst.yandex.net/get-disk/6660/abcdefg/1280_nocrop?webp=true'),
        ('del', 'ava:disk:6660:abcdefg', 'http://avatars-int.mdst.yandex.net:13000/delete-disk/6660/abcdefg'),
        ('get', '320.yadisk:359371010.E134657:240XXX', 'http://storage.stm.yandex.net:10010/gate/get/320.yadisk:359371010.E134657:240XXX'),
        ('del', '320.yadisk:359371010.E134657:240XXX', 'http://storage.stm.yandex.net:10010/gate/del/320.yadisk:359371010.E134657:240XXX'),
    ])
    def test_get_storage_url_for_stid(self, action, stid, correct_url):
        assert correct_url == Mulca().build_url(action, stid)

    @parameterized.expand([
        ('ava:disk:1:2:3',),
        ('ava:disk:1:2:',),
        ('ava:disk:1',),
    ])
    def test_malformed_avatars_stid(self, stid):
        assert_that(calling(Mulca()._build_url_for_avatars).with_args('get', stid), raises(MalformedAvatarsStidError))
