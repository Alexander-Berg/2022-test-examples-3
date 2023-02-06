from typing import List
import json
from pydantic import parse_obj_as
from pydantic.json import pydantic_encoder
from travel.avia.country_restrictions.lib.types.rich_string import new_rich_text, new_rich_url, RichString, TextBlock, UrlBlock


def test_text_from_json():
    data = '{"data": [{"block_type": "text", "data": {"text": "hello"}}]}'
    actual = parse_obj_as(RichString, json.loads(data))
    expected = RichString(data=[TextBlock.create(text='hello')])
    assert actual == expected


def test_url_from_json():
    data = '{"data": [{"block_type": "url", "data": {"url": "example.com", "text": "demo"}}]}'
    actual = parse_obj_as(RichString, json.loads(data))
    expected = RichString(data=[UrlBlock.create(text='demo', url='example.com')])
    assert actual == expected


def test_list_from_json():
    data = '[{"data": [{"block_type": "url", "data": {"url": "example.com", "text": "demo"}}]},' \
           '{"data": [{"block_type": "text", "data": {"text": "hello"}}]}]'
    actual = parse_obj_as(List[RichString], json.loads(data))
    expected = [
        RichString(data=[UrlBlock.create(text='demo', url='example.com')]),
        RichString(data=[TextBlock.create(text='hello')])
    ]
    assert actual == expected


def test_list_to_json():
    data = [
        new_rich_url(text='demo', url='example.com'),
        new_rich_text(text='hello')
    ]
    actual = json.dumps(data, default=pydantic_encoder)
    expected = '[{"data": [{"block_type": "url", "data": {"text": "demo", "url": "example.com"}}]}, ' \
               '{"data": [{"block_type": "text", "data": {"text": "hello"}}]}]'
    assert actual == expected
