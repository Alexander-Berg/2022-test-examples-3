# -*- coding: utf-8 -*-
from mock import Mock
from copy import copy
from datetime import datetime, time, date, timedelta
from itertools import combinations
from collections import Iterable

from django.db import models

from travel.avia.library.python.common.models.country_covid_info import CountryCovidInfo
from travel.avia.library.python.common.models.geo import (
    CodeSystem, Country, Direction, DirectionMarker,
    District, PointSynonym, Region, Settlement, Station,
    StationCode, StationPhone, StationMajority,
    SuburbanZone, Station2Settlement
)
from travel.avia.library.python.common.models.holidays import Holiday, HolidayDirection
from travel.avia.library.python.common.models.iatacorrection import IataCorrection
from travel.avia.library.python.common.models.partner import (
    Partner, DohopVendor, UpdateHistoryRecord, DohopVendorIndexInfo, DohopIndex, CPCPrice, PartnerUser
)
from travel.avia.library.python.common.models.tariffs import ThreadTariff
from travel.avia.library.python.common.models.team import Team
from travel.avia.library.python.common.models.transport import TransportType, TransportSubtype, TransportModel, TransportSubtypeColor
from travel.avia.library.python.common.models.schedule import (
    AviaAlliance, Company, RThread, RThreadType, Route, RTStation, Supplier, ExpressTypeLite,
    StationSchedule, TrainSchedulePlan, DeLuxeTrain
)
from travel.avia.library.python.common.models.service import Service
from travel.avia.library.python.common.utils.date import MSK_TIMEZONE, RunMask, smart_localize, MSK_TZ
from travel.avia.library.python.common.utils.title_generator import build_simple_title_common

from travel.avia.library.python.tester.helpers.mask_description import run_mask_from_mask_description

from travel.avia.library.python.avia_data.models import NearCountries, SeoDirection, MinPrice, TopFlight, AmadeusMerchant
from travel.avia.library.python.avia_data.models.company import AviaCompany, CompanyTariff
from travel.avia.library.python.avia_data.models.review import FlightReview, FlightNumber, FlightReviewSource
from travel.avia.library.python.avia_data.models.images import SettlementBigImage
from travel.avia.library.python.avia_data.models.tablo import TabloSource, AirportTabloSource
from travel.avia.library.python.common.models.currency import Currency
from travel.avia.library.python.avia_data.models.currency import Currency as AviaCurrency, \
    CurrencyTranslation
from travel.avia.library.python.common.models.staticpages import StaticPage
from travel.avia.library.python.common.models.translations import TranslatedTitle, TranslatedText
from travel.avia.library.python.common.models.scripts import Script, ScriptResult

DEFAULT_TRANSPORT_TYPE = u'plane'

factories = {}
_empty_object_key = object()
_empty_object = object()


class BaseFactory(object):
    default_kwargs = {}

    def __init__(self, update_default_kwargs=None, set_default_kwargs=None):
        if set_default_kwargs:
            self.default_kwargs = set_default_kwargs
        if update_default_kwargs:
            self.default_kwargs = self.extend_kwargs(update_default_kwargs)

        self.key_processors = (
            self.process_key_factory,
            self.process_key_dict,
            self.process_none,
        )

    def __call__(self, __object_key=_empty_object_key, **kwargs):
        if __object_key is not _empty_object_key:
            for key_processor in self.key_processors:
                obj = key_processor(__object_key, kwargs)
                if obj is _empty_object:
                    return None
                elif obj is not None:
                    return obj

            else:
                raise Exception("Can not process object_key {!r}".format(__object_key))

        try:
            kwargs = self.extend_kwargs(kwargs, resolve_callable=True)
            return self.create_object(kwargs)
        except Exception:
            try:
                print 'Cannot create object in factory {}: {!r}'.format(self.__class__.__name__, kwargs)
            except Exception:
                print 'Cannot create object in factory {}'.format(self.__class__.__name__)
            raise

    def extend_kwargs(self, kwargs, resolve_callable=False):
        new_kwargs = copy(self.default_kwargs)
        new_kwargs.update(kwargs)

        if resolve_callable:
            for key, value in new_kwargs.items():
                if callable(value):
                    new_kwargs[key] = value()

        return new_kwargs

    def create_model(self, kwargs):
        raise NotImplementedError()

    def create_object(self, kwargs):
        raise NotImplementedError()

    def mutate(self, default_kwargs=None, **kwargs):
        return self.__class__(self.extend_kwargs(kwargs), set_default_kwargs=default_kwargs)

    def process_key_factory(self, key, kwargs):
        if isinstance(key, BaseFactory):
            kwargs = self.extend_kwargs(kwargs)
            return key(**kwargs)

    def process_key_dict(self, key, kwargs):
        if isinstance(key, dict):
            if kwargs:
                raise ValueError('Provide params via dict or kwargs')

            return self(**key)

    def process_none(self, key, kwargs):
        if key is None:
            return _empty_object


