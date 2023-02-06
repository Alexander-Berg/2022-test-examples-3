import os
import shutil
import yatest
import difflib
from market.library.shiny.server.gen.lib.gen_app import generate_app


def test_build_n_run():
    tests_path = os.path.realpath(yatest.common.build_path('market/library/shiny/server/gen/lib/tests'))
    tmpl_path = os.path.realpath(yatest.common.source_path('market/library/shiny/server/gen/lib/tmpl'))
    server_path = os.path.realpath(yatest.common.source_path('market/library/shiny/server/gen/lib/tests/app'))
    physical_path = os.path.join(tests_path, 'app')

    shutil.rmtree(physical_path, ignore_errors=True)
    generate_app(
        name='app',
        path=server_path,
        namespace='NApp',
        bin_name='app',
        user='yuraaka',
        physical_path=physical_path
    )

    for subdir, _, files in os.walk(server_path):
        relpath = os.path.relpath(subdir, start=server_path)
        for filename in files:
            realpath = os.path.join(subdir, filename)
            if os.path.islink(realpath) or filename.endswith('.run') or filename.endswith('pb2.py'):
                continue
            testpath = os.path.join(physical_path, relpath, filename)
            assert os.path.exists(testpath)
            with open(realpath) as real, open(testpath) as test:
                diff = difflib.context_diff(
                    real.readlines(),
                    test.readlines(),
                    fromfile=realpath,
                    tofile=os.path.join(tmpl_path, relpath, filename + '.t'),
                    n=0
                )
                delta = ''.join(diff)
                equal = bool(delta)
                assert not equal, '\n' + delta
