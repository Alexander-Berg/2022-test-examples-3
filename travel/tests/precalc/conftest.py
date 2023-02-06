# -*- coding: utf-8 -*-

import mock
import pytest


@pytest.yield_fixture
def precalc_db_mock(tmpdir):
    import precalc.utils.db

    with mock.patch.object(precalc.utils.db, 'DATA_ROOT', str(tmpdir)), \
            mock.patch.object(precalc.utils.db, 'TMP_DIR', str(tmpdir)):
        yield precalc.utils.db
