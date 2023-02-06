import os
from unittest import TestCase

import mock

from lib import release
from lib.build import BuildResult
from lib.release import ReleaseResult
from lib.tests import test_utils
from lib.utils.conductor import ConductorTicket


class TestProjectRelease(TestCase):
    def setUp(self):
        self.cwd = os.getcwd()
        self.create_tickets = True
        self.create_conductor_tickets = True
        self.build_user = 'test-user'
        self.custom_message = 'My custom message\n'
        self.conductor_branch = 'unstable'

        self.project_release = release.ProjectRelease(self.create_tickets, self.create_conductor_tickets,
                                                      self.build_user, self.custom_message, self.conductor_branch)

        self.prj, self.build_mod, self.not_build_mod = test_utils.build_simple_project()
        self.build_results = test_utils.build_simple_build_results()

    @mock.patch('lib.utils.deb.get_version_changelog')
    @mock.patch('lib.utils.conductor.create_ticket')
    def test_create_c_ticket(self, mock_create_ticket, mock_get_ch_log):
        changes = ['First change', 'Second change']
        mock_get_ch_log.return_value = changes
        self.project_release._create_c_ticket(self.prj, self.build_results)
        ticket_changes = self.custom_message + '\n'.join(set(changes))
        expected = [(self.conductor_branch, self.build_user, ticket_changes, ['deb-package=1.1'])]
        test_utils.check_method_calls(self, mock_create_ticket, expected)

    def test_create_sandbox_release(self):
        with mock.patch('lib.release.sandbox.create_sandbox_release') as mock_sb_release:
            sb_resource = filter(lambda br: br.pack_type == BuildResult.PackageType.SANDBOX, self.build_results)[0]
            self.project_release._create_sandbox_release(self.prj, self.build_results)
            mock_sb_release.assert_called_once_with(self.build_mod, sb_resource.result.ticket)

    @mock.patch('lib.release.deb')
    def test_release_project(self, mock_deb):
        change_log = ['First change', ' Second change']
        mock_deb.get_version_changelog.return_value = change_log
        with mock.patch('lib.release.ProjectRelease._create_c_ticket') as mock_c_ticket:
            c_ticket = ConductorTicket("url", 111)
            mock_c_ticket.return_value = c_ticket

            with mock.patch('lib.release.ProjectRelease._create_sandbox_release') as mock_sb_release:
                result = self.project_release.release_project(self.prj, self.build_results)

                mock_c_ticket.assert_called_once_with(self.prj, self.build_results)
                mock_sb_release.assert_called_once_with(self.prj, self.build_results)

                self.assertEqual(2, len(result))
                c_result = filter(lambda r: r.result_type == ReleaseResult.ReleaseType.CONDUCTOR_TICKET, result)[0]
                ch_log_result = filter(lambda r: r.result_type == ReleaseResult.ReleaseType.CHANGELOG, result)[0]

                self.assertEqual(c_result.module_name, self.build_mod.name)
                self.assertEqual(ch_log_result.module_name, self.build_mod.name)
                self.assertTrue(c_result.result, c_ticket)
                self.assertTrue(ch_log_result.result, '\n'.join(change_log))
