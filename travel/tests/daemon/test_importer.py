# -*- coding: utf-8 -*-
import unittest
from functools import partial

from gevent import Timeout
from mock import patch, Mock, MagicMock
from requests import RequestException
from travel.avia.library.python.tester.factories import create_partner
from travel.avia.library.python.tester.testcase import TestCase
from travel.proto.avia.wizard.search_result_pb2 import SearchResult

from travel.avia.ticket_daemon.tests.daemon import fake_module_without_query, fake_module_with_None_query
from travel.avia.ticket_daemon.ticket_daemon.api.query import QueryIsNotValid
from travel.avia.ticket_daemon.ticket_daemon.api.result import Statuses
from travel.avia.ticket_daemon.ticket_daemon.daemon.big_beauty_collector import BigBeautyCollectorByPartner
from travel.avia.ticket_daemon.ticket_daemon.daemon.importer import ModuleImporter, Importer
from travel.avia.ticket_daemon.ticket_daemon.daemon.utils import BadPartnerResponse

DEFAULT_PARTNER_CODE = 'test_partner'


def raise_Exception(*args):
    raise Exception


def raise_BadPartnerResponse(*args):
    raise BadPartnerResponse(Mock(), Mock())


def raise_RequestException(*args):
    raise RequestException()


def return_None(*args, **kwargs):
    return None


class QueryMock(object):
    def __init__(self):
        self._passed_arguments = {}
        self.id = 'test_qid'
        self.partners = [create_partner(code=DEFAULT_PARTNER_CODE)]
        self.lang = 'ru'

    def validate(self, arg):
        self._passed_arguments['validate'] = (arg,)

    def validate_args(self):
        return self._passed_arguments['validate']

    def key(self):
        return 'test_qkey'

    @property
    def qkey(self):
        return 'test_qkey'


class TestModuleImporter(unittest.TestCase):
    def test_import_module_should_return_imported_module_if_module_exists(self):
        existing_module_path = 'travel.avia.ticket_daemon.tests.daemon.fake_module_without_query'
        imported_module = ModuleImporter.import_module(existing_module_path)

        assert imported_module == fake_module_without_query

    def test_import_module_should_return_None_if_module_is_absent(self):
        absent_module = 'travel.avia.ticket_daemon.tests.daemon.absent_module'
        imported_module = ModuleImporter.import_module(absent_module)

        assert imported_module is None


