# encoding=utf-8

from unittest import TestCase

import mock

from lib.utils import git


class TestGit(TestCase):
    def test_log(self):
        with mock.patch('lib.utils.deb.lib.common.do_shell_once') as mock_do_shell:
            git.log()
            mock_do_shell.assert_called_once_with(['git', 'log'], False)

    def test_log_with_params(self):
        with mock.patch('lib.utils.deb.lib.common.do_shell_once') as mock_do_shell:
            git.log(path='some-path', limit=10, since_commit='dfsfhuw92381')
            mock_do_shell.assert_called_once_with(['git', 'log', '-n', '10', 'dfsfhuw92381..HEAD', 'some-path'], False)

    def test_log_with_out(self):
        with mock.patch('lib.utils.deb.lib.common.do_shell_once') as mock_do_shell:
            mock_do_shell.return_value = [
                'commit 77de64c19098c55c7fadd220dc77b21941b06217',
                'Author: teamcity <teamcity@yandex-team.ru>',
                '',
                'Date:   Fri Apr 28 13:34:22 2017 +0300',
                '',
                '    yandex-market-teamcity-scripts=1.0-29 changelog',
                ''
            ]
            log = git.log()
            mock_do_shell.assert_called_once_with(['git', 'log'], False)

            self.assertEqual(1, len(log))
            self.assertEqual('77de64c19098c55c7fadd220dc77b21941b06217', log[0].hash_id)
            self.assertEqual('teamcity <teamcity@yandex-team.ru>', log[0].author)
            self.assertEqual('2017-04-28 10:34:22', str(log[0].date))  # because it's UTC date
            self.assertListEqual(['yandex-market-teamcity-scripts=1.0-29 changelog'], log[0].message_rows)

    def test_tag(self):
        with mock.patch('lib.utils.deb.lib.common.do_shell_once') as mock_do_shell:
            git.tag('tag-name', 'message')
            mock_do_shell.assert_called_once_with(['git', 'tag', '-a', 'tag-name', '-m', 'message'], True)

    def test_commit(self):
        with mock.patch('lib.utils.deb.lib.common.do_shell_once') as mock_do_shell:
            git.commit('some-message', 'some-path')
            mock_do_shell.assert_called_once_with(['git', 'commit', '-m', 'some-message', 'some-path'], True)

    def test_push(self):
        with mock.patch('lib.utils.deb.lib.common.do_shell_once') as mock_do_shell:
            git.push('some-params')
            mock_do_shell.assert_called_once_with(['git', 'push', 'some-params'], True)

    def test_parse_log_rows(self):
        log_out = [
            'commit fdc17e7a898e5d4c74f5ecf40204b6fdab49fb9e',
            'Author: Vlad Vinogradov <vladvin@yandex-team.ru>',
            'Date:  Wed Jun 7 10:50:28 2017 +0300',
            '',
            '    MARKETINFRA-1101: Сборка не java-проектов в Market Java Deploy project',
            '    changes in builder',
            '',
            'commit cbd3f912ac3ef3657e7fc1feba309a3e92a4a4c1',
            'Author: Vlad Vinogradov <vladvin@yandex-team.ru>',
            'Date:   Wed Jun 7 10:50:09 2017 +0300',
            '',
            '    MARKETINFRA-1101: Сборка не java-проектов в Market Java Deploy project',
            '    changes in project',
            ''
        ]
        commits = git.parse_log_rows(log_out)
        self.assertEqual(2, len(commits))
        self.assertEqual('fdc17e7a898e5d4c74f5ecf40204b6fdab49fb9e', commits[0].hash_id)
        self.assertEqual('Vlad Vinogradov <vladvin@yandex-team.ru>', commits[0].author)
        self.assertEqual('2017-06-07 07:50:28', str(commits[0].date))  # UTC date
        self.assertListEqual(['MARKETINFRA-1101: Сборка не java-проектов в Market Java Deploy project',
                              'changes in builder'], commits[0].message_rows)

        self.assertEqual('cbd3f912ac3ef3657e7fc1feba309a3e92a4a4c1', commits[1].hash_id)
        self.assertEqual('Vlad Vinogradov <vladvin@yandex-team.ru>', commits[1].author)
        self.assertEqual('2017-06-07 07:50:09', str(commits[1].date))  # UTC date
        self.assertListEqual(['MARKETINFRA-1101: Сборка не java-проектов в Market Java Deploy project',
                              'changes in project'], commits[1].message_rows)

    def test_extract_repo_name(self):
        command_out = '''origin	git@github.yandex-team.ru:market-infra/test-app.git (fetch)
origin	git@github.yandex-team.ru:market-infra/test-app.git (push)
'''
        self.assertEqual('test-app', git._extract_repo_name(command_out))
