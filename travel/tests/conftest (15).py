#!/usr/bin/env python
# encoding: utf-8

from test_context import TestItemsRegistry

from mapreduce.yt.python.yt_stuff import YtConfig, YtStuff
from offercache import OfferCacheApp
from searcher import SearcherServer
from travel.hotels.test_helpers.label_codec import LabelCodec
from travel.hotels.test_helpers.message_bus import yt_config_args, MessageBus
from yatest.common import network
import yatest.common

import logging
import pytest
import time
import random


LOG = logging.getLogger(__name__)


@pytest.fixture(scope="session")
def yt_stuff(request):
    yt = YtStuff(YtConfig(**yt_config_args))
    yt.start_local_yt()
    request.addfinalizer(yt.stop_local_yt)
    return yt


@pytest.fixture(scope='session')
def yt_config(request):
    return YtConfig(**yt_config_args)


@pytest.fixture(scope='session')
def oc_app(yt_stuff, test_items_registry: TestItemsRegistry):
    random.seed(1)
    with network.PortManager() as pm:
        http_port = pm.get_port()
        grpc_port = pm.get_port()
        mon_port = pm.get_port()
        searcher_port = pm.get_port()
        promo_service_grpc_port = pm.get_port()

        LOG.info('http port: {}'.format(http_port))
        LOG.info('grpc port: {}'.format(grpc_port))
        LOG.info('mon port: {}'.format(mon_port))
        LOG.info('searcher port: {}'.format(searcher_port))
        LOG.info('promo_service grpc port: {}'.format(promo_service_grpc_port))

        yatest.common.execute([
            yatest.common.binary_path('travel/hotels/devops/cfg_tool/cfg_tool'),
            '-e', 'fake-env-for-tests', '--i-know-what-i-am-doing',
            '--for-tests-only-enable-all-operators',
            '--yt-path', '//home/travel/testing/config',
            '--yt-proxy', yt_stuff.get_server(),
            '-c', yatest.common.source_path('travel/hotels/devops/config/cfg_tool'),
            'push'
        ]).wait(check_exit_code=True)
        LOG.info('Yt Config written')

        message_bus = MessageBus(yt_stuff, '//home/travel/test/offer_bus')
        LOG.info('MessageBus created')

        outdated_offer_bus = MessageBus(yt_stuff, '//home/travel/test/outdated_offer_bus')
        LOG.info('OutdatedOfferBus created')

        searcher = SearcherServer(port=searcher_port, message_bus=message_bus)
        LOG.info('Searcher created')

        lc = LabelCodec()
        lc.start()
        LOG.info('LabelCodec created')

        app = OfferCacheApp(
            http_port=http_port, grpc_port=grpc_port, mon_port=mon_port,
            promo_service_grpc_port=promo_service_grpc_port,
            searcher=searcher, message_bus=message_bus,
            outdated_offer_bus=outdated_offer_bus, lc=lc,
            yt_client=yt_stuff.yt_client
        )
        LOG.info('OfferCache created')

        test_items_registry._set_oc(app)

        OfferCacheApp.write_blacklist_table(yt_stuff, test_items_registry.build_blacklist())
        LOG.info('Blacklist written')

        OfferCacheApp.write_whitelist_table(yt_stuff, test_items_registry.build_whitelist())
        LOG.info('Whitelist written')

        OfferCacheApp.write_user_order_counters_table(yt_stuff)
        LOG.info('UserOrderCounters written')

        OfferCacheApp.write_mir_white_list(yt_stuff)
        LOG.info('MirWhiteLists written')

        OfferCacheApp.write_plus_user_lists(yt_stuff)
        LOG.info('PlusUserLists written')

        OfferCacheApp.write_plus_hotel_lists(yt_stuff)
        LOG.info('PlusHotelLists written')

        OfferCacheApp.write_yandex_eda_lists(yt_stuff)
        LOG.info('YandexEdaLists written')

        OfferCacheApp.write_plus_additional_fee(yt_stuff)
        LOG.info('Plus additional fee written')

        searcher.write_initial_messages(app)
        test_items_registry.write_initial_messages(message_bus)

        OfferCacheApp.write_permarooms_table(yt_stuff, test_items_registry.build_permarooms())

        OfferCacheApp.append_travelline_rate_plans_table(yt_stuff, test_items_registry.build_travelline_rate_plans())
        OfferCacheApp.append_dolphin_tours_table(yt_stuff, test_items_registry.build_dolphin_tours())
        OfferCacheApp.append_dolphin_pansions_table(yt_stuff, test_items_registry.build_dolphin_pansions())
        OfferCacheApp.append_dolphin_rooms_table(yt_stuff, test_items_registry.build_dolphin_rooms())
        OfferCacheApp.append_dolphin_room_cats_table(yt_stuff, test_items_registry.build_dolphin_room_cats())
        OfferCacheApp.append_bnovo_rate_plans_table(yt_stuff, test_items_registry.build_bnovo_rate_plans())

        app.start()

        try:
            app.wait_ready(method='ping')
            LOG.info('OfferCache is ready')
            app.wait_ready(method='ping_nanny')  # To ensure that searcher is considered to be ready
            LOG.info('Searcher is pinged')
            yield app
        finally:
            app.stop()
            time.sleep(1)
            searcher.stop()
            lc.stop()


@pytest.fixture()
def searcher_session(oc_app):
    s = oc_app.searcher.start_session()
    yield s
    oc_app.searcher.finish_session()


@pytest.fixture(scope="session")
def test_items_registry():
    return TestItemsRegistry()


@pytest.fixture()
def ctx(request, test_items_registry: TestItemsRegistry, oc_app: OfferCacheApp):
    test_items_registry.ensure_oc_app_is_set()
    context = test_items_registry.get_test_context(request.node.name, register_phase=False)
    try:
        yield context
    finally:
        context._finish_run()


@pytest.fixture(scope="session", autouse=True)
def init_tests(request, test_items_registry):
    LOG.info("init_tests called")
    session = request.node
    for test in session.items:
        if 'ctx' in test.fixturenames:
            LOG.info(f'Running register phase for test {test.name}')
            ctx = test_items_registry.get_test_context(test.name, register_phase=True)
            test.obj(ctx)
            ctx._finish_registration()
