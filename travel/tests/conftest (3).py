from __future__ import absolute_import

import os

from travel.avia.library.python.shared_objects import SharedFlag

os.environ['DJANGO_SETTINGS_MODULE'] = 'travel.avia.backend.tests.tests_settings'

import django
django.setup()

import pytest
from faker import Faker

from travel.avia.backend.main.application import create_app


pytest_plugins = [
    'travel.avia.library.python.tester.initializer',
    'travel.avia.library.python.tester.plugins.transaction',
]


@pytest.fixture
def faker():
    return Faker()


@pytest.fixture
def client():
    app = create_app()
    app.shutdown_flag = SharedFlag()
    with app.test_client() as client:
        yield client
