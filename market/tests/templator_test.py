# -*- coding: utf-8 -*-

from __future__ import print_function
import yatest.common
import os
import re
import zlib
import base64
import json

from req_base import Stub
from req_buker import Buker, Templator
from req_cataloger import Cataloger
from req_saas import Saas
from req_dj import DJ


class Config(object):
    def __init__(self, path, testing):
        self.buker_file = yatest.common.test_output_path(os.path.join(path, "buker.json"))
        self.cataloger_file = yatest.common.test_output_path(os.path.join(path, "cataloger.json"))
        self.cataloger_preview_file = yatest.common.test_output_path(os.path.join(path, "cataloger_preview.json"))
        self.templator_file = yatest.common.test_output_path(os.path.join(path, "templator.json"))
        self.report_file = yatest.common.test_output_path(os.path.join(path, "report.json"))
        self.geobase_file = yatest.common.test_output_path(os.path.join(path, "geobase.json"))
        self.saas_file = yatest.common.test_output_path(os.path.join(path, "saas.json"))
        self.saas_preview_file = yatest.common.test_output_path(os.path.join(path, "saas_preview.json"))
        self.dj_file = yatest.common.test_output_path(os.path.join(path, "dj.json"))

        self.log_file = yatest.common.test_output_path(os.path.join(path, "log.txt"))
        self.tskv_log_file = yatest.common.test_output_path(os.path.join(path, "tskv_log.txt"))

        directory = os.path.dirname(self.log_file)
        if not os.path.exists(directory):
            os.makedirs(directory)

        self.web_user = "test_user"
        self.testing = testing

    def save(self, filename, config_params):
        with open(filename, "w") as f:
            print("LOG %s" % self.log_file, file=f)
            print("TSKV_LOG %s" % self.tskv_log_file, file=f)
            print("WEB_USER %s" % self.web_user, file=f)
            print("BUKER_OFFLINE_FILE       %s" % self.buker_file, file=f)
            print("PREVIEW_BUKER_OFFLINE_FILE       %s" % self.buker_file, file=f)
            print("CATALOGER_OFFLINE_FILE   %s" % self.cataloger_file, file=f)
            print("PREVIEW_CATALOGER_OFFLINE_FILE   %s" % self.cataloger_preview_file, file=f)
            print("GEOBASE_OFFLINE_FILE     %s" % self.geobase_file, file=f)
            print("REPORT_OFFLINE_FILE      %s" % self.report_file, file=f)
            print("TARANTINO_OFFLINE_FILE   %s" % self.templator_file, file=f)
            print("SAAS_OFFLINE_FILE        %s" % self.saas_file, file=f)
            print("PREVIEW_SAAS_OFFLINE_FILE        %s" % self.saas_preview_file, file=f)
            print("DJ_OFFLINE_FILE          %s" % self.dj_file, file=f)
            if self.testing is not None:
                print("ENVIRONMENT              %s" % ("testing" if self.testing else "production"), file=f)
            for param in config_params:
                print(param, file=f)


class Data(object):
    def __init__(self, path, testing=None):
        self.path = path

        self.cfg = Config(self.path, testing)
        self.buker = Buker()
        self.cataloger = Cataloger()
        self.cataloger_preview = Cataloger()
        self.geobase = Stub()
        self.report = Stub()
        self.templator = Templator()
        self.saas = Saas()
        self.saas_preview = Saas()
        self.dj = DJ()

    def save(self, config_file_path, config_params):
        self.cfg.save(config_file_path, config_params)
        self.buker.save(self.cfg.buker_file)
        self.cataloger.save(self.cfg.cataloger_file)
        self.cataloger_preview.save(self.cfg.cataloger_preview_file)
        self.geobase.save(self.cfg.geobase_file)
        self.report.save(self.cfg.report_file)
        self.templator.save(self.cfg.templator_file)
        self.saas.save(self.cfg.saas_file)
        self.saas_preview.save(self.cfg.saas_preview_file)
        self.dj.save(self.cfg.dj_file)


def get_errors(data):
    result = {}
    error_match = re.compile(r".*Error code:\s+\[(\d+)\]")
    with open(data.cfg.log_file, "r") as log_file:
        for line in log_file:
            error_code = error_match.match(line)
            if error_code:
                code = error_code.group(1)
                result[code] = result.get('code', []) + [line]
    return result


def run_test_internal(data, reqs, config_params=[]):
    config_file_path = yatest.common.test_output_path(os.path.join(data.path, 'test.cfg'))
    reqs_file_path = yatest.common.test_output_path(os.path.join(data.path, 'reqs.txt'))
    out_path = yatest.common.test_output_path(os.path.join(data.path, 'templator_print.out'))

    data.save(config_file_path, config_params)

    with open(reqs_file_path, "w") as f:
        for r in reqs:
            print(r, file=f)

    command = [
        yatest.common.binary_path("market/tarantino/templator-cmdl/templator-cmdl"),
        "-c", config_file_path, '--debug'
    ]

    with open(out_path, "w") as out:
        with open(reqs_file_path) as input:
            yatest.common.execute(command, stdout=out, stdin=input)
    return out_path


def run_test(data, reqs, config_params=[], expected_errors={}):
    out_path = run_test_internal(data, reqs, config_params)
    errors = get_errors(data)
    if expected_errors:
        for error_code, count in expected_errors.iteritems():
            if count:
                real_errors = errors.pop(error_code)
                assert len(real_errors) == count, "Unexpected errors: {}".format(real_errors)
            else:
                assert error_code not in errors

    # Все ошибки должны быть учтены
    assert not errors

    return yatest.common.canonical_file(out_path, diff_tool_timeout=60, local=True)


cnt = 1


def add_to_saas(data, params, answer, is_first_record=False):
    global cnt
    if is_first_record:
        cnt = 1

    key = '#'.join([
        param[0] + '=' + param[1] for param in sorted(params, key=(lambda param: param[0]))
    ])

    data.saas.add_base_key(
        [key],
        [
            (key, str(cnt)),
        ]
    )
    data.saas.add_final_ans({
        str(cnt): answer,
    })

    cnt += 1


def test_doomy_templates():
    '''
        test doomy templates with no substitution
    '''
    data = Data('doomy')
    data.saas.add_base_key(
        ['format=json#key=value'],
        [
            ('format=json#key=value', '1'),
        ]
    )
    data.saas.add_final_ans({
        '1': '{"simple_key": "simple_value"}',
    })
    data.saas.add_base_key(
        ['format=xml#key=value'],
        [
            ('format=xml#key=value',  '2'),
        ]
    )
    data.saas.add_final_ans({
        '2': '<page id="111">some simple content</page>',
    })

    reqs = [
        'templator/getcontextpage?key=value&format=json',
        'templator/getcontextpage?key=value&format=xml',
        'templator/getcontextpage?key=bad_value',  # Упадет с 1003
    ]
    return run_test(data, reqs, expected_errors={'1003': 1})


def braces_test_base_internal(name, braces):
    data = Data(name)

    # Response on main request
    add_to_saas(data, [('key', 'template'), ('format', 'json')], '{"simple_key": "%(open)starantino.getcontextpage(key=value@@format=json).[0]%(close)s"}' % braces, is_first_record=True)
    # Response on subrequest
    add_to_saas(data, [('key', 'value'), ('format', 'json')], '{"simple_key": "simple_value",\t"simple_key2": "simple_value"}')

    # Response on main request
    add_to_saas(data, [('key', 'template'), ('format', 'xml')], '<page id="111">%(open)starantino.getcontextpage(key=value@@format=xml)%(close)s</page>' % braces)
    # Response on subrequest
    add_to_saas(data, [('key', 'value'), ('format', 'xml')], 'simple xml content')

    # exception && no default value
    add_to_saas(data, [('key', 'template_ex'), ('format', 'json')], '{"simple_key": "%(open)starantino.getcontextpage(key=value)%(close)s"}' % braces)
    # exception && empty default value
    add_to_saas(data, [('key', 'template_ex_edv'), ('format', 'json')], '{"simple_key": "%(open)starantino.getcontextpage(key=value).[0]:%(close)s"}' % braces)
    # exception && empty default value
    add_to_saas(data, [('key', 'template_ex_dv'), ('format', 'json')], '{"simple_key": "%(open)starantino.getcontextpage(key=value).[0]:qwe%(close)s"}' % braces)

    # empty json, no default value
    add_to_saas(data, [('key', 'template_ej'), ('format', 'json')], '{"simple_key": "%(open)starantino.getcontextpage(key=empty).[0]%(close)s"}' % braces)
    # empty json && empty default value
    add_to_saas(data, [('key', 'template_ej_edv'), ('format', 'json')], '{"simple_key": "%(open)starantino.getcontextpage(key=empty).[0]:%(close)s"}' % braces)
    # empty json && empty default value
    add_to_saas(data, [('key', 'template_ej_dv'), ('format', 'json')], '{"simple_key": "%(open)starantino.getcontextpage(key=empty).[0]:qwe%(close)s"}' % braces)

    reqs = [
        'templator/getcontextpage?key=template&format=json&debug_tarantino=1',
        'templator/getcontextpage?key=template&format=xml&debug_tarantino=1',

        'templator/getcontextpage?key=template_ex&format=json&debug_tarantino=1',
        'templator/getcontextpage?key=template_ex_edv&format=json&debug_tarantino=1',
        'templator/getcontextpage?key=template_ex_dv&format=json&debug_tarantino=1',
        'templator/getcontextpage?key=template_ej&format=json&debug_tarantino=1',
        'templator/getcontextpage?key=template_ej_edv&format=json&debug_tarantino=1',
        'templator/getcontextpage?key=template_ej_dv&format=json&debug_tarantino=1',
    ]
    return run_test(data, reqs, expected_errors={'1003': 1})