class ModelFactory(BaseFactory):
    Model = None

    def __init__(self, update_default_kwargs=None, set_default_kwargs=None):
        super(ModelFactory, self).__init__(
            update_default_kwargs=update_default_kwargs,
            set_default_kwargs=set_default_kwargs,
        )

        self.key_processors = (
            self.process_key_pk,
            self.process_key_model,
        ) + self.key_processors

    def process_key_pk(self, key, kwargs):
        if isinstance(key, int):
            return self.Model.objects.get(pk=key)

    def process_key_model(self, key, kwargs):
        if isinstance(key, self.Model):
            return key

    def _create_model(self, kwargs):
        create_kwargs = {}

        for field in self.Model._meta.fields:
            if field.name not in kwargs:
                continue

            if isinstance(field, models.ForeignKey):
                factory = get_model_factory(field.rel.to)
                key = kwargs.pop(field.name)
                if key is not None:
                    create_kwargs[field.name] = factory(key)
            else:
                create_kwargs[field.name] = kwargs.pop(field.name)

        create_kwargs.update(kwargs)

        return self.Model(**create_kwargs)

    def create_object(self, kwargs):
        model = self._create_model(kwargs)
        model.save()
        return model

    def create_model(self, **kwargs):
        kwargs = self.extend_kwargs(kwargs, resolve_callable=True)
        return self._create_model(kwargs)

    def fill_title(self, params, title_id_field, title_field):
        if title_id_field in params:
            return
        title = params.get(title_field, 'unknown_{}'.format(title_field))
        params[title_id_field] = create_translated_title(ru_nominative=title).id

        return self

    def fill_text(self, params, title_id_field, title_field):
        if title_id_field in params:
            return
        title = params.get(title_field, 'unknown_{}'.format(title_field))
        params[title_id_field] = create_translated_text(ru=title).id

        return self


def get_model_factory(model_class):
    if model_class in factories:
        return factories[model_class]
    else:
        class TmpFactory(ModelFactory):
            Model = model_class

        return TmpFactory()


class ProcessStringKeyMixin(object):
    string_key = 'code'

    def __init__(self, **kwargs):
        super(ProcessStringKeyMixin, self).__init__(**kwargs)
        self.key_processors = (self.process_key_string,) + self.key_processors

    def process_key_string(self, key, kwargs):
        if isinstance(key, basestring):
            return self.Model.objects.get(**{self.string_key: key})


class TranslatedTitleFactory(ModelFactory):
    Model = TranslatedTitle

create_translated_title = TranslatedTitleFactory()


class TranslatedTextFactory(ModelFactory):
    Model = TranslatedText

create_translated_text = TranslatedTextFactory()


class CodeSystemFactory(ProcessStringKeyMixin, ModelFactory):
    Model = CodeSystem


create_code_system = factories[CodeSystem] = CodeSystemFactory()


class StationCodeFactory(ModelFactory):
    Model = StationCode


create_station_code = factories[StationCode] = StationCodeFactory()


class DirectionFactory(ModelFactory):
    Model = Direction


create_direction = factories[Direction] = DirectionFactory()


class Station2SettlementFactory(ModelFactory):
    Model = Station2Settlement


create_station2settlement = factories[Station2Settlement] = Station2SettlementFactory()


