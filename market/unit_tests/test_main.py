# coding=utf-8
import copy
from unittest import TestCase

import mock
from datetime import datetime

from lib import main
from lib.build import ChangelogPushMoment, ProjectBuildResult
from lib.main import TargetSettings
from lib.project import Project, Module
from lib.tests import test_utils
from lib.utils import git
from lib.utils.git import Commit


class TestMain(TestCase):
    def test_str_to_bool(self):
        self.assertTrue(main._str_to_bool("true"))
        self.assertTrue(main._str_to_bool("TRUE"))
        self.assertFalse(main._str_to_bool("false"))
        self.assertFalse(main._str_to_bool(""))
        self.assertFalse(main._str_to_bool("1"))

    def test_str_to_list(self):
        self.assertListEqual(main._str_to_list("fst,snd"), ["fst", "snd"])
        self.assertListEqual(main._str_to_list("fst,snd,third"), ["fst", "snd", "third"])

    def test_process_lists(self):
        prj = Project('/project_path')
        prj.check_and_add_module(Module('all_module', '/all_path'))
        prj.check_and_add_module(Module('c_rpm_module', '/c_rpm_module_path'))
        prj.check_and_add_module(Module('c_module', '/c_module_path'))
        prj.check_and_add_module(Module('not_build_c_sb_module', '/not_build_c_sb_module_path'))

        buld_modules = 'all_module,c_rpm_module,c_module'
        c_modules = 'all'
        r_modules = 'all_module,c_rpm_module'
        sb_modules = 'all_module,not_build_c_sb_module'

        def check_mod(mod, is_sb, is_cond, is_rpm, is_build):
            # type: (Module, bool, bool, bool, bool) -> None
            self.assertEqual(mod.is_sandbox, is_sb, "Sandbox check failed for module %s" % mod.name)
            self.assertEqual(mod.is_conductor, is_cond, "Conductor check failed for module %s" % mod.name)
            self.assertEqual(mod.is_rpm, is_rpm, "RPM check failed for module %s" % mod.name)
            self.assertEqual(mod.needs_to_be_built, is_build, "Build check failed for module %s" % mod.name)

        expected = {
            'all_module': lambda m: check_mod(m, is_sb=True, is_cond=True, is_rpm=True, is_build=True),
            'c_rpm_module': lambda m: check_mod(m, is_sb=False, is_cond=True, is_rpm=True, is_build=True),
            'c_module': lambda m: check_mod(m, is_sb=False, is_cond=True, is_rpm=False, is_build=True),
            # Not processed
            'not_build_c_sb_module': lambda m: check_mod(m, is_sb=False, is_cond=False, is_rpm=False, is_build=False)
        }

        main._process_lists(prj,
                            build_modules=buld_modules,
                            sandbox_modules=sb_modules,
                            conductor_modules=c_modules,
                            rpm_modules=r_modules)

        for mod in prj.get_modules():
            expected.get(mod.name)(mod)

    def test_build_custom_message(self):
        expected = 'Build by: cool_user\n' \
                   'Teamcity task: http://teamcity.yandex-team.ru/viewLog.html?buildId=123\n' \
                   'Build comment: My awesome message\nReally cool\n\n\n'
        self.assertEqual(expected, main._build_custom_message('cool_user', '123', 'My awesome message\nReally cool'))

    def test_process_target(self):
        def build_strategy(cr_tickets, changelog_push_moment, c_branch, v_strategy):
            # type: (bool, ChangelogPushMoment, str, str) -> TargetSettings
            result = TargetSettings()
            result.version_strategy = v_strategy
            result.create_tickets = cr_tickets
            result.changelog_push_moment = changelog_push_moment
            result.conductor_branch = c_branch
            return result

        expected = {
            'nowhere':
                build_strategy(cr_tickets=False, changelog_push_moment=ChangelogPushMoment.AFTER_BUILD,
                               c_branch='testing', v_strategy='DEFAULT'),
            'testing':
                build_strategy(cr_tickets=True, changelog_push_moment=ChangelogPushMoment.AFTER_BUILD,
                               c_branch='testing', v_strategy='DEFAULT'),
            'unstable':
                build_strategy(cr_tickets=True, changelog_push_moment=ChangelogPushMoment.AFTER_BUILD,
                               c_branch='unstable', v_strategy='DEFAULT'),
            'hotfix':
                build_strategy(cr_tickets=True, changelog_push_moment=ChangelogPushMoment.AFTER_BUILD,
                               c_branch='hotfix', v_strategy='DEFAULT'),
            'demo-testing':
                build_strategy(cr_tickets=True, changelog_push_moment=ChangelogPushMoment.NEVER,
                               c_branch='testing', v_strategy='TIMESTAMP_BRANCH'),
            'multitesting-stand':
                build_strategy(cr_tickets=False, changelog_push_moment=ChangelogPushMoment.NEVER,
                               c_branch='testing', v_strategy='SHA1'),
            'demo':
                build_strategy(cr_tickets=False, changelog_push_moment=ChangelogPushMoment.NEVER,
                               c_branch='testing', v_strategy='TIMESTAMP_BRANCH')
        }

        def targets_eq(fst, snd, target):
            # type: (TargetSettings, TargetSettings, str) -> None
            self.assertEqual(fst.create_tickets, snd.create_tickets,
                             "Create tickets not equal in target %s %s != %s" % (
                                 target, fst.create_tickets, snd.create_tickets))
            self.assertEqual(fst.conductor_branch, snd.conductor_branch,
                             "Conductor branch not equal in target %s %s != %s" % (
                                 target, fst.conductor_branch, snd.conductor_branch))
            self.assertEqual(fst.changelog_push_moment, snd.changelog_push_moment,
                             "Commit change log log not equal in target %s %s != %s" % (
                                 target, fst.changelog_push_moment, snd.changelog_push_moment))
            self.assertEqual(fst.version_strategy, snd.version_strategy,
                             "Version strategy not equal in target %s %s != %s" % (
                                 target, fst.version_strategy, snd.version_strategy))

        for target_type, settings in expected.iteritems():
            targets_eq(main._process_target(target_type, 'AUTO', None), settings, target_type)

        # check that override of changelog_push_moment works
        for target_type, settings in expected.iteritems():
            settings = copy.copy(settings)
            settings.changelog_push_moment = ChangelogPushMoment.BEFORE_BUILD
            targets_eq(main._process_target(target_type, 'AUTO', ChangelogPushMoment.BEFORE_BUILD), settings,
                       target_type)

    def test_to_build_result(self):
        b_results = test_utils.build_simple_build_results()
        r_results = test_utils.build_simple_release_results()
        prj = test_utils.build_simple_project()[0]

        changelog_commits_by_module_name = {
            'my_module': [
                Commit('0', date=datetime(2017, 8, 1), author='First Author <f_author@mail>',
                       message_rows=['* First change', 'second line']),
                Commit('1', date=datetime(2017, 8, 2), author='Second Author <s_author@mail>',
                       message_rows=['* Second change'])
            ]
        }
        project_build_result = ProjectBuildResult(b_results, changelog_commits_by_module_name)

        build_result = main._to_build_result(prj, project_build_result, r_results, False)

        expected = {
            'conductorTicket': {'url': 'https://c-url', 'id': 111},
            "packages": [
                {
                    'version': '1.1',
                    'moduleName': 'my_module',
                    'debPackageName': 'deb-package',
                    'sandboxResourceType': 'MY_TYPE',
                    'sandboxResourceId': 42,
                    'sandboxTicket': {'url': 'https://sb-url', 'id': '222'},
                    'rpmPackages': [
                        {'name': 'rpm-package', 'version': '1.1'},
                        {'name': 'rpm-package-for-test', 'version': '1.1'}
                    ],
                    'changelog': '* First change\nsecond line\n* Second change',
                    'changelogDetails': [
                        {
                            'revision': '0',
                            'timestampSeconds': 1501545600,
                            'author': 'First Author <f_author@mail>',
                            'change': '* First change\nsecond line'
                        },
                        {
                            'revision': '1',
                            'timestampSeconds': 1501632000,
                            'author': 'Second Author <s_author@mail>',
                            'change': '* Second change'
                        }
                    ]
                }
            ]
        }

        self.maxDiff = 2000
        self.assertDictEqual(build_result, expected)