def test_simple_braces_templates_internal():
    '''
        test simple braces substitution (internal call workflow)
    '''
    return braces_test_base_internal('braces_simple', {'open': '@@#', 'close': '#@@'})


def test_extended_braces_templates_internal():
    '''
        test extended braces substitution (internal call workflow)
    '''
    return braces_test_base_internal('braces_extended', {'open': '@@##', 'close': '##@@'})


def test_multireq():
    '''
        test same requests with different paths
    '''
    data = Data('multireq')

    add_to_saas(
        data,
        [('key', 'template'), ('format', 'json'), ('brand_id', '153043')],
        '{"simple_key": "@@#cataloger.getbrandinfo.seoTitle#@@ sometext @@#cataloger.getbrandinfo.seoTitle#@@ some more text @@#cataloger.getbrandinfo.name#@@'
        '@@#tarantino.getcontextpage(key=empty):#@@@@#templator.getcontextpage(key=empty):#@@"}',
        is_first_record=True
    )

    add_to_saas(
        data,
        [('key', 'empty')],
        '{}'
    )

    data.cataloger.add_json(
        'getbrandinfo',
        [('format', 'json'), ('id', '153043')],
        '{"seoTitle": "seo_title", "name": "plane_name"}'
    )

    reqs = [
        'templator/getcontextpage?key=template&format=json&brand_id=153043&debug_tarantino=yes',
    ]

    return run_test(data, reqs)


def test_cgi_params_req():
    data = Data('cgi_params')

    params = [('param{}'.format(i), 'value{}'.format(i)) for i in range(0, 5)]
    add_to_saas(
        data,
        [('key', 'template_cgi_req'), ('format', 'json')] + params,
        '{"res0": "@@#request.param0#@@", \
          "res1": "@@#request.param1#@@", \
          "res2": "@#request.param2#@", \
          "res3": "string with @@#request.param3#@@", \
          "res4": "@@#request.param3#@@@@#request.param4#@@", \
          "res5": "@@#request.param100:#@@", \
          "res6": "@@#request.param101:default#@@", \
          "res7": "@@#request.param101:abba#@@", \
        }',
        is_first_record=True
    )

    params_str = '&'.join('{}={}'.format(param, value) for param, value in params)
    reqs = [
        'templator/getcontextpage?key=template_cgi_req&format=json&debug_tarantino=yes&{}'.format(params_str),
    ]

    return run_test(data, reqs)


def test_cgi_params_bad_req():
    data = Data('cgi_params_bad')

    params = [('param{}'.format(i), 'value{}'.format(i)) for i in range(0, 5)]
    add_to_saas(
        data,
        [('key', 'template_cgi_req_bad'), ('format', 'json')] + params,
        '{"res5": "@@#request.param100#@@"}',
        is_first_record=True
    )

    params_str = '&'.join('{}={}'.format(param, value) for param, value in params)
    reqs = [
        'templator/getcontextpage?key=template_cgi_req_bad&format=json&debug_tarantino=yes&{}'.format(params_str),
    ]

    return run_test(data, reqs, expected_errors={'1003': 1})


def test_cgi_param_in_getcontextpage():
    def names_to_json_str(names):
        json_str = '{'
        for domain, regional_name in names.items():
            json_str += '"%s": "%s",' % (domain, regional_name)
        return json_str + '}'

    data = Data('cgi_param_in_getcontextpage')

    brands_names = {100: {"ru": "Рога и копыта", "com": "Roga i kopyta"},
                    200: {"ru": "Danone", "com": "Dannon"}
                    }

    template_all_names = """
    {
        "id": @@#request.brand_id#@@,
        "names": @@##tarantino.getcontextpage(id=@@#request.brand_id#@@@@type=brand_name@@format=json).[0]##@@
    }
    """

    template_regional_name = """
    {
        "id": @@#request.brand_id#@@,
        "domain": @@#request.domain#@@,
        "name": "@@#tarantino.getcontextpage(id=@@#request.brand_id#@@@@type=brand_name@@domain=@@#request.domain#@@@@format=json).[0]#@@"
    }
    """

    template_regional_name_ru = """
    {
        "id": @@#request.brand_id#@@,
        "dmn": @@#request.dmn#@@,
        "names": @@##tarantino.getcontextpage(id=@@#request.brand_id#@@@@type=brand_name@@format=json).[0]##@@,
        "name": "@@#tarantino.getcontextpage(id=@@#request.brand_id#@@@@type=brand_name@@format=json).[0].ru#@@"
    }
    """

    template_regional_name_com = """
    {
        "id": @@#request.brand_id#@@,
        "dmn": @@#request.dmn#@@,
        "names": @@##tarantino.getcontextpage(id=@@#request.brand_id#@@@@type=brand_name@@format=json).[0]##@@,
        "name": "@@#tarantino.getcontextpage(id=@@#request.brand_id#@@@@type=brand_name@@format=json).[0].com#@@"
    }
    """

    reqs = list()
    for brand_id, regional_names in brands_names.items():
        add_to_saas(data, [('brand_id', str(brand_id)), ('format', 'json')], template_all_names)
        add_to_saas(data, [('brand_id', str(brand_id)), ('dmn', 'ru'), ('format', 'json')], template_regional_name_ru)
        add_to_saas(data, [('brand_id', str(brand_id)), ('dmn', 'com'), ('format', 'json')], template_regional_name_com)
        add_to_saas(data, [('type', 'brand_name'), ('id', str(brand_id)), ('format', 'json')], names_to_json_str(regional_names))
        reqs.append('templator/getcontextpage?debug_tarantino=1&format=json&brand_id={}'.format(brand_id))
        reqs.append('templator/getcontextpage?debug_tarantino=1&format=json&brand_id={}&dmn=ru'.format(brand_id))
        reqs.append('templator/getcontextpage?debug_tarantino=1&format=json&brand_id={}&dmn=com'.format(brand_id))
        for domain, regional_name in regional_names.items():
            add_to_saas(data, [('brand_id', str(brand_id)), ('domain', domain), ('format', 'json')], template_regional_name)
            add_to_saas(data, [('type', 'brand_name'), ('id', str(brand_id)), ('domain', domain), ('format', 'json')], '"{}"'.format(regional_name))
            reqs.append('templator/getcontextpage?debug_tarantino=1&format=json&brand_id={}&domain={}'.format(brand_id, domain))

    return run_test(data, reqs)


def test_reask_on_error():
    data = Data('reask_on_error')

    add_to_saas(
        data,
        [('key', 'template'), ('format', 'json'), ('brand_id', '153043')],
        '{"simple_key": "@@#cataloger.getbrandinfo.seoTitle:#@@ sometext '
        '@@#tarantino.getcontextpage(key=@@#cataloger.getbrandinfo.seoTitle:#@@):#@@'
        '@@#tarantino.getcontextpage(key=@@#cataloger.getbrandinfo.name:#@@):#@@"}',
        is_first_record=True
    )

    add_to_saas(
        data,
        [('key', '')],
        '{}'
    )

    reqs = [
        'templator/getcontextpage?key=template&format=json&brand_id=153043&debug_tarantino=yes',
    ]

    return run_test(data, reqs)


def test_array_in_json_path():
    data = Data('array_in_json_path')
    struct_with_array = """
    {
        "int_array": [1, 2, 3],
        "struct_array": [
            {
                "type": "str",
                "value": "aaa"
            },
            {
                "type": "int",
                "value": "100500"
            },
            {
                "type": "inside_struct_array",
                "value": [{"a": 100, "b": 500}, {"a": 200, "b": 300}]
            }
        ]
    }
    """

    template = """
    {
        "full_int_array": @@##tarantino.getcontextpage(key=value@@format=json).[0].int_array##@@,
        "int_elem_0": @@##tarantino.getcontextpage(key=value@@format=json).[0].int_array.[0]##@@,
        "int_elem_1": @@##tarantino.getcontextpage(key=value@@format=json).[0].int_array.[1]##@@,
        "full_struct_array": @@##tarantino.getcontextpage(key=value@@format=json).[0].struct_array##@@,
        "struct_elem_1": @@##tarantino.getcontextpage(key=value@@format=json).[0].struct_array.[1]##@@,
        "struct_elem_1_type": @@##tarantino.getcontextpage(key=value@@format=json).[0].struct_array.[1].type##@@,
        "inside_struct": @@##tarantino.getcontextpage(key=value@@format=json).[0].struct_array.[2]##@@,
        "inside_struct_value": @@##tarantino.getcontextpage(key=value@@format=json).[0].struct_array.[2].value##@@,
        "inside_struct_value_1": @@##tarantino.getcontextpage(key=value@@format=json).[0].struct_array.[2].value.[1]##@@,
        "inside_struct_value_1_a": @@##tarantino.getcontextpage(key=value@@format=json).[0].struct_array.[2].value.[1].a##@@
    }
    """
    add_to_saas(data, [('format', 'json')], template, is_first_record=True)
    add_to_saas(data, [('key', 'value'), ('format', 'json')], struct_with_array)

    reqs = [
        'templator/getcontextpage?format=json',
    ]

    return run_test(data, reqs)


