# -*- coding: utf-8 -*-
from __future__ import unicode_literals, absolute_import

import os

from travel.avia.backend.main.lib.file_flag import set_temporary_flag, is_temporary_flag_up, get_flag_info


def test_file_flag(tmp_path):
    pause_ping_file = str(tmp_path / 'pause_ping.flag')
    assert not os.path.exists(pause_ping_file)

    set_temporary_flag(pause_ping_file, 100)
    assert is_temporary_flag_up(pause_ping_file)
    _mtime, content = get_flag_info(pause_ping_file)
    assert int(content) == 100
