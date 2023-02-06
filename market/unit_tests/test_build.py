import os
from datetime import datetime
from unittest import TestCase

import mock

from lib import build
from lib.build import BuildResult, ChangelogPushMoment
from lib.project import Module, Project
from lib.tests import test_utils
from lib.utils import deb
from lib.utils.git import Commit
from lib.utils.sandbox import SandboxResource, SandboxTicket


class TestProjectBuilder(TestCase):
    def setUp(self):
        self.cwd = os.getcwd()
        self.v_strategy = 'DEFAULT'
        self.commit_ch_lpg = True
        self.create_v_tags = True
        self.deb_repo = 'common'
        self.custom_message = 'My cool comment'
        self.rpm_dist_paths = ['host1:/repo/cool-repo/incoming/6/', 'host2:/repo/not-cool-repo/incoming/4/']

        self.project_builder = build.ProjectBuilder(self.v_strategy, self.create_v_tags,
                                                    self.deb_repo, self.rpm_dist_paths, 'My cool comment', None, None,
                                                    ChangelogPushMoment.AFTER_BUILD, True, False, False)

    def test_build_changelog_message(self):
        mod_and_ver = [('module1', '1.0'), ('module2', '1.1')]

        b_message = build.ProjectBuilder._build_changelog_message
        self.assertEqual('[Teamcity] module1 changelog 1.0', b_message(mod_and_ver[:1]))

        two_mod_expected = '[Teamcity] Update changelogs\n\nmodule1 changelog 1.0\nmodule2 changelog 1.1'
        self.assertEqual(two_mod_expected, b_message(mod_and_ver))

    def test_rpm_version(self):
        with mock.patch('lib.build.ModuleBuilder') as mock_module_builder:
            package_name = 'yandex-mbi-db'
            package_version = '1.689-2017.4.140MBI-24990+2'
            mock_module_builder.get_pack_name_and_version.return_value = (package_name, package_version)
            module = Module(package_name, Module.Type.COMMON)
            module.is_rpm = True
            mock_module_builder.build_module = module
            collect_package_names_and_versions = build.ProjectBuilder._collect_package_names_and_versions

            self.assertEqual({(package_name, '1.689.2017.4.140MBI.24990.2')},
                             collect_package_names_and_versions([mock_module_builder]))

    @mock.patch('lib.build.os.chdir')
    @mock.patch('lib.build.os.listdir')
    @mock.patch('lib.utils.tsum.send_report_to_tsum')
    @mock.patch('lib.build.rpm.get_rpm_info')
    @mock.patch('lib.build.sandbox')
    def test_do_build(self, mock_sandbox, mock_rpm_info, mock_tsum_report, mock_listdir, mock_chdir):
        prj, build_mod, not_build_mod = test_utils.build_simple_project()

        mod_version = '2.1'
        test_mod_and_version = (build_mod.name, mod_version)
        rpm_package_and_version_list = [test_mod_and_version, (build_mod.name + '-testing', mod_version)]
        mock_rpm_info.return_value = rpm_package_and_version_list
        with mock.patch('lib.build.GradleModuleBuilder') as mock_builder, mock.patch('lib.build.git') as mock_git:
            tar_name = build_mod.name + '.tar'
            mock_listdir.return_value = [tar_name]
            sb_resource = SandboxResource(build_mod.sandbox_res_type, 1234, mod_version, SandboxTicket('url', 111))
            mock_sandbox.create_sandbox_resource.return_value = sb_resource

            g_instance = mock_builder.return_value
            g_instance.get_pack_name_and_version.return_value = test_mod_and_version
            type(g_instance).build_module = mock.PropertyMock(return_value=build_mod)

            result = self.project_builder._do_build(prj)
            chdir_calls = map(lambda a: a[0][0], mock_chdir.call_args_list)
            chdir_expected_calls = [
                build_mod.get_path(),  # updating changelogs
                prj.path,  # go back
                build_mod.get_path(),
                os.path.join(build_mod.get_path() + '/build/distributions/'),
                build_mod.get_path(),
                prj.path
            ]
            self.assertListEqual(chdir_calls, chdir_expected_calls)

            mock_listdir.assert_called_once()
            mock_sandbox.create_sandbox_resource.assert_called_once_with(
                build_mod.sandbox_res_type, tar_name, build_mod.name, mod_version, self.custom_message)

            mock_tsum_report.assert_called_once_with(tar_name, build_mod.sandbox_res_type,
                                                     str(sb_resource.ticket.number))

            g_instance.build_deb.assert_called_once_with()
            g_instance.build_rpm.assert_called_once()
            g_instance.build_sandbox.assert_called_once()

            self.assertEqual(4, len(result.package_build_results))  # 1 deb + 1 sandbox + 2 rpm
            deb_results = filter(lambda r: r.pack_type == BuildResult.PackageType.DEB, result.package_build_results)
            rpm_results = filter(lambda r: r.pack_type == BuildResult.PackageType.RMP, result.package_build_results)
            sb_results = filter(lambda r: r.pack_type == BuildResult.PackageType.SANDBOX, result.package_build_results)

            def check_results(b_results):
                for b_result in b_results:
                    r = b_result.result
                    pack_name = b_result.module_name if isinstance(r, SandboxResource) else r.package_name
                    self.assertEqual((pack_name, r.version), test_mod_and_version)
                    self.assertEqual(b_result.module_name, build_mod.name)

            def check_rpm_results(b_results):
                self.assertEqual(len(rpm_package_and_version_list), len(b_results))
                for b_result, expected_package_name_and_version in zip(b_results, rpm_package_and_version_list):
                    r = b_result.result
                    self.assertEqual((r.package_name, r.version), expected_package_name_and_version)
                    self.assertEqual(b_result.module_name, build_mod.name)

            check_results(deb_results)
            check_rpm_results(rpm_results)
            check_results(sb_results)

    def test_do_after_build(self):
        package_names_and_versions = [('deb-package', '1.1'), ('my_module', '2.1'), ('rpm-package', '1.1'),
                                      ('rpm-package-for-test', '1.1')]

        with mock.patch('lib.build.git') as mock_git:
            mock_git.status.return_value = 'Not clean status'

            self.project_builder._tag_commit_push(package_names_and_versions)
            expected_tags = [
                ('deb-package=1.1', 'Build deb-package=1.1'),
                ('my_module=2.1', "Build my_module=2.1"),
                ('rpm-package=1.1', "Build rpm-package=1.1"),
                ('rpm-package-for-test=1.1', 'Build rpm-package-for-test=1.1')
            ]
            test_utils.check_method_calls(self, mock_git.tag, expected_tags)

            expected_commit = [('[Teamcity] Update changelogs\n\n'
                                'deb-package changelog 1.1\n'
                                'my_module changelog 2.1\n'
                                'rpm-package changelog 1.1\n'
                                'rpm-package-for-test changelog 1.1',
                                '*debian/changelog')]
            test_utils.check_method_calls(self, mock_git.commit, expected_commit)
            mock_git.push.c('--follow-tags')

    @mock.patch('lib.build.rpm.upload_to_dist')
    @mock.patch('lib.build.deb.debrelease')
    @mock.patch('lib.build.deb.fix_permissions')
    def test_upload_to_repositories(self, mock_fix_permissions, mock_debrelease, mock_rpm_upload):
        project_path = '/path_to_project'

        mod_name, mod_path = 'my_module', '/path_to_project/module_subpath'
        prj = test_utils.build_simple_project()[0]

        with mock.patch('lib.build.os.chdir') as mock_chdir:
            self.project_builder._upload_to_repositories(prj)

            chdir_calls = map(lambda a: a[0][0], mock_chdir.call_args_list)
            chdir_expected_calls = [mod_path, project_path]
            self.assertListEqual(chdir_calls, chdir_expected_calls)

            mock_fix_permissions.assert_called_once_with(project_path, mod_path)
            mock_debrelease.assert_called_once_with(self.deb_repo)
            mock_rpm_upload.assert_called_once_with(project_path, self.rpm_dist_paths)

    def tearDown(self):
        os.chdir(self.cwd)


