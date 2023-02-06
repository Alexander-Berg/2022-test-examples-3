# -*- coding: utf-8 -*-
import argparse

from rtcc.core.common import ConfigurationID
from rtcc.core.discoverer import Discoverer
from rtcc.core.session import Session


def _create_test(parser=None):
    def handler(args):
        try:
            import py, pytest

            def download_tests(path):
                svnpath = py.path.svnurl(
                        "svn+ssh://arcadia.yandex.ru/arc/trunk/arcadia/search/garden/runtime_tests/configuration")
                svnpath.export(path)

            def generate_config(stream, type):
                conf_id = ConfigurationID.parse(type, "NOAPACHE", "NOAPACHE")
                c_type = Discoverer().get("noapache")
                c_object = c_type(conf_id, session=Session())
                view = c_object.view("complete")
                stream.write(view)

            basepath = py.path.local.make_numbered_dir(prefix='gencfg.upper.test-')
            download_tests(basepath.join('tests').strpath)
            generate_config(basepath.join('config.cfg'), args.type)
            pytest.main([basepath.join('tests', 'tests', 'test_noapache_config.py').strpath,
                         "--config={generated}".format(generated=basepath.join('config.cfg').strpath),
                         "-m=not dynamic and not depricated"])
        except ImportError:
            print "You should have py, pytest to be installed befor run test"
            raise

    parser = parser or argparse.ArgumentParser()
    parser.add_argument('--type', default='MSK_WEB_RKUB_PRODUCTION',
                        help="Configuration type (ex. MSK_WEB_RKUB_PRODUCTION)")
    parser.set_defaults(func=handler)
