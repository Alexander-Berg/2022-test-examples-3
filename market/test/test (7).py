# coding: utf-8

import json
import yatest
import os


def test_bundle_config_content():
    bundles_config_dir = yatest.common.source_path("market/report/data/formulas/blender_bundles")
    for bundle in os.listdir(bundles_config_dir):
        path = os.path.join(bundles_config_dir, bundle)
        with open(path) as fobj:
            try:
                json.load(fobj)
            except Exception as e:
                assert False, 'cannot load bundle {} {}'.format(path, e)
