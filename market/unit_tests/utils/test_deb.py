import os
from unittest import TestCase

import mock

from lib.tests import test_utils
from lib.utils import deb


def check_do_shell(expected, method, *args):
    # type: (list, callable, iter) -> None
    with mock.patch('lib.utils.deb.lib.common.do_shell_once') as mock_do_shell:
        method(*args)
        mock_do_shell.assert_called_once_with(expected)


class TestDeb(TestCase):
    def test_debuild(self):
        check_do_shell(['debuild'], deb.debuild)

    def test_debuild_with_params(self):
        expect_params = ['debuild', '-eJAVA_HOME', '-eGRADLE_USER_HOME', '--no-tgz-check', '-us', '-uc']

        check_do_shell(expect_params,
                       deb.debuild, ['-eJAVA_HOME', '-eGRADLE_USER_HOME', '--no-tgz-check', '-us', '-uc'])

    def test_debsign(self):
        check_do_shell(['debsign', '-krobot-mrk-teamcity@yandex-team.ru', '--re-sign'],
                       deb.debsign, 'robot-mrk-teamcity@yandex-team.ru')

    def test_dch_i_no_changes(self):
        check_do_shell(
            ['dch', '--vendor=debian', '--release-heuristic=unstable', '--no-auto-nmu',
             '-i', '[Teamcity] Update changelog version'],
            deb.dch_i, [])

    def test_dch_i_single_change(self):
        check_do_shell(['dch', '--vendor=debian', '--release-heuristic=unstable', '--no-auto-nmu',
                        '-i', 'Just one change'], deb.dch_i,
                       ['Just one change'])

    def test_dch_i_few_changes(self):
        with mock.patch('lib.utils.deb.lib.common.do_shell_once') as mock_do_shell:
            deb.dch_i(['First change', 'Second change', 'Third change'])

            expected_calls = [
                ['dch', '--vendor=debian', '--release-heuristic=unstable', '--no-auto-nmu', '-i', 'First change'],
                ['dch', '-a', 'Second change'],
                ['dch', '-a', 'Third change']
            ]

            actual_calls = [params_list[0] for params_list, params_dict in mock_do_shell.call_args_list]
            self.assertListEqual(expected_calls, actual_calls)

    def test_debrelease(self):
        check_do_shell(['debrelease', '--to', 'market-common', '--nomail'], deb.debrelease, 'market-common')

    def test_get_deb_file_location(self):
        expected = os.path.join(test_utils.COMMON_PROJECT_PATH, 'debian', 'changelog')
        actual = deb.get_deb_file_location(test_utils.COMMON_PROJECT_PATH, deb.CHANGELOG_FILE)
        self.assertEqual(expected, actual)

        expected = os.path.join(test_utils.GRADLE_MULTI_APP_PROJECT_PATH, 'debian', 'rules')
        actual = deb.get_deb_file_location(test_utils.GRADLE_MULTI_APP_PROJECT_PATH, deb.RULES_FILE)
        self.assertEqual(expected, actual)

    def test_get_package_name_and_version(self):
        with mock.patch('lib.utils.deb._get_changelog_first_row') as mock_ch_log_row:
            mock_ch_log_row.return_value = 'yandex-market-project-builder-gradle-single (1.0-1) unstable; urgency=low'
            actual = deb.get_package_name(test_utils.GRADLE_SINGLE_PROJECT_PATH)
            self.assertEqual('yandex-market-project-builder-gradle-single', actual)

            actual = deb.get_package_version(test_utils.GRADLE_SINGLE_PROJECT_PATH)
            self.assertEqual('1.0-1', actual)

    def test_project_has_deb_rules(self):
        self.assertTrue(deb.project_has_deb_rules(test_utils.GRADLE_SINGLE_PROJECT_PATH))
        self.assertFalse(deb.project_has_deb_rules(test_utils.GRADLE_MULTI_PROJECT_PATH))

    def test_get_version_changelog(self):
        actual = deb.get_version_changelog(test_utils.GRADLE_MULTI_APP_PROJECT_PATH)
        self.assertEqual(['* Initial release'], actual)

        actual = deb.get_version_changelog(test_utils.GRADLE_MULTI_DEB_PROJECT_PATH)
        self.assertEqual(['* New version', '* New cool features'], actual)
