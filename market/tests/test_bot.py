from market.tools.resource_monitor.lib.bot import BotClient, BotJsonClient

import mock
from mock import patch
from library.python import resource
import json


def consists_of_response(request_url):
    if request_url.find('inv=900156141') != -1:
        data_str = resource.find('/data/bot_consists_of_response.json')
        return json.loads(data_str)
    return {}

@patch.object(BotJsonClient, '_get')
@patch.object(BotClient, '_get')
def test_market_hosts(method, json_method):
    method.return_value = resource.find('/data/bot_servers_response.tsv')
    json_method.side_effect = consists_of_response
    b = BotClient()
    hosts = b.get_market_hosts()
    assert hosts
    assert len(hosts) == 4
    attached = b.get_attached_items(instance_number=900156141)['Connected']
    storages = filter(lambda x: x.is_storage(), attached)
    assert len(storages) == 1
    assert storages[0].serial == '101665291'


@patch.object(BotClient, '_get')
def test_warehouse_hosts(method):
    method.return_value = resource.find('/data/bot_servers_response.tsv')
    b = BotClient()
    servers = b.get_warehouse_servers()
    assert servers
    assert len(servers) == 17
