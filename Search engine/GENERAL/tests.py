#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import logging
from contextlib import contextmanager

import pytest
import pandas as pd

from helpers import resample_full_periods, get_last_full_fact_period

logger = logging.getLogger(__name__)
logging.basicConfig(level=logging.DEBUG)


@contextmanager
def does_not_raise():
    yield


get_last_full_fact_period_testdata = [
    ('2021-11-30', 'monthly', does_not_raise(), '2021-11-01'),
    ('2021-11-29', 'monthly', does_not_raise(), '2021-10-01'),
    ('2021-01-31', 'monthly', does_not_raise(), '2021-01-01'),
    ('2021-01-30', 'monthly', does_not_raise(), '2020-12-01'),
    ('2021-11-30', 'weekly', does_not_raise(), '2021-11-22'),
    ('2021-11-28', 'weekly', does_not_raise(), '2021-11-22'),
    ('2021-01-31', 'weekly', does_not_raise(), '2021-01-25'),
    ('2022-01-02', 'weekly', does_not_raise(), '2021-12-27'),
    ('2021-12-31', 'yearly', does_not_raise(), '2021-01-01'),
    ('2021-11-28', 'yearly', does_not_raise(), '2020-01-01'),
    ('2021-11-30', 'quarterly', pytest.raises(ValueError), None),
    ('2021-11-30', None, pytest.raises(ValueError), None),
    ('2021-11-30', 'test', pytest.raises(ValueError), None),
]


@pytest.mark.parametrize('df,scale,expectation,expected', get_last_full_fact_period_testdata)
def test_get_last_full_fact_period(df, scale, expectation, expected):
    with expectation:
        assert get_last_full_fact_period(df, scale) == pd.to_datetime(expected)


get_resample_full_periods_testdata = [
    (pd.DataFrame([1, 2], index=['2021-12-31', '2021-11-01']), 'monthly', does_not_raise(), '2021-11-01', '2021-12-01'),
    (pd.DataFrame([1, 2], index=['2021-12-30', '2021-10-11']), 'monthly', does_not_raise(), '2021-11-01', '2021-11-01'),
    (pd.DataFrame([1, 2], index=['2021-11-08', '2021-11-16']), 'weekly', does_not_raise(), '2021-11-08', '2021-11-08'),
    (pd.DataFrame([1, 2], index=['2021-11-08', '2021-11-21']), 'weekly', does_not_raise(), '2021-11-08', '2021-11-15'),
    (pd.DataFrame([1, 2], index=['2021-11-09', '2021-11-21']), 'weekly', does_not_raise(), '2021-11-15', '2021-11-15'),
    (pd.DataFrame([1, 2], index=['2021-12-31', '2020-11-21']), 'yearly', does_not_raise(), '2021-01-01', '2021-01-01'),
    (pd.DataFrame([1, 2], index=['2022-12-30', '2020-01-01']), 'yearly', does_not_raise(), '2020-01-01', '2021-01-01'),
    (pd.DataFrame([1, 2], index=['2021-11-31', '2021-11-01']), 'test', pytest.raises(ValueError), None, None),
    (pd.DataFrame([], index=['2021-09-08', '2021-07-03']), 'monthly', does_not_raise(), '2021-07-03', '2021-09-08'),
]


@pytest.mark.parametrize('df,scale,expectation,expected_min,expected_max', get_resample_full_periods_testdata)
def test_resample_full_periods(df, scale, expectation, expected_min, expected_max):
    with expectation:
        df.index = pd.to_datetime(df.index)
        res = resample_full_periods(df, scale)
        assert res.index.min() == pd.to_datetime(expected_min)
        assert res.index.max() == pd.to_datetime(expected_max)