class TestImporter(TestCase):
    def setUp(self):
        self.query_mock = QueryMock()
        self.importer = self.create_importer()
        self.patches = {
            'travel.avia.ticket_daemon.ticket_daemon.api.result.cache_backends.shared_cache': Mock(),
            'gevent.spawn': lambda method, *args, **kwargs: method(*args, **kwargs),
            'travel.avia.ticket_daemon.ticket_daemon.daemon.importer.HarvesterClosingDb': MagicMock(),
        }
        self.applied_patches = [
            patch(_patch, data) for _patch, data in self.patches.iteritems()
        ]
        [_patch.start() for _patch in self.applied_patches]

    def tearDown(self):
        patch.stopall()

    def create_importer(self):
        partners_mock = [create_partner(code='code')]

        query_mock = Mock()
        response_collector_mock = Mock()
        flight_fabric_mock = Mock()
        big_beauty_collectors_mock = [Mock()]

        return Importer(
            'code',
            partners_mock,
            query_mock,
            response_collector_mock,
            flight_fabric_mock,
            big_beauty_collectors_mock,
        )

    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.ModuleImporter.import_module', return_value=None)
    def test_prepare_query_chunk_should_call_import_module_with_(self, mocked_import_module):
        partners_modules_prefix = 'travel.avia.ticket_daemon.ticket_daemon.partners.'
        code_value = 'code'
        try:
            Importer.prepare_chunked_queryfun(code_value, self.query_mock)
        except:
            pass

        mocked_import_module.assert_called_with(partners_modules_prefix + code_value)

    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.ModuleImporter.import_module', return_value=None)
    def test_prepare_query_chunk_should_raise_QueryIsNotValid_if_ModuleImporter_returned_None(self, mocked_import_module):
        self.assertRaises(QueryIsNotValid, partial(Importer.prepare_chunked_queryfun, 'code', self.query_mock))

    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.ModuleImporter.import_module')
    def test_prepare_query_chunk_should_call_query_validate(self, mocked_import_module):
        o = object()
        mocked_import_module.return_value = o

        try:
            Importer.prepare_chunked_queryfun('code', self.query_mock)
        except:
            pass

        assert self.query_mock.validate_args()[0] is o

    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.ModuleImporter.import_module', return_value=fake_module_without_query)
    def test_prepare_query_chunk_should_raise_QueryIsNotValid_if_module_has_no_query_method(self, mocked_import_module):
        self.assertRaises(QueryIsNotValid, partial(Importer.prepare_chunked_queryfun, 'code', self.query_mock))

    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.ModuleImporter.import_module', return_value=fake_module_with_None_query)
    def test_prepare_query_chunk_should_raise_QueryIsNotValid_if_module_does_not_have_normal_query(self, mocked_import_module):
        self.assertRaises(QueryIsNotValid, partial(Importer.prepare_chunked_queryfun, 'code', self.query_mock))

    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer_dialog.ImporterDialog.write_dialog_status')
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.Importer.prepare_chunked_queryfun')
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.variants_saver.VariantsSaver.store_skip')
    def test_do_import_should_set_skip_status_if_prepare_chunked_queryfun_raises_QueryIsNotValidException(
            self, store_skip_mock, prepare_chunked_queryfun_mock, write_dialog_status_mock
    ):
        prepare_chunked_queryfun_mock.side_effect = QueryIsNotValid('query is not valid')

        self.importer.do_import()

        assert write_dialog_status_mock.call_count == 1
        assert write_dialog_status_mock.call_args[0][0] == Statuses.SKIP

    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.Importer.prepare_chunked_queryfun', return_value=raise_BadPartnerResponse)
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.variants_saver.VariantsSaver.set_failure')
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.variants_saver.VariantsSaver.finalize')
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.Importer.got_bad_partner_response')
    def test_do_import_should_set_failure_if_BadPartnerResponseException_was_raised(
            self, got_bad_partner_response_mock, finalize_mock, set_failure_mock, prepare_chunked_queryfun_mock
    ):
        self.importer.do_import()

        assert set_failure_mock.call_count == 1
        assert got_bad_partner_response_mock.call_count == 1
        assert finalize_mock.call_count == 1

    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.log_flow')
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.log_yt_partners_query')
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.log_error_responses')
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer_dialog.ImporterDialog.write_dialog_status')
    def test_got_bad_partner_response_writes_errors_to_yt_log(
            self, write_dialog_status_mock, log_error_responses_mock, log_yt_partners_query_mock , log_flow_mock
    ):
        test_error_message = 'test_error'
        exception = BadPartnerResponse(Mock(), Mock(), errors=test_error_message)
        self.importer.got_bad_partner_response(exception)
        assert log_yt_partners_query_mock.log.call_count == 1
        assert log_yt_partners_query_mock.log.call_args_list[0][0][0]['errors'] == test_error_message

    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.Importer.prepare_chunked_queryfun', return_value=raise_RequestException)
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.variants_saver.VariantsSaver.set_failure')
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.variants_saver.VariantsSaver.finalize')
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.Importer.got_failure')
    def test_do_import_should_set_failure_if_RequestException_was_raised(
            self, got_failure_mock, finalize_mock, set_failure_mock, prepare_chunked_queryfun_mock
    ):
        self.importer.do_import()

        assert got_failure_mock.call_count == 1
        assert set_failure_mock.call_count == 1
        assert finalize_mock.call_count == 1

    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.Importer.prepare_chunked_queryfun', return_value=raise_Exception)
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.variants_saver.VariantsSaver.set_failure')
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.variants_saver.VariantsSaver.finalize')
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.Importer.got_failure')
    def test_do_import_should_set_failure_if_Exception_was_raised(
            self, got_failure_mock, finalize_mock, set_failure_mock, prepare_chunked_queryfun_mock
    ):
        self.importer.do_import()

        assert got_failure_mock.call_count == 1
        assert set_failure_mock.call_count == 1
        assert finalize_mock.call_count == 1

    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.consume_with_whole_timeout', side_effect=Timeout())
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.Importer.prepare_chunked_queryfun')
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.variants_saver.VariantsSaver.set_failure')
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.variants_saver.VariantsSaver.finalize')
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.Importer.got_timeout')
    def test_do_import_should_set_failure_if_Timeout_by_gevent_was_raised(
            self, got_timeout, finalize_mock, set_failure_mock, prepare_chunked_queryfun_mock, consume_with_whole_timeout_mock
    ):
        self.importer.do_import()

        assert got_timeout.call_count == 1
        assert set_failure_mock.call_count == 1
        assert finalize_mock.call_count == 1

    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.bbc.get_stored_search_result', Mock(return_value=SearchResult()))
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.bbc.get_stored_search_results_by_partner', Mock(return_value={}))
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.big_beauty_collector.BigBeautyCollector.store', new=MagicMock())
    def test_build_collectors_wizard_caches_isnt_specified_and_flag_is_disabled(self):
        self.query_mock.meta = {}

        with patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.feature_flags.store_experimental_wizard_results', return_value=False):
            collectors = Importer._build_big_beauty_collectors(self.query_mock)

        assert len(collectors) == 1
        assert not collectors[0]._is_experimental

    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.bbc.get_stored_search_result', Mock(return_value=SearchResult()))
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.bbc.get_stored_search_results_by_partner', Mock(return_value={}))
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.big_beauty_collector.BigBeautyCollector.store', new=MagicMock())
    def test_build_collectors_wizard_caches_isnt_specified_and_flag_is_enabled(self):
        self.query_mock.meta = {}

        with patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.feature_flags.store_experimental_wizard_results', return_value=True):
            collectors = Importer._build_big_beauty_collectors(self.query_mock)

        assert len(collectors) == 2
        assert not collectors[0]._is_experimental
        assert collectors[1]._is_experimental

    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.bbc.get_stored_search_result', Mock(return_value=SearchResult()))
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.bbc.get_stored_search_results_by_partner', Mock(return_value={}))
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.big_beauty_collector.BigBeautyCollector.store', new=MagicMock())
    def test_build_collectors_wizard_caches_is_empty_and_flag_is_disabled(self):
        self.query_mock.meta = {'wizard_caches': []}

        with patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.feature_flags.store_experimental_wizard_results', return_value=False):
            collectors = Importer._build_big_beauty_collectors(self.query_mock)

        assert len(collectors) == 1
        assert not collectors[0]._is_experimental

    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.bbc.get_stored_search_result', Mock(return_value=SearchResult()))
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.bbc.get_stored_search_results_by_partner', Mock(return_value={}))
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.big_beauty_collector.BigBeautyCollector.store', new=MagicMock())
    def test_build_collectors_wizard_caches_is_empty_and_flag_is_enabled(self):
        self.query_mock.meta = {'wizard_caches': []}

        with patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.feature_flags.store_experimental_wizard_results', return_value=True):
            collectors = Importer._build_big_beauty_collectors(self.query_mock)

        assert len(collectors) == 2
        assert not collectors[0]._is_experimental
        assert collectors[1]._is_experimental

    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.bbc.get_stored_search_result', Mock(return_value=SearchResult()))
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.bbc.get_stored_search_results_by_partner', Mock(return_value={}))
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.big_beauty_collector.BigBeautyCollector.store', new=MagicMock())
    def test_build_collectors_wizard_caches_contains_only_main_cache(self):
        self.query_mock.meta = {'wizard_caches': ['wizard_results']}

        collectors = Importer._build_big_beauty_collectors(self.query_mock)

        assert len(collectors) == 1
        assert not collectors[0]._is_experimental

    def test_build_collectors_wizard_caches_contains_only_experimental_cache_and_flag_is_disabled(self):
        self.query_mock.meta = {'wizard_caches': ['wizard_results_experimental']}

        with patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.feature_flags.store_experimental_wizard_results', return_value=False):
            collectors = Importer._build_big_beauty_collectors(self.query_mock)

        assert len(collectors) == 0

    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.bbc.get_stored_search_result', Mock(return_value=SearchResult()))
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.bbc.get_stored_search_results_by_partner', Mock(return_value={}))
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.big_beauty_collector.BigBeautyCollector.store', new=MagicMock())
    def test_build_collectors_wizard_caches_contains_only_experimental_cache_and_flag_is_enabled(self):
        self.query_mock.meta = {'wizard_caches': ['wizard_results_experimental']}

        with patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.feature_flags.store_experimental_wizard_results', return_value=True):
            collectors = Importer._build_big_beauty_collectors(self.query_mock)

        assert len(collectors) == 1
        assert collectors[0]._is_experimental

    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.bbc.get_stored_search_result', Mock(return_value=SearchResult()))
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.bbc.get_stored_search_results_by_partner', Mock(return_value={}))
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.big_beauty_collector.BigBeautyCollector.store', new=MagicMock())
    def test_build_collectors_wizard_caches_contains_both_caches_and_flag_is_disabled(self):
        self.query_mock.meta = {'wizard_caches': ['wizard_results', 'wizard_results_experimental']}

        with patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.feature_flags.store_experimental_wizard_results', return_value=False):
            collectors = Importer._build_big_beauty_collectors(self.query_mock)

        assert len(collectors) == 1
        assert not collectors[0]._is_experimental

    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.bbc.get_stored_search_result', Mock(return_value=SearchResult()))
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.bbc.get_stored_search_results_by_partner', Mock(return_value={}))
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.big_beauty_collector.BigBeautyCollector.store', new=MagicMock())
    def test_build_collectors_wizard_caches_contains_both_caches_and_flag_is_enabled(self):
        self.query_mock.meta = {'wizard_caches': ['wizard_results', 'wizard_results_experimental']}

        with patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.feature_flags.store_experimental_wizard_results', return_value=True):
            collectors = Importer._build_big_beauty_collectors(self.query_mock)

        assert len(collectors) == 2
        assert not collectors[0]._is_experimental
        assert collectors[1]._is_experimental

    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.bbc.get_stored_search_result', Mock(return_value=SearchResult()))
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.bbc.get_stored_search_results_by_partner', Mock(return_value={DEFAULT_PARTNER_CODE: SearchResult()}))
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.big_beauty_collector.BigBeautyCollector.store', new=MagicMock())
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.feature_flags.store_experimental_wizard_results', Mock(return_value=True))
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.WRITE_IN_PARTNER_CACHE', Mock(return_value=True))
    def test_build_collectors_wizard_caches_contains_partner_caches_with_env_enabled_and_flag_is_enabled(self):
        self.query_mock.meta = {'wizard_caches': ['wizard_results', 'wizard_results_experimental']}
        self.query_mock.partners[0].is_aviacompany = False

        collectors = Importer._build_big_beauty_collectors(self.query_mock)
        collectors_by_partner = [c for c in collectors if isinstance(c, BigBeautyCollectorByPartner)]

        assert len(collectors_by_partner) == 1

    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.bbc.get_stored_search_result', Mock(return_value=SearchResult()))
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.bbc.get_stored_search_results_by_partner', Mock(return_value={DEFAULT_PARTNER_CODE: SearchResult()}))
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.big_beauty_collector.BigBeautyCollector.store', new=MagicMock())
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.feature_flags.store_experimental_wizard_results', Mock(return_value=False))
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.WRITE_IN_PARTNER_CACHE', Mock(return_value=True))
    def test_build_collectors_wizard_caches_contains_partner_caches_with_env_enabled_and_flag_is_disabled(self):
        self.query_mock.meta = {'wizard_caches': ['wizard_results', 'wizard_results_experimental']}
        self.query_mock.partners[0].is_aviacompany = False

        collectors = Importer._build_big_beauty_collectors(self.query_mock)
        collectors_by_partner = [c for c in collectors if isinstance(c, BigBeautyCollectorByPartner)]

        assert len(collectors_by_partner) == 1

    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.bbc.get_stored_search_result', Mock(return_value=SearchResult()))
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.bbc.get_stored_search_results_by_partner', Mock(return_value={}))
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.big_beauty_collector.BigBeautyCollector.store', new=MagicMock())
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.feature_flags.store_experimental_wizard_results', Mock(return_value=True))
    def test_build_collectors_wizard_caches_contains_partner_caches_with_env_disabled_and_flag_is_enabled(self):
        self.query_mock.meta = {'wizard_caches': ['wizard_results', 'wizard_results_experimental']}

        collectors = Importer._build_big_beauty_collectors(self.query_mock)
        collectors_by_partner = [c for c in collectors if isinstance(c, BigBeautyCollectorByPartner)]

        assert len(collectors_by_partner) == 0

    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.bbc.get_stored_search_result', Mock(return_value=SearchResult()))
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.bbc.get_stored_search_results_by_partner', Mock(return_value={}))
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.big_beauty_collector.BigBeautyCollector.store', new=MagicMock())
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.feature_flags.store_experimental_wizard_results', Mock(return_value=False))
    def test_build_collectors_wizard_caches_contains_partner_caches_with_env_disabled_and_flag_is_disabled(self):
        self.query_mock.meta = {'wizard_caches': ['wizard_results', 'wizard_results_experimental']}

        collectors = Importer._build_big_beauty_collectors(self.query_mock)
        collectors_by_partner = [c for c in collectors if isinstance(c, BigBeautyCollectorByPartner)]

        assert len(collectors_by_partner) == 0

    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.bbc.get_stored_search_result', Mock(return_value=SearchResult()))
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.bbc.get_stored_search_results_by_partner', Mock(return_value={}))
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.big_beauty_collector.BigBeautyCollector.store', new=MagicMock())
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.feature_flags.store_experimental_wizard_results', Mock(return_value=True))
    @patch('travel.avia.ticket_daemon.ticket_daemon.daemon.importer.WRITE_IN_PARTNER_CACHE', Mock(return_value=True))
    def test_build_collectors_with_empty_partners_wizard_caches_contains_partner_caches_with_env_enabled_and_flag_is_enabled(self):
        self.query_mock.meta = {'wizard_caches': ['wizard_results', 'wizard_results_experimental']}
        self.query_mock.partners = []

        collectors = Importer._build_big_beauty_collectors(self.query_mock)
        collectors_by_partner = [c for c in collectors if isinstance(c, BigBeautyCollectorByPartner)]

        assert len(collectors_by_partner) == 0
