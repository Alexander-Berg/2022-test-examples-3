from attrdict import AttrDict
from nose_parameterized import parameterized

from mpfs.common.static import tags
from mpfs.platform.handlers import BasePlatformHandler
from test.unit.base import NoDBTestCase


class DsiablingAuthMethods(NoDBTestCase):

    @parameterized.expand([
        ('single_method', 'test1', {'test1'}),
        ('single_method_sith_spaces', '   test1\t', {'test1'}),
        ('several_methods', 'test1;test2;test3', {'test1', 'test2', 'test3'}),
        ('repeats', 'test1;test2;test2', {'test1', 'test2'}),
        ('empty_and_spaces_methods', 'test1;\t;   ;;', {'test1'}),
        ('empty_header', '', set()),
    ]
    )
    def test_correct(self, case_name, header, correct_answer):
        request = AttrDict({
            'mode': tags.platform.INTERNAL,
            'raw_headers': {'X-Disable-Auth-Methods': header}
        })
        assert BasePlatformHandler._get_disabled_auth_methods_names(request) == correct_answer

    def test_no_header(self):
        request = AttrDict({
            'mode': tags.platform.INTERNAL,
            'raw_headers': {},
        })
        assert BasePlatformHandler._get_disabled_auth_methods_names(request) == set()

    def test_external(self):
        request = AttrDict({
            'mode': tags.platform.EXTERNAL,
            'raw_headers': {'X-Disable-Auth-Methods': 'test1'},
        })
        assert BasePlatformHandler._get_disabled_auth_methods_names(request) == set()
