# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import time

from travel.rasp.library.python.common23.date.run_mask import RunMask
from travel.rasp.library.python.common23.models.tariffs.aeroex_tariff import AeroexTariff
from travel.rasp.library.python.common23.models.tariffs.tariff_group import TariffGroup
from travel.rasp.library.python.common23.models.tariffs.tariff_type import TariffType
from travel.rasp.library.python.common23.models.tariffs.thread_tariff import ThreadTariff

from travel.rasp.library.python.common23.tester.factories import ModelFactory, ThreadFactory, factories, DEFAULT_TRANSPORT_TYPE


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


class TariffGroupFactory(ModelFactory):
    Model = TariffGroup

    default_kwargs = {
        'title': u'Название группы тарифов'
    }


create_tariff_group = factories[TariffGroup] = TariffGroupFactory()


class TariffTypeFactory(ModelFactory):
    Model = TariffType

    default_kwargs = {
        'title': u'Название типа',
        'category': TariffType.USUAL_CATEGORY,
        'code': lambda: TariffTypeFactory.gen_code(),
    }

    @classmethod
    def gen_code(cls):
        cls.code_counter = getattr(cls, 'code_counter', 0) + 1
        return u'TARIFF_TYPE_CODE_{}'.format(cls.code_counter)

    def create_object(self, kwargs):
        extra_params = kwargs.pop('__', None) or {}
        tariff_groups_params = extra_params.get('tariff_groups', [{}])

        tariff_type = super(TariffTypeFactory, self).create_object(kwargs)

        tariff_groups = []
        for tariff_group in tariff_groups_params:
            if isinstance(tariff_group, TariffGroup):
                tariff_groups.append(tariff_group)
            else:
                tariff_groups.append(create_tariff_group(tariff_group))

        tariff_type.tariff_groups.add(*tariff_groups)
        return tariff_type


create_tariff_type = factories[TariffType] = TariffTypeFactory()


class AeroexTariffFactory(ModelFactory):
    Model = AeroexTariff

    default_kwargs = {
        'station_from': {},
        'station_to': {},
        'tariff': 100.5,
        'type': {},
    }


create_aeroex_tariff = factories[AeroexTariff] = AeroexTariffFactory()
