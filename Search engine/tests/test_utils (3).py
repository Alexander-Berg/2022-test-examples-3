import os
import re
import pathlib

from search.rpslimiter.rpslimiter_agent.lib.utils import in_tempdir


def test_in_tempdir(tmpdir):
    def abs_dir(dd):
        return str(pathlib.Path(dd).resolve())

    curdir = abs_dir('.')

    data = '111'
    fname = 'aaa.txt'

    dirs = []

    for i in range(3):
        # working in a tempdir
        with in_tempdir(prefix_dir=os.path.join(tmpdir, 'cfg_update'), keep=2) as d:
            abs_d = pathlib.Path(tmpdir) / d
            assert abs_d.exists()
            with open(fname, 'w') as f:
                f.write(data)
            assert abs_dir('.') == str(abs_d.resolve())
            assert re.match('.*/cfg_update/temp[.]\\d{19,}[.].*$', d)
        assert abs_dir('.') == curdir
        dirs.append(abs_d)

    # last 2 tempdirs should persist
    assert not dirs[0].exists()
    assert dirs[1].exists()
    assert dirs[2].exists()