def test_array_after_getcontextpage():
    data = Data('array_after_getcontextpage')
    widget1 = """
    {
        "id": 100,
        "value": [1, 2, 3]
    }
    """
    widget2 = """
    {
        "id": 200,
        "value": {"aaa": "bbb"}
    }
    """
    widget3 = """
    {
        "id": 300,
        "value": {"aaa": [5, 6, 7]}
    }
    """
    buker_result = [widget1, widget2, widget3]

    template = """
    {
        "buker_result": @@##tarantino.getcontextpage(key=value@@format=json)##@@,
        "id_without_index": @@##tarantino.getcontextpage(key=value@@format=json).id:null##@@,
        "id0_by_index": @@##tarantino.getcontextpage(key=value@@format=json).[0].id:error0##@@,
        "id1_by_index": @@##tarantino.getcontextpage(key=value@@format=json).[1].id:error1##@@,
        "id2_by_index": @@##tarantino.getcontextpage(key=value@@format=json).[2].id:error2##@@,
        "full_widget0": @@##tarantino.getcontextpage(key=value@@format=json).[0]##@@,
        "widget1_value": @@##tarantino.getcontextpage(key=value@@format=json).[1].value##@@,
        "widget2_subvalue": @@##tarantino.getcontextpage(key=value@@format=json).[2].value.aaa.[0]##@@,
    }
    """
    add_to_saas(data, [('format', 'json')], template, is_first_record=True)
    add_to_saas(data, [('key', 'value'), ('format', 'json')], ','.join(buker_result))

    reqs = [
        'templator/getcontextpage?format=json',
    ]

    return run_test(data, reqs)


def test_calcforbuker():
    data = Data('calcforbuker')

    data.cataloger.add_json(
        'getnavigationnode',
        [('format', 'json'), ('id', '153043')],
        '{"type": "buker_type"}'
    )

    data.saas.add_base_key(
        ['brand_id=153043#format=json#key=template#nodetype=buker_type', 'brand_id=153043#format=json#key=template'],
        [
            ('brand_id=153043#format=json#key=template#nodetype=buker_type', '10'),
        ]
    )
    data.saas.add_final_ans({
        '10': '{"simple_key": "page with type"}',
    })

    add_to_saas(
        data,
        [('key', 'template'), ('format', 'json'), ('brand_id', '153043')],
        '{"simple_key": "page with type"}',
        is_first_record=True
    )

    add_to_saas(
        data,
        [('key', 'template'), ('format', 'json'), ('brand_id', '153043')],
        '{"simple_key": "page without type"}'
    )

    reqs = [
        'templator/getcontextpage?key=template&format=json&brand_id=153043',
        'templator/getcontextpage?key=template&format=json&brand_id=153043&calcforbuker=nodetype',
        'templator/getcontextpage?key=template&format=json&brand_id=153043&calcforbuker=wrongkey',
    ]

    return run_test(data, reqs, expected_errors={'1003': 1})


def test_calcforbuker_ancestors_hid():
    data = Data('calcforbuker_hid_ancestors')

    data.cataloger.add_json(
        'gethidpathlite',
        [('format', 'json'), ('hid', '3')],
        '{"pathToRoot": [3,\n\t2, 1]}'
    )

    data.cataloger.add_json(
        'gethidpathlite',
        [('format', 'json'), ('hid', '13')],
        '{"pathToRoot": [13,\n\t\r12, 11]}'
    )

    data.cataloger.add_json(
        'gethidpathlite',
        [('format', 'json'), ('hid', '23')],
        '{"pathToRoot": [23,22, \r\n21]}'
    )

    data.saas.add_base_key(
        [
            "format=json#hid=3#key=template",
            "format=json#hid=2#key=template",
            "format=json#hid=1#key=template",
        ],
        [
            ("format=json#hid=3#key=template", '1000003'),
            ("format=json#hid=2#key=template", '1000002'),
            ("format=json#hid=1#key=template", '1000001'),
        ]
    )
    data.saas.add_final_ans({
        '1000003': '{"simple_key": "Hid with ancestors (1000003)"}',
    })
    data.saas.add_final_ans({
        '1000002': '{"wrong_key": "WRONG! (1000002)"}',
    })
    data.saas.add_final_ans({
        '1000001': '{"wrong_key": "WRONG! (1000001)"}',
    })

    data.saas.add_base_key(
        [
            "format=json#hid=13#key=template",
            "format=json#hid=12#key=template",
            "format=json#hid=11#key=template",
        ],
        [
            ("format=json#hid=12#key=template", '10000012'),
            ("format=json#hid=11#key=template", '10000011'),
        ]
    )
    data.saas.add_final_ans({
        '10000012': '{"simple_key": "Hid with ancestors (10000012)"}',
    })
    data.saas.add_final_ans({
        '10000011': '{"wrong_key": "WRONG! (10000011)"}',
    })

    data.saas.add_base_key(
        [
            "format=json#hid=23#key=template",
            "format=json#hid=22#key=template",
            "format=json#hid=21#key=template",
        ],
        [
            ("format=json#hid=21#key=template", '10000021'),
        ]
    )
    data.saas.add_final_ans({
        '10000021': '{"simple_key": "Hid with ancestors (10000021)"}',
    })

    add_to_saas(
        data,
        [('key', 'template'), ('format', 'json'), ('hid', '4')],
        '{"simple_key": "Unknown hid with ancestors"}'
    )

    add_to_saas(
        data,
        [('key', 'template'), ('format', 'json'), ('hid', '3')],
        '{"simple_key": "Simple hid"}'
    )

    reqs = [
        'templator/getcontextpage?key=template&hid=3&format=json&calcforbuker=hid_ancestors',
        'templator/getcontextpage?key=template&hid=13&format=json&calcforbuker=hid_ancestors',
        'templator/getcontextpage?key=template&hid=23&format=json&calcforbuker=hid_ancestors',
        'templator/getcontextpage?key=template&hid=4&format=json&calcforbuker=hid_ancestors',
        'templator/getcontextpage?key=template&hid=3&format=json',
    ]

    return run_test(data, reqs)


def test_one_of_cgi_list():
    '''
    Test that when we request both with cgi params list & one_of
    we travers value list first and after that one_of
    e.g. in case of request a=1,2&b=1&one_of=a,&use_first_batch_value_for=a we will prioritize base case as follows:
    a=1#b=1
    a=2#b=1
    b=1
    b=1
    '''

    data = Data('one_of_cgi_list')
    data.saas.add_base_key(
        ['a=1#b=1', 'a=2#b=1', 'b=1'],
        [
            ('a=1#b=1', '1'),
            ('a=2#b=1', '2')
        ]
    )
    data.saas.add_base_key(
        ['a=3#b=1', 'b=1'],
        [
            ('b=1', '3')
        ]
    )
    data.saas.add_final_ans('1')
    data.saas.add_final_ans('2')
    data.saas.add_final_ans('3')

    reqs = [
        'templator/getcontextpagesaas?a=1,2&b=1&one_of=a,&use_first_batch_value_for=a',  # should select doc 1
        'templator/getcontextpagesaas?a=2,1&b=1&one_of=a,&use_first_batch_value_for=a',  # should select doc 2
        'templator/getcontextpagesaas?a=3&b=1&one_of=a,&use_first_batch_value_for=a',  # should select doc 3
    ]

    return run_test(data, reqs)


