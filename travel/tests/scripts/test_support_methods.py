# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import os

from travel.rasp.admin.scripts.support_methods import StateSaver


def test_state_saver(tmpdir):
    file_name = 'state.txt'
    directory = tmpdir.mkdir('xxx')

    with mock.patch.object(StateSaver, 'BASE_STATE_PATH', str(directory)):
        state_saver = StateSaver(file_name)
        state_saver.set_state('состояние')
        assert state_saver.get_state() == 'состояние'
        assert state_saver.path == os.path.join(str(directory), file_name)
        assert state_saver.key == os.path.join('run-states', file_name)

        state_saver.clean_state()
        assert state_saver.get_state() is None
