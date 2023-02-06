# -*- coding: utf-8 -*-

import pytest

from .common import backctld_app  # noqa


@pytest.yield_fixture(scope='class')
def updater(tmpdir_factory, backctld_app):  # noqa
    root_dir = tmpdir_factory.mktemp('updater.rollback')
    backctld_app.set_root_dir(root_dir)
    yield backctld_app


@pytest.mark.parametrize("pipeline", [
    'qbid',
])
def test_not_supported_rollback(updater, pipeline):
    result = updater.run('updater rollback {}'.format(pipeline))
    assert '! error: not supported for {} pipeline'.format(pipeline) == result