class StationFactory(ModelFactory):
    Model = Station
    default_kwargs = {
        'settlement': None,
        'title': u'НазваниеСтанции',
        't_type': DEFAULT_TRANSPORT_TYPE,
        'majority': u'main_in_city',
        'time_zone': MSK_TIMEZONE,
        'latitude': 1,
        'longitude': 1,
    }

    def create_object(self, kwargs):
        extra_params = kwargs.pop('__', None) or {}
        self.fill_text(kwargs, 'new_L_address_id', 'address')
        self.fill_text(kwargs, 'new_L_how_to_get_to_city_id', 'how_to_get_to_city')
        self.fill_title(kwargs, 'new_L_title_id', 'title')
        self.fill_title(kwargs, 'new_L_popular_title_id', 'popular_title')
        self.fill_title(kwargs, 'new_L_short_title_id', 'short_title')

        settlement = create_settlement(kwargs.pop('settlement', None))
        if settlement:
            kwargs['time_zone'] = kwargs.pop('time_zone', settlement.time_zone)
            kwargs['latitude'] = kwargs.pop('latitude', settlement.latitude)
            kwargs['longitude'] = kwargs.pop('longitude', settlement.longitude)
            kwargs['settlement'] = settlement

        station = super(StationFactory, self).create_object(kwargs)

        for system, code in extra_params.get('codes', {}).items():
            create_station_code(station=station, system=system, code=code)

        for phone_data in extra_params.get('phones', []):
            assert 'station' not in phone_data
            phone_data['station'] = station
            create_station_phone(**phone_data)

        direction = create_direction(extra_params.get('direction', None))
        if direction:
            dm_order = DirectionMarker.objects.aggregate(models.Max('order')).values()[0]

            DirectionMarker.objects.create(
                direction=direction, station=station,
                order=(0 if dm_order is None else dm_order + 1)
            )

        return station


create_station = StationFactory()
factories[Station] = create_station


def create_airport(iata, **kwargs):
    iata_system = CodeSystem.objects.get(code='iata')
    return create_station(t_type='plane', __={'codes': {iata_system: iata}}, **kwargs)


class TransportTypeFactory(ProcessStringKeyMixin, ModelFactory):
    Model = TransportType


create_transport_type = TransportTypeFactory()
factories[TransportType] = create_transport_type


class TransportSubtypeFactory(ProcessStringKeyMixin, ModelFactory):
    Model = TransportSubtype


create_transport_subtype = factories[TransportSubtype] = TransportSubtypeFactory()


class TransportSubtypeColorFactory(ModelFactory):
    Model = TransportSubtypeColor


create_transport_subtype_color = factories[TransportSubtypeColor] = TransportSubtypeColorFactory()


class ExpressTypeLiteFactory(ModelFactory):
    Model = ExpressTypeLite


create_express_type_lite = factories[ExpressTypeLite] = ExpressTypeLiteFactory()


class StationMajorityFactory(ProcessStringKeyMixin, ModelFactory):
    Model = StationMajority


create_station_majority = StationMajorityFactory()
factories[StationMajority] = create_station_majority


class SettlementFactory(ModelFactory):
    Model = Settlement
    default_kwargs = {
        'title': u'НазваниеГорода',
        'majority': 1,
        'time_zone': MSK_TIMEZONE,
        'latitude': 1,
        'longitude': 1,
    }

    def create_object(self, kwargs):
        self.fill_title(kwargs, 'new_L_title_id', 'title')
        self.fill_title(kwargs, 'new_L_abbr_title_id', 'abbr_title')
        return super(SettlementFactory, self).create_object(kwargs)


create_settlement = SettlementFactory()
factories[Settlement] = create_settlement


class AviaAllianceFactory(ModelFactory):
    Model = AviaAlliance
    default_kwargs = {
        'title': u'НазваниеСтраны'
    }

    def create_object(self, kwargs):
        self.fill_title(kwargs, 'new_L_title_id', 'title')
        self.fill_text(kwargs, 'new_L_description_id', 'description')
        return super(AviaAllianceFactory, self).create_object(kwargs)


create_aviaalliance = AviaAllianceFactory()
factories[AviaAlliance] = create_aviaalliance


class CountryFactory(ModelFactory):
    Model = Country
    default_kwargs = {
        'title': u'НазваниеСтраны'
    }

    def create_object(self, kwargs):
        self.fill_title(kwargs, 'new_L_title_id', 'title')
        return super(CountryFactory, self).create_object(kwargs)


create_country = CountryFactory()
factories[Country] = create_country


class RegionFactory(ModelFactory):
    Model = Region
    default_kwargs = {
        'title': u'НазваниеРегиона',
        'country': {}
    }

    def create_object(self, kwargs):
        self.fill_title(kwargs, 'new_L_title_id', 'title')
        return super(RegionFactory, self).create_object(kwargs)


create_region = RegionFactory()
factories[Region] = create_region


class DistrictFactory(ModelFactory):
    Model = District
    default_kwargs = {
        'title': u'НазваниеРайона',
        'region': {}
    }


