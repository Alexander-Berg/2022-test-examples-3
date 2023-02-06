# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import travel.rasp.admin.scripts.load_project  # noqa

import argparse
import codecs
import json
import logging
import os.path
from travel.rasp.admin.admin.red.models import Package, MetaRoute, MetaRouteStation
from travel.rasp.library.python.common23.db.switcher import switcher
from travel.rasp.library.python.common23.models.core.geo.region import Region
from travel.rasp.library.python.common23.models.core.geo.station import Station
from travel.rasp.library.python.common23.models.core.schedule.supplier import Supplier
from travel.rasp.library.python.common23.models.transport.transport_type import TransportType
from travel.rasp.library.python.common23.models.transport.transport_subtype import TransportSubtype
from travel.rasp.library.python.common23.tester.factories import create_train_schedule_plan
from common.utils.admindumps import get_latest_dump_sandbox_url
from common.utils.dump import DbDumpManager
from datetime import date
from django.conf import settings
from django.db import connection, connections

from travel.rasp.admin.importinfo.models.af import AFScheduleFile
from travel.rasp.admin.importinfo.models.two_stage_import import TwoStageImportPackage, TSISetting
from travel.rasp.admin.lib.logs import create_current_file_run_log, print_log_to_stdout
from travel.rasp.admin.lib.maintenance.db_update import run_migrations
from travel.rasp.admin.lib.maintenance.flags import set_flag
from travel.rasp.admin.tests_integration.utils import get_log_file


log = logging.getLogger(__name__)
ADMIN_INTEGRATION_PATH = settings.PROJECT_PATH


def prepare_db(log_file=None):
    log_file = get_log_file(log_file)
    load_dump_into_integration_databases()
    run_migrations()

    """
    reset:
        delete from www_supplier where code='integration-tsi-supplier' or code='integration-red-supplier';
        delete from red_package where title = 'bus' or title = 'sea' or title = 'river';
        delete from www_trainscheduleplan where code='g17';
        delete from www_routepath;
    """

    setup_www_stations()
    setup_tsi_autoimport()
    setup_red_autoimport()
    setup_af_data()
    setup_routepaths()

    setup_transport_data()


def load_dump_into_integration_databases():
    exclude_tables = (
        'django_admin_log',
        'importinfo_afdbchangesfile',
        'importinfo_afschedulefile',
        'importinfo_afupdatefile',
        'importinfo_blacklist',
        'importinfo_originalthreaddata',
        'importinfo_relatedlog',
        'order_partnerreview',
        'red_metaroute',
        'red_metaroutestation',
        'red_package',
        'urban_package',
        'urban_package_routes',
        'urban_package_stations',
        'urban_packageerror',
        'urban_packagepreparsedlink',
        'urban_packagepreparsedstation',
        'urban_packagepreparsedstationexit',
        'urban_packagepreparsedsupplier',
        'urban_packagepreparsedthread',
        'urban_packagepreparsedthreadgeometry',
        'urban_packagepreparsedthreadschedule',
        'urban_packagepreparsedthreadstation',
        'www_mincountryprice',
        'www_minprice',
        'www_namesearchindex',
        'www_route',
        'www_routenumberindex',
        'www_routeuidmap',
        'www_routepath',
        'www_rthread',
        'www_rthreaduidmap',
        'www_rtstation',
        'www_stationschedule',
        'www_suggest',
        'www_threadtariff',
        'www_znoderoute2',
        'z_tablo2',
    )
    latest_dump_url = get_latest_dump_sandbox_url()

    for db_role in (settings.WORK_DB, settings.SERVICE_DB):
        alias = switcher.get_db_alias(db_role)
        dbwrapper = connections[alias]

        dump_manager = DbDumpManager(dbwrapper.get_db_name(), connection=dbwrapper, exclude_tables=exclude_tables)
        dump_manager.update_database_from_dump_url(latest_dump_url)

    for db_role in (settings.WORK_DB, settings.SERVICE_DB):
        set_flag('maintenance', False, db_role)


