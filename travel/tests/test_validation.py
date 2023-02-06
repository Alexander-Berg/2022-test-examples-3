import csv
from typing import Any

import pytest
import tempfile

from travel.avia.ad_feed.ad_feed.validation import K50Validator, ValidationError


@pytest.fixture()
def validator() -> K50Validator:
    return K50Validator(rows_max_number=2)


@pytest.mark.parametrize(
    ('data', 'ok'),
    [
        ([{'a': '1', 'b': '2'}], False),
        ([{'a': '1', 'b': '2', 'url': 'some_url'}], False),
        ([{'a*': '1', 'b*': '2', 'url': 'some_url'}, {'a*': '1', 'b*': '3', 'url': 'some_url'}], True),
        (
            [
                {'a*': '1', 'b*': '2', 'url': 'some_url'},
                {'a*': '1', 'b*': '3', 'url': 'some_url'},
                {'a*': '2', 'b*': '3', 'url': 'some_url'},
            ],
            False,
        ),
        ([{'a*': '1', 'b*': '2', 'url': 'some_url'}, {'a*': '1', 'b*': '2', 'url': 'some_url'}], False),
        ([{'id': '1', 'url': 'some_url'}, {'id': '2', 'url': 'some_url'}], True),
        ([{'id': '1', 'url': 'some_url'}, {'id': '1', 'url': 'some_url'}], False),
    ],
)
def test_k50_validator(data: list[dict[str, Any]], ok: bool, validator: K50Validator):
    with tempfile.NamedTemporaryFile(mode='w+') as f:
        writer = csv.DictWriter(f, fieldnames=data[0].keys())
        writer.writeheader()
        writer.writerows(data)
        f.flush()
        f.seek(0)
        if ok:
            validator.validate(f)
        else:
            with pytest.raises(ValidationError):
                validator.validate(f)
