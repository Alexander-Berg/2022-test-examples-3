# -*- coding: utf-8 -*-

from crypta.lib.python import templater
from crypta.profile.runners.segments.lib.coded_segments import marketplaces_ltv_users


def test_checkout_visits_query():
    return templater.render_template(
        marketplaces_ltv_users.CHECKOUT_VISITS_QUERY,
        vars={
            'input_table': '//input',
            'output_table': '//output',
        },
        strict=True,
    )


def test_pick_up_points_visits_query():
    return templater.render_template(
        marketplaces_ltv_users.PICK_UP_POINTS_VISITS,
        vars={
            'organization_categories': '//organization_categories',
            'deep_visits': '//deep_visits',
            'output_table': '//output_table',
        },
        strict=True,
    )


def test_build_segment_query():
    return templater.render_template(
        marketplaces_ltv_users.BUILD_SEGMENT_QUERY,
        vars={
            'output_table': '//output',
            'idfa_crypta_id': '//idfa/crypta_id',
            'gaid_crypta_id': '//gaid/crypta_id',
            'yandexuid_crypta_id': '//yandexuid/crypta_id',
            'bar': '//bar',
            'metrics': '//metrics',
            'visits': '//visits',
            "market": "//market",
            "start_timestamp": "1000000"
        },
        strict=True,
    )
