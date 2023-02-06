# -*- coding: utf-8 -*-
from attrdict import AttrDict
import mock

from mpfs.common.util import to_json
from mpfs.core.services.tvm_daemon_service import TVMDaemonService
from mpfs.core.services.tvm_service import TVMBaseTicket
from test.unit.base import NoDBTestCase


SERVICE_TICKET = '3:serv:xxx'
TVM_DAEMON_RESPONSE = AttrDict({
            'content': to_json({'service-name': {'ticket': SERVICE_TICKET, 'tvm_id': 1}})
})


class TVMDaemonServiceTestCase(NoDBTestCase):

    def test_correctly_fetching_ticket_from_response(self):
        with mock.patch('mpfs.core.services.tvm_daemon_service.TVMDaemonService.request', return_value=TVM_DAEMON_RESPONSE):
            ticket = TVMDaemonService().get_service_ticket_for_client(1)
        assert isinstance(ticket, TVMBaseTicket)
        assert ticket.value() == SERVICE_TICKET
