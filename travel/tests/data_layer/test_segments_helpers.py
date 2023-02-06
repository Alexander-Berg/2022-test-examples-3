# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import uuid

import pytest

from common.models.schedule import TrainPurchaseNumber
from common.models.transport import TransportType
from common.tester.factories import create_rthread_segment, create_thread, create_transport_subtype

from travel.rasp.morda_backend.morda_backend.data_layer.segments_helpers import fetch_and_filter_train_purchase_numbers


@pytest.mark.dbuser
def test_fetch_and_filter_train_purchase_numbers():
    def _create_segment(t_type_id, number=None, has_train_tariffs=None):
        kwargs = {'t_type': t_type_id}
        if number:
            kwargs['number'] = number
        if has_train_tariffs is not None:
            kwargs['t_subtype'] = create_transport_subtype(has_train_tariffs=has_train_tariffs, t_type_id=t_type_id,
                                                           code=str(uuid.uuid4()))
        return create_rthread_segment(thread=create_thread(**kwargs))

    suburban_with_train_numbers = _create_segment(TransportType.SUBURBAN_ID, has_train_tariffs=True)
    suburban_with_no_train_numbers = _create_segment(TransportType.SUBURBAN_ID, has_train_tariffs=True)
    suburban_non_express = _create_segment(TransportType.SUBURBAN_ID)
    train_duplicate = _create_segment(TransportType.TRAIN_ID, '666')
    train_non_duplicate = _create_segment(TransportType.TRAIN_ID, '777')
    segments = [
        suburban_with_train_numbers, suburban_with_no_train_numbers,
        suburban_non_express, train_duplicate, train_non_duplicate
    ]

    TrainPurchaseNumber.objects.create(thread=suburban_with_train_numbers.thread, number='666')

    segments = fetch_and_filter_train_purchase_numbers(segments)

    assert suburban_with_train_numbers in segments
    assert getattr(suburban_with_train_numbers, 'train_purchase_numbers', None) == [train_duplicate.number]
    assert suburban_with_no_train_numbers in segments
    assert getattr(suburban_with_no_train_numbers, 'train_purchase_numbers', None) is None
    assert suburban_non_express in segments
    assert getattr(suburban_non_express, 'train_purchase_numbers', None) is None
    assert train_duplicate not in segments
    assert train_non_duplicate in segments
