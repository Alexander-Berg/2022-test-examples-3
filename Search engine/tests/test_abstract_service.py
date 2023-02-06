# -*- coding: utf-8 -*-

__author__ = 'denkoren'

import os
import shutil
from subprocess import CalledProcessError
from unittest import TestCase, skip

import mock
from search.pumpkin.yalite_service.libyalite.services import abstract_service

from search.pumpkin.yalite_service.libyalite.common import exceptions

TEST_DIRS_ROOT = "./testdir/tmp/"
TEST_DIRS = os.path.join(TEST_DIRS_ROOT, "dirs")


class TestYaLiteAbstractService(TestCase):

    @classmethod
    def setUpClass(cls):
        print "Testing 'services.abstract_service.YaLiteAbstractService' class:"

        if not os.path.isdir(TEST_DIRS_ROOT):
            os.makedirs(TEST_DIRS_ROOT)

    @classmethod
    def tearDownClass(cls):
        print ""

        if os.path.isdir(TEST_DIRS):
            shutil.rmtree(TEST_DIRS)

    @mock.patch("search.pumpkin.yalite_service.libyalite.services.abstract_service.check_call")
    def test__control_service(self, mock_check_call):
        # Success calls:
        mock_check_call.return_value = True

        abstract_service.YaLiteAbstractService._control_service("system_service", "start")

        error_message = "Incorrect control service command: {0}".format(mock_check_call.call_args)
        self.assertIn((['/usr/bin/sudo', '/usr/bin/service', "system_service", "start"],),
                      mock_check_call.call_args, error_message)

        abstract_service.YaLiteAbstractService._control_service("system_service", "start", "web-search")

        error_message = "Incorrect control service command: {0}".format(mock_check_call.call_args)
        self.assertIn((['/usr/bin/sudo', '/usr/bin/service', "system_service", "start", "web-search"],),
                      mock_check_call.call_args, error_message)

        # Failure calls:
        mock_check_call.side_effect = CalledProcessError(100, ['/usr/bin/service', "nginx", "stop"])

        self.assertRaises(exceptions.YaLiteActionError,
                          abstract_service.YaLiteAbstractService._control_service,
                          "nginx", "stop")

    def test__create_data_dirs(self):
        data_1 = abstract_service.YaLiteData(symlink=None,
                                             archive_dir=os.path.join(TEST_DIRS, "archive_dir1"),
                                             data_dir=os.path.join(TEST_DIRS, "data_dir1"),
                                             data_series=None,
                                             resource_type=None,
                                             system_services=[])

        data_2 = abstract_service.YaLiteData(symlink=None,
                                             archive_dir=os.path.join(TEST_DIRS, "archive_dir2"),
                                             data_dir=os.path.join(TEST_DIRS, "data_dir2"),
                                             data_series=None,
                                             resource_type=None,
                                             system_services=[])

        # service = abstract_service.YaLiteAbstractService(name="test-service",
        #                                                  data_list=[data_1, data_2],
        #                                                  core=None)
        service = abstract_service.YaLiteAbstractService(data_list=[data_1, data_2],
                                                         core=None)

        service._create_data_dirs()

        self.assertTrue(os.path.isdir(os.path.join(TEST_DIRS, "archive_dir1")))
        self.assertTrue(os.path.isdir(os.path.join(TEST_DIRS, "data_dir1")))
        self.assertTrue(os.path.isdir(os.path.join(TEST_DIRS, "archive_dir2")))
        self.assertTrue(os.path.isdir(os.path.join(TEST_DIRS, "data_dir2")))

        # Check for command idempotency: second run shouldn't raise exceptions.
        service._create_data_dirs()

        shutil.rmtree("./testdir/tmp/dirs")

    def test__switch_data(self):
        data = abstract_service.YaLiteData(symlink=os.path.join(TEST_DIRS_ROOT, "data_symlink"),
                                           archive_dir=None,
                                           data_dir=None,
                                           data_series=None,
                                           resource_type=None,
                                           system_services=[])

        service = abstract_service.YaLiteAbstractService(data_list=[data],
                                                         core=None)

        service._switch_data(data, TEST_DIRS_ROOT)

        self.assertTrue(os.path.islink(data.symlink))

    def test__switch_back(self):
        # No last data.
        data = abstract_service.YaLiteData(symlink=os.path.join(TEST_DIRS_ROOT, "data_symlink"),
                                           archive_dir=None,
                                           data_dir=None,
                                           data_series=None,
                                           resource_type=None,
                                           system_services=[])

        service = abstract_service.YaLiteAbstractService(data_list=[data],
                                                         core=None)

        self.assertRaises(exceptions.YaLiteActionError, service._switch_back, data)

        # Last data is defined:

        data.last_data = os.path.realpath(os.path.join(TEST_DIRS_ROOT, "last_data"))

        service._switch_back(data)
        self.assertTrue(os.path.islink(data.symlink))
        self.assertEqual(os.path.realpath(data.symlink), data.last_data)

    @skip("Current method implementation not requires testing.")
    def test_list_depends(self):
        self.assertTrue(False, "Make test implementation for YaLiteAbstractService.list_depends")

    def test_stop_start_restart(self):
        data_1 = abstract_service.YaLiteData(symlink=None,
                                             archive_dir=None,
                                             data_dir=None,
                                             data_series=None,
                                             resource_type=None,
                                             system_services=["system_service_1"])

        data_2 = abstract_service.YaLiteData(symlink=None,
                                             archive_dir=None,
                                             data_dir=None,
                                             data_series=None,
                                             resource_type=None,
                                             system_services=["system_service_2"])

        service = abstract_service.YaLiteAbstractService(data_list=[data_1, data_2],
                                                         core=None)

        control_service = mock.MagicMock()

        service._control_service = control_service

        service.start()

        self.assertEqual(control_service.call_args_list,
                         [((), {"system_service": "system_service_1",
                                "command": "start",
                                "yalite_service": service.NAME}),
                          ((), {"system_service": "system_service_2",
                                "command": "start",
                                "yalite_service": service.NAME})],
                         "'start' command test failed")

        control_service.reset_mock()
        service.stop()

        self.assertEqual(control_service.call_args_list,
                         [((), {"system_service": "system_service_1",
                                "command": "stop",
                                "yalite_service": service.NAME}),
                          ((), {"system_service": "system_service_2",
                                "command": "stop",
                                "yalite_service": service.NAME})],
                         "'stop' command test failed")

        control_service.reset_mock()
        service.restart()

        self.assertEqual(control_service.call_args_list,
                         [((), {"system_service": "system_service_1",
                                "command": "restart",
                                "yalite_service": service.NAME}),
                          ((), {"system_service": "system_service_2",
                                "command": "restart",
                                "yalite_service": service.NAME})],
                         "'restart' command test failed")


