# -*- coding: utf-8 -*-

import os
from unittest import TestCase

import mock
from search.pumpkin.yalite_service.libyalite.actions import checks
from search.pumpkin.yalite_service.libyalite.common.config import YaLiteConfiguration
from search.pumpkin.yalite_service.libyalite.core import YaLiteCore

import utils_for_tests as utils
from search.pumpkin.yalite_service.libyalite.common import exceptions

TUNNEL_CHECKS = {
    "correct": "4: tunl0: <NOARP,UP,LOWER_UP> mtu 9000 qdisc noqueue state UNKNOWN\n"
               "    link/ipip 0.0.0.0 brd 0.0.0.0",
    "incorrect": {
        "down": "4: tunl0: <NOARP,DOWN> mtu 9000 qdisc noqueue state UNKNOWN\n"
                "    link/ipip 0.0.0.0 brd 0.0.0.0",
        "mtu": "4: tunl0: <NOARP,UP,LOWER_UP> mtu 1000 qdisc noqueue state UNKNOWN\n"
               "    link/ipip 0.0.0.0 brd 0.0.0.0",
        "type": "4: tunl0: <NOARP,UP,LOWER_UP> mtu 9000 qdisc noqueue state UNKNOWN\n"
                "    link/gre 0.0.0.0 brd 0.0.0.0"
    }
}

SYSCTLS_CHECK = {
    "correct": {"net.ipv4.ip_forward": "1",
                "net.ipv4.conf.all.rp_filter": "0",
                "net.ipv4.conf.tunl0.rp_filter": "0"},
    "incorrect": [
        {"net.ipv4.ip_forward": "0",
         "net.ipv4.conf.all.rp_filter": "0",
         "net.ipv4.conf.tunl0.rp_filter": "0"},
        {"net.ipv4.ip_forward": "1",
         "net.ipv4.conf.all.rp_filter": "1",
         "net.ipv4.conf.tunl0.rp_filter": "0"},
        {"net.ipv4.ip_forward": "1",
         "net.ipv4.conf.all.rp_filter": "0",
         "net.ipv4.conf.tunl0.rp_filter": "1"}
    ]
}

RESINFOD_CORRECT_ANSWER = "pumpkin=\n" \
                          "UserQuery:\ttest\n" \
                          "NormalizedUserQuery:\ttest\n" \
                          "Empty:\t0\n" \
                          "SerpId:\t5461\n" \
                          "IsFrequentQuery:\t1\n" \
                          "NormalizedSerpQuery:\ttest\n" \
                          "OriginalSerpQuery:\ttest\n"


def return_sysctl_values(sysctl_values):
    def ret_func(sysctl_name):
        return sysctl_values[sysctl_name[-1]]

    return ret_func


