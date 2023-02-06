# -*- coding: utf-8 -*-

import os
import pytest

from .common import backctld_app  # noqa


@pytest.yield_fixture(scope='class')  # noqa
def root_dir(tmpdir_factory):
    return tmpdir_factory.mktemp('updater.update_service')


@pytest.yield_fixture(scope='class')
def updater(root_dir, backctld_app):  # noqa
    backctld_app.set_root_dir(root_dir)
    return backctld_app


def test_undefined_pipeline(updater):
    result = updater.run('updater update_service i_does_not_exist http://some.url')
    assert '! error: undefined pipeline i_does_not_exist' == result


@pytest.mark.parametrize("pipeline", [
    'qbid',
])
def test_no_url(updater, pipeline):
    result = updater.run('updater update_service {}'.format(pipeline))
    assert '! error: no url' == result


@pytest.mark.parametrize("pipeline", [
    'qbid',
])
def test_bad_url(updater, pipeline):
    updater.run('updater update_service {} bad_url'.format(pipeline))
    result = updater.run('updater check {}'.format(pipeline))
    assert '! error: bad url' == result


class Response(object):
    def __init__(self, code=200):
        self.code = code
        self.total_time = 10

    def __bool__(self):
        return self.code == 200

    def __nonzero__(self):
        return self.code == 200


@pytest.mark.parametrize("pipeline", [
    'qbid',
])
def test_ban_pipeline(updater, pipeline, root_dir):
    banned_file_path = os.path.join(str(root_dir), 'search', 'banned.lst')
    with open(banned_file_path, 'w') as f:
        f.write(pipeline)
    result = updater.run('updater update_service {} http://some.url'.format(pipeline))
    os.remove(banned_file_path)
    assert '! error: pipeline {} banned'.format(pipeline) == result


@pytest.mark.parametrize("pipeline", [
    'qbid',
])
def test_noban_pipeline(updater, pipeline, root_dir):
    banned_file_path = os.path.join(str(root_dir), 'search', 'banned.lst')
    with open(banned_file_path, 'w') as f:
        f.write('some_unexisted_pipeline')
    result = updater.run('updater update_service {} http://some.url'.format(pipeline))
    os.remove(banned_file_path)
    assert 'ok' == result