class TestDataPackRecord(TestCase):
    class Resource(object):
        def __init__(self, id):
            self.id = id

        def __eq__(self, other):
            return self.id == other.id

    @classmethod
    def setUpClass(cls):
        print "Testing 'services.abstract_service.DataPackRecord' class:"

    @classmethod
    def tearDownClass(cls):
        print ""

    def test___str__(self):
        datapack_record = abstract_service.DataPackRecord(ids=[20, 140, 212])

        check_str = "20\t140\t212\n"

        self.assertEqual(check_str, datapack_record.__str__(), "DataPackRecord wrongly casts to str")

    def test___eq__(self):
        datapack_record1 = abstract_service.DataPackRecord(ids=[20, 140, 212],
                                                           resources=[self.Resource(1),
                                                                      self.Resource(2),
                                                                      self.Resource(3)])
        datapack_record2 = abstract_service.DataPackRecord(ids=[20, 140, 212],
                                                           resources=[self.Resource(1),
                                                                      self.Resource(2),
                                                                      self.Resource(3)])

        datapack_record3 = abstract_service.DataPackRecord(ids=[20, 140],
                                                           resources=[self.Resource(1),
                                                                      self.Resource(2),
                                                                      self.Resource(3)])
        datapack_record4 = abstract_service.DataPackRecord(ids=[20, 141, 212],
                                                           resources=[self.Resource(1),
                                                                      self.Resource(2),
                                                                      self.Resource(3)])
        datapack_record5 = abstract_service.DataPackRecord(ids=[20, 140, 212],
                                                           resources=[self.Resource(1),
                                                                      self.Resource(2)])
        datapack_record6 = abstract_service.DataPackRecord(ids=[20, 140, 212],
                                                           resources=[self.Resource(1),
                                                                      self.Resource(2),
                                                                      self.Resource(4)])

        self.assertTrue(datapack_record1 == datapack_record2, "Equal DataPackRecords equality check failed.")
        self.assertFalse(datapack_record1 == datapack_record3,
                         "Unequal DataPackRecords equality check succeed (unequal ids size).")
        self.assertFalse(datapack_record1 == datapack_record4,
                         "Unequal DataPackRecords equality check succeed (unequal ids).")
        self.assertFalse(datapack_record1 == datapack_record5,
                         "Unequal DataPackRecords equality check succeed (unequal resources size).")
        self.assertFalse(datapack_record1 == datapack_record6,
                         "Unequal DataPackRecords equality check succeed (unequal resources).")

    def test_get_resources(self):
        sandbox = mock.MagicMock()

        def get_resource(id):
            res = self.Resource(id)
            res.sandbox = sandbox
            return res

        sandbox.get_resource.side_effect = get_resource

        collector = mock.MagicMock()
        collector.get_resource_failsafe.side_effect = get_resource

        datapack = abstract_service.DataPackRecord(ids=[1, 2, 3])

        datapack.get_resources(collector=collector)

        check_datapack = abstract_service.DataPackRecord(ids=[1, 2, 3],
                                                         resources=[self.Resource(1),
                                                                    self.Resource(2),
                                                                    self.Resource(3)])

        self.assertEqual(datapack, check_datapack, "Incorrect resources list after get_resource()")
        self.assertEqual(collector.get_resource_failsafe.call_args_list,
                         [((1,),)],
                         "Incorrect collector.get_resource_failsafe call.")
        self.assertEqual(sandbox.get_resource.call_args_list,
                         [((2,),),
                          ((3,),)],
                         "Incorrect sandbox.get_resource call(s)")

    def test_parse_line(self):
        # Correct tests
        correct_record1 = "000\t20\t33\n"
        correct_record2 = "23\t12\n"

        check1 = abstract_service.DataPackRecord(ids=[0, 20, 33])
        check2 = abstract_service.DataPackRecord(ids=[23, 12])

        result1 = abstract_service.DataPackRecord.parse_line(correct_record1)
        result2 = abstract_service.DataPackRecord.parse_line(correct_record2)

        self.assertEqual(result1, check1)
        self.assertEqual(result2, check2)

        # Incorrect tests
        wrong_record1 = "id"
        wrong_record2 = "0"
        wrong_record3 = "10 archive data"
        wrong_record4 = "20\t\t\n"
        wrong_record5 = "0 1 2"

        self.assertRaises(exceptions.LineParseError, abstract_service.ResourceRecord.parse_line, wrong_record1)
        self.assertRaises(exceptions.LineParseError, abstract_service.ResourceRecord.parse_line, wrong_record2)
        self.assertRaises(exceptions.LineParseError, abstract_service.ResourceRecord.parse_line, wrong_record3)
        self.assertRaises(exceptions.LineParseError, abstract_service.ResourceRecord.parse_line, wrong_record4)
        self.assertRaises(exceptions.LineParseError, abstract_service.ResourceRecord.parse_line, wrong_record5)

    def test_transformation(self):
        resource_record = abstract_service.DataPackRecord(ids=[100, 23, 12])

        transformed = abstract_service.DataPackRecord.parse_line(resource_record.__str__())

        self.assertEqual(resource_record, transformed, "ResourceRecord unexpectedly changed after transformation")


