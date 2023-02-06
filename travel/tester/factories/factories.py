# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import six
from datetime import datetime, timedelta

from travel.rasp.library.python.common23.date.date_const import MSK_TIMEZONE

from travel.rasp.library.python.common23.models.core.directions.direction import Direction
from travel.rasp.library.python.common23.models.core.directions.external_direction import ExternalDirection
from travel.rasp.library.python.common23.models.core.directions.external_direction_marker import ExternalDirectionMarker
from travel.rasp.library.python.common23.models.core.geo.code_system import CodeSystem
from travel.rasp.library.python.common23.models.core.geo.country import Country
from travel.rasp.library.python.common23.models.core.geo.district import District
from travel.rasp.library.python.common23.models.core.geo.region import Region
from travel.rasp.library.python.common23.models.core.geo.settlement import Settlement
from travel.rasp.library.python.common23.models.core.geo.station_code import StationCode
from travel.rasp.library.python.common23.models.core.geo.station_majority import StationMajority
from travel.rasp.library.python.common23.models.core.geo.station_phone import StationPhone
from travel.rasp.library.python.common23.models.core.geo.station_terminal import StationTerminal
from travel.rasp.library.python.common23.models.core.geo.suburban_zone import SuburbanZone
from travel.rasp.library.python.common23.models.core.geo.way_to_airport import WayToAirport
from travel.rasp.library.python.common23.models.core.schedule.company import Company
from travel.rasp.library.python.common23.models.core.schedule.express_type_lite import ExpressTypeLite
from travel.rasp.library.python.common23.models.core.schedule.rthread_type import RThreadType
from travel.rasp.library.python.common23.models.core.schedule.rtstation import RTStation
from travel.rasp.library.python.common23.models.core.schedule.station_schedule import StationSchedule
from travel.rasp.library.python.common23.models.core.schedule.supplier import Supplier
from travel.rasp.library.python.common23.models.core.schedule.train_schedule_plan import TrainSchedulePlan
from travel.rasp.library.python.common23.models.currency.currency import Currency
from travel.rasp.library.python.common23.models.transport.deluxe_train import DeLuxeTrain
from travel.rasp.library.python.common23.models.transport.transport_model import TransportModel
from travel.rasp.library.python.common23.models.transport.transport_subtype import TransportSubtype
from travel.rasp.library.python.common23.models.transport.transport_subtype_color import TransportSubtypeColor
from travel.rasp.library.python.common23.models.transport.transport_type import TransportType


from travel.rasp.library.python.common23.tester.factories.base_factory import factories, ModelFactory


DEFAULT_TRANSPORT_TYPE = 'bus'


class ProcessStringKeyMixin(object):
    string_key = 'code'

    def __init__(self, **kwargs):
        super(ProcessStringKeyMixin, self).__init__(**kwargs)
        self.key_processors = (self.process_key_string,) + self.key_processors

    def process_key_string(self, key, kwargs):
        if isinstance(key, six.string_types):
            return self.Model.objects.get(**{self.string_key: key})


class CodeSystemFactory(ProcessStringKeyMixin, ModelFactory):
    Model = CodeSystem


create_code_system = factories[CodeSystem] = CodeSystemFactory()


class StationCodeFactory(ModelFactory):
    Model = StationCode


create_station_code = factories[StationCode] = StationCodeFactory()


class DirectionFactory(ModelFactory):
    Model = Direction


create_direction = factories[Direction] = DirectionFactory()


class StationTerminalFactory(ModelFactory):
    default_kwargs = {
        'name': 'НазваниеТерминала',
        'is_international': False,
        'is_domestic': False
    }
    Model = StationTerminal


create_station_terminal = StationTerminalFactory()
factories[StationTerminal] = create_station_terminal


class WayToAirportFactory(ModelFactory):
    default_kwargs = {
        'way_type': 'aeroexpress',
        'title_ru': 'Путь в аэропорт'
    }
    Model = WayToAirport


create_way_to_airport = WayToAirportFactory()
factories[WayToAirport] = create_way_to_airport


class TransportTypeFactory(ProcessStringKeyMixin, ModelFactory):
    Model = TransportType


create_transport_type = TransportTypeFactory()
factories[TransportType] = create_transport_type


class TransportSubtypeFactory(ProcessStringKeyMixin, ModelFactory):
    default_kwargs = {
        'title_ru': 'Подтип Транспорта',
    }
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
        'title': 'НазваниеГорода',
        'majority': 1,
        'time_zone': MSK_TIMEZONE,
        'latitude': 1,
        'longitude': 1,
    }


create_settlement = SettlementFactory()
factories[Settlement] = create_settlement


class CountryFactory(ModelFactory):
    Model = Country
    default_kwargs = {
        'title': 'НазваниеСтраны'
    }


