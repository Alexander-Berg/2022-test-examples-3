from travel.avia.country_restrictions.lib.types import Metric
from travel.avia.country_restrictions.lib.types.metric_type.list_metric_type import ListMetricType
from travel.avia.country_restrictions.lib.types.rich_string import new_rich_text


def new_list_metric_type(none_if_empty: bool):
    return ListMetricType(
        name='',
        title=new_rich_text(''),
        icon24='',
        short_if_no_advanced_info=False,
        singular_prefix_text='one',
        plural_prefix_text='many',
        trim_empty_elements=True,
        none_if_empty=none_if_empty,
        empty_list_text='empty',
    )


def test_singular():
    metric_type = new_list_metric_type(none_if_empty=True)
    actual = metric_type.generate_metric(['aa', None, ''])
    expected = Metric(value=['aa'], text=new_rich_text('one aa'))
    assert actual == expected


def test_plural():
    metric_type = new_list_metric_type(none_if_empty=True)
    actual = metric_type.generate_metric(['aa', None, '', 'bb'])
    expected = Metric(value=['aa', 'bb'], text=new_rich_text('many aa, bb'))
    assert actual == expected


def test_empty_none():
    metric_type = new_list_metric_type(none_if_empty=True)
    assert metric_type.generate_metric([None, '']) is None


def test_empty_not_none():
    metric_type = new_list_metric_type(none_if_empty=False)
    actual = metric_type.generate_metric([None, ''])
    expected = Metric(value=[], text=new_rich_text('empty'))
    assert actual == expected
    assert actual.text == new_rich_text('empty')
