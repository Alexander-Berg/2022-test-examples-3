# -*- coding: utf-8 -*-

from tempfile import NamedTemporaryFile
from unittest import TestCase

import mock
from search.pumpkin.yalite_service.libyalite.actions.abstract_action import YaLiteAction
from search.pumpkin.yalite_service.libyalite.core import YaLiteCore
from search.pumpkin.yalite_service.libyalite.services.abstract_service import ResourceRecord, DataPackRecord

import utils_for_tests as utils
from search.pumpkin.yalite_service.libyalite.common.exceptions import FileParseError

__author__ = 'denkoren'


class TestYaLiteCore(TestCase):

    @classmethod
    def setUpClass(cls):
        print "Testing YaLiteCore class:"

    @classmethod
    def tearDownClass(cls):
        print ""

    def setUp(self):
        self.tempfile = NamedTemporaryFile()

        self.core = YaLiteCore(utils.test_config_path)

        utils.mock_core_cachefiles(self.core, self.tempfile, self.tempfile)

    def tearDown(self):
        self.tempfile.close()

    def test__read_oauth_token(self):
        token = "abcdef_key_oauth_ghijklmn"

        f = open(self.tempfile.name, "w")
        f.truncate(0)
        f.writelines(["abcdef_key_oauth_ghijklmn"])
        f.close()

        read_token = self.core._read_oauth_token(self.tempfile.name)

        self.assertEqual(read_token, token, "OAuth token read by core incorrectly")

    def test__read_downloads_list(self):
        # Correct test
        correct_data_generator1 = [
            {"line": utils.resource_lines["correct"]["full"],
             "count": 3},
            {"line": "\t\t\n",
             "count": 1},
            {"line": utils.resource_lines["correct"]["part"],
             "count": 1}
        ]
        check1 = [ResourceRecord(0, "/path/to/archive/archive_2000.01.01_00-00",
                                 "/path/to/extracted/data/2000.01.01_00-00"),
                  ResourceRecord(1, "/path/to/archive/archive_2001.02.02_00-00",
                                 "/path/to/extracted/data/2001.02.02_00-00"),
                  ResourceRecord(2, "/path/to/archive/archive_2002.03.03_00-00",
                                 "/path/to/extracted/data/2002.03.03_00-00"),
                  ResourceRecord(4, "/path/to/archive/archive_2004.05.05_00-00", None)]

        correct_data_generator2 = []
        check2 = []

        utils.fill_testfile(self.tempfile, correct_data_generator1)
        contents = self.core._read_downloads_list()
        self.assertEqual(contents, check1, "List read from file differs from standard.")

        utils.fill_testfile(self.tempfile, correct_data_generator2)
        contents = self.core._read_downloads_list()
        self.assertEqual(contents, check2, "List read from file differs from standard.")

        # Incorrect tests
        incorrect_data_generator1 = [
            {"line": utils.resource_lines["correct"]["full"],
             "count": 2},
            {"line": utils.resource_lines["broken"]["spaces"],
             "count": 1},
        ]
        incorrect_data_generator2 = [
            {"line": utils.resource_lines["correct"]["full"],
             "count": 2},
            {"line": utils.resource_lines["broken"]["balderdash"],
             "count": 1},
        ]
        incorrect_data_generator3 = [
            {"line": utils.resource_lines["correct"]["full"],
             "count": 2},
            {"line": utils.resource_lines["broken"]["empty_fields"],
             "count": 1},
        ]

        utils.fill_testfile(self.tempfile, incorrect_data_generator1)
        self.assertRaises(FileParseError, self.core._read_downloads_list)

        utils.fill_testfile(self.tempfile, incorrect_data_generator2)
        self.assertRaises(FileParseError, self.core._read_downloads_list)

        utils.fill_testfile(self.tempfile, incorrect_data_generator3)
        self.assertRaises(FileParseError, self.core._read_downloads_list)

    def test__read_datapack_list(self):
        # Correct test
        correct_data_generator1 = [
            {"line": utils.datapack_lines["correct"]["3id"],
             "count": 3},
            {"line": utils.datapack_lines["correct"]["empty"],
             "count": 1},
            {"line": utils.datapack_lines["correct"]["2id"],
             "count": 1}
        ]
        check1 = [DataPackRecord(ids=[0, 1000, 2000]),
                  DataPackRecord(ids=[1, 1001, 2001]),
                  DataPackRecord(ids=[2, 1002, 2002]),
                  DataPackRecord(ids=[4, 1004])]

        correct_data_generator2 = []
        check2 = []

        utils.fill_testfile(self.tempfile, correct_data_generator1)
        contents = self.core._read_datapack_list()
        self.assertEqual(contents, check1, "List read from file differs from standard.")

        utils.fill_testfile(self.tempfile, correct_data_generator2)
        contents = self.core._read_datapack_list()
        self.assertEqual(contents, check2, "List read from file differs from standard.")

        # Incorrect tests
        incorrect_data_generator1 = [
            {"line": utils.datapack_lines["correct"]["3id"],
             "count": 2},
            {"line": utils.datapack_lines["broken"]["balderdash"],
             "count": 1},
        ]
        incorrect_data_generator2 = [
            {"line": utils.datapack_lines["broken"]["spaces"],
             "count": 1},
        ]
        incorrect_data_generator3 = [
            {"line": utils.datapack_lines["broken"]["not_numbers"],
             "count": 1},
        ]

        utils.fill_testfile(self.tempfile, incorrect_data_generator1)
        self.assertRaises(FileParseError, self.core._read_downloads_list)
        utils.fill_testfile(self.tempfile, incorrect_data_generator2)
        self.assertRaises(FileParseError, self.core._read_downloads_list)
        utils.fill_testfile(self.tempfile, incorrect_data_generator3)
        self.assertRaises(FileParseError, self.core._read_downloads_list)

    @mock.patch('search.pumpkin.yalite_service.libyalite.core.services')
    def test_reload_all_services(self, services_mock):
        #
        # Prepare environment
        #
        service_mock_1 = mock.MagicMock()
        service_mock_1.NAME = 'test-service-name'

        service_mock_2 = mock.MagicMock()
        service_mock_2.NAME = 'test-service-name2'

        service_mock_3 = mock.MagicMock()
        service_mock_3.NAME = 'test-service-name3'

        services_mock.__services__ = [service_mock_1,
                                      service_mock_2,
                                      service_mock_3]

        services_mock.__service_names__ = {service_mock_1.NAME: service_mock_1,
                                           service_mock_2.NAME: service_mock_2,
                                           service_mock_3.NAME: service_mock_3}

        self.core.config.services = {'test-service-name': 'config1',
                                     'test-service-name2': 'config2'}

        #
        # Make action
        #
        self.core.reload_service()

        #
        # Check that all is done properly
        #
        self.assertTrue(service_mock_1.called)
        self.assertTrue(service_mock_2.called)
        self.assertFalse(service_mock_3.called)

        self.assertTrue(service_mock_1.call_args[1]['configuration'] == 'config1')
        self.assertTrue(service_mock_2.call_args[1]['configuration'] == 'config2')

    @mock.patch('search.pumpkin.yalite_service.libyalite.core.services')
    def test_reload_one_service(self, services_mock):
        #
        # Prepare environment
        #
        service_mock_1 = mock.MagicMock()
        service_mock_1.NAME = 'test-service-name'

        service_mock_2 = mock.MagicMock()
        service_mock_2.NAME = 'test-service-name2'

        services_mock.__services__ = [service_mock_1,
                                      service_mock_2]

        services_mock.__service_names__ = {service_mock_1.NAME: service_mock_1,
                                           service_mock_2.NAME: service_mock_2}

        services_configurations = {'test-service-name': 'config1',
                                   'test-service-name2': 'config2'}

        read_services_mock = mock.MagicMock()
        read_services_mock.return_value = services_configurations

        self.core.config.services = services_configurations
        self.core.config.read_services = read_services_mock

        #
        # Make action
        #
        self.core.reload_service('test-service-name2')

        #
        # Check that all is done properly
        #
        self.assertTrue(read_services_mock.called)
        self.assertFalse(service_mock_1.called)
        self.assertTrue(service_mock_2.called)

        self.assertTrue(service_mock_2.call_args[1]['configuration'] == 'config2')

    @mock.patch('search.pumpkin.yalite_service.libyalite.core.services')
    def test_reload_one_service_with_custom_config(self, services_mock):
        #
        # Prepare environment
        #
        service_mock_1 = mock.MagicMock()
        service_mock_1.NAME = 'test-service-name'

        services_mock.__services__ = [service_mock_1]

        services_mock.__service_names__ = {service_mock_1.NAME: service_mock_1}

        self.core.config.services = {'test-service-name': 'config1'}

        #
        # Make action
        #
        self.core.reload_service('test-service-name', 'config2')

        #
        # Check that all is done properly
        #
        self.assertTrue(service_mock_1.called)
        self.assertTrue(service_mock_1.call_args[1]['configuration'] == 'config2')

    def test_load_actions(self):
        loaded_actions = self.core._load_actions()

        self.assertTrue(len(loaded_actions) > 0)

        for a in loaded_actions:
            self.assertTrue(isinstance(a, YaLiteAction))

    def test_get_resource_record(self):
        correct_data_generator = [
            {"line": utils.resource_lines["correct"]["part"],
             "count": 1},
            {"line": utils.resource_lines["correct"]["full"],
             "count": 30}
        ]

        utils.fill_testfile(self.tempfile, correct_data_generator)

        # Correct tests:
        pattern1 = 10
        pattern2 = "/path/to/archive/archive_2010.11.11_00-00"
        pattern3 = "/path/to/extracted/data/2010.11.11_00-00"

        check = ResourceRecord(10, "/path/to/archive/archive_2010.11.11_00-00",
                               "/path/to/extracted/data/2010.11.11_00-00")

        resource_record = self.core.get_resource_record(pattern1)
        self.assertEqual(resource_record, check, "Wrong record found in downloads list")

        resource_record = self.core.get_resource_record(pattern2)
        self.assertEqual(resource_record, check, "Wrong record found in downloads list")

        resource_record = self.core.get_resource_record(pattern3)
        self.assertEqual(resource_record, check, "Wrong record found in downloads list")

        # Incorrect tests:
        pattern4 = 100
        resource_record = self.core.get_resource_record(pattern4)
        self.assertEqual(resource_record, None, "None was waited, but get_resource_record found some record")

    def test_get_datapack_record(self):
        correct_data_generator = [
            {"line": utils.datapack_lines["correct"]["2id"],
             "count": 1},
            {"line": utils.datapack_lines["correct"]["3id"],
             "count": 30},
            {"line": "111\t222\t333\n",
             "count": 1},
            {"line": "123\t999\t222\n",
             "count": 1},
        ]

        utils.fill_testfile(self.tempfile, correct_data_generator)

        pattern = 10
        resource_record = self.core.get_datapack_record(pattern)
        check = DataPackRecord(ids=[10, 1010, 2010])
        self.assertEqual(resource_record, check, "First field search failed: "
                                                 "found '{0}' instead of '{1}'".format(resource_record, check))

        pattern = 222
        resource_record = self.core.get_datapack_record(pattern)
        check = DataPackRecord(ids=[123, 999, 222])
        self.assertEqual(resource_record, check, "Other fields search failed: "
                                                 "found '{0}' instead of '{1}'".format(resource_record, check))

    @mock.patch('os.path.isfile')
    def test_check_resource_download(self, mock_isfile):
        mock_isfile.return_value = True

        correct_data_generator = [
            {"line": utils.resource_lines["correct"]["part"],
             "count": 1},
            {"line": utils.resource_lines["correct"]["full"],
             "count": 30}
        ]

        utils.fill_testfile(self.tempfile, correct_data_generator)

        pattern = 20
        resource_record = self.core.check_resource_download(pattern)
        check = ResourceRecord(20, "/path/to/archive/archive_2020.21.21_00-00",
                               "/path/to/extracted/data/2020.21.21_00-00")
        self.assertEqual(resource_record, check, "Record '{0}' found in list "
                                                 "instead of '{1}'".format(resource_record, check))

        mock_isfile.return_value = False
        resource_record = self.core.check_resource_download(pattern)
        self.assertEqual(resource_record, None, "Record '{0}' found in list "
                                                "instead of None".format(resource_record))

        pattern = "20"
        resource_record = self.core.check_resource_download(pattern)
        self.assertEqual(resource_record, None, "Record '{0}' found in list "
                                                "instead of None".format(resource_record))

        utils.fill_testfile(self.tempfile, [])

        pattern = 0
        resource_record = self.core.check_resource_download(pattern)
        self.assertEqual(resource_record, None, "Record '{0}' found in list "
                                                "instead of None".format(resource_record))

    @mock.patch('os.path.isfile')
    @mock.patch('os.path.isdir')
    def test_check_resource_unpack(self, mock_isdir, mock_isfile):
        mock_isfile.return_value = True
        mock_isdir.return_value = True

        correct_data_generator = [
            {"line": utils.resource_lines["correct"]["part"],
             "count": 1},
            {"line": utils.resource_lines["correct"]["full"],
             "count": 30}
        ]

        utils.fill_testfile(self.tempfile, correct_data_generator)

        pattern = 20
        resource_record = self.core.check_resource_unpack(pattern)
        check = ResourceRecord(20, "/path/to/archive/archive_2020.21.21_00-00",
                               "/path/to/extracted/data/2020.21.21_00-00")
        self.assertEqual(resource_record, check, "Record '{0}' found in list "
                                                 "instead of '{1}'".format(resource_record, check))

        mock_isdir.return_value = False
        resource_record = self.core.check_resource_unpack(pattern)
        self.assertEqual(resource_record, None, "Record '{0}' found in list "
                                                "instead of 'None'".format(resource_record))

        pattern = 0
        resource_record = self.core.check_resource_unpack(pattern)
        self.assertEqual(resource_record, None, "Record '{0}' found in list "
                                                "instead of 'None'".format(resource_record))

        # TODO: Check for None patterng

    def test_save_resource_record(self):
        # Before limit check
        list_size = self.core.config.cache_lists_size
        new_record = ResourceRecord(999, "/new/resource/archive", "/new/resource/extracted/data")

        correct_data_generator = [
            {"line": utils.resource_lines["correct"]["full"],
             "count": list_size - 3},
            {"line": utils.resource_lines["correct"]["part"],
             "count": 1}
        ]

        correct_data = utils.generate_file_data(correct_data_generator)

        utils.update_testfile(self.tempfile, correct_data)
        self.core.save_resource_record(new_record)

        result = self.core._read_downloads_list()

        check = map(ResourceRecord.parse_line, correct_data)
        check.append(new_record)

        # pdb.set_trace()

        self.assertEqual(result, check, "Resource file save failed (<limit)")

        # On limit check
        correct_data_generator = [
            {"line": utils.resource_lines["correct"]["part"],
             "count": 1},
            {"line": utils.resource_lines["correct"]["full"],
             "count": list_size - 2}
        ]

        correct_data = utils.generate_file_data(correct_data_generator)

        utils.update_testfile(self.tempfile, correct_data)
        self.core.save_resource_record(new_record)

        result = self.core._read_downloads_list()

        check = map(ResourceRecord.parse_line, correct_data)
        check.append(new_record)

        self.assertEqual(result, check, "Resource file save failed (=limit)")

        # After limit
        correct_data_generator = [
            {"line": utils.resource_lines["correct"]["full"],
             "count": list_size - 1},
            {"line": utils.resource_lines["correct"]["part"],
             "count": 1}
        ]

        correct_data = utils.generate_file_data(correct_data_generator)
        check_data = correct_data[list_size / 2:]

        utils.update_testfile(self.tempfile, correct_data)
        self.core.save_resource_record(new_record)

        result = self.core._read_downloads_list()

        check = map(ResourceRecord.parse_line, check_data)
        check.append(new_record)

        self.assertEqual(result, check, "Resource file save failed (>limit)")

    def test_save_datapack_record(self):
        # Before limit check
        list_size = self.core.config.cache_lists_size
        new_record = DataPackRecord(ids=[777, 888, 999])

        correct_data_generator = [
            {"line": utils.datapack_lines["correct"]["3id"],
             "count": list_size - 3},
            {"line": utils.datapack_lines["correct"]["2id"],
             "count": 1}
        ]

        correct_data = utils.generate_file_data(correct_data_generator)
        utils.update_testfile(self.tempfile, correct_data)
        self.core.save_datapack_record(new_record)

        result = self.core._read_datapack_list()

        check = map(DataPackRecord.parse_line, correct_data)
        check.append(new_record)

        self.assertEqual(result, check, "Datapack file save failed (<limit)")

        # On limit check
        correct_data_generator = [
            {"line": utils.datapack_lines["correct"]["3id"],
             "count": 1},
            {"line": utils.datapack_lines["correct"]["2id"],
             "count": list_size - 2}
        ]

        correct_data = utils.generate_file_data(correct_data_generator)
        utils.update_testfile(self.tempfile, correct_data)
        self.core.save_datapack_record(new_record)

        result = self.core._read_datapack_list()

        check = map(DataPackRecord.parse_line, correct_data)
        check.append(new_record)

        self.assertEqual(result, check, "Datapack file save failed (=limit)")

        # After limit
        correct_data_generator = [
            {"line": utils.datapack_lines["correct"]["2id"],
             "count": list_size - 1},
            {"line": utils.datapack_lines["correct"]["3id"],
             "count": 1}
        ]

        correct_data = utils.generate_file_data(correct_data_generator)
        utils.update_testfile(self.tempfile, correct_data)
        self.core.save_datapack_record(new_record)

        result = self.core._read_datapack_list()

        check_data = correct_data[list_size / 2:]
        check = map(DataPackRecord.parse_line, check_data)
        check.append(new_record)

        self.assertEqual(result, check, "Resource file save failed (>limit)")