def test_calcforbuker_ancestors_region():
    '''
    Проверяем укрупнение региона.
    Если пришел параметр region, темплатор:
        * делает дозапрос в каталогер с регионом,
        * получает путь до корня дерева
        * забирает страницы для всех регионов
        * выбирает ближайший к региону пользователя по пути, который вернул каталогер
    '''
    data = Data('region_ancestors')

    # Text=5. Есть специализированная страница в регион 5.
    data.saas.add_base_key(
        ['format=json#region=5#text=5#type=mp_promo', 'format=json#text=5#type=mp_promo'],
        [('format=json#region=5#text=5#type=mp_promo', '51')]
    )

    # Text=4. Здесь не сделал специаизированных страниц для региона 4
    data.saas.add_base_key(
        ['format=json#region=4#text=4#type=mp_promo', 'format=json#text=4#type=mp_promo'],
        [('format=json#text=4#type=mp_promo', '41')]
    )

    # Text=3
    data.saas.add_base_key(
        [
            'format=json#region=3#text=3#type=mp_promo',
            'format=json#region=2#text=3#type=mp_promo',
            'format=json#region=1#text=3#type=mp_promo',
            'format=json#text=3#type=mp_promo'
        ], [
            ('format=json#region=3#text=3#type=mp_promo', '33'),
            ('format=json#region=2#text=3#type=mp_promo', '0'),
            ('format=json#region=1#text=3#type=mp_promo', '0'),
            ('format=json#text=3#type=mp_promo', '0')
        ]
    )
    data.saas.add_base_key(
        ['format=json#region=2#text=3#type=mp_promo', 'format=json#region=1#text=3#type=mp_promo', 'format=json#text=3#type=mp_promo'],
        [('format=json#region=2#text=3#type=mp_promo', '32'), ('format=json#region=1#text=3#type=mp_promo', '0'), ('format=json#text=3#type=mp_promo', '0')]
    )

    data.saas.add_base_key(
        ['format=json#region=1#text=3#type=mp_promo', 'format=json#text=3#type=mp_promo'],
        [('format=json#region=1#text=3#type=mp_promo', '31'), ('format=json#text=3#type=mp_promo', '0')]
    )

    # Text=2
    data.saas.add_base_key(
        ['format=json#region=3#text=2#type=mp_promo', 'format=json#region=2#text=2#type=mp_promo', 'format=json#region=1#text=2#type=mp_promo', 'format=json#text=2#type=mp_promo'],
        [('format=json#region=2#text=2#type=mp_promo', '22'), ('format=json#region=1#text=2#type=mp_promo', '0'), ('format=json#text=2#type=mp_promo', '0')]
    )

    data.saas.add_base_key(
        ['format=json#region=2#text=2#type=mp_promo', 'format=json#region=1#text=2#type=mp_promo', 'format=json#text=2#type=mp_promo'],
        [('format=json#region=2#text=2#type=mp_promo', '22'), ('format=json#region=1#text=2#type=mp_promo', '0'), ('format=json#text=2#type=mp_promo', '0')]
    )

    data.saas.add_base_key(
        ['format=json#region=1#text=2#type=mp_promo', 'format=json#text=2#type=mp_promo'],
        [('format=json#region=1#text=2#type=mp_promo', '21'), ('format=json#text=2#type=mp_promo', '0')]
    )

    # Text=1
    data.saas.add_base_key(
        ['format=json#region=2#text=1#type=mp_promo', 'format=json#region=1#text=1#type=mp_promo', 'format=json#text=1#type=mp_promo'],
        [('format=json#region=1#text=1#type=mp_promo', '11'), ('format=json#text=1#type=mp_promo', '0')]
    )

    data.saas.add_base_key(
        ['format=json#region=1#text=1#type=mp_promo', 'format=json#text=1#type=mp_promo'],
        [('format=json#region=1#text=1#type=mp_promo', '11'), ('format=json#text=1#type=mp_promo', '0')]
    )

    data.saas.add_final_ans('11', False)

    data.saas.add_final_ans('20', False)
    data.saas.add_final_ans('21', False)
    data.saas.add_final_ans('22', False)

    data.saas.add_final_ans('31', False)
    data.saas.add_final_ans('32', False)
    data.saas.add_final_ans('33', False)

    data.saas.add_final_ans('41', False)

    data.saas.add_final_ans('51', False)

    #
    # Ответ каталогера содержит путь до корня
    #
    data.cataloger.add_json(
        'getregionpathlite',
        [('format', 'json'), ('region', '3')],
        '{"pathToRoot": [3,\n\t2, 1]}'
    )

    data.cataloger.add_json(
        'getregionpathlite',
        [('format', 'json'), ('region', '2')],
        '{"pathToRoot": [2, 1]}'
    )

    # Путь до региона 1 не вернет ответа, т.к. мы его не добавляем, но страница будет отдана для региона 1

    # Одиночный регион
    data.cataloger.add_json(
        'getregionpathlite',
        [('format', 'json'), ('region', '4')],
        '{"pathToRoot": [4]}'
    )

    # Ответ каталогера для неизвестного региона
    data.cataloger.add_json(
        'getregionpathlite',
        [('format', 'json'), ('region', '5')],
        '{"error": "no region id=5"}'
    )

    reqs = [
        # Ключ text=5. Каталогер не знает о регионе 5. А CMS знает. Поэтому вернет страницу 5
        'templator/getcontextpagesaas?type=mp_promo&skip=region&format=json&rearr-factors=market_cms_use_saas_instead_buker=all&debug_tarantino=1&text=5&region=5',

        # Ключ text=4. CMS не знает о регионе 4.
        'templator/getcontextpagesaas?type=mp_promo&skip=region&format=json&rearr-factors=market_cms_use_saas_instead_buker=all&debug_tarantino=1&text=4&region=4',

        # Ключ text=3 определен в трех регионах. Будет выбран самый близкий
        'templator/getcontextpagesaas?type=mp_promo&skip=region&format=json&rearr-factors=market_cms_use_saas_instead_buker=all&debug_tarantino=1&text=3&region=3',
        'templator/getcontextpagesaas?type=mp_promo&skip=region&format=json&rearr-factors=market_cms_use_saas_instead_buker=all&debug_tarantino=1&text=3&region=2',
        'templator/getcontextpagesaas?type=mp_promo&skip=region&format=json&rearr-factors=market_cms_use_saas_instead_buker=all&debug_tarantino=1&text=3&region=1',

        # Ключ text=2 определен в двух регионах. Регион 3 и регион 2 ссылаются на одну страницу
        'templator/getcontextpagesaas?type=mp_promo&skip=region&format=json&rearr-factors=market_cms_use_saas_instead_buker=all&debug_tarantino=1&text=2&region=3',
        'templator/getcontextpagesaas?type=mp_promo&skip=region&format=json&rearr-factors=market_cms_use_saas_instead_buker=all&debug_tarantino=1&text=2&region=2',
        'templator/getcontextpagesaas?type=mp_promo&skip=region&format=json&rearr-factors=market_cms_use_saas_instead_buker=all&debug_tarantino=1&text=2&region=1',

        # Ключ text=1 есть страница с регионом и без
        'templator/getcontextpagesaas?type=mp_promo&skip=region&format=json&rearr-factors=market_cms_use_saas_instead_buker=all&debug_tarantino=1&text=1&region=2',
        'templator/getcontextpagesaas?type=mp_promo&skip=region&format=json&rearr-factors=market_cms_use_saas_instead_buker=all&debug_tarantino=1&text=1&region=1',
    ]

    return run_test(data, reqs)


def __test_add_skip(name, testing):
    data = Data(name, testing)

    data.saas.add_base_key(
        [
            "brand_id=153043#format=json#key=template#nid=2#region=1#rgb=white",
            "brand_id=153043#format=json#key=template#nid=2#rgb=white",
            "brand_id=153043#format=json#key=template#nid=2",
            'brand_id=153043#format=json#key=template',
        ],
        [
            ("brand_id=153043#format=json#key=template#nid=2#region=1#rgb=white", '1'),
            ("brand_id=153043#format=json#key=template#nid=2#region=1", '9'),
            ("brand_id=153043#format=json#key=template#nid=2", '9'),
            ("brand_id=153043#format=json#key=template", '9'),
        ]
    )

    data.saas.add_final_ans({
        '1': '{"simple_key": "default_skip"}',
    })
    data.saas.add_final_ans({
        '9': '{"unexpected_key": "Bruh, that\'s strange..."}',  # При нескольких подходящих ключах, должен вернуться первый (то есть не этот)
    })

    data.saas.add_base_key(
        [
            "brand_id=153043#format=json#key=template#nid=2#region=1#rgb=white",
            "brand_id=153043#format=json#key=template#nid=2#region=1",
            "brand_id=153043#format=json#key=template#region=1",
            'brand_id=153043#format=json#key=template',
        ],
        [
            ("brand_id=153043#format=json#key=template#region=1", '2'),  # Подходит любой из запрошенных ключей
        ]
    )

    data.saas.add_final_ans({
        '2': '{"simple_key": "skip_region"}',
    })

    data.saas.add_base_key(
        [
            "brand_id=153043#format=json#key=template#nid=2#qwe=3#rgb=white",
            "brand_id=153043#format=json#key=template#nid=2#qwe=3",
            "brand_id=153043#format=json#key=template#qwe=3",
            'brand_id=153043#format=json#key=template',
        ],
        [
            ("brand_id=153043#format=json#key=template#nid=2#qwe=3#rgb=white", '3'),
            ("brand_id=153043#format=json#key=template#qwe=3", '9'),
        ]
    )

    data.saas.add_final_ans({
        '3': '{"simple_key": "skip_qwe_no_region"}',
    })

    reqs = [
        'templator/getcontextpage?key=template&format=json&brand_id=153043&nid=2&rgb=white&region=1',
        'templator/getcontextpage?key=template&format=json&brand_id=153043&nid=2&rgb=white&region=1&skip=region',
        'templator/getcontextpage?key=template&format=json&brand_id=153043&nid=2&qwe=3&rgb=white&skip=qwe',
    ]

    return run_test(data, reqs)


def test_add_skip_prod():
    return __test_add_skip("add_skip_prod", False)


def test_add_skip_prod_default():
    return __test_add_skip("add_skip_prod_default", None)


def test_add_skip_testing():
    return __test_add_skip("add_skip_testing", True)


def test_manual_servers():
    '''
        Проверяем, что "ручные" адреса букера, сааса и каталогера не попадают в запросы к букеру и каталогеру,
        но прокидываются в запросы к темплатору (if any)
    '''

    data = Data('manual_servers')

    add_to_saas(
        data,
        [('key', 'template'), ('format', 'json'), ('brand_id', '153043')],
        '{"simple_key": "@@#cataloger.getbrandinfo.seoTitle#@@ @@#tarantino.getcontextpage(key=empty@@format=json):#@@"}',
        is_first_record=True
    )

    add_to_saas(
        data,
        [('key', 'empty'), ('format', 'json')],
        '{}',
    )

    data.cataloger.add_json(
        'getbrandinfo',
        [('format', 'json'), ('id', '153043')],
        '{"seoTitle": "seo_title", "name": "plane_name"}'
    )

    reqs = [
        'templator/getcontextpage?key=template&format=json&brand_id=153043&debug_tarantino=yes&buker_host=buker.host.ru&buker_port=15&cataloger_host=cataloger.host.ru&cataloger_port=25'
        '&saas_host=saas.host.ru&saas_port=35',
    ]

    return run_test(data, reqs)


def test_ignore_params():
    data = Data('ignore_params')

    add_to_saas(
        data,
        [('key', 'template'), ('format', 'json'), ('brand_id', '1')],
        '{"simple_key": "just page"}',
        is_first_record=True
    )

    add_to_saas(
        data,
        [('key', 'template'), ('format', 'json'), ('brand_id', '1'), ('hollow', '1'), ('nothing', 'senseoflife')],
        '{"simple_key": "not page"}'
    )

    reqs = [
        'templator/getcontextpage?key=template&format=json&brand_id=1&hollow=1, 2, 3&nothing=senseoflife&hollow=42&ignore_cgi_params=hollow, nothing',
        'templator/getcontextpage?key=template&format=json&brand_id=1&hollow=1&nothing=senseoflife',
    ]

    return run_test(data, reqs)


def test_ask_saas():
    data = Data('ask_saas', True)
    data.saas.add_json(
        "",
        [('text', 'tratatat'), ('some_cgi', 'some_value')],
        '{"simple_key": "just page"}'
    )
    reqs = [
        'templator/asksaas?text=tratatat&some_cgi=some_value',
    ]

    return run_test(data, reqs)


