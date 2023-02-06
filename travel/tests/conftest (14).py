from mapreduce.yt.python.yt_stuff import YtConfig
from geocounter import GeoCounterApp
from yatest.common import network
import yatest.common

import logging
import pytest
import time
import random


LOG = logging.getLogger(__name__)


@pytest.fixture(scope='session')
def yt_config():
    return YtConfig(wait_tablet_cell_initialization=True)


@pytest.fixture(scope='module')
def geocounter_app(yt_stuff):
    random.seed(1)
    with network.PortManager() as pm:
        yatest.common.execute([
            yatest.common.binary_path('travel/hotels/devops/cfg_tool/cfg_tool'),
            '-e', 'testing', '--i-know-what-i-am-doing',
            '--yt-proxy', yt_stuff.get_server(),
            '-c', yatest.common.source_path('travel/hotels/devops/config/cfg_tool'),
            'push'
        ]).wait(check_exit_code=True)

        http_port = pm.get_port()
        grpc_port = pm.get_port()
        mon_port = pm.get_port()

        LOG.info('http port: {}'.format(http_port))
        LOG.info('grpc port: {}'.format(grpc_port))
        LOG.info('mon port: {}'.format(mon_port))

        GeoCounterApp.write_geocounter_table(yt_stuff)
        LOG.info('Geocounter table written')

        GeoCounterApp.write_regions_table(yt_stuff)
        LOG.info('Regions table written')

        GeoCounterApp.write_prices_table(yt_stuff)
        LOG.info('Prices table written')

        GeoCounterApp.write_hotel_traits_table(yt_stuff)
        LOG.info('Hotel traits table written')

        GeoCounterApp.write_original_id_to_permalink_mapper_table(yt_stuff)
        LOG.info('OriginalId to permalink mapper table written')

        GeoCounterApp.write_offer_bus_messages(yt_stuff)
        LOG.info('OfferBus messages written')

        app = GeoCounterApp(yt_stuff=yt_stuff, http_port=http_port, grpc_port=grpc_port, mon_port=mon_port)
        LOG.info('GeoCounter created')

        app.start()

        try:
            app.wait_ready(method='ping')
            LOG.info('GeoCounter is ready')
            yield app
        finally:
            app.stop()
            time.sleep(1)