create_district = DistrictFactory()
factories[District] = create_district


class CovidInfoFactory(ModelFactory):
    Model = CountryCovidInfo


create_covid_info = factories[CountryCovidInfo] = CovidInfoFactory()


class PointSynonymFactory(ModelFactory):
    Model = PointSynonym
    default_kwargs = {
        'title': u'Синоним',
        'content_type_id': 13,
        'object_id': 100500,
        'search_type': 'synonym',
        'language': 'ru',
    }


create_pointsynonym = PointSynonymFactory()
factories[PointSynonym] = create_pointsynonym


class ThreadFactory(ModelFactory):
    Model = RThread

    default_kwargs = {
        'year_days': '1' * RunMask.MASK_LENGTH,
        'tz_start_time': time(0),
        't_type': 'bus',
        'type': 'basic',
        'supplier': {},
        'schedule_v1': [
            [None, 0],
            [10, None],
        ],
        'ordinal_number': 1,
        'number': u'НомерНитки',
        'time_zone': MSK_TIMEZONE,
        'import_uid': lambda: ThreadFactory.gen_thread_uuid(),
        'uid': lambda: ThreadFactory.gen_thread_uid(),
    }

    @classmethod
    def gen_thread_uuid(cls):
        cls.thread_import_uid_counter = getattr(cls, 'thread_import_uid_counter', 0) + 1
        return u'THREAD_IMPORT_UUID_{}'.format(cls.thread_import_uid_counter)

    @classmethod
    def gen_thread_uid(cls):
        cls.uid_counter = getattr(cls, 'uid_counter', 0) + 1
        return u'THREAD_UID_{}'.format(cls.uid_counter)

    def create_object(self, kwargs):
        extra_params = kwargs.pop('__', None) or {}
        calculate_noderoute = extra_params.get('calculate_noderoute', False)
        calculate_noderouteadmin = extra_params.get('calculate_noderouteadmin', False)

        kwargs['t_type'] = create_transport_type(kwargs.pop('t_type', None))
        kwargs['supplier'] = create_supplier(kwargs.pop('supplier', None))
        kwargs['year_days'] = self.build_year_days(kwargs.pop('year_days', None))
        kwargs['tz_start_time'] = self.build_time(kwargs.pop('tz_start_time', None))
        kwargs['begin_time'] = self.build_time(kwargs.pop('begin_time', None))
        kwargs['end_time'] = self.build_time(kwargs.pop('end_time', None))

        rtstations = []

        if 'schedule_v1' in kwargs:
            rtstation_infos = kwargs.pop('schedule_v1')
            for rts_info in rtstation_infos:
                if len(rts_info) == 2:
                    (tz_arrival, tz_departure), station, rts_kwargs_update = rts_info, {}, {}
                elif len(rts_info) == 3:
                    (tz_arrival, tz_departure, station), rts_kwargs_update = rts_info, {}
                elif len(rts_info) == 4:
                    tz_arrival, tz_departure, station, rts_kwargs_update = rts_info
                else:
                    raise ValueError(rts_info)

                rts_kwargs = {
                    'tz_arrival': tz_arrival,
                    'tz_departure': tz_departure,
                    'station': create_station(station),
                }
                rts_kwargs.update(rts_kwargs_update)

                rtstations.append(rts_kwargs)

        route_kwargs = kwargs.pop('route', {
            '__': None,
            't_type': kwargs['t_type'],
            'supplier': kwargs['supplier']
        })
        route = create_route(route_kwargs)
        kwargs['route'] = route

        if 'number' in kwargs:
            if ('reversed_number' not in kwargs) or not kwargs['reversed_number']:
                kwargs['reversed_number'] = kwargs['number'][::-1]

        thread = super(ThreadFactory, self).create_object(kwargs)

        created_rtstations = []
        for rts_kwargs in rtstations:
            rts_kwargs['thread'] = thread

            created_rtstations.append(create_rtstation(rts_kwargs))

        # Много логики в наших библиотеках (типа route_search и stationschedule) расчитывает, что
        # у любой нитки есть как минимум 2 станции, и что у первой станции arrival = None,
        # а у последней departure = None. Всё это проверяется на уровне импортов.
        # В тестах, соответственно, необходимо создать такие же условия.
        assert len(created_rtstations) >= 2
        assert created_rtstations[0].tz_arrival is None
        assert created_rtstations[-1].tz_departure is None

        def get_point_for_title(rts):
            return rts.station.settlement or rts.station

        thread.title_common = build_simple_title_common(
            thread.t_type, [get_point_for_title(created_rtstations[0]), get_point_for_title(created_rtstations[-1])])
        thread.save()

        if calculate_noderoute:
            from travel.avia.library.python.route_search.models import ZNodeRoute2
            for rts_from, rts_to in combinations(thread.rtstation_set.all(), 2):
                ZNodeRoute2(
                    route_id=thread.route_id,
                    thread=thread,
                    settlement_from=rts_from.station.settlement,
                    station_from_id=rts_from.station_id,
                    rtstation_from=rts_from,
                    settlement_to=rts_to.station.settlement,
                    station_to_id=rts_to.station_id,
                    rtstation_to=rts_to,
                    stops_translations=''
                ).save()

        if calculate_noderouteadmin:
            from travel.avia.library.python.route_search.models import ZNodeRoute2
            for rts_from, rts_to in combinations(thread.rtstation_set.all(), 2):
                ZNodeRoute2(
                    route_id=thread.route_id,
                    thread=thread,
                    settlement_from=rts_from.station.settlement,
                    station_from_id=rts_from.station_id,
                    rtstation_from=rts_from,
                    settlement_to=rts_to.station.settlement,
                    station_to_id=rts_to.station_id,
                    rtstation_to=rts_to,
                    stops_translations='',
                    supplier=thread.supplier,
                    two_stage_package=route.two_stage_package
                ).save()

        return thread

    def build_year_days(self, year_days_param):
        if isinstance(year_days_param, basestring):
            if year_days_param.isdigit() and len(year_days_param) == RunMask.MASK_LENGTH:
                return year_days_param

            return run_mask_from_mask_description(year_days_param)

        if isinstance(year_days_param, RunMask):
            return str(year_days_param)
        if isinstance(year_days_param, Iterable):
            dates = list(year_days_param)
            if all(isinstance(d, date) for d in dates):
                return str(RunMask(days=list(year_days_param)))

        raise ValueError('Bad year_days parameter {!r}'.format(year_days_param))

    def build_time(self, time_param):
        if time_param is None:
            return None
        if isinstance(time_param, time):
            return time_param
        if isinstance(time_param, basestring):
            return time(*map(int, time_param.split(':')))

        raise ValueError('Bad time parameter {!r}'.format(time_param))