def setup_tsi_autoimport():
    TwoStageImportPackage.objects.update(autoimport=False)

    tsi_filepath = os.path.join(ADMIN_INTEGRATION_PATH, 'data', 'tsi_data', 'cysix.xml')
    supplier = Supplier.objects.create(code='integration-tsi-supplier')
    with open(tsi_filepath, 'rt') as f:
        package = TwoStageImportPackage.objects.create(supplier=supplier, package_file=f, autoimport=True)
    TSISetting.objects.get_or_create(package=package)


def setup_af_data():
    af_filepath = os.path.join(ADMIN_INTEGRATION_PATH, 'data', 'af_test.xml')
    with codecs.open(af_filepath, 'r', encoding='cp1251') as f:
        content = f.read()
    create_train_schedule_plan(start_date=date(2016, 12, 10),
                               end_date=date(2017, 12, 8),
                               code='g17')
    AFScheduleFile.objects.create(region=Region.objects.get(pk=Region.MOSCOW_REGION_ID),
                                  schedule_file_name='af_test.xml',
                                  schedule_file=content)


def setup_routepaths():
    sql_filepath = os.path.join(ADMIN_INTEGRATION_PATH, 'data', 'www_routepath.sql')
    cursor = connection.cursor()
    with open(sql_filepath) as f:
        cursor.execute(f.read())


def setup_red_autoimport():
    supplier = Supplier.objects.create(code='integration-red-supplier')

    bus_package = Package.objects.create(title='bus', autoimport=True, t_type_id=TransportType.BUS_ID,
                                         t_subtype_id=TransportSubtype.BUS_ID)
    bus_metaroute = MetaRoute.objects.create(package=bus_package, scheme='7.00, 19.00', supplier=supplier,
                                             t_type_id=TransportType.BUS_ID, t_subtype_id=TransportSubtype.BUS_ID)
    MetaRouteStation.objects.create(metaroute=bus_metaroute, departure=0, station_id=9746958)
    MetaRouteStation.objects.create(metaroute=bus_metaroute, arrival=100, station_id=9752600)

    sea_package = Package.objects.create(title='sea', autoimport=True, t_type_id=TransportType.WATER_ID,
                                         t_subtype_id=TransportSubtype.SEA_ID)
    sea_metaroute = MetaRoute.objects.create(package=sea_package, scheme='8.00, 20.00', supplier=supplier,
                                             t_type_id=TransportType.WATER_ID, t_subtype_id=TransportSubtype.SEA_ID)
    MetaRouteStation.objects.create(metaroute=sea_metaroute, departure=0, station_id=9834282)
    MetaRouteStation.objects.create(metaroute=sea_metaroute, arrival=100, station_id=9834281)

    river_package = Package.objects.create(title='river', autoimport=True, t_type_id=TransportType.WATER_ID,
                                           t_subtype_id=TransportSubtype.RIVER_ID)
    river_metaroute = MetaRoute.objects.create(package=river_package, scheme='9.00, 21.00', supplier=supplier,
                                               t_type_id=TransportType.WATER_ID, t_subtype_id=TransportSubtype.RIVER_ID)
    MetaRouteStation.objects.create(metaroute=river_metaroute, departure=0, station_id=9752598)
    MetaRouteStation.objects.create(metaroute=river_metaroute, arrival=100, station_id=9752599)


def setup_www_stations():
    # Скрываем все станции чтобы www_stations проходил быстрее
    Station.objects.all().update(hidden=1)


def setup_transport_data():
    """
    https://st.yandex-team.ru/RASPFRONT-6606

    travel/rasp/library/python/common/data_api/transport_page/transport_country.py
    """

    from travel.rasp.library.python.common23.db.mongo import database

    transport_data_json = os.path.join(ADMIN_INTEGRATION_PATH, 'data/transport_data.json')
    transport_data = json.load(open(transport_data_json))
    database.transport.remove({})
    database.transport.insert(transport_data)


def main():
    log_file = create_current_file_run_log()

    parser = argparse.ArgumentParser()
    parser.add_argument('-v', '--verbose', action='store_true', help=u'выводить лог на экран')

    args = parser.parse_args()

    if args.verbose:
        print_log_to_stdout()

    prepare_db(log_file)


if __name__ == '__main__':
    main()
