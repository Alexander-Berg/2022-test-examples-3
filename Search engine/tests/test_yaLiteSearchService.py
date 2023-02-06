# -*- coding: utf-8 -*-

from tempfile import NamedTemporaryFile
from unittest import TestCase

import mock
from search.pumpkin.yalite_service.libyalite.core import YaLiteCore
from search.pumpkin.yalite_service.libyalite.services.abstract_service import ResourceRecord, DataPackRecord
from search.pumpkin.yalite_service.libyalite.services.search_service import YaLiteSearchService

import utils_for_tests as utils
from search.pumpkin.yalite_service.libyalite.common import exceptions

from search.pumpkin.serp_collector_deploy.lib import (
    S_TESTING,
    S_TEST_OK,
    S_TEST_TIMEOUT,
)


class SearchServiceTester(YaLiteSearchService):
    NAME = 'search-service-example'

    def __init__(self, core):
        super(SearchServiceTester, self).__init__(core=core)


class TestYaLiteSearchService(TestCase):
    ServiceClass = SearchServiceTester

    @classmethod
    def setUpClass(cls):
        print "Testing YaLiteSearchService class:"

    @classmethod
    def tearDownClass(cls):
        print ""

    def update_client(self):
        res = mock.MagicMock()
        self.core.collector.main_sandbox.resource = res
        del res.keys
        d = {
            self.main_resource[u"id"]: self.main_resource,
            self.depend_resource[u"id"]: self.depend_resource
        }
        res.__getitem__.side_effect = lambda k: d[k]

    def setUp(self):
        self.downloads = NamedTemporaryFile()
        self.datapacks = NamedTemporaryFile()

        self.core = YaLiteCore(utils.test_config_path)
        utils.mock_core_cachefiles(self.core, self.downloads, self.datapacks)

        self.search_service = self.ServiceClass(core=self.core)

        self.main_resource, self.depend_resource = utils.generate_test_resource()
        self.update_client()

    def tearDown(self):
        self.downloads.close()
        self.datapacks.close()

    def test__generate_datapack(self):
        get_resource = utils.mock_core_collector_get_resource(self.core)

        self.search_service._generate_datapack(123)
        self.assertTrue(get_resource.called)

        lookup = utils.mock_core_collector_lookup(self.core, self.main_resource)

        self.search_service._generate_datapack()
        self.assertTrue(lookup.called)

    @mock.patch('os.path.isfile')
    @mock.patch('os.path.isdir')
    def test__get_resource_data(self, mock_isdir, mock_isfile):
        mock_isdir.return_value = True
        mock_isfile.return_value = True
        utils.mock_core_collector_deploy(self.core)

        data_generator = [
            {"line": utils.resource_lines["correct"]["full"],
             "count": 10},
            {"line": utils.resource_lines["correct"]["part"],
             "count": 1},
        ]

        utils.fill_testfile(self.downloads, data_generator)

        # Deploy unknown resource
        record = self.search_service._get_resource_data(resource=self.main_resource,
                                                        data_object=self.search_service.data_list[0])
        check = ResourceRecord(self.main_resource[u"id"], "archive_path", "data_path")

        self.assertEqual(record, check)
        self.assertTrue(self.core.collector.deploy.called)
        self.core.collector.deploy.called = False

        # Get record for already downloaded, but not unpacked resource
        mock_isdir.return_value = False
        record = self.search_service._get_resource_data(resource=8,
                                                        data_object=self.search_service.data_list[1])
        self.assertEqual(record, check)
        self.assertTrue(self.core.collector.deploy.called)
        self.core.collector.deploy.called = False

        # Get the record for already downloaded and unpacked resource
        mock_isdir.return_value = True
        record = self.search_service._get_resource_data(resource=8, data_object=None)
        check = ResourceRecord(8, "/path/to/archive/archive_2008.09.09_00-00",
                               "/path/to/extracted/data/2008.09.09_00-00")

        self.assertEqual(record, check)
        self.assertTrue(not self.core.collector.deploy.called)

    def test__test_data(self):
        # Patch 'run_sandbox_task' function to return mocks instead of running real sandbox task.
        task = mock.Mock()
        task.id = 123456789
        api = mock.Mock()
        api.wait_task.return_value = True
        run_task = mock.Mock()
        run_task.return_value = task, api

        self.core.run_sandbox_task = run_task

        self.search_service._test_data()

        self.assertTrue(run_task.called, "run_sandbox_task core method was not called.")
        self.assertTrue(api.wait_task.called, "run_sandbox_task was not waiting for test task completion.")
        self.assertEqual(api.wait_task.call_args, ((task.id,), {"timeout": 3600}))

    def test_list_depends(self):
        # self.update_client()
        main_resource = self.main_resource

        deps = self.search_service.list_depends(main_resource)

        self.assertEqual(deps.main_resource, main_resource)
        self.assertEqual(deps.ids, [123, 321])
        self.assertEqual(deps.resources, [self.main_resource, self.depend_resource])
        self.assertEqual(self.core.collector.main_sandbox.resource.__getitem__.call_args[0], (321,))

    def test_get_data(self):
        datapack = DataPackRecord(resources=[self.main_resource, self.depend_resource])

        mock_get_resource_data = mock.Mock()
        mock_create_dirs = mock.Mock()

        self.search_service._get_resource_data = mock_get_resource_data
        self.search_service._create_data_dirs = mock_create_dirs

        self.search_service.get_data(datapack=datapack)

        self.assertEqual(mock_get_resource_data.call_args_list[0], ((), {"resource": self.main_resource,
                                                                         "data_object": self.search_service.data_list[
                                                                             0]}))
        self.assertEqual(mock_get_resource_data.call_args_list[1], ((), {"resource": self.depend_resource,
                                                                         "data_object": self.search_service.data_list[
                                                                             1]}))
        self.assertTrue(mock_create_dirs.called)

    @mock.patch('os.path.isfile')
    @mock.patch('os.path.isdir')
    def test_switch_service(self, mock_isdir, mock_isfile):
        mock_isdir.return_value = True
        mock_isfile.return_value = True

        restart = mock.Mock()
        self.search_service.restart = restart
        switch_data = mock.Mock()
        self.search_service._switch_data = switch_data

        resource_records_generator = [
            {"line": utils.resource_lines["correct"]["full"],
             "count": 5},
            {"line": utils.resource_lines["correct"]["part"],
             "count": 5},
            {"line": utils.resource_lines["correct"]["full"],
             "count": 5},
        ]

        utils.fill_testfile(self.downloads, resource_records_generator)

        datapack_records_generator = [
            {"line": "{l0}\t{l1}\n",
             "count": 5}
        ]

        utils.fill_testfile(self.datapacks, datapack_records_generator)

        # ResourceRecord for given identificator is present, DataPackRecord is present, resource is unpacked.
        self.search_service.switch_service(0)

        self.assertEqual(switch_data.call_args_list[0][0],
                         (self.search_service.serp_collection, "/path/to/extracted/data/2000.01.01_00-00"))
        self.assertEqual(switch_data.call_args_list[1][0],
                         (self.search_service.generalization_index, "/path/to/extracted/data/2001.02.02_00-00"))
        self.assertTrue(restart.called, "Restart service command was not called.")

        switch_data.reset_mock()
        restart.reset_mock()

        # Same conditions, but do not restart service after switching.
        self.search_service.switch_service(0, restart='norestart')

        self.assertEqual(switch_data.call_args_list[0][0],
                         (self.search_service.serp_collection, "/path/to/extracted/data/2000.01.01_00-00"))
        self.assertEqual(switch_data.call_args_list[1][0],
                         (self.search_service.generalization_index, "/path/to/extracted/data/2001.02.02_00-00"))
        self.assertTrue(not restart.called, "Restart service command was called.")

        switch_data.reset_mock()
        restart.reset_mock()

        # ResourceRecord for given identificator is present, DataPackRecord is present, resource is NOT unpacked
        self.assertRaises(exceptions.YaLiteActionError, self.search_service.switch_service, 4)
        self.assertRaises(exceptions.YaLiteActionError, self.search_service.switch_service, 5)

        # ResourceRecord for given identificator is present, DataPackRecord is NOT present.
        self.assertRaises(exceptions.YaLiteActionError, self.search_service.switch_service, 12)

        # ResourceRecord for given identificator is NOT present.
        self.assertRaises(exceptions.YaLiteActionError, self.search_service.switch_service, 30)

    @mock.patch('time.time')
    def test_mark_timeouted(self, mock_time):
        # Resource with timeouted test timestamp attribute: set_collection_status called.
        mock_lookup = utils.mock_core_collector_lookup(self.core, self.main_resource)
        mock_time.return_value = 200000

        self.core.collector.main_sandbox = mock.MagicMock()
        main_resource_update_mock = mock.Mock()
        self.core.collector.main_sandbox.resource[self.main_resource[u"id"]].update = main_resource_update_mock

        self.search_service.mark_timeouted()

        resource_status = self.main_resource[u'attributes']['status']
        self.assertEqual(resource_status, S_TEST_TIMEOUT,
                         "Resource status is wrong: {0}, but should be {1}.".format(resource_status, S_TEST_TIMEOUT))

        status_host = self.main_resource[u"attributes"]['status_host']
        self.assertEqual(status_host, 'some_test_host_name',
                         "Resource status host was changed: {0}. "
                         "It should stay unchanged on test timeout resource mark.".format(status_host))

        status_timestamp = self.main_resource[u"attributes"]['status_timestamp']
        self.assertEqual(status_timestamp, 200000,
                         "resource status timestamp is wrong: {0}".format(status_timestamp))

        self.assertTrue(main_resource_update_mock.called,
                        "Resource attribute was possibly changed directly"
                        "instead of using sandbox.resource[id].update function.")
        self.assertTrue(mock_lookup.called, "Lookup resource function was not called on mark_timeouted.")

        # Resource with not timeouted test timestamp attribute: set_collection_status not called.
        main_resource, depend_resource = utils.generate_test_resource()
        mock_lookup = utils.mock_core_collector_lookup(self.core, main_resource)
        main_resource_update_mock = mock.Mock()
        self.core.collector.main_sandbox.resource[self.main_resource[u"id"]].update = main_resource_update_mock

        self.core.config.testing_timeout = float('inf')

        self.search_service.mark_timeouted()

        resource_status = main_resource[u"attributes"]['status']
        self.assertEqual(resource_status, S_TEST_OK,
                         "Resource status is wrong: {0}, but should be {1}".format(resource_status, S_TEST_OK))

        status_host = main_resource[u"attributes"]['status_host']
        self.assertEqual(status_host, 'some_test_host_name',
                         "Resource status host was changed: {0}. "
                         "It should stay unchanged on test timeout resource mark.".format(status_host))

        status_timestamp = main_resource[u"attributes"]['status_timestamp']
        self.assertEqual(status_timestamp, 100000,
                         "resource status timestamp is wrong: {0}".format(status_timestamp))

        self.assertTrue(mock_lookup.called, "Lookup resource function was not called on mark_timeouted.")
        self.assertTrue(not main_resource_update_mock.called,
                        "Test was not timeouted, so sandbox.resource[id].update mustn't be called.")

        # self.assertTrue(False, "It's a plug. Implement real test instead.")

    @mock.patch('os.makedirs')
    def test_test_service(self, makedirs):
        mock_mark_timeouted = mock.Mock()
        self.search_service.mark_timeouted = mock_mark_timeouted

        main_resource, depend_resource = utils.generate_test_resource()

        mock__test_data = mock.Mock()
        mock__test_data.return_value = True
        self.search_service._test_data = mock__test_data

        mock_switch_service = mock.Mock()
        self.search_service.switch_service = mock_switch_service

        utils.mock_core_collector_deploy(self.core)

        mock_run_action = mock.Mock()
        self.core.run_action = mock_run_action

        # Just reference
        main_resource_mock = self.core.collector.main_sandbox.resource[main_resource[u"id"]]
        depend_resource_mock = self.core.collector.main_sandbox.resource[depend_resource[u"id"]]
        self.search_service.test_service(123)

        # Check for timeouted tests.
        self.assertTrue(mock_mark_timeouted.called, "Resources had not been checked for timeout.")
        # self.assertTrue(mock_get_resource.called,
        #                 "Resource information had not been fetched from sandbox (get_resource_failsafe).")

        # Lock resources for testing
        self.assertIn(mock.call({"status": S_TESTING}),
                      main_resource_mock.update.call_args_list,
                      "Main resource possibly had not been locked for testing.")
        self.assertIn(mock.call({"status": S_TESTING}),
                      depend_resource_mock.update.call_args_list,
                      "Dependent resource possibly had not been locked for testing.")

        # Save current HTTP check state
        self.assertEqual(mock_run_action.call_args_list[0][0], ("status", "http"),
                         "Service state had not been saved before test running.")

        # Disable service.
        self.assertEqual(mock_run_action.call_args_list[1][0], ("disable", "http"),
                         "Service had not been disabled before test running.")

        # Switch data to new set and test it
        self.assertTrue(mock_switch_service.called,
                        "Service possibly had not been switched to new data for testing.")
        self.assertTrue(mock__test_data.called,
                        "Data test had not been run")

        # Mark data as successfully tested.
        self.assertIn(mock.call({"status": S_TEST_OK}),
                      main_resource_mock.update.call_args_list,
                      "Main resource possibly had not been marked as 'tested'.")
        self.assertIn(mock.call({"status": S_TEST_OK}),
                      depend_resource_mock.update.call_args_list,
                      "Dependent resource possibly had not been marked as 'tested'.")
