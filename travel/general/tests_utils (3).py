# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from travel.rasp.wizards.train_wizard_api.direction.segments import TrainSegment
from travel.rasp.wizards.train_wizard_api.lib.pgaas_price_store.models.place import Place


def make_places_group(coach_type=None, count=None, max_seats_in_the_same_car=None, price=None, service_class=None):
    return Place(coach_type, count, max_seats_in_the_same_car, price, price_details=None, service_class=service_class)


def make_train_segment(**kwargs):
    return TrainSegment(**{field: kwargs.get(field) for field in TrainSegment._fields})
