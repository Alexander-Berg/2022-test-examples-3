from hamcrest import assert_that, has_items, equal_to, has_entries, contains_inanyorder
import pytest

from market.idx.admin.isupport.lib.stfilter import filter_by_keys


def test_filter_by_keys():
    issues = [{'key': 'One'},
              {'key': 'Two'},
              {'key': 'Three'}]
    pred = filter_by_keys(['One', 'Three'])
    filtered = [i for i in issues if pred(i)]
    assert_that(filtered == [{'key': 'One'}, {'key': 'Three'}]


def test_weght_calculator():
    assert_that(2*2, equal_to(4))
