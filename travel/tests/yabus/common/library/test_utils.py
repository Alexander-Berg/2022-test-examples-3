# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import contextlib
import importlib
import itertools
import mock

from travel.library.python.dicts.buses.point_matching_repository import PointMatchingRepository
from travel.library.python.dicts.station_repository import StationRepository
from travel.library.python.dicts.station_to_settlement_repository import StationToSettlementRepository
from travel.proto.dicts.buses.point_matching_pb2 import TPointMatching
from travel.proto.dicts.rasp.station_pb2 import TStation
from travel.proto.dicts.rasp.station_to_settlement_pb2 import TStation2Settlement
from travel.proto.dicts.buses.supplier_pb2 import TSupplier
from travel.proto.dicts.buses.register_type_pb2 import TRegisterType

from util.point_key import PointKey
from yabus.providers import point_matching_provider
from yabus.providers.point_matching_provider import PointMatchingProvider
from yabus.providers.point_relations_provider import PointRelationsProvider
from yabus.providers import supplier_provider
from yabus.providers import register_type_provider


def _format_call(name, args, kwargs):
    return '{}({})'.format(
        name,
        ', '.join(itertools.chain(map('{}'.format, args), itertools.starmap('{}={}'.format, kwargs.items())))
    )


@contextlib.contextmanager
def converter_patch(connector_name):
    module = importlib.import_module('yabus.{}'.format(connector_name))
    converter = module.converter.point_converter
    with mock.patch.object(converter, '_was_setup', True), \
        mock.patch.object(converter, 'backmap', side_effect=lambda *args, **kwargs:
                            _format_call('backmap', args, kwargs)), \
        mock.patch.object(converter, 'deprecated_relations_backmap', side_effect=lambda *args, **kwargs:
                            frozenset([_format_call('relations_backmap', args, kwargs)])), \
        mock.patch.object(converter, 'backmap_to_parents', side_effect=lambda *args, **kwargs:
                            frozenset([_format_call('backmap_to_parents', args, kwargs)])), \
        mock.patch.object(converter, 'backmap_to_children', create=True, side_effect=lambda *args, **kwargs:
                            frozenset([_format_call('backmap_to_children', args, kwargs)])), \
        mock.patch.object(converter, 'map', side_effect=lambda *args, **kwargs:
                            frozenset([_format_call('map', args, kwargs)])), \
        mock.patch.object(converter, 'map_to_children', side_effect=lambda *args, **kwargs:
                            frozenset([_format_call('map_to_children', args, kwargs)])):
        yield


_SUPPLIER_ID = 1


@contextlib.contextmanager
def supplier_provider_patch(connector_name):
    with mock.patch.object(
        supplier_provider,
        'get_by_code',
        side_effect=lambda *args: TSupplier(id=_SUPPLIER_ID, code=connector_name, name=connector_name)
    ):
        yield


@contextlib.contextmanager
def register_type_provider_patch():
    with mock.patch.object(
        register_type_provider,
        'get_by_id',
        side_effect=lambda *args: TRegisterType(id=1, code='register_type_code')
    ):
        yield


def make_matching_provider(mapping):
    matching_repo = PointMatchingRepository()
    i = 1
    for point_key, supplier_ids_types in mapping.items():
        for supplier_point_id, supplier_type in supplier_ids_types:
            matching_repo.add_object(TPointMatching(
                id=i,
                point_key=PointKey.load(point_key).dump_to_proto(),
                supplier_point_id=supplier_point_id,
                supplier_id=_SUPPLIER_ID,
                type=supplier_type,
            ))
            i += 1
    matching_provider = PointMatchingProvider()
    matching_provider.setup(point_matching_repo=matching_repo)
    return matching_provider


@contextlib.contextmanager
def matching_provider_patch(mapping):
    provider = make_matching_provider(mapping)
    with mock.patch.object(
        point_matching_provider,
        'get',
        side_effect=lambda *args: provider.get(*args),
    ):
        yield


def make_point_relations_provider(relations):
    station_repo = StationRepository()
    settlement2station_repo = StationToSettlementRepository()

    for parent, children in relations.items():
        settlement_id = PointKey.load(parent).id
        for child in children:
            station_id = PointKey.load(child).id
            if station_repo.get(station_id) is None:
                station_repo.add_object(TStation(
                    Id=station_id,
                    SettlementId=settlement_id,
                ))
            else:
                settlement2station_repo.add_object(TStation2Settlement(
                    SettlementId=settlement_id,
                    StationId=station_id,
                ))
    point_relations_provider = PointRelationsProvider()
    point_relations_provider.setup(station_repo=station_repo, station2settlement_repo=settlement2station_repo)
    return point_relations_provider