def test_page_saas_simple():
    data = Data('default_page', True)
    data.saas.add_base_key(
        ["base=val#format=json"],
        [('base=val#format=json', '1')]
    )
    data.saas.add_final_ans(1)

    reqs = [
        'templator/getcontextpagesaas?base=val&format=json',
    ]

    return run_test(data, reqs)


def test_page_saas_exp_simple():
    data = Data('default_exp', True)
    data.saas.add_base_key(
        ["base=val#format=json"],
        [('base=val#format=json', '1'), ('base=val#format=json#rearr-factors=fst=1', '2')]
    )
    data.saas.add_final_ans(1)
    data.saas.add_final_ans(2)
    reqs = [
        'templator/getcontextpagesaas?base=val&rearr-factors=fst=1&format=json',
        'templator/getcontextpagesaas?base=val&format=json',
    ]

    return run_test(data, reqs)


def test_page_saas_empty():
    data = Data('empty', True)
    reqs = [
        'templator/getcontextpagesaas?base=val&format=json',
    ]

    return run_test(data, reqs)


def test_page_saas_skip():
    data = Data('skip', True)
    data.saas.add_base_key(
        ['a=1#format=json', 'a=1#b=2#format=json'],
        [('a=1#format=json', '1')]
    )
    data.saas.add_final_ans(1)

    data.saas.add_base_key(
        ['a=3#format=json', 'a=3#b=2#format=json'],
        [('a=3#b=2#format=json', '2')]
    )
    data.saas.add_final_ans(2)

    reqs = [
        'templator/getcontextpagesaas?a=1&b=2&format=json&skip=b',
        'templator/getcontextpagesaas?a=3&b=2&format=json&skip=b'
    ]

    return run_test(data, reqs)


def test_page_saas_one_of():
    data = Data('one_of', True)
    data.saas.add_base_key(
        ['a=1#format=json', 'b=2#format=json'],
        [('a=1#format=json', '1'), ('b=2#format=json', '2')]
    )
    data.saas.add_final_ans(1)
    data.saas.add_final_ans(2)

    reqs = [
        'templator/getcontextpagesaas?one_of=a,b&format=json&a=1&b=2',
        'templator/getcontextpagesaas?one_of=b,a&format=json&a=1&b=2'
    ]

    return run_test(data, reqs)


def test_page_saas_xml_simple():
    data = Data('xml_simple', True)
    data.saas.add_base_key(
        ['a=1'],
        [('a=1', '1')]
    )
    data.saas.add_final_ans(1, True)

    reqs = [
        'templator/getcontextpagesaas?a=1'
    ]

    return run_test(data, reqs)


def test_saas_order():
    '''
     GetContextPageSaas must always give documents in order provided by first request (indices located in 'Value' field)
     In first add_base_key 'Value' is '1,2' so order of pages should be 1,2
     The second one is '2,1' so we get 2,1
     add_final_ans adds documents ordered 1,2 so request with order=reversed should rearrange them before giving back
    '''
    data = Data('saas_order', True)
    data.saas.add_base_key(
        ['order=usual'],
        [('order=usual', '1,2')]
    )
    data.saas.add_base_key(
        ['order=reversed'],
        [('order=reversed', '2,1')]
    )

    data.saas.add_final_ans('1,2', True)
    reqs = [
        'templator/getcontextpagesaas?order=usual',
        'templator/getcontextpagesaas?order=reversed'
    ]
    return run_test(data, reqs)


def test_saas_req_without_host():
    data = Data('test_saas_req_without_host', True)
    data.saas.add_base_key(
        ['key=1'],
        [('key=1', '1')]
    )
    data.saas.add_final_ans(1, True)

    reqs = [
        'templator/getcontextpagesaas?buker_host=1&cataloger_host=2&buker_port=3&cataloger_port=4&saas_host=5&saas_port=6&key=1'
    ]
    return run_test(data, reqs)


def test_saas_empty_answer_without_error_1000():
    '''
    Проверяем, что ошибка 1000 не генерируется, даже если ответ от Саас пуст
    '''
    data = Data('saas_buker_empty', True)

    # Запрос пустой в СааС
    data.saas.add_base_key(
        ['content=empty#format=json'],
        []
    )
    # Ответ в букере не определен, но он не будет вызван
    reqs = [
        'templator/getcontextpage?content=empty&format=json&debug_tarantino=1&rearr-factors=market_cms_use_saas_instead_buker=all',
    ]

    return run_test(data, reqs)


def test_saas_indices_pagination():
    data = Data('saas_indices_pagination', True)

    def get_closed_interval_as_str(stard, end):
        return ','.join(map(str, range(stard, end + 1)))

    data.saas.add_base_key(
        ["device=desktop#domain=ru#format=json#tag=food#zoom=entrypoints"],
        [('device=desktop#domain=ru#format=json#tag=food#zoom=entrypoints', get_closed_interval_as_str(1, 8))]
    )

    # count 1
    data.saas.add_final_ans('8')                                    # page  8

    # count 2
    data.saas.add_final_ans(get_closed_interval_as_str(7, 8))       # page  4

    # count 3
    data.saas.add_final_ans(get_closed_interval_as_str(1, 3))       # page None
    data.saas.add_final_ans(get_closed_interval_as_str(1, 3))       # page -1
    data.saas.add_final_ans(get_closed_interval_as_str(1, 3))       # page  0
    data.saas.add_final_ans(get_closed_interval_as_str(1, 3))       # page  1

    # count 4
    data.saas.add_final_ans(get_closed_interval_as_str(5, 8))       # page  2

    # count 7
    data.saas.add_final_ans(get_closed_interval_as_str(1, 7))       # page  1
    data.saas.add_final_ans('8')                                    # page  2

    # count 20
    data.saas.add_final_ans(get_closed_interval_as_str(1, 8))       # page  None

    count_param = '&count={count}'
    page_param = '&templator_page={page}'

    params = 'templator/getcontextpage?zoom=entrypoints&tag=food&domain=ru&device=desktop&format=json'
    params_with_count = params + count_param
    params_with_page = params + page_param
    params_with_count_and_page = params + count_param + page_param

    reqs = [
        params,

        params_with_page.format(page=1),
        params_with_page.format(page=2),

        params_with_count_and_page.format(count=1, page=8),
        params_with_count_and_page.format(count=2, page=4),

        params_with_count.format(count=3),
        params_with_count_and_page.format(count=3, page=-1),
        params_with_count_and_page.format(count=3, page=0),
        params_with_count_and_page.format(count=3, page=1),

        params_with_count_and_page.format(count=4, page=2),

        params_with_count_and_page.format(count=7, page=1),
        params_with_count_and_page.format(count=7, page=2),

        params_with_count_and_page.format(count=20, page=1),
        params_with_count_and_page.format(count=20, page=2),
    ]

    return run_test(data, reqs)


def test_content_preview():
    '''
    Досту к превью данным, записанным в конфиге
    '''
    config_params = [
        "PREVIEW_SAAS_CMS_SERVICE preview_service",
        "PREVIEW_SAAS_SERVER saas_preview_host",
        "PREVIEW_SAAS_PORT 88",

        "PREVIEW_CATALOGER_SERVER cataloger_preview_host",
        "PREVIEW_CATALOGER_PORT 87",
    ]

    data = Data('test_content_preview', True)

    # ######################
    # Простой запрос

    # в прод
    data.saas.add_base_key(
        ['format=json#key=1'],
        [('format=json#key=1', '1')],
    )
    data.saas.add_final_ans(1, False)

    # в превью
    data.saas_preview.add_base_key(
        ['format=json#key=1'],
        [('format=json#key=1', '2')],
        service='preview_service'
    )
    data.saas_preview.add_final_ans(2, False, service='preview_service')

    # ######################
    # Запрос с подзапросами

    text_with_subrequests = '{"cataloger": "@@#cataloger.getbrandinfo.seoTitle#@@", "templator": @@#templator.getcontextpage(key=empty@@format=json):#@@}'

    # в прод
    data.saas.add_base_key(
        ['format=json#key=2'],
        [('format=json#key=2', '3')],
    )
    data.saas.add_final_ans({'3' : text_with_subrequests})

    # в превью
    data.saas_preview.add_base_key(
        ['format=json#key=2'],
        [('format=json#key=2', '4')],
        service='preview_service'
    )
    data.saas_preview.add_final_ans({'4' : text_with_subrequests}, service='preview_service')

    # ######################
    # Подзапрос в темплатор

    data.saas.add_base_key(
        ['format=json#key=empty'],
        [('format=json#key=empty', '5')],
    )
    data.saas.add_final_ans({'5': '{}'})

    data.saas_preview.add_base_key(
        ['format=json#key=empty'],
        [('format=json#key=empty', '6')],
        service='preview_service'
    )
    data.saas_preview.add_final_ans({'6': '{}'}, service='preview_service')

    data.cataloger.add_json(
        'getbrandinfo',
        [('format', 'json')],
        '{"seoTitle": "seo_title_prod", "name": "plane_name"}'
    )

    data.cataloger_preview.add_json(
        'getbrandinfo',
        [('format', 'json')],
        '{"seoTitle": "seo_title_preview", "name": "plane_name"}'
    )

    reqs = [
        'templator/getcontextpage?debug_tarantino=1&format=json&key=1',  # запрос в прод
        'templator/getcontextpage?debug_tarantino=1&format=json&key=1&templator_content=preview',  # запрос в превью

        'templator/getcontextpagesaas?debug_tarantino=1&format=json&key=2',  # запрос в прод
        'templator/getcontextpagesaas?debug_tarantino=1&format=json&key=2&templator_content=preview',  # запрос в превью
    ]
    return run_test(data, reqs, config_params)


