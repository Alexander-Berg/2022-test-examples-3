# -*- coding: utf-8 -*-


def test_zebra(session_context):
    order = session_context.get_order()
    order.add_snapshot()
    order.add_snapshot(copy_previous=True)
    order.add_snapshot()
    order.add_snapshot(copy_previous=True)
    order.add_snapshot()
    order.add_snapshot(copy_previous=True)
    order.add_snapshot()
    order.add_snapshot(copy_previous=True)
    order.add_snapshot()
    order.add_snapshot(copy_previous=True)
    order.add_snapshot()
    order.check()


def test_zebra_ending_with_copy(session_context):
    order = session_context.get_order()
    order.add_snapshot()
    order.add_snapshot(copy_previous=True)
    order.add_snapshot()
    order.add_snapshot(copy_previous=True)
    order.add_snapshot()
    order.add_snapshot(copy_previous=True)
    order.add_snapshot()
    order.add_snapshot(copy_previous=True)
    order.add_snapshot()
    order.add_snapshot(copy_previous=True)
    order.add_snapshot()
    order.add_snapshot(copy_previous=True)
    order.check()


def test_pair_zebra(session_context):
    order = session_context.get_order()
    order.add_snapshot()
    order.add_snapshot()
    order.add_snapshot(copy_previous=True)
    order.add_snapshot(copy_previous=True)
    order.add_snapshot()
    order.add_snapshot()
    order.add_snapshot(copy_previous=True)
    order.add_snapshot(copy_previous=True)
    order.add_snapshot()
    order.add_snapshot()
    order.add_snapshot(copy_previous=True)
    order.add_snapshot(copy_previous=True)
    order.add_snapshot()
    order.add_snapshot()
    order.check()


def test_pair_zebra_ending_with_copy(session_context):
    order = session_context.get_order()
    order.add_snapshot()
    order.add_snapshot()
    order.add_snapshot(copy_previous=True)
    order.add_snapshot(copy_previous=True)
    order.add_snapshot()
    order.add_snapshot()
    order.add_snapshot(copy_previous=True)
    order.add_snapshot(copy_previous=True)
    order.add_snapshot()
    order.add_snapshot()
    order.add_snapshot(copy_previous=True)
    order.add_snapshot(copy_previous=True)
    order.check()


def test_copies_hole(session_context):
    order = session_context.get_order()
    order.add_snapshot()
    order.add_snapshot(copy_previous=True)
    order.add_snapshot(copy_previous=True)
    order.add_snapshot(copy_previous=True)
    order.add_snapshot(copy_previous=True)
    order.add_snapshot(copy_previous=True)
    order.add_snapshot(copy_previous=True)
    order.add_snapshot()
    order.check()


def test_first_saved(session_context):
    order = session_context.get_order()
    order.add_snapshot(last_saved=True)
    order.add_snapshot()
    order.add_snapshot()
    order.check()


def test_middle_saved(session_context):
    order = session_context.get_order()
    order.add_snapshot()
    order.add_snapshot(last_saved=True)
    order.add_snapshot()
    order.check()


def test_saved_only(session_context):
    order = session_context.get_order()
    order.add_snapshot()
    order.add_snapshot()
    order.add_snapshot(last_saved=True)
    order.check()


def test_middle_saved_skipped(session_context):
    order = session_context.get_order()
    order.add_snapshot()
    order.add_snapshot(skip_save=True)
    order.add_snapshot(last_saved=True)
    order.add_snapshot()
    order.check()


def test_saved_only_skipped(session_context):
    order = session_context.get_order()
    order.add_snapshot()
    order.add_snapshot(skip_save=True)
    order.add_snapshot(last_saved=True)
    order.check()


def test_middle_saved_skipped_copy(session_context):
    order = session_context.get_order()
    order.add_snapshot()
    order.add_snapshot(copy_previous=True, skip_save=True)
    order.add_snapshot(last_saved=True)
    order.add_snapshot()
    order.check()


def test_saved_only_skipped_copy(session_context):
    order = session_context.get_order()
    order.add_snapshot()
    order.add_snapshot(copy_previous=True, skip_save=True)
    order.add_snapshot(last_saved=True)
    order.check()


def test_not_saved_not_unique_key(session_context):
    order = session_context.get_order()
    order.add_snapshot()
    order.add_snapshot(copy_key=True)
    order.add_snapshot()
    order.check()


def test_first_saved_not_unique_key(session_context):
    order = session_context.get_order()
    order.add_snapshot(last_saved=True)
    order.add_snapshot(copy_key=True)
    order.add_snapshot()
    order.check()


def test_middle_saved_not_unique_key(session_context):
    order = session_context.get_order()
    order.add_snapshot()
    order.add_snapshot(copy_key=True, last_saved=True)
    order.add_snapshot()
    order.check()


def test_saved_only_not_unique_key(session_context):
    order = session_context.get_order()
    order.add_snapshot()
    order.add_snapshot(copy_key=True, skip_save=True)
    order.add_snapshot(last_saved=True)
    order.check()