create_thread = ThreadFactory()
factories[RThread] = create_thread


class RThreadTypeFactory(ProcessStringKeyMixin, ModelFactory):
    Model = RThreadType


create_rthreadtype = RThreadTypeFactory()
factories[RThreadType] = create_rthreadtype


class RTStationFactory(ModelFactory):
    Model = RTStation
    default_kwargs = {
        'time_zone': MSK_TIMEZONE
    }


create_rtstation = RTStationFactory()
factories[RTStation] = create_rtstation


class RouteFactory(ModelFactory):
    Model = Route

    default_kwargs = {
        '__': {
            'threads': []
        },
        't_type': DEFAULT_TRANSPORT_TYPE,
        'supplier': {},
        'route_uid': lambda: RouteFactory.gen_route_uid(),
        'script_protected': False,
    }

    @classmethod
    def gen_route_uid(cls):
        cls.uid_counter = getattr(cls, 'uid_counter', 0) + 1
        return u'ROUTE_UID_{}'.format(cls.uid_counter)

    def create_object(self, kwargs):
        extra_params = kwargs.pop('__', None) or {}
        threads_params = extra_params.get('threads', [])

        route = super(RouteFactory, self).create_object(kwargs)

        for index, thread_kwargs in enumerate(threads_params):
            thread_kwargs = copy(thread_kwargs)
            thread_kwargs['route'] = route
            thread_kwargs.setdefault('t_type', route.t_type)
            thread_kwargs.setdefault('supplier', route.supplier)
            thread_kwargs.setdefault('ordinal_number', index + 1)
            thread_kwargs.setdefault('uid', u'THREAD_UID_{}_{}'.format(route.route_uid, index + 1))

            create_thread(**thread_kwargs)

        return route


create_route = RouteFactory()
factories[Route] = create_route


class SupplierFactory(ProcessStringKeyMixin, ModelFactory):
    Model = Supplier

    default_kwargs = {
        'code': lambda: SupplierFactory.gen_supplier_code()
    }

    @classmethod
    def gen_supplier_code(cls):
        cls.uid_counter = getattr(cls, 'uid_counter', 0) + 1
        return u'SUPPLIER_CODE_{}'.format(cls.uid_counter)

