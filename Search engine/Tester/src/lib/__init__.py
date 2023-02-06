import logging.config
import datetime
import os
from startrek_client import Startrek
from search.mon.tester.src.lib.juggler import JugglerPushes
from aiohttp import web
import asyncio


logger = logging.getLogger('search')
ST_TOKEN = os.environ.get('ST_TOKEN', '')


class Handler(object):
    def __init__(self, config):
        self._config = config

    async def handle_push(self, request):
        try:
            if request.method != 'POST':
                logger.error('wrong method')
                return web.json_response(status=400)
            data = await request.json()
            start_time = datetime.datetime.now()
            logger.debug('Processing request')
            pushes = JugglerPushes(config=self._config, pushes=data)
            queue = request.match_info.get('queue', "Test")
            if queue.lower() == 'spi':
                queue = 'SPI'
                append_original_message = False
            elif queue.lower() == 'nocrequests':
                queue = 'NOCREQUESTS'
                append_original_message = False
            else:
                queue = 'TEST'
                append_original_message = True
            logger.debug(f'{data}')
            logger.debug(f'Got {len(pushes)} checks')
            client = Startrek(useragent='tickenator-auto', token=ST_TOKEN)
            final_result = pushes.process_pushes(
                client=client, queue=queue, append_original_message=append_original_message
            )
            stop_time = datetime.datetime.now()
            timer = stop_time - start_time
            logger.debug(f'Processing time: {timer}')
            result = web.json_response(data=final_result)
        except Exception as _e:
            logger.exception(_e)
            result = web.json_response(data={'failure': 'Internal error'})
        return result

    async def config(self, request):
        result = web.json_response(data=self._config)
        return result