def test_indicies_parsing_error():
    '''
    Проверяем генерацию ошибки при парсинге ответа cms
    '''
    data = Data('indicies_parsing_error')

    data.saas.add_base_key(
        ['format=json#text=1#type=mp_promo'],
        [('format=json#text=1#type=mp_promo', 'wrong')]
    )

    reqs = [
        'templator/getcontextpagesaas?type=mp_promo&format=json&text=1',
    ]
    return run_test(data, reqs, expected_errors={'1006': 1})


def test_reverse_exp():
    '''
    Проверяем обратные эксперименты
    '''
    data = Data('test_reverse_exp')

    data.saas.add_base_key(
        ['format=json#text=1#type=mp_promo'],
        [
            ('format=json#rearr-factors=!A=1;!A=2#text=1#type=mp_promo', '1'),
            ('format=json#text=1#type=mp_promo', '2')
        ]
    )
    data.saas.add_final_ans(1, False)
    data.saas.add_final_ans(2, False)

    # Для этой страницы требуется наличие одного экспа и отсутствие другого
    data.saas.add_base_key(
        ['format=json#text=2#type=mp_promo'],
        [
            ('format=json#rearr-factors=A=1;!A=2#text=2#type=mp_promo', '3'),
            ('format=json#text=2#type=mp_promo', '4')
        ]

    )
    data.saas.add_final_ans(3, False)
    data.saas.add_final_ans(4, False)

    reqs = [
        'templator/getcontextpagesaas?type=mp_promo&format=json&text=1',
        'templator/getcontextpagesaas?type=mp_promo&format=json&text=1&rearr-factors=A=1',
        'templator/getcontextpagesaas?type=mp_promo&format=json&text=1&rearr-factors=A=2',
        'templator/getcontextpagesaas?type=mp_promo&format=json&text=1&rearr-factors=A=3',

        'templator/getcontextpagesaas?type=mp_promo&format=json&text=2',
        'templator/getcontextpagesaas?type=mp_promo&format=json&text=2&rearr-factors=A=1',
        'templator/getcontextpagesaas?type=mp_promo&format=json&text=2&rearr-factors=A=1&rearr-factors=A=2',
        'templator/getcontextpagesaas?type=mp_promo&format=json&text=2&rearr-factors=A=2',
        'templator/getcontextpagesaas?type=mp_promo&format=json&text=2&rearr-factors=A=3&rearr-factors=A=2',
    ]
    return run_test(data, reqs)


def test_experiment_errors():
    '''
    Проверяем ошибку подбора страницы в зависимости от экспа пользователя
    '''
    data = Data('test_experiment_errors')

    data.saas.add_base_key(
        ['format=json#text=1#type=mp_error'],
        [
            ('format=json#rearr-factors=A=1;A=2#text=1#type=mp_error', '1'),
            ('format=json#rearr-factors=A=3#text=1#type=mp_error', '1'),
            ('format=json#text=1#type=mp_error', '2')
        ]
    )
    data.saas.add_final_ans(1, False)
    data.saas.add_final_ans(2, False)

    reqs = [
        'templator/getcontextpagesaas?type=mp_error&format=json&text=1',
        'templator/getcontextpagesaas?type=mp_error&format=json&text=1&rearr-factors=A=1',
        'templator/getcontextpagesaas?type=mp_error&format=json&text=1&rearr-factors=A=2',
        # Тут проблемы нет, т.к. оба эти теста находятся в одном документе
        'templator/getcontextpagesaas?type=mp_error&format=json&text=1&rearr-factors=A=1;A=2',

        'templator/getcontextpagesaas?type=mp_error&format=json&text=1&rearr-factors=A=3',
        # Здесь будет проблема, т.к. будут взяты страницы из двух разных документов
        'templator/getcontextpagesaas?type=mp_error&format=json&text=1&rearr-factors=A=1;A=3',
    ]
    return run_test(data, reqs, expected_errors={'1007': 1})


def test_throw_rearr_factors():
    '''
    Проверяем, что rearr-factors пробрасывается в templator.getcontextpage
    '''
    data = Data('test_throw_rearr_factors')

    add_to_saas(data, [('key', 'template'), ('format', 'json')], '{"subrequestValue": "@@#templator.getcontextpage(format=json@@subrequest=1).[0]#@@"}', is_first_record=True)

    data.saas.add_base_key(
        ['format=json#subrequest=1'],
        [
            ('format=json#subrequest=1', '2'),
            ('format=json#subrequest=1#rearr-factors=A=1', '3'),
            ('format=json#subrequest=1#rearr-factors=A=2', '4')
        ]
    )

    data.saas.add_final_ans({'2': '"value without rearr"'})
    data.saas.add_final_ans({'3': '"value with rearr A=1"'})
    data.saas.add_final_ans({'4': '"value with rearr A=2"'})

    reqs = [
        'templator/getcontextpagesaas?key=template&format=json',
        'templator/getcontextpagesaas?key=template&rearr-factors=A=1&format=json',
        'templator/getcontextpagesaas?key=template&rearr-factors=A=2&format=json',
    ]
    return run_test(data, reqs)


def prepare_prefix_suffix():
    data = Data('prefix-suffix')

    add_to_saas(data, [('key', 'value'), ('format', 'json')], """
        {
            "regular": {
                "empty": "@@#templator.getcontextpage(key=data@@format=json).[0].empty#@@",
                "non_empty": "@@#templator.getcontextpage(key=data@@format=json).[0].non_empty#@@",
                "some_json": "@@#templator.getcontextpage(key=data@@format=json).[0].some_json#@@"
            },
            "prefix": {
                "empty": "@@#+(prefix)templator.getcontextpage(key=data@@format=json).[0].empty#@@",
                "non_empty": "@@#+(prefix)templator.getcontextpage(key=data@@format=json).[0].non_empty#@@",
                "some_json": "@@#+(prefix)templator.getcontextpage(key=data@@format=json).[0].some_json#@@"
            },
            "suffix": {
                "empty": "@@#templator.getcontextpage(key=data@@format=json).[0].empty(suffix)+#@@",
                "non_empty": "@@#templator.getcontextpage(key=data@@format=json).[0].non_empty(suffix)+#@@",
                "some_json": "@@#templator.getcontextpage(key=data@@format=json).[0].some_json(suffix)+#@@"
            },
            "both": {
                "empty": "@@#+(prefix)templator.getcontextpage(key=data@@format=json).[0].empty(suffix)+#@@",
                "non_empty": "@@#+(prefix)templator.getcontextpage(key=data@@format=json).[0].non_empty(suffix)+#@@",
                "some_json": "@@#+(prefix)templator.getcontextpage(key=data@@format=json).[0].some_json(suffix)+#@@"
            },
            "with_defaults": {
                "empty_prefix": "@@#+(prefix)templator.getcontextpage(key=data@@format=json).[0].empty:default#@@",
                "non_existent_prefix": "@@#+(prefix)templator.getcontextpage(key=data@@format=json).[0].non_existent:default#@@",
                "empty_suffix": "@@#templator.getcontextpage(key=data@@format=json).[0].empty:default(suffix)+#@@",
                "non_existent_suffix": "@@#templator.getcontextpage(key=data@@format=json).[0].non_existent:default(suffix)+#@@",
                "empty_both": "@@#+(prefix)templator.getcontextpage(key=data@@format=json).[0].empty:default(suffix)+#@@",
                "non_existent_both": "@@#+(prefix)templator.getcontextpage(key=data@@format=json).[0].non_existent:default(suffix)+#@@"
            },
            "usecase_example": [
                "first_list_element"
                @@##+(,)templator.getcontextpage(key=data@@format=json).[0].some_json##@@
                @@##+(,)templator.getcontextpage(key=data@@format=json).[0].some_json##@@
                @@##+(,)templator.getcontextpage(key=data@@format=json).[0].empty##@@
                @@##+(,)templator.getcontextpage(key=data@@format=json).[0].some_json##@@
                @@##+(,)templator.getcontextpage(key=data@@format=json).[0].empty##@@
                @@##+(,)templator.getcontextpage(key=data@@format=json).[0].empty##@@
            ]
        }
    """, is_first_record=True)

    weird_templates = [
        "@@#+()templator.getcontextpage(key=data@@format=json).[0].non_empty#@@",
        "@@#+()templator.getcontextpage(key=data@@format=json).[0].non_empty()+#@@",
        "@@#templator.getcontextpage(key=data@@format=json).[0].non_empty()+#@@",

        "@@#+())templator.getcontextpage(key=data@@format=json).[0].non_empty#@@",
        "@@#+(()templator.getcontextpage(key=data@@format=json).[0].non_empty#@@",
        "@@#templator.getcontextpage(key=data@@format=json).[0].non_empty(()+#@@",
        "@@#templator.getcontextpage(key=data@@format=json).[0].non_empty())+#@@",

        "@@##@@",
        "@@#+#@@",
        "@@#+(a)#@@",
        "@@#(a)+#@@"
    ]

    reqs = [
        'templator/getcontextpage?key=value&format=json',
    ]

    for i, templ in enumerate(weird_templates):
        add_to_saas(data, [('key', 'v{}'.format(i)), ('format', 'json')], """{ "template": """ + templ + """ }""")
        reqs.append('templator/getcontextpage?key=v{}&format=json'.format(i))

    add_to_saas(data, [('key', 'data'), ('format', 'json')], """
        {
            "empty": "",
            "non_empty": "non_empty",
            "some_json": {
                "some": "json"
            }
        }
    """)

    return data, reqs


def good_prefix_suffix_req(s):
    return not ("key=v3" in s or "key=v5" in s or "key=v8" in s or "key=v9" in s or "key=v10" in s)


def test_prefix_suffix():
    '''
        Tests support for prefixes and suffixes in templates
    '''
    data, reqs = prepare_prefix_suffix()
    return run_test(data, filter(good_prefix_suffix_req, reqs))