class TestResourceRecord(TestCase):
    @classmethod
    def setUpClass(cls):
        print "Testing 'services.abstract_service.ResourceRecord' class:"

    @classmethod
    def tearDownClass(cls):
        print ""

    def test___str__(self):
        resource_record = abstract_service.ResourceRecord(20, "archive_path", "data_path")

        check_str = "20\tarchive_path\tdata_path\n"

        self.assertEqual(check_str, resource_record.__str__(), "ResourceRecord wrongly casts to str")

    def test___eq__(self):
        resource_record1 = abstract_service.ResourceRecord(20, "archive_path", "data_path")
        resource_record2 = abstract_service.ResourceRecord(20, "archive_path", "data_path")

        resource_record3 = abstract_service.ResourceRecord(20, "archive_path", "data_path_2")
        resource_record4 = abstract_service.ResourceRecord(20, "archive_path_2", "data_path")
        resource_record5 = abstract_service.ResourceRecord(21, "archive_path", "data_path")

        self.assertTrue(resource_record1 == resource_record2, "Equal ResourceRecords equality check failed.")
        self.assertFalse(resource_record1 == resource_record3,
                         "Unequal ResourceRecords equality check succeed (unequal data_path).")
        self.assertFalse(resource_record1 == resource_record4,
                         "Unequal ResourceRecords equality check succeed (unequal archive_path).")
        self.assertFalse(resource_record1 == resource_record5,
                         "Unequal ResourceRecords equality check succeed (unequal resource ID).")

    def test_parse_line(self):
        # Correct tests
        correct_record1 = "000\tarchive\tdata\n"
        correct_record2 = "23\t   archive     \t"

        check1 = abstract_service.ResourceRecord(0, "archive", "data")
        check2 = abstract_service.ResourceRecord(23, "archive", None)

        result1 = abstract_service.ResourceRecord.parse_line(correct_record1)
        result2 = abstract_service.ResourceRecord.parse_line(correct_record2)

        self.assertEqual(result1, check1)
        self.assertEqual(result2, check2)

        # Incorrect tests
        wrong_record1 = "id\tarchive\tdata\t"
        wrong_record2 = "10 archive data"
        wrong_record3 = "20\tarchive\n"
        wrong_record4 = "0.2\tarchive\tdata\n"

        self.assertRaises(exceptions.LineParseError, abstract_service.ResourceRecord.parse_line, wrong_record1)
        self.assertRaises(exceptions.LineParseError, abstract_service.ResourceRecord.parse_line, wrong_record2)
        self.assertRaises(exceptions.LineParseError, abstract_service.ResourceRecord.parse_line, wrong_record3)
        self.assertRaises(exceptions.LineParseError, abstract_service.ResourceRecord.parse_line, wrong_record4)

    def test_transformation(self):
        resource_record = abstract_service.ResourceRecord(100, "archive_path", "data_path")

        transformed = abstract_service.ResourceRecord.parse_line(resource_record.__str__())

        self.assertEqual(resource_record, transformed, "ResourceRecord unexpectedly changed after transformation")
