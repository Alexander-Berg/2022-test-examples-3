# -*- coding: utf-8 -*-


import logging

from mapreduce.yt.python.yt_stuff import YtConfig, YtStuff
import pytest

from context import SessionContext
from flow_app import FlowApp
from results import Results
from yt_helper import YtHelper


@pytest.fixture(scope='session')
def local_yt(request):
    yt = YtStuff(YtConfig(wait_tablet_cell_initialization=True))
    yt.start_local_yt()
    request.addfinalizer(yt.stop_local_yt)
    return yt


@pytest.fixture(scope='session')
def new_context(local_yt):
    return SessionContext()


@pytest.fixture(scope='session')
def session_context(new_context, local_yt):
    logging.info('Running app')
    session_context = new_context

    snapshots_to_send = session_context.get_snapshots_to_send()
    logging.info('Snapshots to send: %s', snapshots_to_send)

    yt_helper = YtHelper(local_yt.yt_client, local_yt.get_server())
    flow_app = FlowApp(yt_helper)
    flow_app.run_app(
        processed_snapshots=session_context.get_processed_snapshots(),
        saved_snapshots=session_context.get_saved_snapshots(),
        purgatory_items=session_context.get_purgatory_items(),
        labels_to_send=session_context.get_labels_to_send(),
        snapshots_to_send=session_context.get_snapshots_to_send(),
    )
    table_data = yt_helper.read_tables()
    logging.info('Got table data: %s', table_data)
    session_context.results = Results(table_data)
    return session_context


@pytest.fixture(scope="session", autouse=True)
def init_tests(request, new_context):
    logging.info('Preparing context')
    session_context = new_context
    session = request.node
    for test in session.items:
        if 'session_context' in test.fixturenames:
            test.obj(session_context)
    session_context.finish_preparing()
