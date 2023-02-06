from unittest import TestCase

import mock

from lib.utils import rpm


class TestRpm(TestCase):
    def test_upload_to_dist(self):
        rpm_dist_paths = ['paysys-rhel.dist.yandex.ru:/repo/paysys-rhel/incoming/6',
                          'paysys-rhel.dist.yandex.ru:/repo/paysys-rhel/incoming/7']

        with mock.patch('lib.utils.rpm.lib.common.do_shell_once') as mock_do_shell, \
            mock.patch('lib.utils.rpm.os.listdir') as mock_listdir:
            mock_listdir.return_value = ['package-test.rpm','package-prod.rpm', 'package-test.not-rpm']

            rpm.upload_to_dist('/project/path/', rpm_dist_paths)

            expected_calls = [
                ['scp', '/project/path/package-test.rpm', 'paysys-rhel.dist.yandex.ru:/repo/paysys-rhel/incoming/6'],
                ['scp', '/project/path/package-test.rpm', 'paysys-rhel.dist.yandex.ru:/repo/paysys-rhel/incoming/7'],
                ['scp', '/project/path/package-prod.rpm', 'paysys-rhel.dist.yandex.ru:/repo/paysys-rhel/incoming/6'],
                ['scp', '/project/path/package-prod.rpm', 'paysys-rhel.dist.yandex.ru:/repo/paysys-rhel/incoming/7'],
                ['ssh', '-o PreferredAuthentications=publickey', 'paysys-rhel.dist.yandex.ru', 'sudo /usr/sbin/rpm-install -n paysys-rhel -v 6'],
                ['ssh', '-o PreferredAuthentications=publickey', 'paysys-rhel.dist.yandex.ru', 'sudo /usr/sbin/rpm-install -n paysys-rhel -v 7']
            ]

            actual_calls = [params_list[0] for params_list, params_dict in mock_do_shell.call_args_list]
            self.assertListEqual(expected_calls, actual_calls)

    def test_get_rpm_info(self):
        with mock.patch('lib.utils.rpm.os') as mock_os:
            mock_os.listdir.return_value = [
                'my-module-first.rpm',
                'my-module-second.not-rpm',
                'my-module-third-rpm']
            mock_os.path.isfile.return_value = 'my-module-path.rpm'

            with mock.patch('lib.utils.rpm.lib.common.do_shell_once') as mock_do_shell:
                mock_do_shell.return_value = ['Name: first-package', 'Version: 11.01', 'Release: 15.01', '']

                info = rpm.get_rpm_info('my-module', 'my-module-path')
                expected_info = [('first-package', '11.01-15.01')]
                self.assertEqual(expected_info, info)

                mock_os.listdir.assert_called_once_with('my-module-path')
                self.assertEqual(len(expected_info), mock_do_shell.call_count)
