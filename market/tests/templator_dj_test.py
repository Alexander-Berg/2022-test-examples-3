from templator_test import Config, Data, DJ, run_test


def test_dj_requests():
    '''
    Run 'test dj requests'
    '''
    data = Data('dj')

    user_id = DJ.make_userid(puid="1234567", yandexuid="y1234567890123")
    dj_rearr_factors = DJ.make_rearr_factors([('factor1', 'value1'), ('factor2', 'value2')])
    data.saas.add_base_key(
        [
            'format=json#key=value',
            'format=json#key=value#nid=100',
        ],
        [
            ('format=json#key=value', '2'),
            ('format=json#key=value#nid=100#rearr-factors=factor1=value1', '1'),
        ],
    )
    data.saas.add_final_ans(
        {
            '1': '{"simple_key": "simple_value", "context": "@@#dj.recommend(experiment=get_context).context#@@"}',
        }
    )

    data.dj.add_json(
        "get_context",
        user_id,
        '{"context": "Das ist Kontext"}',
        add_params={
            'rearr-factors': dj_rearr_factors,
            'nid': '100',
            'hyperid': '123',
        },
    )

    data.dj.add_json(
        "get_context",
        user_id,
        '{"context": "Das ist Kontext für Seite 1"}',
        add_params={
            'rearr-factors': dj_rearr_factors,
            'nid': '100',
            'hyperid': '123',
            'page': '1',
            'page-view-unique-id': '666',
            'numdoc': '1',
        },
    )

    data.dj.add_json(
        "get_context",
        user_id,
        '{"context": "Das ist Kontext für Seite 2"}',
        add_params={
            'rearr-factors': dj_rearr_factors,
            'nid': '100',
            'hyperid': '123',
            'page': '2',
            'page-view-unique-id': '777',
            'numdoc': '1',
        },
    )

    req_query_tail = dj_rearr_factors + DJ.userid_to_cgi(user_id) + DJ.get_cgi_ignore()

    reqs = [
        'templator/getcontextpage?key=value&nid=100&product_id=123&format=json&rearr-factors=' + req_query_tail,
        'templator/getcontextpage?key=value&nid=100&numdoc=1&page=1&page-view-unique-id=666&product_id=123&format=json&rearr-factors='
        + req_query_tail,
        'templator/getcontextpage?key=value&nid=100&numdoc=1&page=2&page-view-unique-id=777&product_id=123&format=json&rearr-factors='
        + req_query_tail,
    ]

    return run_test(data, reqs)


def __init_test_dynamic_morda_phase1():
    data = Data('dynamic_morda')

    user_id = DJ.make_userid(puid="1234567", yandexuid="y1234567890123")

    data.saas.add_base_key(
        ['format=json#key=dj_templates'],
        [
            ('format=json#key=dj_templates', '1'),
            ('format=json#key=dj_templates#rearr-factors=factor=value', '3'),
        ],
    )
    data.saas.add_final_ans(
        {
            '3': """
        {
            "product_scrollbox": {
                "widget_type": "ScrollBox",
                "content": {
                    "items": "product_cards",
                    "garcon": {
                      "dj_place": "@@#dj.recommend(experiment=get_context).blocks.[@@#counters.block_inner_i.inc()#@@].dj_place:#@@",
                      "range": "@@#counters.thematics_range.inc()#@@"
                    }
                }
            },
            "category_gridbox": {
                "widget_type": "GridBox",
                "content": {
                    "items": "category_cards",
                    "garcon": {
                      "dj_place": "@@#dj.recommend(experiment=get_context).blocks.[@@#counters.block_inner_i.inc()#@@].dj_place:#@@",
                      "range": "@@#counters.thematics_range.inc()#@@"
                    }
                }
            }
        }
    """
        }
    )

    rearr_factors = DJ.make_rearr_factors([('factor', 'value')])
    data.dj.add_json(
        "get_context",
        user_id,
        """
        {
            "context": "Das ist Kontext",
            "blocks": [
                {
                    "widget_type": "null",
                    "dj_place": "null"
                },
                {
                    "widget_type": "product_scrollbox",
                    "dj_place": "market_thematics_from_context"
                },
                {
                    "widget_type": "category_gridbox",
                    "dj_place": "market_thematics_from_context"
                },
                {
                    "widget_type": "product_scrollbox",
                    "dj_place": "market_thematics_from_context_special"
                },
                {
                    "widget_type": "product_scrollbox",
                    "dj_place": "market_thematics_from_context"
                },
                {
                    "widget_type": "category_gridbox",
                    "dj_place": "market_thematics_from_context"
                },
                {
                    "widget_type": "product_scrollbox",
                    "dj_place": "market_thematics_from_context_special"
                },
                {
                    "widget_type": "product_scrollbox",
                    "dj_place": "market_thematics_from_context_special2"
                }
            ]
        }
    """,
        add_params={'rearr-factors': rearr_factors},
    )

    reqs = ['templator/getcontextpage?raw_response=1&key=dj_templates&format=json&rearr-factors=' + rearr_factors]

    return data, reqs, user_id, rearr_factors


