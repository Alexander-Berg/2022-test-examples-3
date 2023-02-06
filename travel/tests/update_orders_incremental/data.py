# -*- coding: utf-8 -*-

from datetime import datetime
from typing import Any, Dict, Iterable, NamedTuple, Type

from travel.cpa.lib.order_snapshot import OrderSnapshot


Fields = Dict[str, Any]


TableData = Iterable[Fields]


class OrderKey(NamedTuple):
    partner_name: str
    partner_order_id: str


class SnapshotProducer:

    __generated_fields__ = [
        'category',
        'partner_name',
        'partner_order_id',
        'travel_order_id',
        'created_at',
        'currency_code',
        'check_in',
        'check_out',
    ]

    def __init__(self, snapshot_cls: Type[OrderSnapshot]):
        self.snapshot_cls = snapshot_cls
        now = datetime.now()
        self.now_ts = int(now.timestamp())
        self.today = str(now.date())

    def get_snapshot(self, category, partner_name: str, partner_order_id: int, currency_code: str):
        snapshot = self.snapshot_cls()
        snapshot.category = category
        snapshot.partner_name = partner_name
        snapshot.partner_order_id = str(partner_order_id)
        snapshot.travel_order_id = f'{partner_name}:{partner_order_id}'
        snapshot.created_at = self.now_ts
        snapshot.currency_code = currency_code
        if 'check_in' in snapshot.__fields__:
            snapshot.check_in = self.today
        if 'check_out' in snapshot.__fields__:
            snapshot.check_out = self.today
        for field_name, field in snapshot.__fields__.items():
            if field_name in self.__generated_fields__:
                continue
            field_class = type(field)
            underlying_type = field_class.__underlying_type__
            if underlying_type is None:
                setattr(snapshot, field_name, list())
            elif issubclass(underlying_type, str):
                setattr(snapshot, field_name, f'{field_name}_{partner_order_id}')
            elif issubclass(underlying_type, bool):
                setattr(snapshot, field_name, True)
            elif issubclass(underlying_type, int):
                setattr(snapshot, field_name, partner_order_id)
            elif issubclass(underlying_type, float):
                setattr(snapshot, field_name, float(partner_order_id))
            else:
                raise Exception(f'Unhandled field type {field_class} for {field_name}')
        return snapshot
