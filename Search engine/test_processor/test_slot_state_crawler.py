# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import six
import ujson

from google.protobuf.json_format import MessageToDict

from infra.callisto.protos.multibeta.slot_state_pb2 import Response

from search.priemka.yappy.proto.structures.beta_component_pb2 import BetaComponent
from search.priemka.yappy.proto.structures.slot_pb2 import Slot
from search.priemka.yappy.src.processor.modules.crawler.slot_state_crawler import SlotStateCrawler
from search.priemka.yappy.src.yappy_lib.config_utils import get_config
from search.priemka.yappy.tests.utils.test_cases import TestCase

from yp import data_model

if six.PY2:
    import mock
else:
    import unittest.mock as mock


class CrawlStateTestCase(TestCase):
    @classmethod
    def setUpClass(cls):
        cls.crawler = SlotStateCrawler(config=get_config())


class CrawlNannyStateTestCase(CrawlStateTestCase):
    def setUp(self):
        self.get_current_state_patch = mock.patch.object(self.crawler.clients.nanny, 'get_current_state')
        self.get_current_state = self.get_current_state_patch.start()
        self.get_last_snapshot_state_patch = mock.patch.object(self.crawler.clients.nanny, 'get_last_snapshot_state')
        self.get_last_snapshot_state = self.get_last_snapshot_state_patch.start()
        self.addCleanup(self.get_current_state_patch.stop)
        self.addCleanup(self.get_last_snapshot_state_patch.stop)

    def test_do_not_manage(self):
        slot = Slot(id='slot')
        state_value = 'ONLINE'
        current_state = {'content': {'summary': {'value': state_value, 'entered': 123}}}
        self.get_current_state.return_value = current_state
        self.crawler.crawl_nanny_slot_state(slot, do_not_manage=True)
        self.assertEqual(slot.state, Slot.State[state_value])

    def test_managed_slot(self):
        slot = Slot(id='slot')
        state_value = 'ONLINE'
        last_state = state_value
        self.get_last_snapshot_state.return_value = {'value': last_state, 'entered': 123}
        self.crawler.crawl_nanny_slot_state(slot, do_not_manage=False)
        self.assertEqual(slot.state, Slot.State[state_value])

    def test_process_call(self):
        component = BetaComponent(slot=Slot(id='slot', type=Slot.Type.NANNY), do_not_manage=True)
        with mock.patch.object(self.crawler, 'crawl_nanny_slot_state') as crawl:
            self.crawler.process(component)
            crawl.assert_called_with(component.slot, component.do_not_manage)

    def test_process_result(self):
        component = BetaComponent(slot=Slot(id='slot', type=Slot.Type.NANNY), do_not_manage=True)
        expected = Slot.State.ONLINE
        def set_state(slot, *args, **kwargs):
            slot.state = expected
        with mock.patch.object(self.crawler, 'crawl_nanny_slot_state', side_effect=set_state):
            self.crawler.process(component)
            self.assertEqual(component.slot.state, expected)


