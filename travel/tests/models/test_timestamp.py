from datetime import datetime

import pytest
from django.db import connection

from common.models.timestamp import Timestamp


@pytest.mark.dbuser
def test_get_via_connection():
    expected_value = datetime(2017, 11, 12)
    timestamp = Timestamp.objects.create(code='code1', value=datetime(2017, 11, 12))

    value = timestamp.get_via_connection('code1', connection.connection)
    assert value == expected_value
