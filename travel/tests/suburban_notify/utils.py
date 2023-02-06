# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from django.conf import settings

from common.db.mongo import databases
from common.models.geo import Station
from common.models.schedule import RThread, RTStation
from common.tester.factories import create_station, create_thread
from travel.rasp.info_center.info_center.suburban_notify.db import TThread, TRts, TStation, clear_caches
from travel.rasp.info_center.info_center.suburban_notify.subscriptions.models import Subscription

create_station = create_station.mutate(t_type='suburban')
create_thread = create_thread.mutate(t_type='suburban', __={'calculate_noderoute': True})


def convert_db_obj(obj):
    if isinstance(obj, RThread):
        return TThread.get(obj.id)
    elif isinstance(obj, RTStation):
        return TRts.get(obj.id)
    elif isinstance(obj, Station):
        return TStation.get(obj.id)

    return obj


def convert_db_objs(*objs):
    return map(convert_db_obj, objs)


def create_subscription(**kwargs):
    Subscription.get_collection().insert_one(kwargs)


def create_changes(**kwargs):
    kwargs['script_run_id'] = 'script_id'
    databases[settings.SUBURBAN_NOTIFICATION_DATABASE_NAME].subscription_changes.insert_one(kwargs)


class BaseNotificationTest(object):
    def setup(self):
        clear_caches()
