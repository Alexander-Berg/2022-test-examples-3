# -*- coding: utf-8 -*-

from unittest import TestCase

import mock

from search.pumpkin.yalite_service.libyalite.common import utils

ip_stdout_nolabel = """
1: lo: <LOOPBACK,UP,LOWER_UP> mtu 16436 qdisc noqueue state UNKNOWN
    link/loopback 00:00:00:00:00:00 brd 00:00:00:00:00:00
    inet 127.0.0.1/8 scope host lo
    inet 141.8.146.72/32 scope global lo:yalite
    inet 93.158.134.11/32 scope global lo:yalite
    inet 213.180.193.11/32 scope global lo:yalite
    inet 213.180.204.11/32 scope global lo:yalite
    inet6 ::1/128 scope host
       valid_lft forever preferred_lft forever
"""

ip_stdout_label = """
    inet 141.8.146.72/32 scope global lo:yalite
    inet 93.158.134.11/32 scope global lo:yalite
    inet 213.180.193.11/32 scope global lo:yalite
    inet 213.180.204.11/32 scope global lo:yalite
"""


class TestUtils(TestCase):

    @classmethod
    def setUpClass(cls):
        print "Testing 'utils' module:"

    @classmethod
    def tearDownClass(cls):
        print ""

    def run_test_merge_dict(self, old_dict, new_dict, check):
        result = utils.merge_dict(old_dict, new_dict)

        self.assertIsInstance(result, dict, "Merge result is not dictionary.")
        self.assertDictEqual(result, check, "Merge result is not equal to standard.")
        self.assertIsNot(result, old_dict, "Merge result IS 'old' dict, not the copy.")
        self.assertIsNot(result, new_dict, "Merge result IS 'new' dict, not the copy.")

    def test_merge_dict(self):
        # Both new and old
        old_dict = {"field_1": "value_1",
                    "dict_1": {"field_1": "value_1",
                               "field_2": 2},
                    "dict_2": {"field_1": "value_1"},
                    False: ["one", 2, {1: "one"}],
                    2: -2}

        new_dict = {"field_1": "new_value_1",
                    "dict_1": {"field_2": None,
                               "field_3": "new_value_3"},
                    "dict_2": {"field_2": "new_value_2"},
                    False: (True, "new_value_1")}

        check = {"field_1": "new_value_1",
                 "dict_1": {"field_1": "value_1",
                            "field_2": None,
                            "field_3": "new_value_3"},
                 "dict_2": {"field_1": "value_1",
                            "field_2": "new_value_2"},
                 False: (True, "new_value_1"),
                 2: -2}

        self.run_test_merge_dict(old_dict=old_dict, new_dict=new_dict, check=check)

        # Just new
        self.run_test_merge_dict(old_dict={}, new_dict=new_dict, check=new_dict)

        # Just old
        self.run_test_merge_dict(old_dict=old_dict, new_dict={}, check=old_dict)

        # Clear test
        self.run_test_merge_dict(old_dict={}, new_dict={}, check={})

        # Old and None
        self.run_test_merge_dict(old_dict=old_dict, new_dict=None, check=old_dict)

    @mock.patch('libyalite.utils.check_output')
    def test_get_interface_info(self, mock_check_call):
        check_nolabel = {
            "link": ["LOOPBACK", "UP", "LOWER_UP"],
            "mtu": 16436,
            "type": "loopback",
            "ip": {"127.0.0.1", "141.8.146.72", "93.158.134.11", "213.180.193.11", "213.180.204.11"}
        }

        mock_check_call.return_value = ip_stdout_nolabel
        result_nolabel = utils.get_interface_info("some")
        self.assertEqual(result_nolabel, check_nolabel)

        check_label = {
            "link": [],
            "mtu": 0,
            "type": "UNKNOWN",
            "ip": {"141.8.146.72", "93.158.134.11", "213.180.193.11", "213.180.204.11"}
        }

        mock_check_call.return_value = ip_stdout_label
        result_label = utils.get_interface_info("lo", "yalite")
        self.assertEqual(result_label, check_label)

        self.assertTrue(mock_check_call.called)

    def test_get_first_item(self):
        # Full list
        result = utils.get_first_item([4, 3, 2, 1])
        self.assertEqual(result, 4, "Got incorrect first element from full list")

        # One element list
        result = utils.get_first_item([10])
        self.assertEqual(result, 10, "Got incorrect element from list of one element")

        # Empty list
        result = utils.get_first_item([])
        self.assertEqual(result, None, "Incorrect behaviour for empty list")

        # None
        result = utils.get_first_item(None)
        self.assertEqual(result, None, "Incorrect behaviour for 'None' as argument")

        # Tuple, str
        result = utils.get_first_item((7,))
        self.assertEqual(result, 7, "Incorrect behaviour for tuple as argument")

        result = utils.get_first_item("abcd")
        self.assertEqual(result, "a", "Incorrect behaviour for str as argument")

    @mock.patch('libyalite.utils.get_first_item')
    def test_filter_first_item(self, mock_get_first_item):
        filter_function = mock.Mock()

        test_list = [4, 3, 2, 1]
        mock_get_first_item.return_value = test_list[0]

        result = utils.filter_first_item(filter_function, test_list)

        self.assertEqual(filter_function.call_count, 4)
        self.assertTrue(mock_get_first_item.called)
        self.assertEqual(result, 4)

    def test_get_last_item(self):
        # Full list
        result = utils.get_last_item([4, 3, 2, 1])
        self.assertEqual(result, 1, "Got incorrect first element from full list")

        # One element list
        result = utils.get_last_item([10])
        self.assertEqual(result, 10, "Got incorrect element from list of one element")

        # Empty list
        result = utils.get_last_item([])
        self.assertEqual(result, None, "Incorrect behaviour for empty list")

        # None
        result = utils.get_last_item(None)
        self.assertEqual(result, None, "Incorrect behaviour for 'None' as argument")

        # Tuple, str
        result = utils.get_last_item((7,))
        self.assertEqual(result, 7, "Incorrect behaviour for tuple as argument")

        result = utils.get_last_item("abcd")
        self.assertEqual(result, "d", "Incorrect behaviour for str as argument")

    @mock.patch('libyalite.utils.get_last_item')
    def test_filter_last_item(self, mock_get_last_item):
        filter_function = mock.Mock()

        test_list = [4, 3, 2, 1]
        mock_get_last_item.return_value = test_list[3]

        result = utils.filter_last_item(filter_function, test_list)

        self.assertEqual(filter_function.call_count, 4)
        self.assertTrue(mock_get_last_item.called)
        self.assertEqual(result, 1)