class TestChecks(TestCase):

    @classmethod
    def setUpClass(cls):
        print "Testing 'actions.checks' module:"

        cls.config = YaLiteConfiguration(utils.test_config_path)

        cls.core_mock = mock.MagicMock()
        cls.core_mock.config = cls.config

        check_dir = os.path.dirname(cls.config.http_check_file)
        if not os.path.isdir(check_dir):
            os.makedirs(check_dir)

    @classmethod
    def tearDownClass(cls):
        print ""

        if os.path.isfile(cls.config.http_check_file):
            os.remove(cls.config.http_check_file)

    @mock.patch('search.pumpkin.yalite_service.libyalite.common.utils.check_output')
    def test_YaLiteCheckTunnel(self, mock_check_call):

        check_tunnel = checks.YaLiteCheckTunnel(None, "tunl0")

        mock_check_call.return_value = TUNNEL_CHECKS["correct"]
        self.assertTrue(check_tunnel.run_action(), "Correct tunnel interface check failed.")

        for check_name, interface_info in TUNNEL_CHECKS["incorrect"].iteritems():
            mock_check_call.return_value = interface_info
            self.assertRaises(exceptions.YaLiteCheckFailed, check_tunnel.run_action)

    @mock.patch('search.pumpkin.yalite_service.libyalite.common.utils.check_output')
    def test_YaLiteCheckSysctls(self, mock_check_output):

        check_rules = [{"name": "net.ipv4.ip_forward",
                        "value": "1",
                        "warn_msg": "IP Forwarding is turned off"},
                       {"name": "net.ipv4.conf.all.rp_filter",
                        "value": "0",
                        "warn_msg": "RPF is ON for ALL interfaces"},
                       {"name": "net.ipv4.conf.tunl0.rp_filter",
                        "value": "0",
                        "warn_msg": "RPF is ON for tunnel interface"}]

        check_sysctls = checks.YaLiteCheckSysctls(None, check_rules=check_rules)

        mock_check_output.side_effect = return_sysctl_values(SYSCTLS_CHECK["correct"])
        self.assertTrue(check_sysctls.run_action(), "Correct sysctl values check failed.")

        for sysctl_values in SYSCTLS_CHECK["incorrect"]:
            mock_check_output.side_effect = return_sysctl_values(sysctl_values=sysctl_values)
            self.assertRaises(exceptions.YaLiteCheckFailed, check_sysctls.run_action)

    @mock.patch("search.pumpkin.yalite_service.libyalite.actions.checks.requests.get")
    def test_YaLiteCheckResinfod(self, mock_requests_get):
        # TODO: make test for YaLiteCheckResinfod

        core = YaLiteCore(utils.test_config_path)

        # Mocks for services.
        test_service_1 = mock.MagicMock()
        test_service_1.NAME = "test-service_1"
        test_service_1.resinfod = {"host": "localhost",
                                   "port": 9933,
                                   "check_request": "/test1-request"}
        test_service_2 = mock.MagicMock()
        test_service_2.NAME = "test-service_2"
        test_service_2.resinfod = {"host": "localhost",
                                   "port": 9944,
                                   "check_request": "/test2-request"}
        test_service_3 = mock.MagicMock(spec="NAME")
        test_service_3.NAME = "test-service"

        services = [test_service_1, test_service_2, test_service_3]

        core.services = services

        check_resinfod = checks.YaLiteCheckResinfod(core=core)

        get_result = mock.MagicMock()
        get_result.content = RESINFOD_CORRECT_ANSWER

        mock_requests_get.return_value = get_result

        # Check one service
        self.assertTrue(check_resinfod.run_action("test-service_1"))
        self.assertEqual(mock_requests_get.call_args_list,
                         [(("http://localhost:9933/test1-request",),)])

        # Check full list of services
        mock_requests_get.reset_mock()
        get_result.content = RESINFOD_CORRECT_ANSWER
        mock_requests_get.return_value = get_result

        self.assertTrue(check_resinfod.run_action())
        self.assertEqual(mock_requests_get.call_args_list,
                         [(("http://localhost:9933/test1-request",),),
                          (("http://localhost:9944/test2-request",),)])

        # Failed check
        mock_requests_get.reset_mock()
        get_result.content = "some incorrect resinfod answer"
        mock_requests_get.return_value = get_result

        self.assertRaises(exceptions.YaLiteCheckFailed, check_resinfod.run_action)

        # Resinfod configuration not found:
        mock_requests_get.reset_mock()
        mock_requests_get.return_value = get_result

        # Suppress error message about absence of resinfod configuration for selected service.
        loglevel = core.config.loglevel
        core.config.loglevel = "CRITICAL"
        self.assertRaises(exceptions.YaLiteCheckFailed, check_resinfod.run_action, "test-service")
        core.config.loglevel = loglevel

        # Incorrect service name:
        mock_requests_get.reset_mock()
        mock_requests_get.return_value = get_result

        self.assertRaises(exceptions.YaLiteServiceNameError, check_resinfod.run_action, "incorrect-service-name")

        # self.assertTrue(False, "Make test implementation for YaLiteCheckResinfod")

    @mock.patch("search.pumpkin.yalite_service.libyalite.actions.checks.YaLiteCheckTunnel.run_action")
    @mock.patch("search.pumpkin.yalite_service.libyalite.actions.checks.YaLiteCheckSysctls.run_action")
    def test_YaLiteCheckNetwork(self, mock_sysctls, mock_tunnel):

        core = YaLiteCore(utils.test_config_path)

        check_network = core.get_action("check").get_action("network")

        network_checks = check_network.actions

        # Successful check:
        for c in network_checks:
            c.run_action.return_value = True

        self.assertTrue(check_network.run_action(), "Network complex check failed for all checks succeed.")

        # Failed one check:
        network_checks[-1].run_action.side_effect = exceptions.YaLiteCheckFailed("Check failed")

        self.assertRaises(exceptions.YaLiteCheckFailed, check_network.run_action)

        # Failed all checks:
        for c in network_checks:
            c.run_action.side_effect = exceptions.YaLiteCheckFailed("Check failed")

        self.assertRaises(exceptions.YaLiteCheckFailed, check_network.run_action)
