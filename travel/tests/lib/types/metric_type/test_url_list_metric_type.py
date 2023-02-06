from travel.avia.country_restrictions.lib.types import Metric
from travel.avia.country_restrictions.lib.types.metric_type.url_list_metric_type import UrlListMetricType
from travel.avia.country_restrictions.lib.types.rich_string import new_rich_text, RichString, TextBlock, UrlBlock


def new_url_list_metric_type():
    return UrlListMetricType(
        name='',
        title=new_rich_text(''),
        icon24='',
        short_if_no_advanced_info=False,
        singular_prefix_text='one',
        plural_prefix_text='many',
        trim_empty_elements=True,
        none_if_empty=True,
        empty_list_text='empty',
        urls_to_numbers=True,
        singular_link_text='one link',
    )


def test_singular():
    metric_type = new_url_list_metric_type()
    actual = metric_type.generate_metric(['aa'])
    expected = Metric(value=['aa'], text=RichString(data=[TextBlock.create('one '), UrlBlock.create(text='one link', url='aa')]))
    assert actual == expected


def test_plural():
    metric_type = new_url_list_metric_type()
    actual = metric_type.generate_metric(['aa', 'bb'])
    expected = Metric(value=['aa', 'bb'], text=RichString(data=[
        TextBlock.create('many '),
        UrlBlock.create(text='1', url='aa'),
        TextBlock.create(', '),
        UrlBlock.create(text='2', url='bb'),
    ]))
    assert actual == expected