def test_bad_prefix_suffix():
    '''
        Tests support for incorect prefixes and suffixes in templates
    '''
    data, reqs = prepare_prefix_suffix()
    return run_test(data, filter(lambda s: not good_prefix_suffix_req(s), reqs), expected_errors={'1003': 1})


def test_raw_response():
    '''
        Tests support for raw responses and multiple procesing iterations needed to process templates
        generated with raw response flag
    '''
    data = Data('raw_response')

    page = """
        {
            "template": "@@#templator.getcontextpage(key=inner@@format=json).[0].data#@@"
        }
    """
    inner = """
        {
            "data": "@@#templator.getcontextpage(key=data@@format=json).[0].value#@@"
        }
    """
    value = """
        {
            "value": "1"
        }
    """

    add_to_saas(data, [('key', 'page'), ('format', 'json')], page, is_first_record=True)
    add_to_saas(data, [('key', 'inner'), ('format', 'json')], inner)
    add_to_saas(data, [('key', 'data'), ('format', 'json')], value)

    reqs = [
        'templator/getcontextpage?key=page&format=json',
        'templator/getcontextpage?raw_response=1&key=page&format=json',
        'templator/getcontextpagesaas?key=page&format=json',
        'templator/getcontextpagesaas?raw_response=1&key=page&format=json',
    ]

    return run_test(data, reqs)


def test_counters():
    '''
        test counters in templates: initialization, getting the value, incrementing the value, multiple counters
        method without arguments return the value of the counter (increment acts as prefix C++ increment)
        methoids with arguments returns empty strings since the user is probably doing something tricky
    '''
    data = Data('counters')

    page = """
        {
            "initialization": [
                "@@#counters.first.get#@@",
                "@@#counters.second.set(10)#@@"
            ],
            "get": [
                "@@#counters.first.get#@@",
                "@@#counters.second.get#@@"
            ],
            "increment": [
                "@@#counters.first.inc#@@",
                "@@#counters.second.inc(5)#@@"
            ],
            "read_again": [
                "@@#counters.first.get#@@",
                "@@#counters.second.get#@@"
            ],
            "reset": [
                "@@#counters.first.set#@@",
                "@@#counters.second.set(20)#@@"
            ],
            "read_yet_again": [
                "@@#counters.first.get#@@",
                "@@#counters.second.get#@@"
            ],
            "set_to_nonzero_value": [
                "@@#counters.first.set(100500)#@@",
                "@@#counters.second.set(100500)#@@"
            ]
        }
    """

    add_to_saas(
        data,
        [('key', 'value1'), ('format', 'json')], page, is_first_record=True)
    add_to_saas(
        data,
        [('key', 'value2'), ('format', 'json')], page)

    # do 2 requests to make sure the state of the counters is not being dragged between requests
    reqs = [
        'templator/getcontextpage?key=value1&format=json',
        'templator/getcontextpage?key=value2&format=json'
    ]
    return run_test(data, reqs)


def test_counters_complicated():
    '''
        test counters in messed templates: different combinations of parameters and sub parameters
    '''
    data = Data('counters_complicated')

    page = """
        {
            "array": [
                @@#counters.first.inc()#@@,
                @@#counters.first.inc()#@@,
                @@#counters.first.inc()#@@,
                @@#counters.first.inc(@@#counters.second.set(2)#@@)#@@,
                @@#counters.first.inc()#@@,
                @@#counters.first.inc(@@#templator.getcontextpage(key=data@@format=json).[0].a#@@)#@@,
                @@#counters.first.inc()#@@,
                @@#counters.first.inc()#@@,
                @@#templator.getcontextpage(key=data@@format=json).[0].b.[@@#counters.second.inc()#@@]#@@,
                @@#counters.first.inc(@@#templator.getcontextpage(key=data@@format=json).[0].b.[@@#counters.second.inc()#@@]#@@)#@@
            ]
        }
    """

    add_to_saas(data, [('key', 'page'), ('format', 'json')], page, is_first_record=True)
    add_to_saas(data, [('key', 'data'), ('format', 'json')], """
        {
            "a": 10,
            "b": [0,1,2,3,4,5,6]
        }
    """)

    reqs = [
        'templator/getcontextpage?key=page&format=json'
    ]
    return run_test(data, reqs)


def test_counters_with_prefixes():
    '''
        test counters in combination with prefixes. Needed since part of counters logic relies of template subsittution
    '''
    data = Data('counters_with_prefixes')

    page = """
        {
            "array": [
                "@@#counters.first.inc()#@@",
                "@@#+()counters.first.inc()#@@",
                "@@#+(prefix )counters.first.inc()#@@",
            ]
        }
    """

    add_to_saas(
        data,
        [('key', 'page'), ('format', 'json')], page, is_first_record=True)

    reqs = [
        'templator/getcontextpage?key=page&format=json'
    ]
    return run_test(data, reqs)


def test_runtime_mocks():
    '''
        test runtime service response mocks
    '''
    data = Data('runtime_mocks')

    page = """
        {
            "mocked": "@@#templator.getcontextpage(format=json@@key=mocked).[0].data#@@",
            "not_mocked": "@@#templator.getcontextpage(format=json@@key=not_mocked).[0].data#@@"
        }
    """

    mocked_page = """
    {
        "data": "test_in_mocked_page"
    }
    """
    not_mocked_page = """
    {
        "data": "test_in_non_mocked_page"
    }
    """

    page_mock = {
        "market_kgb_templator": {
            "http://templator.vs.market.yandex.net:29338/templator/getcontextpage?depth_tarantino=4&format=json&key=mocked":
                "{\"__info__\":{\"servant\":\"buker\"},\"result\":[\n{\n\"data\":\"mocked_result\"\n}\n]}"
        }
    }

    page_mock_packed = base64.urlsafe_b64encode(zlib.compress(json.dumps(page_mock)))

    add_to_saas(data, [('key', 'page'), ('format', 'json')], page, is_first_record=True)

    add_to_saas(data, [('key', 'mocked'), ('format', 'json')], mocked_page)
    add_to_saas(data, [('key', 'not_mocked'), ('format', 'json')], not_mocked_page)

    reqs = [
        'templator/getcontextpage?format=json&key=page',
        'templator/getcontextpage?format=json&key=page&mock_responses=' + page_mock_packed,
        'templator/getcontextpage?format=json&key=page&mock_responses=',
        'templator/getcontextpagesaas?format=json&key=page',
        'templator/getcontextpagesaas?format=json&key=page&mock_responses=' + page_mock_packed,
        'templator/getcontextpagesaas?format=json&key=page&mock_responses=',
    ]

    return run_test(data, reqs)


def test_string():
    '''
        test string
    '''
    data = Data('strings')

    page = """
        {
            "hide": [
                @@#string.hide(v=@@#counters.__c__.set(25)#@@)#@@
                @@#counters.__c__.get()#@@
            ],
            "join": [
                @@#string.join(what=1234567890@@with=012)#@@
            ],
            "insert": [
                @@#string.insert(what=1234567890@@with=012)#@@,
                @@#string.insert(what=1234567890@@with=012@@pos=5)#@@,
                @@#string.insert(what=1234567890@@with=012@@len=2)#@@,
                @@#string.insert(what=1234567890@@with=012@@len=5)#@@,
                @@#string.insert(what=1234567890@@with=012@@len=5@@filler=0)#@@,
                @@#string.insert(what=1234567890@@with=012@@len=5@@filler=0@@pos=2)#@@,
                @@#string.insert(what=1234567890@@with=012@@pos=25)#@@
            ],
            "replace": [
                @@#string.replace(what=1234567890@@with=012)#@@,
                @@#string.replace(what=1234567890@@with=012@@len=2)#@@,
                @@#string.replace(what=1234567890@@with=01234567890213456)#@@,
                @@#string.replace(what=1234567890@@with=012@@pos=5)#@@,
                @@#string.replace(what=1234567890@@with=012@@len=5)#@@,
                @@#string.replace(what=1234567890@@with=012@@len=5@@with_len=5)#@@,
                @@#string.replace(what=1234567890@@with=012@@len=5@@with_len=1)#@@,
                @@#string.replace(what=1234567890@@with=012@@len=5@@filler=0)#@@,
                @@#string.replace(what=1234567890@@with=012@@len=5@@filler=0@@pos=2)#@@,
                @@#string.replace(what=1234567890@@with=012@@pos=9)#@@,
                @@#string.replace(what=1234567890@@with=012@@pos=25)#@@
            ]
        }
    """

    add_to_saas(data, [('key', 'page'), ('format', 'json')], page, is_first_record=True)

    reqs = [
        'templator/getcontextpage?key=page&format=json'
    ]
    return run_test(data, reqs)


def test_operator_quotes():
    '''
        test operator quotes needed to mask templates so one can substitute with parts of pages containing
        templator templates combined with raw_response=1 that would break json otherwise.
        (See how templator fails to substitute from the "invalid_page")
    '''
    data = Data('operator_quotes')

    simple_test_page = """
        {
            "test_left": "12345"@@rlq@@54321",
            "test_right": "12345@@rrq@@"54321"
        }
    """

    invalid_page = """
        {
            "data": [
                @@#counters.__invalid_json__.set(100)#@@
            ]
        }
    """

    valid_page = """
        {
            "data": [
                "@@rlq@@@@#counters.__invalid_json__.set(100)#@@@@rrq@@"
            ]
        }
    """

    mega_page = """
        {
            "invalid": "@@#templator.getcontextpage(key=invalid_page@@raw_response=1@@format=json).[0].data.[0]:failure#@@",
            "valid": "@@#templator.getcontextpage(key=valid_page@@raw_response=1@@format=json).[0].data.[0]:failure#@@"
        }
    """

    add_to_saas(data, [('key', 'simple_page'), ('format', 'json')], simple_test_page, is_first_record=True)
    add_to_saas(data, [('key', 'invalid_page'), ('format', 'json')], invalid_page)
    add_to_saas(data, [('key', 'valid_page'), ('format', 'json')], valid_page)
    add_to_saas(data, [('key', 'invalid_page'), ('format', 'json')], invalid_page)
    add_to_saas(data, [('key', 'valid_page'), ('format', 'json')], valid_page)
    add_to_saas(data, [('key', 'mega_page'), ('format', 'json')], mega_page)
    add_to_saas(data, [('key', 'invalid_page'), ('format', 'json'), ('raw_response', '1')], invalid_page)
    add_to_saas(data, [('key', 'valid_page'), ('format', 'json'), ('raw_response', '1')], valid_page)

    reqs = [
        'templator/getcontextpage?key=simple_page&format=json',
        'templator/getcontextpage?key=invalid_page&format=json',
        'templator/getcontextpage?key=valid_page&format=json',
        'templator/getcontextpage?key=mega_page&format=json',
    ]
    return run_test(data, reqs)


