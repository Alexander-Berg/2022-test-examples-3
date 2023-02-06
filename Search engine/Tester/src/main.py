from aiohttp import web
import logging.config
from search.mon.tester.src.lib.args import arg_init
from search.mon.tester.src.lib import Handler


def main():
    port, debug_param, tickets_ages_cfg = arg_init()
    logger = logging.getLogger(__name__)
    app = web.Application()
    handler = Handler(tickets_ages_cfg)
    app.add_routes([web.post('/get_alert', handler.handle_push)])
    app.add_routes([web.post('/api/tickenator.services.TickenatorService/createTicket{queue}Auto', handler.handle_push)])
    app.add_routes([web.post('/config', handler.config)])
    web.run_app(app=app, host='::', port=port, print=logger.error)


if __name__ == '__main__':
    main()
