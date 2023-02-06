# -*- coding: utf-8 -*-
import pytest
import mock

from selenium import webdriver

from travel.avia.revise.extractor.extract import get_extractor, ExtractedInfo, Price
from travel.avia.revise.revise_task.celery_app import extract_with_retries
from travel.avia.revise.extractor.report import ExtractionError


def test_fake_partner():
    with pytest.raises(Exception):
        get_extractor('nonexistent_partner', mock.create_autospec(webdriver.Remote))


def test_retries():
    attempt = 3

    expected = ExtractedInfo(price=Price(value=10, currency='RUB'), screenshots=[], meta=None)

    def extractor(*args, **kwargs):
        nonlocal attempt

        attempt -= 1
        if attempt == 0:
            return expected
        return ExtractionError(exc_info=None, screenshots=[])

    actual = extract_with_retries(extractor=extractor, url='some_url', tries=3)
    assert actual == expected