def __init_test_dynamic_morda_phase2(data, user_id, rearr_factors):
    data.path = 'dynamic_morda2'
    data.cfg = Config(data.path, None)

    data.saas.add_base_key(
        ['format=json#key=page'],
        [
            ('format=json#key=page', '2'),
        ],
    )
    data.saas.add_final_ans(
        {
            '2': """
        {
            "context": "@@#dj.recommend(experiment=get_context).context#@@",
            "content": [
                {
                    "widget_type": "Header Block"
                },
                @@##templator.getcontextpage(raw_response=1@@key=dj_templates@@format=json).[0].@@#dj.recommend(experiment=get_context).blocks.[@@#counters.block_i.inc()#@@].widget_type:null#@@:null##@@,
                @@##templator.getcontextpage(raw_response=1@@key=dj_templates@@format=json).[0].@@#dj.recommend(experiment=get_context).blocks.[@@#counters.block_i.inc()#@@].widget_type:null#@@:null##@@,
                {
                    "widget_type": "Fixed Block"
                },
                @@##templator.getcontextpage(raw_response=1@@key=dj_templates@@format=json).[0].@@#dj.recommend(experiment=get_context).blocks.[@@#counters.block_i.inc()#@@].widget_type:null#@@:null##@@,
                {
                    "widget_type": "Yet Another Fixed Block"
                },
                @@##templator.getcontextpage(raw_response=1@@key=dj_templates@@format=json).[0].@@#dj.recommend(experiment=get_context).blocks.[@@#counters.block_i.inc()#@@].widget_type:null#@@:null##@@,
                @@##templator.getcontextpage(raw_response=1@@key=dj_templates@@format=json).[0].@@#dj.recommend(experiment=get_context).blocks.[@@#counters.block_i.inc()#@@].widget_type:null#@@:null##@@,
                @@##templator.getcontextpage(raw_response=1@@key=dj_templates@@format=json).[0].@@#dj.recommend(experiment=get_context).blocks.[@@#counters.block_i.inc()#@@].widget_type:null#@@:null##@@,
                @@##templator.getcontextpage(raw_response=1@@key=dj_templates@@format=json).[0].@@#dj.recommend(experiment=get_context).blocks.[@@#counters.block_i.inc()#@@].widget_type:null#@@:null##@@,
                @@##templator.getcontextpage(raw_response=1@@key=dj_templates@@format=json).[0].@@#dj.recommend(experiment=get_context).blocks.[@@#counters.block_i.inc()#@@].widget_type:null#@@:null##@@,
                @@##templator.getcontextpage(raw_response=1@@key=dj_templates@@format=json).[0].@@#dj.recommend(experiment=get_context).blocks.[@@#counters.block_i.inc()#@@].widget_type:null#@@:null##@@,
                @@##templator.getcontextpage(raw_response=1@@key=dj_templates@@format=json).[0].@@#dj.recommend(experiment=get_context).blocks.[@@#counters.block_i.inc()#@@].widget_type:null#@@:null##@@
            ]
        }
    """
        }
    )

    reqs = [
        'templator/getcontextpage?key=page&format=json&rearr-factors='
        + rearr_factors
        + DJ.userid_to_cgi(user_id)
        + DJ.get_cgi_ignore()
    ]

    return data, reqs


def test_dynamic_morda_internal():
    '''
    Emulates dynamic morda 2.0 pipeline:
    page block templates are stored separately from the actual page
    and are ordered and filled according to dj response
    '''
    data, _, user_id, rearr_factors = __init_test_dynamic_morda_phase1()

    data, reqs = __init_test_dynamic_morda_phase2(data, user_id, rearr_factors)

    return run_test(data, reqs)