create_supplier = SupplierFactory()
factories[Supplier] = create_supplier


class CurrencyFactory(ModelFactory):
    Model = Currency

    default_kwargs = dict(
        name=u'рубли',
        code=u'RUR',
        iso_code=u'RUB',
        **{
            __key: 'some_value' for __key in
            [
                'template', 'template_whole', 'template_cents',
                'template_tr', 'template_whole_tr', 'template_cents_tr',
                'name_in'
            ]
        }
    )


create_currency = factories[Currency] = CurrencyFactory()


class AviaCurrencyFactory(ModelFactory):
    Model = AviaCurrency

    default_kwargs = dict(
        title=u'Рубли',
        code=u'RUR',
        iso_code=u'RUB',
        priority=123,
        enable=True
    )


create_avia_currency = factories[AviaCurrency] = AviaCurrencyFactory()


class AviaCurrencyTranslationFactory(ModelFactory):
    Model = CurrencyTranslation

    default_kwargs = dict(
        title=u'titke',
        title_in=u'title_in',
        template=u'template',
        template_whole=u'template_whole',
        template_cents=u'template_cents'
    )


create_avia_currency_translation = factories[CurrencyTranslation] = AviaCurrencyTranslationFactory()


class ThreadTariffFactory(ModelFactory):
    Model = ThreadTariff

    default_kwargs = {
        'thread_uid': lambda: ThreadFactory.gen_thread_uid(),
        'station_from': {},
        'station_to': {},
        'tariff': 100.5,
        'year_days_from': '1' * RunMask.MASK_LENGTH,
        'time_from': time(),
        'duration': 42,
        'time_to': time(),
        'supplier': {},
        't_type': DEFAULT_TRANSPORT_TYPE
    }

create_thread_tariff = factories[ThreadTariff] = ThreadTariffFactory()


def create_rthread_segment(**kwargs):
    from travel.avia.library.python.route_search.models import RThreadSegment

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
    )
    attr_values.update(kwargs)

    segment = RThreadSegment()
    segment.thread = attr_values.pop('thread')
    segment._init_data()

    for attr, value in attr_values.items():
        setattr(segment, attr, value)

    return segment


class TransportModelFactory(ModelFactory):
    Model = TransportModel

    default_kwargs = {
        't_type': DEFAULT_TRANSPORT_TYPE,
        'title': lambda: TransportModelFactory.gen_title(),
    }

    @classmethod
    def gen_title(cls):
        cls.title_counter = getattr(cls, 'title_counter', 0) + 1
        return u'TransportModel{}'.format(cls.title_counter)


create_transport_model = factories[TransportModel] = TransportModelFactory()


class StationScheduleFactory(ModelFactory):
    Model = StationSchedule


create_station_schedule = factories[StationSchedule] = StationScheduleFactory()


class StationPhoneFactory(ModelFactory):
    Model = StationPhone
    default_kwargs = {
        'station': {},
        'phone': u'+79123456789'
    }


create_station_phone = factories[StationPhone] = StationPhoneFactory()


class SuburbanZoneFactory(ModelFactory):
    Model = SuburbanZone
    default_kwargs = {
        'title': u'НазваниеПригороднойЗоны',
        'title_from': u'ОтЦентраПригороднойЗоны',
        'title_to': u'НаЦентрПригороднойЗоны'
    }


create_suburban_zone = SuburbanZoneFactory()
factories[SuburbanZone] = create_suburban_zone


class TrainSchedulePlanFactory(ModelFactory):
    Model = TrainSchedulePlan
    default_kwargs = {
        'title': u'График',
        'start_date': datetime.now(),
        'end_date': datetime.now() + timedelta(days=1),
        'code': lambda: TrainSchedulePlanFactory.gen_code(),
    }

    @classmethod
    def gen_code(cls):
        cls.code_counter = getattr(cls, 'code_counter', 0) + 1
        return u'plan_code{}'.format(cls.code_counter)


create_train_schedule_plan = factories[TrainSchedulePlan] = TrainSchedulePlanFactory()


class DeLuxeTrainFactory(ModelFactory):
    Model = DeLuxeTrain
    default_kwargs = {
        'title_ru': u'Сапсан',
        'title_en': u'Sapsan',
        'numbers': u'101/102/142',
        'deluxe': False
    }

    def create_object(self, kwargs):
        self.fill_title(kwargs, 'new_L_title_id', 'title')
        return super(DeLuxeTrainFactory, self).create_object(kwargs)


