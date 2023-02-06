# -*- coding: utf-8 -*-

from context import SessionContext


def test_no_label(session_context: SessionContext):
    order = session_context.get_order()
    order.add_snapshot()
    order.add_snapshot(fields_to_change=dict(status='pending'))
    order.add_snapshot(fields_to_change=dict(status='confirmed'))
    order.check()


def test_label(session_context: SessionContext):
    order = session_context.get_order(has_label=True)
    order.add_snapshot()
    order.add_snapshot(fields_to_change=dict(status='pending'))
    order.add_snapshot(fields_to_change=dict(status='confirmed'))
    order.check()