class TestModuleBuilder(TestCase):
    def setUp(self):
        self.mod_path = test_utils.COMMON_PROJECT_PATH
        self.mod = Module('common', Module.Type.COMMON)
        self.builder = build.ModuleBuilder(self.mod, 'DEFAULT')
        self.mod._path = self.mod_path

    def test_get_pack_name_and_version(self):
        with mock.patch('lib.build.deb') as mock_deb:
            self.builder.get_pack_name_and_version()
            self.assertEqual(2, len(mock_deb.mock_calls))
            mock_deb.get_package_name.assert_called_once_with(self.mod_path)
            mock_deb.get_package_version.assert_called_once_with(self.mod_path)

    def test_update_changelog_fail(self):
        with mock.patch('lib.utils.git.log') as mock_log:
            mock_log.return_value = None
            try:
                self.builder.update_changelog()
                self.fail()
            except RuntimeError:
                changelog_path = deb.get_deb_file_location(self.mod_path, deb.CHANGELOG_FILE)
                mock_log.assert_called_once_with(path=changelog_path, limit=1, before_timestamp=None)

    def test_update_changelog(self):
        log_calls = []

        def git_log(**kvargs):
            log_calls.append(kvargs)
            return _simple_two_commit_log(kvargs)

        with mock.patch('lib.build.git') as mock_git:
            mock_git.log = git_log

            with mock.patch('lib.build.deb.dch_i') as mock_dch_i:
                self.builder.update_changelog()

                mock_dch_i.assert_called_once_with(['Second commit first message\nSecond commit second message'])

        expected_calls = [
            {'path': deb.get_deb_file_location(self.mod_path, deb.CHANGELOG_FILE), 'limit': 1,
             'before_timestamp': None},
            {'path': self.mod_path, 'since_commit': '0'}
        ]
        self.assertListEqual(expected_calls, log_calls)

    def test_update_changelog_since_commit(self):
        log_calls = []

        def git_log(**kvargs):
            log_calls.append(kvargs)
            return _simple_two_commit_log(kvargs)

        with mock.patch('lib.build.git') as mock_git:
            mock_git.log = git_log

            with mock.patch('lib.build.deb.dch_i') as mock_dch_i:
                # act
                changelog_commits = self.builder.update_changelog(since_commit_hash='0')

                # assert
                mock_dch_i.assert_called_once_with(['Second commit first message\nSecond commit second message'])

        expected_calls = [
            {'path': self.mod_path, 'since_commit': '0'}
        ]
        self.assertListEqual(expected_calls, log_calls)

        expected_changelog_commits = [
            Commit('1', ['Second commit first message', 'Second commit second message'],
                   date=datetime(2017, 8, 2), author='Anton Sukhonosenko <algebraic@yandex-team.ru>')
        ]
        self.assertListEqual(expected_changelog_commits, changelog_commits)

    def test_update_changelog_using_timestamp(self):
        log_calls = []

        def git_log(**kvargs):
            log_calls.append(kvargs)
            return _simple_two_commit_log(kvargs)

        timestamp = (datetime(2017, 8, 1, hour=12, minute=0, second=0) - datetime(1970, 1, 1)).total_seconds()
        with mock.patch('lib.build.git') as mock_git:
            mock_git.log = git_log

            with mock.patch('lib.build.deb.dch_i') as mock_dch_i:
                # act
                changelog_commits = self.builder.update_changelog(prev_changelog_commit_max_timestamp=timestamp)

                # assert
                mock_dch_i.assert_called_once_with(['Second commit first message\nSecond commit second message'])

        expected_calls = [
            {'path': deb.get_deb_file_location(self.mod_path, deb.CHANGELOG_FILE), 'limit': 1,
             'before_timestamp': timestamp},
            {'path': self.mod_path, 'since_commit': '0'}
        ]
        self.assertListEqual(expected_calls, log_calls)

        expected_changelog_commits = [
            Commit('1', ['Second commit first message', 'Second commit second message'],
                   date=datetime(2017, 8, 2), author='Anton Sukhonosenko <algebraic@yandex-team.ru>')
        ]
        self.assertListEqual(expected_changelog_commits, changelog_commits)

    def test_get_sandbox_res_type(self):
        with mock.patch('lib.utils.sandbox.read_sandbox_parameters') as mock_sandbox_params:
            mock_sandbox_params.return_value = {'resource_type': 'resource_type_value'}
            r_type = self.builder.get_sandbox_res_type()
            mock_sandbox_params.assert_called_once_with(self.mod_path)
            self.assertEqual('resource_type_value', r_type)

    def test_build_deb(self):
        with mock.patch('lib.build.deb') as mock_deb:
            self.builder.build_deb()
            self.assertEqual(2, len(mock_deb.mock_calls))
            mock_deb.debuild.assert_called_once_with(['--no-tgz-check', '-krobot-mrk-teamcity@yandex-team.ru'])
            mock_deb.debsign.assert_called_once_with('robot-mrk-teamcity@yandex-team.ru')


