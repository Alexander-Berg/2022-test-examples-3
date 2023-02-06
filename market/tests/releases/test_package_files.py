import pytest
import yatest

import json
import os


def scan_package_files():
    for root, _, files in os.walk(yatest.common.source_path('market/idx')):
        for filename in ['package.json', 'pkg.json', 'pkg-deploy.json', 'pkg-sandbox.json']:
            if filename in files:
                yield os.path.relpath(os.path.join(root, filename), yatest.common.source_path())


@pytest.mark.parametrize('file_path', scan_package_files())
def test_json_validness(file_path):
    with open(yatest.common.source_path(file_path), 'r') as fd:
        data = json.load(fd)

    assert data is not None
