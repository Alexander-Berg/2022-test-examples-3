import pytest
from pydantic import BaseModel, Field

from travel.avia.ad_feed.ad_feed.dumper.yt_dumper import get_schema


def test_get_schema():
    class A(BaseModel):
        x: int = Field(yt_type='int64')
        y: str = Field(yt_type='string')

    assert get_schema(A) == [{'name': 'x', 'type': 'int64'}, {'name': 'y', 'type': 'string'}]


def test_raise_error_on_incorrect_model():
    class A(BaseModel):
        x: int = Field(1)

    with pytest.raises(ValueError):
        get_schema(A)
