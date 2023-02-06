# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from common.models.partner import Partner
from common.models.pathfinder_maps import PathfinderMapsNearestSettlement
from common.models.staticpages import ArticleBinding, StaticPage

from travel.rasp.library.python.common23.date.date import smart_localize
from travel.rasp.library.python.common23.date.date_const import MSK_TZ

from travel.rasp.library.python.common23.tester.factories import *  # noqa
from travel.rasp.library.python.common23.models.tariffs.tester.factories import ThreadTariffFactory  # noqa


def create_rthread_segment(**kwargs):
    from route_search.models import RThreadSegment

    rts_from, rts_to = kwargs.get('rts_from'), kwargs.get('rts_to')
    if rts_from and rts_to:
        assert rts_to.thread == rts_from.thread
        station_from = rts_from.station
        station_to = rts_to.station
        thread = rts_from.thread

    else:
        station_from = kwargs.get('station_from')
        if not station_from:
            station_from = create_station()

        station_to = kwargs.get('station_to')
        if not station_to:
            station_to = create_station()

        thread = kwargs.get('thread')
        if not thread:
            thread = create_thread(
                __={
                    'schedule_v1': [
                        [None, 0, station_from],
                        [10, None, station_to],
                    ]
                }
            )

        rtstations = list(thread.rtstation_set.all())
        rts_from = [rts for rts in rtstations if rts.tz_departure is not None][0]
        rts_to = [rts for rts in rtstations if rts.tz_arrival is not None][0]

    attr_values = dict(
        thread=thread,
        station_from=station_from,
        station_to=station_to,
        rtstation_from=rts_from,
        rtstation_to=rts_to,
        mask_shift=0,
        departure=smart_localize(datetime(2015, 1, 1, 10, 00), MSK_TZ),
        arrival=smart_localize(datetime(2015, 1, 1, 20, 00), MSK_TZ),
        gone=False,
        url='ya.some.url'
    )
    attr_values.update(kwargs)

    segment = RThreadSegment()
    segment.thread = attr_values.pop('thread')
    segment._init_data()

    for attr, value in attr_values.items():
        setattr(segment, attr, value)

    return segment


class PartnerFactory(ModelFactory):
    Model = Partner
    default_kwargs = {
        'current_balance': 1,
        'click_price': 0,
        'click_price_ru': 0,
        'click_price_ua': 0,
        'click_price_tr': 0,
        'click_price_com': 0,
    }


create_partner = PartnerFactory()
factories[PartnerFactory] = create_partner


class ArticleBindingFactory(ModelFactory):
    Model = ArticleBinding


create_article_binding = ArticleBindingFactory()
factories[ArticleBinding] = create_article_binding


class StaticPageFactory(ModelFactory):
    Model = StaticPage

    def create_object(self, kwargs):
        binding_infos = []
        if 'article_bindings' in kwargs:
            binding_infos = kwargs.pop('article_bindings')

        page = super(StaticPageFactory, self).create_object(kwargs)

        for binding_kwargs in binding_infos:
            binding_kwargs['page'] = page
            create_article_binding(binding_kwargs)

        return page


create_static_page = StaticPageFactory()
factories[StaticPage] = create_static_page


class PathfinderMapsNearestSettlementFactory(ModelFactory):
    Model = PathfinderMapsNearestSettlement


create_pathfinder_maps_nearest_settlement = PathfinderMapsNearestSettlementFactory()
factories[PathfinderMapsNearestSettlement] = create_pathfinder_maps_nearest_settlement