create_deluxe_train = DeLuxeTrainFactory()
factories[DeLuxeTrain] = create_deluxe_train


class PartnerFactory(ModelFactory):
    Model = Partner
    default_kwargs = {
        'current_balance': 1,
        'click_price': 0,
        'click_price_ru': 0,
        'click_price_ua': 0,
        'click_price_tr': 0,
        'click_price_com': 0,
        'code': lambda: PartnerFactory.gen_code(),
    }

    @classmethod
    def gen_code(cls):
        cls.code_counter = getattr(cls, 'code_counter', 0) + 1
        return u'partner_code_{}'.format(cls.code_counter)


create_partner = PartnerFactory()
factories[PartnerFactory] = create_partner


class DohopVendorFactory(ModelFactory):
    Model = DohopVendor

create_dohop_vendor = DohopVendorFactory()
factories[DohopVendorFactory] = create_dohop_vendor


class AmadeusMerchantFactory(ModelFactory):
    Model = AmadeusMerchant


create_amadeus_merchant = AmadeusMerchantFactory()
factories[AmadeusMerchant] = create_amadeus_merchant


class CPCPriceFactory(ModelFactory):
    Model = CPCPrice

create_cpc_price = CPCPriceFactory()
factories[CPCPriceFactory] = create_cpc_price


class PartnerUserFactory(ModelFactory):
    Model = PartnerUser

create_partner_user = PartnerUserFactory()
factories[PartnerUserFactory] = create_partner_user


class DohopVendorIndexInfoFactory(ModelFactory):
    Model = DohopVendorIndexInfo

create_dohop_vendor_index_info = DohopVendorIndexInfoFactory()
factories[DohopVendorIndexInfoFactory] = create_dohop_vendor_index_info


class DohopIndexFacrory(ModelFactory):
    Model = DohopIndex

create_dohop_index = DohopIndexFacrory()
factories[DohopIndexFacrory] = create_dohop_index


class CompanyFactory(ProcessStringKeyMixin, ModelFactory):
    Model = Company

    def create_object(self, kwargs):
        self.fill_title(kwargs, 'new_L_title_id', 'title')
        self.fill_title(kwargs, 'new_L_short_title_id', 'short_title')
        self.fill_text(kwargs, 'new_L_registration_url_id', 'registration_url')
        self.fill_text(kwargs, 'new_L_registration_phone_id', 'registration-phone')

        return super(CompanyFactory, self).create_object(kwargs)


create_company = CompanyFactory()
factories[Company] = create_company


class IataCorrectionFactory(ProcessStringKeyMixin, ModelFactory):
    Model = IataCorrection


create_iatacorrection = IataCorrectionFactory()
factories[IataCorrection] = create_iatacorrection


class AviaCompanyFactory(ProcessStringKeyMixin, ModelFactory):
    Model = AviaCompany


create_aviacompany = AviaCompanyFactory()
factories[AviaCompany] = create_aviacompany


class CompanyTariffFactory(ProcessStringKeyMixin, ModelFactory):
    Model = CompanyTariff


create_companytariff = CompanyTariffFactory()
factories[CompanyTariff] = create_companytariff


class UpdateHistoryRecordFabric(ModelFactory):
    Model = UpdateHistoryRecord
    default_kwargs = {
        "updater_yandex_login": "some_login",
        "updater_role": "some_role",
        "description": "some_description"
    }


create_update_history_record = UpdateHistoryRecordFabric()
factories[UpdateHistoryRecordFabric] = create_update_history_record


class ScriptFactory(ModelFactory):
    Model = Script


create_script = ScriptFactory()
factories[ScriptFactory] = create_script


class ScriptResultFactory(ModelFactory):
    Model = ScriptResult
    default_kwargs = {
        "started_at": datetime(2017, 9, 1)
    }

create_script_result = ScriptResultFactory()
factories[ScriptResultFactory] = create_script_result


class TeamFactory(ModelFactory):
    Model = Team


create_team = TeamFactory()
factories[Team] = create_team


class ServiceFactory(ModelFactory):
    Model = Service


create_service = ServiceFactory()
factories[Service] = create_service


def tomorrow():
    return date.today() + timedelta(1)


def date_future(delta=10):
    return date.today() + timedelta(delta)