create_country = CountryFactory()
factories[Country] = create_country


class RegionFactory(ModelFactory):
    Model = Region
    default_kwargs = {
        'title': 'НазваниеРегиона',
        'country': {}
    }


create_region = RegionFactory()
factories[Region] = create_region


class DistrictFactory(ModelFactory):
    Model = District
    default_kwargs = {
        'title': 'НазваниеРайона',
        'region': {}
    }


create_district = DistrictFactory()
factories[District] = create_district


class StationPhoneFactory(ModelFactory):
    Model = StationPhone
    default_kwargs = {
        'station': {},
        'phone': '+79123456789'
    }


create_station_phone = factories[StationPhone] = StationPhoneFactory()


class ExternalDirectionFactory(ModelFactory):
    Model = ExternalDirection
    default_kwargs = {
        'full_title': 'Полное название внешнего направления',
        'title': 'Название внешнего направления',
        'code': lambda: ExternalDirectionFactory.code_generator()
    }

    @classmethod
    def code_generator(cls):
        cls._code_generator_counter += 1
        return 'external_direction_code_{}'.format(cls._code_generator_counter)
    _code_generator_counter = 0


create_external_direction = factories[ExternalDirection] = ExternalDirectionFactory()


class ExternalDirectionMarkerFactory(ModelFactory):
    Model = ExternalDirectionMarker
    default_kwargs = {
        'external_direction': {},
        'station': {},
        'order': lambda: ExternalDirectionMarkerFactory.order_generator(),
    }

    @classmethod
    def order_generator(cls):
        cls._order_generator_counter += 1
        return cls._order_generator_counter
    _order_generator_counter = 0


create_external_direction_marker = factories[ExternalDirectionMarker] = ExternalDirectionMarkerFactory()


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


class SupplierFactory(ProcessStringKeyMixin, ModelFactory):
    Model = Supplier

    default_kwargs = {
        'code': lambda: SupplierFactory.gen_supplier_code()
    }

    @classmethod
    def gen_supplier_code(cls):
        cls.uid_counter = getattr(cls, 'uid_counter', 0) + 1
        return 'SUPPLIER_CODE_{}'.format(cls.uid_counter)


create_supplier = SupplierFactory()
factories[Supplier] = create_supplier


class CurrencyFactory(ModelFactory):
    Model = Currency

    default_kwargs = dict(
        name='рубли',
        code='RUR',
        iso_code='RUB',
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


class TransportModelFactory(ModelFactory):
    Model = TransportModel

    default_kwargs = {
        't_type': DEFAULT_TRANSPORT_TYPE,
        'title': lambda: TransportModelFactory.gen_title(),
    }

    @classmethod
    def gen_title(cls):
        cls.title_counter = getattr(cls, 'title_counter', 0) + 1
        return 'TransportModel{}'.format(cls.title_counter)


create_transport_model = factories[TransportModel] = TransportModelFactory()


class StationScheduleFactory(ModelFactory):
    Model = StationSchedule


create_station_schedule = factories[StationSchedule] = StationScheduleFactory()


class SuburbanZoneFactory(ModelFactory):
    Model = SuburbanZone
    default_kwargs = {
        'title': 'НазваниеПригороднойЗоны',
        'title_from': 'ОтЦентраПригороднойЗоны',
        'title_to': 'НаЦентрПригороднойЗоны',
        'code': lambda: SuburbanZoneFactory.gen_code(),
    }

    @classmethod
    def gen_code(cls):
        cls.code_counter = getattr(cls, 'code_counter', 0) + 1
        return 'zone_code{}'.format(cls.code_counter)


create_suburban_zone = SuburbanZoneFactory()
factories[SuburbanZone] = create_suburban_zone


class TrainSchedulePlanFactory(ModelFactory):
    Model = TrainSchedulePlan
    default_kwargs = {
        'title': 'График',
        'start_date': datetime.now(),
        'end_date': datetime.now() + timedelta(days=1),
        'code': lambda: TrainSchedulePlanFactory.gen_code(),
    }

    @classmethod
    def gen_code(cls):
        cls.code_counter = getattr(cls, 'code_counter', 0) + 1
        return 'plan_code{}'.format(cls.code_counter)


create_train_schedule_plan = factories[TrainSchedulePlan] = TrainSchedulePlanFactory()


class DeLuxeTrainFactory(ModelFactory):
    Model = DeLuxeTrain
    default_kwargs = {
        'title_ru': 'Сапсан',
        'title_en': 'Sapsan',
        'numbers': '101/102/142',
        'deluxe': False
    }


create_deluxe_train = DeLuxeTrainFactory()
factories[DeLuxeTrain] = create_deluxe_train


class CompanyFactory(ProcessStringKeyMixin, ModelFactory):
    Model = Company


create_company = CompanyFactory()
factories[Company] = create_company