class CrawlBaseSlotState(CrawlStateTestCase):
    def setUp(self):
        self.get_slot_state_patch = mock.patch.object(self.crawler.clients.callisto, 'get_slot_state')
        self.get_slot_state = self.get_slot_state_patch.start()
        self.addCleanup(self.get_slot_state_patch.stop)

    @classmethod
    def _setup_state_tests(cls):
        state_map = {
            Slot.State.ONLINE: Response.READY,
            Slot.State.UPDATING: Response.WAIT_FOR_QUORUM,
            Slot.State.UNKNOWN: Response.UNKNOWN,
        }
        for expected, state_value in state_map.items():
            def test(self):
                self.get_slot_state.return_value = Response(status=state_value)
                slot = Slot(id='slot')
                self.crawler.crawl_base_controller_slot_state(slot, revision=123)
                self.assertEqual(slot.state, expected)
            setattr(cls, 'test_state_{}'.format(Slot.State[expected].lower()), test)

    def test_response_result(self):
        expected_response = Response(status=Response.WAIT_FOR_QUORUM)
        expected = ujson.dumps(MessageToDict(expected_response))
        self.get_slot_state.return_value = expected_response
        slot = Slot(id='slot')
        self.crawler.crawl_base_controller_slot_state(slot, revision=123)
        self.assertEqual(slot.callisto_response, expected)

    def test_process_call(self):
        component = BetaComponent(slot=Slot(id='slot', type=Slot.Type.BASE_CONTROLLER))
        component.target_state.revision = 123
        with mock.patch.object(self.crawler, 'crawl_base_controller_slot_state') as crawl:
            self.crawler.process(component)
            crawl.assert_called_with(component.slot, component.target_state.revision)

    def test_process_state_result(self):
        component = BetaComponent(slot=Slot(id='slot', type=Slot.Type.BASE_CONTROLLER))
        component.target_state.revision = 123
        expected = Slot.State.ONLINE
        def set_state(slot, *args, **kwargs):
            slot.state = expected
        with mock.patch.object(self.crawler, 'crawl_base_controller_slot_state', side_effect=set_state):
            self.crawler.process(component)
            self.assertEqual(component.slot.state, expected)

    def test_process_response_result(self):
        component = BetaComponent(slot=Slot(id='slot', type=Slot.Type.BASE_CONTROLLER))
        component.target_state.revision = 123
        expected_response = Response(status=Response.WAIT_FOR_QUORUM)
        expected = ujson.dumps(MessageToDict(expected_response))
        def set_response(slot, *args, **kwargs):
            slot.callisto_response = expected
        with mock.patch.object(self.crawler, 'crawl_base_controller_slot_state', side_effect=set_response):
            self.crawler.process(component)
            self.assertEqual(component.slot.callisto_response, expected)


class CrawlDeploySlotState(CrawlStateTestCase):
    def setUp(self):
        self.deploy_patch = mock.patch.object(
            self.crawler.clients.__class__, 'deploy', new_callable=mock.PropertyMock()
        )
        self.deploy = self.deploy_patch.start()
        self.get_stage = self.deploy.get_stage
        self.addCleanup(self.deploy_patch.stop)

    @classmethod
    def _setup_state_tests(cls):
        status_TRUE = data_model.EConditionStatus.EConditionStatus_TRUE
        state_map = {
            Slot.State.BROKEN: mock.Mock(**{'status.validated.status': None}),
            Slot.State.PREPARING: mock.Mock(
                **{
                    'status.validated.status': status_TRUE,
                    'status.deploy_units': {
                        'dummy': mock.Mock(**{'ready.status': status_TRUE}),
                        'dummy-2': mock.Mock(**{'ready.status': None}),
                    }
                }
            ),
            Slot.State.UPDATING: mock.Mock(
                **{
                    'status.validated.status': status_TRUE,
                    'status.deploy_units': {
                        'dummy': mock.Mock(**{'in_progress.status': status_TRUE}),
                        'dummy-2': mock.Mock(**{'in_progress.status': None}),
                    }
                }
            ),
            Slot.State.ONLINE: mock.Mock(
                **{
                    'status.validated.status': status_TRUE,
                    'status.deploy_units': {},
                }),
        }
        for expected, stage_value in state_map.items():
            def test(self):
                self.get_stage.return_value = stage_value
                slot = Slot(id='slot')
                self.crawler.crawl_deploy_slot_state(slot)
                self.assertEqual(slot.state, expected)
            setattr(cls, 'test_state_{}'.format(Slot.State[expected].lower()), test)

    def test_process_call(self):
        component = BetaComponent(slot=Slot(id='slot', type=Slot.Type.DEPLOY))
        with mock.patch.object(self.crawler, 'crawl_deploy_slot_state') as crawl:
            self.crawler.process(component)
            crawl.assert_called_with(component.slot)

    def test_process_state_result(self):
        component = BetaComponent(slot=Slot(id='slot', type=Slot.Type.DEPLOY))
        expected = Slot.State.ONLINE
        def set_state(slot, *args, **kwargs):
            slot.state = expected
        with mock.patch.object(self.crawler, 'crawl_deploy_slot_state', side_effect=set_state):
            self.crawler.process(component)
            self.assertEqual(component.slot.state, expected)


CrawlBaseSlotState._setup_state_tests()
CrawlDeploySlotState._setup_state_tests()