class NearCountriesFactory(ModelFactory):
    Model = NearCountries

    def create_object(self, kwargs):
        extra_params = kwargs.pop('__', None) or {}

        near_countries = super(NearCountriesFactory, self).create_object(kwargs)

        neighbours = extra_params.get('neighbours', [])
        near_countries.neighbours.add(*neighbours)

        near_countries.save()

        return near_countries


create_near_countries = NearCountriesFactory()


class SeoDirectionFactory(ModelFactory):
    Model = SeoDirection

create_seo_direction = SeoDirectionFactory()


class StaticPageFactory(ModelFactory):
    Model = StaticPage

create_static_page = StaticPageFactory()


class MinPriceFactory(ModelFactory):
    Model = MinPrice
    default_kwargs = {
        # Здесь нельзя создавать модели иначе они останутся в базе
        'date_forward': tomorrow(),
        'date_backward': date_future(),
        'direct_flight': True,
    }

    def create_object(self, kwargs):
        kwargs.pop('__', None)

        kwargs['currency'] = kwargs.pop('currency', None) or create_currency(
            name='Рубли', code='RUR', iso_code='RUB',
            template='t', template_whole='t', template_cents='t',
            template_tr='t', template_whole_tr='t', template_cents_tr='t'
        )
        min_price = super(MinPriceFactory, self).create_object(kwargs)
        min_price.save()
        return min_price


create_min_price = MinPriceFactory()


class ReviewSourceFactory(ModelFactory):
    Model = FlightReviewSource

create_review_source = ReviewSourceFactory()


class FlightNumberFactory(ModelFactory):
    Model = FlightNumber
    default_kwargs = {
        'flight_number': 'U6 264'
    }

create_flight_number = FlightNumberFactory()


class HolidayFactory(ModelFactory):
    Model = Holiday
    default_kwargs = {
        'first_segment_first_day': date(2017, 9, 1),
        'first_segment_last_day': date(2017, 9, 1),
        'is_active': True,
        'name_tanker_key': 'some'
    }

create_holiday = HolidayFactory()


class HolidayDirectionFactory(ModelFactory):
    Model = HolidayDirection
    default_kwargs = {}

create_holiday_direction = HolidayDirectionFactory()


class ReviewFactory(ModelFactory):
    Model = FlightReview
    default_kwargs = {
        'review_id': 1,
        'review_content': u'Lorem ipsum dolor sit amet, ' +
                          u'consectetur adipiscing elit. Proin vehicula.',
        'review_datetime': datetime(2016, 2, 1, 23, 14, 40),
        'review_url': u'https://avia.yandex.ru/',
    }

    def create_object(self, kwargs):
        extra_params = kwargs.pop('__', None) or {}

        kwargs['source'] = kwargs.pop('source', None) or create_review_source()
        kwargs['airline'] = kwargs.pop('airline', None) or create_company()

        review = super(ReviewFactory, self).create_object(kwargs)

        flight_numbers = extra_params.get('flight_numbers', [])
        review.flight_numbers.add(*flight_numbers)

        review.save()

        return review


create_review = ReviewFactory()


class SettlementImageFactory(ModelFactory):
    Model = SettlementBigImage
    default_kwargs = {
        'url2': u'url2_image'
    }


create_settlement_image = SettlementImageFactory()


class TopFlightFactory(ModelFactory):
    Model = TopFlight
    default_kwargs = {
        'from_point_key': 'c194',
        'to_point_key': 'c1094',
        'day_of_week': 1,
        'redirects': 1,
        'national_version': 'ru',
    }


create_top_flight = TopFlightFactory()


class TabloSourceFactory(ModelFactory):
    Model = TabloSource
    default_kwargs = {
        'description': u'Description',
    }


create_tablo_source = TabloSourceFactory()


class AirportTabloSourceFactory(ModelFactory):
    Model = AirportTabloSource


create_airport_tablo_source = AirportTabloSourceFactory()


def create_instance_by_abstract_class(abclass):
    """
    Создаем моковый инстанс по абстрактному классу.
    """
    if "__abstractmethods__" not in abclass.__dict__:
        return abclass
    new_dict = abclass.__dict__.copy()
    for abstractmethod in abclass.__abstractmethods__:
        new_dict[abstractmethod] = lambda x, *args, **kw: (x, args, kw)

    return Mock(type("dummy_concrete_%s" % abclass.__name__, (abclass,), new_dict)())