class TestGradleModuleBuilder(TestCase):
    def setUp(self):
        self.mod_path = test_utils.COMMON_PROJECT_PATH
        self.mod_name = 'common'
        self.mod = Module(self.mod_name, Module.Type.GRADLE)
        self.strategy = 'DEFAULT'
        self.builder = build.GradleModuleBuilder(self.mod, self.strategy, True)
        self.mod._path = self.mod_path

        prj = Project('path')
        prj.check_and_add_module(self.mod)

    def test_get_sandbox_res_type(self):
        with mock.patch('lib.build.gradle.sandbox_resource_type') as mock_res_type:
            mock_res_type.return_value = 'resource_type'
            r_type = self.builder.get_sandbox_res_type()
            self.assertEqual(1, mock_res_type.call_count)
            mock_res_type.assert_called_once_with('')
            self.assertEqual('resource_type', r_type)

    def test_update_changelog(self):
        with mock.patch('lib.build.gradle.update_change_log') as mock_upd_log:
            self.builder.update_changelog()
            self.assertEqual(1, mock_upd_log.call_count)
            mock_upd_log.assert_called_once_with('', self.strategy, since_commit=None,
                                                 prev_changelog_commit_max_timestamp=None)

    def test_build_sandbox(self):
        with mock.patch('lib.build.gradle.dist_tar') as mock_dist_tar:
            self.builder.build_sandbox()
            self.assertEqual(1, mock_dist_tar.call_count)
            mock_dist_tar.assert_called_once_with('', True)

    def test_build_rpm_after_debuild(self):
        self.builder.deb_is_built = True
        self.builder.build_deb = self.fail
        self.builder.build_rpm()

    def test_build_deb(self):
        with mock.patch('lib.build.deb') as mock_deb:
            self.builder.build_deb()
            self.assertEqual(2, len(mock_deb.mock_calls))
            mock_deb.debuild.assert_called_once_with(
                ['--preserve-env', '--no-tgz-check', '-us', '-uc'])
            mock_deb.debsign.assert_called_once_with("robot-mrk-teamcity@yandex-team.ru")
            self.assertTrue(self.builder.deb_is_built)


def _simple_two_commit_log(kvargs):
    """
    :rtype: list of Commit
    """
    first_commit = Commit('0', ['First commit first message', 'First commit second message'],
                          date=datetime(2017, 8, 1), author='Anton Sukhonosenko <algebraic@yandex-team.ru>')
    second_commit = Commit('1', ['Second commit first message', 'Second commit second message'],
                           date=datetime(2017, 8, 2), author='Anton Sukhonosenko <algebraic@yandex-team.ru>')
    commits = [second_commit, first_commit]

    before_timestamp = kvargs.get('before_timestamp')
    if before_timestamp:
        commits = filter(lambda commit: commit.date < datetime.fromtimestamp(before_timestamp), commits)

    since_commit = kvargs.get('since_commit')
    if since_commit:
        commits = filter(lambda commit: int(commit.hash_id) > int(since_commit), commits)

    if kvargs.get('limit') == 1 and kvargs.get('path'):
        # asking about changelog commit
        commits = [first_commit]

    return commits
