# -*- coding: utf-8 -*-
import mock
import time

from mpfs.core.services.common_service import RequestsPoweredServiceBase, Service
from mpfs.core.services.tvm_2_0_service import TVM2Ticket
from mpfs.engine.process import set_tvm_2_0_service_ticket_for_client
from test.helpers.stubs.manager import (
    StubScope,
    StubsManager
)
from test.helpers.stubs.services import TVMStub
from test.unit.base import NoDBTestCase

from mpfs.engine import process
from mpfs.config import settings
from mpfs.core.services import tvm_service


class TVMTestCase(NoDBTestCase):
    stubs_manager = StubsManager(class_stubs={TVMStub})

    fake_passport_settings = settings.services['tvm']
    fake_passport_settings['ticket_lifetime'] = 3
    old_ticket = 'old_ticket'
    new_ticket = 'new_ticket'

    @classmethod
    def setup_class(cls):
        cls.stubs_manager.enable_stubs(scope=StubScope.CLASS)

    @classmethod
    def teardown_class(cls):
        cls.stubs_manager.disable_stubs(scope=StubScope.CLASS)

    def test_is_expired(self):
        """
        Проверяем, что проверка протухания тикета работает корректно
        """
        with mock.patch.object(tvm_service.TVMTicket, 'ticket_lifespan', new=3):
            now = time.time()
            test_ticket = tvm_service.TVMTicket.build_tvm_ticket(self.old_ticket, is_external=False)
            assert not test_ticket.is_expired()
            with mock.patch.object(time, 'time', return_value=now + 1):
                assert not test_ticket.is_expired()
            with mock.patch.object(time, 'time', return_value=now + 4):
                assert test_ticket.is_expired()

    def test_tvm_ticket_refreshing(self):
        """
        Проверяем, что тикет обновляется до того, как протухнет
        """
        process.set_tvm_ticket(tvm_service.TVMTicket.build_tvm_ticket(self.old_ticket, is_external=False))
        assert process.get_tvm_ticket().value() == self.old_ticket
        with mock.patch('mpfs.core.services.tvm_service.TVMTicket.is_expired', return_value=True):
            assert process.get_tvm_ticket().value() == self.new_ticket

    def test_tvm_save_external_ticket(self):
        """
        Проверяем, что внешний тикет не обновляется, если прошел лайфспан
        """
        process.set_external_tvm_ticket(tvm_service.TVMTicket.build_tvm_ticket(self.old_ticket, is_external=True))
        assert process.get_external_tvm_ticket().value() == self.old_ticket
        with mock.patch('mpfs.core.services.tvm_service.TVMTicket.is_expired', return_value=True):
            assert process.get_external_tvm_ticket().value() == self.old_ticket


class TestSendingTVMToExternalServices(NoDBTestCase):

    def test_sending_ticket_for_reqeust_pwered_service(self):
        service_ticket = 'X'
        set_tvm_2_0_service_ticket_for_client(1, TVM2Ticket.build_tvm_ticket(service_ticket))
        settings.services = {
            'ServiceStub': {
                'base_url': 'http://localhost',
                'tvm_2_0': {
                    'client_id': 1,
                }
            }
        }

        class ServiceStub(RequestsPoweredServiceBase):
            pass

        s = ServiceStub()
        with mock.patch('requests.sessions.Session.send') as mock_send:
            s.request('GET', '')
        assert mock_send.call_args[0][0].headers['X-Ya-Service-Ticket'] == service_ticket

    def test_sending_tickets_for_old_style_services(self):
        service_ticket = 'X'
        url = 'http://localhost'
        set_tvm_2_0_service_ticket_for_client(1, TVM2Ticket.build_tvm_ticket(service_ticket))
        settings.services = {
            'test_service': {
                'base_url': url,
                'tvm_2_0': {
                    'client_id': 1,
                }
            },
            'common': {
                'pass_cloud_request_id': False,
                'timeout': 5.0,
            },
        }

        class ServiceStub(Service):
            name = 'test_service'

        s = ServiceStub()
        with mock.patch('mpfs.engine.http.client.open_url') as mock_openurl:
            s.open_url(url)
        assert mock_openurl.call_args[1]['headers'].get('X-Ya-Service-Ticket') == service_ticket
