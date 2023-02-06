import os
import yatest.common
import copy

ynode_path = yatest.common.binary_path('mail/nodejs/cpp/bin/node')
test_path = yatest.common.source_path('mail/nodejs/js/uatraits/test')
browser_path = yatest.common.source_path('mail/nodejs/js/uatraits/test/browser.xml')
profiles_path = yatest.common.source_path('mail/nodejs/js/uatraits/test/profiles.xml')
extra_path = yatest.common.source_path('mail/nodejs/js/uatraits/test/extra.xml')


class Test(object):
    def prepare_env(self):
        test_env = copy.deepcopy(os.environ)
        test_env.update({
            'BROWSER_PATH': browser_path,
            'PROFILES_PATH': profiles_path,
            'EXTRA_PATH': extra_path
        })
        return test_env

    def test_base(self, test_file):
        node_test_path = os.path.join(test_path, test_file)
        res = yatest.common.execute([ynode_path, node_test_path], env=self.prepare_env())
        assert res.exit_code == 0


def pytest_generate_tests(metafunc):
    tests = []
    for test_file in os.listdir(test_path):
        if test_file.endswith('js'):
            tests.append(test_file)
    metafunc.parametrize('test_file', tests)
