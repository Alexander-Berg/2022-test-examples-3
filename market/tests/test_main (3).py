import unittest

from sb_market_debuilder.main import get_svn_url, get_git_url, _exec


class TestMain(unittest.TestCase):
    def test_exec(self):
        with self.assertRaises(Exception):
            _exec('exit 1')

        self.assertEqual(_exec('echo "line1\nline2"'), ['line1', 'line2'])

    def test_get_svn_url(self):
        url = 'svn+ssh://arcadia.yandex.ru/arc/trunk/arcadia'

        def _exec(cmd):
            output = [
                'Path: .',
                'Working Copy Root Path: /Users/d3rp/Projects/arcadia',
                'URL: {}'.format(url)
            ]

            return output

        self.assertEqual(get_svn_url(_exec), url)

    def test_get_git_url(self):
        url = 'git@github.yandex-team.ru:cs-admin/ansible-juggler-configs.git'

        def _exec(cmd):
            output = ['{}'.format(url)]
            return output

        self.assertEqual(get_git_url(_exec), url)
