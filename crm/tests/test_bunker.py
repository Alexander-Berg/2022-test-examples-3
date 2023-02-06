import re
from aiohttp import web


class TestBunker:
    async def test_get_node_no_node(self, bunker_last_check_now, aresponses):
        aresponses.add('bunker-api-dot.yandex.net',
                       re.compile(r'/v1/cat.*'),
                       'GET',
                       response=web.json_response({'OTHER_NODE_KEY': 'other_node_text', 'NODE_KEY': 'node_text'}))
        result = await bunker_last_check_now.get_node('new/path', 'OTHER_NODE_KEY')
        assert result == 'other_node_text'

    async def test_get_node_is_update_required_false(self, bunker_last_check_now, aresponses):
        aresponses.add('bunker-api-dot.yandex.net',
                       re.compile(r'/v1/cat.*'),
                       'GET',
                       response=web.json_response({'NODE_KEY': 'node_new_text'}))
        result = await bunker_last_check_now.get_node('new/path', 'NODE_KEY')
        assert result == 'node_old_text'

    async def test_get_node_is_update_required_true(self, bunker_last_check_in_past, aresponses):
        aresponses.add('bunker-api-dot.yandex.net',
                       re.compile(r'/v1/cat.*'),
                       'GET',
                       response=web.json_response({'NODE_KEY': 'node_new_text'}))
        result = await bunker_last_check_in_past.get_node('new/path', 'NODE_KEY')
        assert result == 'node_new_text'

    async def test_update_cache_invalid_response_cache_exists(self, bunker_last_check_in_past, aresponses):
        aresponses.add('bunker-api-dot.yandex.net',
                       re.compile(r'/v1/cat.*'),
                       'GET',
                       response=web.json_response({}))
        result = await bunker_last_check_in_past.get_node('new/path', 'NODE_KEY')
        assert result == 'node_old_text'

    async def test_get_node_no_path(self, bunker_empty, aresponses):
        aresponses.add('bunker-api-dot.yandex.net',
                       re.compile(r'/v1/cat.*'),
                       'GET',
                       response=web.json_response({'NODE_KEY': 'node_text'}))
        result = await bunker_empty.get_node('new/path', 'NODE_KEY')
        assert result == 'node_text'

    async def test_update_cache_invalid_response_no_cache(self, bunker_empty, aresponses):
        aresponses.add('bunker-api-dot.yandex.net',
                       re.compile(r'/v1/cat.*'),
                       'GET',
                       response=web.json_response({}))
        result = await bunker_empty.get_node('new/path', 'NODE_KEY')
        assert result == 'Что-то сломалось.'

    async def test_all_nodes_in_path_no_path(self, bunker_empty, aresponses):
        aresponses.add('bunker-api-dot.yandex.net',
                       re.compile(r'/v1/cat.*'),
                       'GET',
                       response=web.json_response({'NODE_KEY': 'node_new_text'}))
        result = await bunker_empty.get_all_nodes_in_path('new/path')
        assert result == {'NODE_KEY': 'node_new_text'}

    async def test_all_nodes_in_path_is_update_required_false(self, bunker_last_check_now, aresponses):
        aresponses.add('bunker-api-dot.yandex.net',
                       re.compile(r'/v1/cat.*'),
                       'GET',
                       response=web.json_response({'NODE_KEY': 'node_new_text'}))
        result = await bunker_last_check_now.get_all_nodes_in_path('new/path')
        assert result == {'NODE_KEY': 'node_old_text'}

    async def test_all_nodes_in_path_is_update_required_true(self, bunker_last_check_in_past, aresponses):
        aresponses.add('bunker-api-dot.yandex.net',
                       re.compile(r'/v1/cat.*'),
                       'get',
                       response=web.json_response({'NODE_KEY': 'node_new_text'}))
        result = await bunker_last_check_in_past.get_all_nodes_in_path('new/path')
        assert result == {'NODE_KEY': 'node_new_text'}

    async def test_update_cache_exception_cache_exists(self, bunker_last_check_in_past, aresponses):
        aresponses.add('bunker-api-dot.yandex.net',
                       re.compile(r'/v1/cat.*'),
                       'GET',
                       response=web.json_response({}))
        result = await bunker_last_check_in_past.get_node('new/path', 'NODE_KEY')
        assert result == 'node_old_text'

    async def test_update_cache_exception_no_cache(self, bunker_empty, aresponses):
        aresponses.add('bunker-api-dot.yandex.net',
                       re.compile(r'/v1/cat.*'),
                       'GET',
                       response=web.json_response({}))
        result = await bunker_empty.get_node('new/path', 'NODE_KEY')
        assert result == 'Что-то сломалось.'
