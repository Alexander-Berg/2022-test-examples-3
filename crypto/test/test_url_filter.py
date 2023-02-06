from crypta.profile.utils.segment_utils.url_filter import UrlFilter


def test_url_filter():
    url_filter = UrlFilter()
    url_filter.add_url(
        rule_revision_id=0,
        host='eda.yandex.ru',
        regexp=r'eda\.yandex\.ru/action.*',
    )

    url_filter.add_url(
        rule_revision_id=0,
        host="delivery.club",
        regexp=r'delivery\.club/order.*',
    )

    url_filter.add_url(
        rule_revision_id=1,
        host=u'eda.yandex.ru',
        regexp=r'eda\.yandex\.ru/action.*',
    )

    url_filter.add_url(
        rule_revision_id=2,
        host=u'example.com',
        regexp=r'example\.com/.*',
    )

    url_filter.add_url(
        rule_revision_id=2,
        host="some_site.com",
        regexp=u'some_site.com/.+',
    )

    url_filter.add_url(
        rule_revision_id=3,
        host=u'mail.google.com',
        regexp=r'mail\.google\.com/mail',
    )

    return url_filter.get_yql_queries(
        input_table='//input/table',
        output_table='//output/table',
        source='yandex_referrer',
        url_field='url',
        rule_revision_ids=(0, 1, 2),
    )
