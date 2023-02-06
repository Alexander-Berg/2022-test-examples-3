# coding=utf-8
from unittest import TestCase

import mock
from datetime import datetime

from lib.tests import test_utils
from lib.utils import gradle
from lib.utils.git import Commit


def check_wrapper_run(tasks_and_params, method, is_sub_mod, output, *args):
    # type: (list, callable, iter) -> any
    with mock.patch('lib.utils.gradle.lib.common.do_shell_once') as mock_do_shell:
        mock_do_shell.return_value = output
        method_result = method(*args)
        wrapper = '.' + gradle.GRADLE_WRAPPER if is_sub_mod else gradle.GRADLE_WRAPPER
        mock_do_shell.assert_called_once_with([wrapper] + tasks_and_params + ['--stacktrace'])
        return method_result


class TestGradle(TestCase):
    maxDiff = None

    def test_clean_and_refresh(self):
        tasks = ['clean', '--refresh-dependencies', '--recompile-scripts', '--rerun-tasks']
        check_wrapper_run(tasks, gradle.clean_and_refresh, False, [])

    def test_projects(self):
        check_wrapper_run(['projects'], gradle.projects, False, [])

    def test_path_has_wrapper(self):
        self.assertTrue(gradle.path_has_wrapper(test_utils.GRADLE_MULTI_PROJECT_PATH))
        self.assertTrue(gradle.path_has_wrapper(test_utils.GRADLE_SINGLE_PROJECT_PATH))
        self.assertFalse(gradle.path_has_wrapper(test_utils.COMMON_PROJECT_PATH))

    def test_project_without_wrapper(self):
        try:
            gradle.path_has_wrapper(test_utils.GRADLE_WITHOUT_WRAPPER)
            self.fail("Method call must raise error")
        except RuntimeError as e:
            self.assertEqual("Gradle wrapper 'gradlew' not founded in gradle project", e.message)

    def test_dist_tar(self):
        check_wrapper_run([':my-cool-module:distTar'], gradle.dist_tar, True, [], 'my-cool-module', True)

    def test_dist_tar_without_tests(self):
        check_wrapper_run([':my-cool-module:distTar', '-x', 'test'], gradle.dist_tar, True, [], 'my-cool-module',
                          False)

    def test_update_change_log(self):
        expected = ':my-cool-module:generateChangelogMessage :my-cool-module:printChangelogCommits :my-cool-module:generateChangelogVersion ' \
                   '--versionStrategy my-cool-strategy :my-cool-module:updateChangelog'

        output = """
> Configure project :
Check node version
v7.10.0
Use local node

> Task :tsum-tms:printChangelogCommits
commit 5e91a8b1c7eb9fbaedb2a1d7be3f1f241b10e789
Author: Anton Sukhonosenko <algebraic@yandex-team.ru>
Date:   Tue Jan 16 18:46:42 2018 +0300

    MARKETINFRA-2382: Передача ревизии из DeliveryPipelineParams в тимсити сборку

commit 067e08fcbfa547e60d9abc3b85e17520ba6264d4
Author: Timur Shakurov <timursha@yandex-team.ru>
Date:   Mon Jan 15 15:09:10 2018 +0300

    Проставляем компоненты в релизный тикет
    
    + Возможность писать тесты на джобы
    + Выплил все вхождения депрекейтед BuildJavaPackageJob



BUILD SUCCESSFUL in 3s
4 actionable tasks: 4 executed""".splitlines()

        changelog_entries = check_wrapper_run(expected.split(' '), gradle.update_change_log, True, output,
                                              'my-cool-module', 'my-cool-strategy', None)

        self.assertListEqual([
            Commit('5e91a8b1c7eb9fbaedb2a1d7be3f1f241b10e789', date=datetime(2018, 1, 16, 15, 46, 42),
                   message_rows=['MARKETINFRA-2382: Передача ревизии из DeliveryPipelineParams в тимсити сборку'],
                   author='Anton Sukhonosenko <algebraic@yandex-team.ru>'),
            Commit('067e08fcbfa547e60d9abc3b85e17520ba6264d4', date=datetime(2018, 1, 15, 12, 9, 10),
                   message_rows=['Проставляем компоненты в релизный тикет',
                                 '',
                                 '+ Возможность писать тесты на джобы',
                                 '+ Выплил все вхождения депрекейтед BuildJavaPackageJob'],
                   author='Timur Shakurov <timursha@yandex-team.ru>')
        ], changelog_entries)

    def test_update_change_log_empty(self):
        expected = ':my-cool-module:generateChangelogMessage :my-cool-module:printChangelogCommits :my-cool-module:generateChangelogVersion ' \
                   '--versionStrategy my-cool-strategy :my-cool-module:updateChangelog'

        output = """
> Configure project :
Check node version
v7.10.0
Use local node

> Task :tsum-tms:printChangelogCommits


BUILD SUCCESSFUL in 3s
4 actionable tasks: 4 executed""".splitlines()

        changelog_entries = check_wrapper_run(expected.split(' '), gradle.update_change_log, True, output,
                                              'my-cool-module', 'my-cool-strategy', None)

        self.assertListEqual([], changelog_entries)

    def test_update_change_log_with_timestamp(self):
        expected = ':my-cool-module:generateChangelogMessage --previousChangelogCommitMaxTimestamp 100500 ' \
                   ':my-cool-module:printChangelogCommits ' \
                   ':my-cool-module:generateChangelogVersion --versionStrategy my-cool-strategy ' \
                   ':my-cool-module:updateChangelog'

        check_wrapper_run(expected.split(' '), gradle.update_change_log, True, [], 'my-cool-module', 'my-cool-strategy',
                          None, 100500)

    def test_update_change_log_with_since_commit_hash(self):
        expected = ':my-cool-module:generateChangelogMessage --sinceCommitHash cd58e68 ' \
                   ':my-cool-module:printChangelogCommits ' \
                   ':my-cool-module:generateChangelogVersion --versionStrategy my-cool-strategy ' \
                   ':my-cool-module:updateChangelog'

        check_wrapper_run(expected.split(' '), gradle.update_change_log, True, [], 'my-cool-module', 'my-cool-strategy',
                          'cd58e68', None)

    def test_sandbox_resource_type(self):
        with mock.patch('lib.utils.gradle.lib.common.do_shell_once') as mock_do_shell:
            mock_do_shell.return_value = [
                'Starting a Gradle Daemon (subsequent builds will be faster)'
                ':sandboxResourceType',
                'Sandbox resource type:',
                'MARKET_TEST_APP_BIN',
                '',
                'BUILD SUCCESSFUL',
                '',
                'Total time: 15.292 secs'
            ]
            r_type = gradle.sandbox_resource_type('my-cool-module')
            mock_do_shell.assert_called_once_with(
                ['.' + gradle.GRADLE_WRAPPER, ':my-cool-module:sandboxResourceType', '--stacktrace'])

            self.assertEqual('MARKET_TEST_APP_BIN', r_type)

    def test_extract_gradle_module_names(self):
        gradle_out = ''':projects

------------------------------------------------------------
Root project
------------------------------------------------------------

Root project 'market-health'
+--- Project ':clickphite'
+--- Project ':config-cs-clickphite'
+--- Project ':config-cs-logshatter'
+--- Project ':health-utils'
\--- Project ':logshatter'

To see a list of the tasks of a project, run gradlew <project-path>:tasks
For example, try running gradlew :clickphite:tasks

BUILD SUCCESSFUL

Total time: 1.363 secs
'''
        expected = ['clickphite', 'config-cs-clickphite', 'config-cs-logshatter', 'health-utils', 'logshatter']
        self.assertListEqual(expected, gradle._extract_gradle_module_names(gradle_out))

    def test_extract_gradle_module_names_single(self):
        gradle_out = ''':projects

Root project

Root project 'test-app'

No sub-projects
To see a list of the tasks of a project, run gradlew <project-path>:tasks
For example, try running gradlew :tasks


BUILD SUCCESSFUL


Total time: 9.249 secs
'''
        self.assertListEqual(['test-app'], gradle._extract_gradle_module_names(gradle_out))

    def test_extract_resource_type(self):
        command_out = '''Starting a Gradle Daemon (subsequent builds will be faster)
:sandboxResourceType

Sandbox resource type:

MARKET_TEST_APP_BIN



BUILD SUCCESSFUL



Total time: 0.993 secs
'''

        self.assertEqual('MARKET_TEST_APP_BIN', gradle._extract_resource_type(command_out))