def test_static_page_cache():
    '''
    Проверяем работу кэша для статичных типов страниц
    '''
    data = Data('static_page_cache')

    add_to_saas(data, [('type', 'mp_available_support_channels'), ('format', 'json')], "mp_available_support_channels", is_first_record=True)

    reqs = [
        'templator/getcontextpage?type=mp_available_support_channels&format=json&debug_tarantino=1',
        'templator/getcontextpage?type=mp_available_support_channels&format=json&debug_tarantino=1',  # Повторный запрос должен отдать ответ без похода в саас
        'templator/getcontextpage?type=mp_available_support_channels&format=json',      # Проверяем работу без дебага
    ]
    return run_test(data, reqs)


def test_direct_buker_request():
    '''
    Проверяем прямой запрос в букер для некоторых типов страниц
    Хочу разгрузить саас от тяжелых запросов за малым количеством данных
    '''
    data = Data('test_direct_buker_request')

    data.buker.add_json(
        "GetCards",
        [
            ('format', 'json'),
            ('product_id', '661220018'),
            ('type', 'product-header'),
            ('region', '21623'),
            ('skip', 'region,product_id'),
            ('one_of', 'show_explicit_content*region,region,')
        ],
        '{"normal": "answer"}'
    )
    reqs = [
        'templator/getcontextpage?type=product-header&format=json&region=21623&product_id=661220018&one_of=show_explicit_content*region%2Cregion%2C&skip=product_id',
    ]
    # Хотя нет моков в саас, ошибок тоже нет, т.к. запрос идет сразу в букер
    return run_test(data, reqs)


def test_sharp():
    '''
    Проверяем экранирование символа # в запросах к темплатору
    '''
    data = Data('test_sharp')

    add_to_saas(data, [('type', 'sharp'), ('format', 'json'), ('shopPromoId', '%2391489')], '{ "normal answer": "@@#request.shopPromoId#@@"}', is_first_record=True)

    reqs = [
        'templator/getcontextpage?type=sharp&format=json&rearr-factors=promo_enable_by_anaplan_promo_id=%23123&shopPromoId=%2391489&debug_tarantino=1',
    ]
    return run_test(data, reqs)


def test_type_request():
    '''
        test type request without hash
    '''
    data = Data('type')
    config_params = ['SAAS_SEARCH_BY_TYPES live_stream,recipe']
    search_by_types = {'live_stream', 'recipe'}

    #  Когда тип есть в конфиге --> ищем по type_key
    data.saas.add_base_key(
        ['format=json#type=live_stream'],
        [
            ('format=json#type=live_stream', '1')
        ],
        search_by_types=search_by_types
    )
    data.saas.add_final_ans({'1': 'fromConfig1'})

    # Когда одного из двух типов нет в конфиге --> ищем по base_key
    data.saas.add_base_key(
        ['format=json#type=blog', 'format=json#type=recipe'],
        [
            ('format=json#type=blog', '3'),
            ('format=json#type=recipe', '4'),
        ],

    )
    data.saas.add_final_ans({'3': 'oneOfTypesIsNotInConfig', '4': 'blogIsNotInConfig1'})

    # Запрос по второму в конфиге типу --> ищем по type_key
    data.saas.add_base_key(
        ['format=json#type=recipe'],
        [
            ('format=json#type=recipe', '5')
        ],
        search_by_types=search_by_types
    )
    data.saas.add_final_ans({'5': 'fromConfig2'})

    # Запрос, когда оба типа в конфиге (-/ Не работает /-) --> ищем по type_key
    data.saas.add_base_key(
        ['format=json#type=live_stream', 'format=json#type=recipe'],
        [
            ('format=json#type=live_stream', "8"),
            ('format=json#type=recipe', "9"),
        ],
        search_by_types=search_by_types
    )

    data.saas.add_final_ans({"8": "fromConfig1", "9": "fromConfig2"})

    # Запрос, когда типа нет в конфиге --> ищем по base_key
    data.saas.add_base_key(
        ['format=json#type=blog'],
        [
            ('format=json#type=blog', '10'),
        ],
        search_by_types=search_by_types
    )
    data.saas.add_final_ans({'10': 'isNotInConfig'})

    # Запрос без типа --> ищем по base_key
    data.saas.add_base_key(
        ['format=json#key=value'],
        [
            ('format=json#key=value', '11'),
        ],
        search_by_types=search_by_types
    )
    data.saas.add_final_ans({
        '11': 'missingType',
    })

    reqs = [
        'tarantino/getcontextpagesaas?format=json&type=live_stream&debug_tarantino=1',
        'tarantino/getcontextpagesaas?format=json&type=blog&type=recipe&debug_tarantino=1',
        'tarantino/getcontextpagesaas?format=json&type=recipe&debug_tarantino=1',
        'tarantino/getcontextpagesaas?format=json&type=blog&debug_tarantino=1',
        'tarantino/getcontextpagesaas?format=json&key=value&debug_tarantino=1',
        'tarantino/getcontextpagesaas?format=json&type=live_stream&type=recipe&debug_tarantino=1',
    ]
    return run_test(data, reqs, config_params)


def check_thread_requester(data, max_rps, expected_errors):
    '''
    Провереяе ограничение запросов на клиенты
    '''
    config_params = [
        'CATALOGER_CACHE_MOCKED 1',
        'CATALOGER_REQTASK_THREADS_NUMBER 1',
        'CATALOGER_MAX_RPS {}'.format(max_rps)
    ]

    page = """
        {
            "array": [
                "@@#cataloger.get_some_data(id=0@@format=json).value#@@",
                "@@#cataloger.get_some_data(id=1@@format=json).value#@@",
                "@@#cataloger.get_some_data(id=2@@format=json).value#@@",
                "@@#cataloger.get_some_data(id=3@@format=json).value#@@",
                "@@#cataloger.get_some_data(id=4@@format=json).value#@@",
                "@@#cataloger.get_some_data(id=5@@format=json).value#@@",
                "@@#cataloger.get_some_data(id=6@@format=json).value#@@",
                "@@#cataloger.get_some_data(id=7@@format=json).value#@@",
                "@@#cataloger.get_some_data(id=8@@format=json).value#@@",
                "@@#cataloger.get_some_data(id=9@@format=json).value#@@",
            ]
        }
    """

    add_to_saas(
        data,
        [('key', 'page'), ('format', 'json')], page, is_first_record=True)

    for i in range(10):
        data.cataloger.add_json(
            'get_some_data',
            [('format', 'json'), ('id', str(i))],
            '{"value":' + str(i + 10) + '}'
        )

    reqs = [
        'templator/getcontextpage?key=page&format=json'
    ]
    return run_test(data, reqs, config_params, expected_errors=expected_errors)


def test_thread_requester_limited():
    data = Data('thread_requester_limited')
    return check_thread_requester(data, 1, expected_errors={'1003': 1})


def test_thread_requester_limited_but_unreachable():
    data = Data('thread_requester_limited_but_unreachable')
    return check_thread_requester(data, 100, expected_errors=None)


def test_thread_requester_unlimited():
    data = Data('thread_requester_unlimited')
    return check_thread_requester(data, 0, expected_errors=None)


def test_thread_requester_with_cache():
    '''
    Одинаковые запросы кэшируются
    '''
    data = Data('thread_requester_cache_1')

    config_params = [
        'CATALOGER_CACHE_MOCKED 1',
        'CATALOGER_REQTASK_THREADS_NUMBER 1',
        'CATALOGER_MAX_RPS 1'
    ]

    page = """
        {
            "array": [
                "@@#cataloger.get_some_data(id=1@@format=json).value#@@",
                "@@#cataloger.get_some_data(id=1@@format=json).value#@@",
                "@@#cataloger.get_some_data(id=1@@format=json).value#@@",
                "@@#cataloger.get_some_data(id=1@@format=json).value#@@",
                "@@#cataloger.get_some_data(id=1@@format=json).value#@@",
                "@@#cataloger.get_some_data(id=1@@format=json).value#@@",
                "@@#cataloger.get_some_data(id=1@@format=json).value#@@",
                "@@#cataloger.get_some_data(id=1@@format=json).value#@@",
                "@@#cataloger.get_some_data(id=1@@format=json).value#@@",
                "@@#cataloger.get_some_data(id=1@@format=json).value#@@",
            ]
        }
    """

    add_to_saas(
        data,
        [('key', 'page'), ('format', 'json')], page, is_first_record=True)

    data.cataloger.add_json(
        'get_some_data',
        [('format', 'json'), ('id', '1')],
        '{"value": "1"}'
    )

    reqs = [
        'templator/getcontextpage?key=page&format=json'
    ]
    return run_test(data, reqs, config_params, expected_errors=None)
