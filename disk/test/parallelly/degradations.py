import mock
from attrdict import AttrDict
from parameterized import parameterized

from mpfs.common.static import codes
from mpfs.config import settings
from mpfs.engine.process import setup_tvm_2_0_clients
from test.base import DiskTestCase


class DegradationsTestCase(DiskTestCase):
    @parameterized.expand([
        ('all disable', {'enabled': False, 'by_path': {}}, False),
        ('all correct and disable', {'enabled': False, 'by_path': {'/json/user_info': [1]}}, False),
        ('all correct', {'enabled': True, 'by_path': {'/json/user_info': [1]}}, True),

        ('other shard', {'enabled': True, 'by_path': {'/json/user_info': [2]}}, False),
        ('many shards (current first)', {'enabled': True, 'by_path': {'/json/user_info': [1, 2, 3]}}, True),
        ('many shards (current middle)', {'enabled': True, 'by_path': {'/json/user_info': [2, 1, 3]}}, True),
        ('many shards (current last)', {'enabled': True, 'by_path': {'/json/user_info': [3, 2, 1]}}, True),

        ('other path', {'enabled': True, 'by_path': {'/json/store': [1]}}, False),
        ('other path (current first)', {'enabled': True, 'by_path': {'/json/user_info': [1], '/json/store': [1], '/json/info': [1]}}, True),
        ('other path (current middle)', {'enabled': True, 'by_path': {'/json/store': [1], '/json/user_info': [1], '/json/info': [1]}}, True),
        ('other path (current last)', {'enabled': True, 'by_path': {'/json/store': [1], '/json/info': [1], '/json/user_info': [1]}}, True),

        ('disable by regexp', {'enabled': True, 'by_path': {'/json/.*': [1]}}, True),
        ('disable all shard', {'enabled': True, 'by_path': {'.*': [1]}}, True),

        ('empty shards', {'enabled': True, 'by_path': {'.*': []}}, False),
    ])
    def test_by_path(self, case_name, config, expected_error):
        with mock.patch('mpfs.core.degradations.settings.degradations', new=config):
            if expected_error:
                self.json_error('user_info', {'uid': self.uid}, status=500, code=codes.INTERNAL_ERROR)
            else:
                self.json_ok('user_info', {'uid': self.uid})

    @parameterized.expand([
        ('all disable', {'enabled': False, 'by_client': {}}, False),
        ('all correct and disable', {'enabled': False, 'by_client': {'client_1': [1]}}, False),
        ('all correct', {'enabled': True, 'by_client': {'client_1': [1]}}, True),

        ('other shard', {'enabled': True, 'by_client': {'client_1': [2]}}, False),
        ('many shards (current first)', {'enabled': True, 'by_client': {'client_1': [1, 2, 3]}}, True),
        ('many shards (current middle)', {'enabled': True, 'by_client': {'client_1': [2, 1, 3]}}, True),
        ('many shards (current last)', {'enabled': True, 'by_client': {'client_1': [3, 2, 1]}}, True),

        ('other client', {'enabled': True, 'by_client': {'client_2': [1]}}, False),
        ('other client (current first)', {'enabled': True, 'by_client': {'client_1': [1], 'client_2': [1], 'client_3': [1]}}, True),
        ('other client (current middle)', {'enabled': True, 'by_client': {'client_2': [1], 'client_1': [1], 'client_3': [1]}}, True),
        ('other client (current last)', {'enabled': True, 'by_client': {'client_2': [1], 'client_3': [1], 'client_1': [1]}}, True),

        ('empty shards', {'enabled': True, 'by_client': {'client_1': []}}, False),
    ])
    def test_by_client(self, case_name, config, expected_error):
        clients = {
            'client_1': {
                'tvm_2_0': {
                    'client_ids': [888000],
                    'user_ticket_policy': 'no_checks'}
            },
            'client_2': {
                'tvm_2_0': {
                    'client_ids': [888001],
                    'user_ticket_policy': 'no_checks'}
            },
            'client_3': {
                'tvm_2_0': {
                    'client_ids': [888002],
                    'user_ticket_policy': 'no_checks'}
            }
        }

        with mock.patch('mpfs.core.services.tvm_2_0_service.TVM2Service.validate_service_ticket', return_value=AttrDict({'src': 888000})),\
            mock.patch.dict(settings.auth['clients'], clients),\
            mock.patch('mpfs.core.degradations.settings.degradations', new=config):
                setup_tvm_2_0_clients(settings.auth['clients'])
                if expected_error:
                    self.json_error('user_info', {'uid': self.uid}, headers={'X-Ya-Service-Ticket': 'service_ticket'}, client_addr='10.10.1.54', status=500, code=codes.INTERNAL_ERROR)
                else:
                    self.json_ok('user_info', {'uid': self.uid}, headers={'X-Ya-Service-Ticket': 'service_ticket'}, client_addr='10.10.1.54')

    def test_sharpei_calls(self):
        with mock.patch('mpfs.core.degradations.settings.degradations', new={'enabled': True, 'by_path': {'.*': [2]}}):
                count_calls_before = self.postgres_connections_stub.mocks.connection_info_patch.call_count
                self.json_ok('user_info', {'uid': self.uid})
                count_calls_after = self.postgres_connections_stub.mocks.connection_info_patch.call_count
                assert (count_calls_after - count_calls_before) == 1
