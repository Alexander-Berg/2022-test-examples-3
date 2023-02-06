__author__ = 'aokhotin'

import pytest
from devtools.fleur.util.sandbox import Sandbox

from runtime.simcity import task_runner
from .. import STABLE_PRIEMKA_YANDEX_RU, REGIONS_DATA, mutate_yandex_host_with_region

DEFAULT_REARRANGE_CTX = {
    "prod_host": "dummy.hamster.yandex.ru",
    "beta_host": "dummy.hamster.yandex.ru",
    "prod_cgi_params": "&json_dump=searchdata&json_dump=search_props&json_dump=reqparam&waitall=da&nocache=da&no-tests=1&json_dump=rdat.cgi.hostname&json_dump=unanswer_data&timeout=9999999",
    "beta_cgi_params": "&json_dump=searchdata&json_dump=search_props&json_dump=reqparam&waitall=da&nocache=da&no-tests=1&json_dump=rdat.cgi.hostname&json_dump=unanswer_data&timeout=9999999",
    "config": 0,
    "req_resource_id": 0,
    "use_random_requests": 0,
    "use_personal_uids": 0,
}


def parameters_for_tests_with_predefined_queries():
    return [
        ## RU
        ({"video_priemka-wiz-config-release": "1"},
         {"video_priemka-wiz-queries-release": "1"},
         "VIDEO",
         mutate_yandex_host_with_region(STABLE_PRIEMKA_YANDEX_RU, "RU"),
         mutate_yandex_host_with_region(pytest.config.option.beta, "RU"),
         "RU"),
        pytest.mark.rearrange_dynamic(
        ({"blender_priemka-wiz-config-release": "1"},
         {"blender_priemka-wiz-queries-release": "1"},
         "BLENDER_WIZ",
         mutate_yandex_host_with_region(STABLE_PRIEMKA_YANDEX_RU, "RU"),
         mutate_yandex_host_with_region(pytest.config.option.beta, "RU"),
         "RU")),
        pytest.mark.rearrange_dynamic(
        ({"blender_priemka-static_stream-config-release": "1"},
         {"blender_priemka-static_stream-queries-release": "1"},
         "BLENDER_STATIC_STREAM", mutate_yandex_host_with_region(STABLE_PRIEMKA_YANDEX_RU, "RU"),
         mutate_yandex_host_with_region(pytest.config.option.beta, "RU"),
         "RU")),
        pytest.mark.rearrange_dynamic(
        ({"blender_priemka-nav-config-release": "1"},
         {"blender_priemka-nav-queries-release": "1"},
         "BLENDER_NAV",
         mutate_yandex_host_with_region(STABLE_PRIEMKA_YANDEX_RU, "RU"),
         mutate_yandex_host_with_region(pytest.config.option.beta, "RU"),
         "RU")),
        ## KZ
        pytest.mark.rearrange_dynamic(
        ({"blender_priemka-wiz-config-release": "1"},
         {"blender_priemka-wiz-queries-release": "1"},
         "BLENDER_WIZ",
         mutate_yandex_host_with_region(STABLE_PRIEMKA_YANDEX_RU, "KZ"),
         mutate_yandex_host_with_region(pytest.config.option.beta, "KZ"),
         "KZ")),
        pytest.mark.rearrange_dynamic(
        ({"blender_priemka-static_stream-config-release": "1"},
         {"blender_priemka-static_stream-queries-release": "1"},
         "BLENDER_STATIC_STREAM", mutate_yandex_host_with_region(STABLE_PRIEMKA_YANDEX_RU, "KZ"),
         mutate_yandex_host_with_region(pytest.config.option.beta, "KZ"),
         "KZ")),
        pytest.mark.rearrange_dynamic(
        ({"blender_priemka-nav-config-release": "1"},
         {"blender_priemka-nav-queries-release": "1"},
         "BLENDER_NAV",
         mutate_yandex_host_with_region(STABLE_PRIEMKA_YANDEX_RU, "KZ"),
         mutate_yandex_host_with_region(pytest.config.option.beta, "KZ"),
         "KZ")),
        ## UA
        pytest.mark.rearrange_dynamic(
        ({"blender_priemka-wiz-config-release": "1"},
         {"blender_priemka-wiz-queries-release": "1"},
         "BLENDER_WIZ",
         mutate_yandex_host_with_region(STABLE_PRIEMKA_YANDEX_RU, "UA"),
         mutate_yandex_host_with_region(pytest.config.option.beta, "UA"),
         "UA")),
        pytest.mark.rearrange_dynamic(
        ({"blender_priemka-static_stream-config-release": "1"},
         {"blender_priemka-static_stream-queries-release": "1"},
         "BLENDER_STATIC_STREAM", mutate_yandex_host_with_region(STABLE_PRIEMKA_YANDEX_RU, "UA"),
         mutate_yandex_host_with_region(pytest.config.option.beta, "UA"),
         "UA")),
        pytest.mark.rearrange_dynamic(
        ({"blender_priemka-nav-config-release": "1"},
         {"blender_priemka-nav-queries-release": "1"},
         "BLENDER_NAV",
         mutate_yandex_host_with_region(STABLE_PRIEMKA_YANDEX_RU, "UA"),
         mutate_yandex_host_with_region(pytest.config.option.beta, "UA"),
         "UA")),
        ## BY
        pytest.mark.rearrange_dynamic(
        ({"blender_priemka-wiz-config-release": "1"},
         {"blender_priemka-wiz-queries-release": "1"},
         "BLENDER_WIZ",
         mutate_yandex_host_with_region(STABLE_PRIEMKA_YANDEX_RU, "BY"),
         mutate_yandex_host_with_region(pytest.config.option.beta, "BY"),
         "BY")),
        pytest.mark.rearrange_dynamic(
        ({"blender_priemka-static_stream-config-release": "1"},
         {"blender_priemka-static_stream-queries-release": "1"},
         "BLENDER_STATIC_STREAM", mutate_yandex_host_with_region(STABLE_PRIEMKA_YANDEX_RU, "BY"),
         mutate_yandex_host_with_region(pytest.config.option.beta, "BY"),
         "BY")),
        pytest.mark.rearrange_dynamic(
        ({"blender_priemka-nav-config-release": "1"},
         {"blender_priemka-nav-queries-release": "1"},
         "BLENDER_NAV",
         mutate_yandex_host_with_region(STABLE_PRIEMKA_YANDEX_RU, "BY"),
         mutate_yandex_host_with_region(pytest.config.option.beta, "BY"),
         "BY")),
        ## TR
        pytest.mark.rearrange_dynamic(
        ({"blender_priemka-wiz-config-release": "1"},
         {"blender_priemka-wiz-queries-release": "1"},
         "BLENDER_WIZ",
         mutate_yandex_host_with_region(STABLE_PRIEMKA_YANDEX_RU, "TR"),
         mutate_yandex_host_with_region(pytest.config.option.beta, "TR"),
         "TR")),
        pytest.mark.rearrange_dynamic(
        ({"blender_priemka-static_stream-config-release": "1"},
         {"blender_priemka-static_stream-queries-release": "1"},
         "BLENDER_STATIC_STREAM", mutate_yandex_host_with_region(STABLE_PRIEMKA_YANDEX_RU, "TR"),
         mutate_yandex_host_with_region(pytest.config.option.beta, "TR"),
         "TR")),
        pytest.mark.rearrange_dynamic(
        ({"blender_priemka-nav-config-release": "1"},
         {"blender_priemka-nav-queries-release": "1"},
         "BLENDER_NAV",
         mutate_yandex_host_with_region(STABLE_PRIEMKA_YANDEX_RU, "TR"),
         mutate_yandex_host_with_region(pytest.config.option.beta, "TR"),
         "TR")),
    ]


@pytest.mark.noapache
@pytest.mark.rearrange_dynamic
@pytest.mark.parametrize("config_attrs, queries_attr, test_name, prod_host, beta_host, region",
                         parameters_for_tests_with_predefined_queries())
@pytest.metainfo.set(name="{test_name} {region}",
                     description="{prod_host} vs {beta_host}: config_attrs={config_attrs}, queries_attr={queries_attr}")
def tests_with_predefined_queries(tmpdir, prod_host, beta_host, config_attrs, queries_attr, region, test_name):
    test_config = Sandbox().FindLatestResource("FACTOR_CONFIG", "any", config_attrs)
    queries_attr.update({'locale': region.lower()})
    test_queries = Sandbox().FindLatestResource("PLAIN_TEXT_QUERIES", "any", queries_attr)

    task_ctx = {"prod_host": prod_host,
                "beta_host": beta_host,
                "config": test_config['id'],
                "req_resource_id": test_queries['id'],
                "sandbox.gateway.get.LogFiles": "rearrange.junit.xml", }
    task_ctx = dict(DEFAULT_REARRANGE_CTX.items() + task_ctx.items())

    description = "{prod_host} vs {beta_host}, config_attrs={config_attrs}, queries_attr={queries_attr}".format(
        prod_host=prod_host, beta_host=beta_host,
        config_attrs=config_attrs,
        queries_attr=queries_attr)
    result = task_runner.run("REARRANGE_ACCEPTANCE", task_ctx, "any", description, work_dir=tmpdir.strpath,
                             raise_for_returncode=False)
    report = filter(lambda item: item["type_name"] == "FACTOR_TESTS_RESULT", result)[0]
    try:
        report_url = "{base_url}/main.html".format(base_url=Sandbox().GetResourceHttpLinks(report['id'])[0])
        pytest.metainfo.update(report_url=report_url)
    except:
        pass
    task_id = filter(lambda item: item["type_name"] == "TASK_LOGS", result)[0]['task_id']
    try:
        task_url = 'https://sandbox.yandex-team.ru/sandbox/tasks/view?task_id={task_id}'.format(task_id=task_id)
        pytest.metainfo.update(task_url=task_url)
    except:
        pass
    status = Sandbox().GetTask(report["task_id"])['status']
    assert status == "SUCCESS"


def parameters_for_tests_on_random_streams():
    return [
        # RU
        ({"blender_priemka-stream-config-release": "1"},
         {"use_random_requests": 3000, "use_personal_uids": 0},
         "BLENDER_RANDOM_STREAM",
         mutate_yandex_host_with_region(STABLE_PRIEMKA_YANDEX_RU, "RU"),
         mutate_yandex_host_with_region(pytest.config.option.beta, "RU"),
         "RU"),
        ({"personalization_priemka_rearrange_config": "1"},
         {"use_random_requests": 3000, "use_personal_uids": 1},
         "PERSONALIZATION",
         mutate_yandex_host_with_region(STABLE_PRIEMKA_YANDEX_RU, "RU"),
         mutate_yandex_host_with_region(pytest.config.option.beta, "RU"),
         "RU"),
        ({"rearr_config": "1"},
         {"use_random_requests": 3000, "use_personal_uids": 0},
         "REARRANGE",
         mutate_yandex_host_with_region(STABLE_PRIEMKA_YANDEX_RU, "RU"),
         mutate_yandex_host_with_region(pytest.config.option.beta, "RU"),
         "RU"),
        ({"rearr_config": "1"},
         {
             "use_random_requests": 3000,
             "use_personal_uids": 0,
             "search_prop_filter": "'UPPER.ApplyBlender.IntentWeight/FRESH' > 0.01"},
         "REARRANGE",
         mutate_yandex_host_with_region(STABLE_PRIEMKA_YANDEX_RU, "RU"),
         mutate_yandex_host_with_region(pytest.config.option.beta, "RU"),
         "RU"),
        # KZ
        ({"blender_priemka-stream-config-release": "1"},
         {"use_random_requests": 3000, "use_personal_uids": 0},
         "BLENDER_RANDOM_STREAM",
         mutate_yandex_host_with_region(STABLE_PRIEMKA_YANDEX_RU, "KZ"),
         mutate_yandex_host_with_region(pytest.config.option.beta, "KZ"),
         "KZ"),
        ({"personalization_priemka_rearrange_config": "1"},
         {"use_random_requests": 3000, "use_personal_uids": 1},
         "PERSONALIZATION",
         mutate_yandex_host_with_region(STABLE_PRIEMKA_YANDEX_RU, "KZ"),
         mutate_yandex_host_with_region(pytest.config.option.beta, "KZ"),
         "KZ"),
        ({"rearr_config": "1"},
         {"use_random_requests": 3000, "use_personal_uids": 0},
         "REARRANGE",
         mutate_yandex_host_with_region(STABLE_PRIEMKA_YANDEX_RU, "KZ"),
         mutate_yandex_host_with_region(pytest.config.option.beta, "KZ"),
         "KZ"),
        # UA
        ({"blender_priemka-stream-config-release": "1"},
         {"use_random_requests": 3000, "use_personal_uids": 0},
         "BLENDER_RANDOM_STREAM",
         mutate_yandex_host_with_region(STABLE_PRIEMKA_YANDEX_RU, "UA"),
         mutate_yandex_host_with_region(pytest.config.option.beta, "UA"),
         "UA"),
        ({"personalization_priemka_rearrange_config": "1"},
         {"use_random_requests": 3000, "use_personal_uids": 1},
         "PERSONALIZATION",
         mutate_yandex_host_with_region(STABLE_PRIEMKA_YANDEX_RU, "UA"),
         mutate_yandex_host_with_region(pytest.config.option.beta, "UA"),
         "UA"),
        ({"rearr_config": "1"},
         {"use_random_requests": 3000, "use_personal_uids": 0},
         "REARRANGE",
         mutate_yandex_host_with_region(STABLE_PRIEMKA_YANDEX_RU, "UA"),
         mutate_yandex_host_with_region(pytest.config.option.beta, "UA"),
         "UA"),
        # BY
        ({"blender_priemka-stream-config-release": "1"},
         {"use_random_requests": 3000, "use_personal_uids": 0},
         "BLENDER_RANDOM_STREAM",
         mutate_yandex_host_with_region(STABLE_PRIEMKA_YANDEX_RU, "BY"),
         mutate_yandex_host_with_region(pytest.config.option.beta, "BY"),
         "BY"),
        ({"personalization_priemka_rearrange_config": "1"},
         {"use_random_requests": 3000, "use_personal_uids": 1},
         "PERSONALIZATION",
         mutate_yandex_host_with_region(STABLE_PRIEMKA_YANDEX_RU, "BY"),
         mutate_yandex_host_with_region(pytest.config.option.beta, "BY"),
         "BY"),
        ({"rearr_config": "1"},
         {"use_random_requests": 3000, "use_personal_uids": 0},
         "REARRANGE",
         mutate_yandex_host_with_region(STABLE_PRIEMKA_YANDEX_RU, "BY"),
         mutate_yandex_host_with_region(pytest.config.option.beta, "BY"),
         "BY"),
        # TR
        ({"blender_priemka-stream-config-release": "1"},
         {"use_random_requests": 3000, "use_personal_uids": 0},
         "BLENDER_RANDOM_STREAM",
         mutate_yandex_host_with_region(STABLE_PRIEMKA_YANDEX_RU, "TR"),
         mutate_yandex_host_with_region(pytest.config.option.beta, "TR"),
         "TR"),
        ({"personalization_priemka_rearrange_config": "1"},
         {"use_random_requests": 3000, "use_personal_uids": 1},
         "PERSONALIZATION",
         mutate_yandex_host_with_region(STABLE_PRIEMKA_YANDEX_RU, "TR"),
         mutate_yandex_host_with_region(pytest.config.option.beta, "TR"),
         "TR"),
        ({"rearr_config": "1"},
         {"use_random_requests": 3000, "use_personal_uids": 0},
         "REARRANGE",
         mutate_yandex_host_with_region(STABLE_PRIEMKA_YANDEX_RU, "TR"),
         mutate_yandex_host_with_region(pytest.config.option.beta, "TR"),
         "TR"),
        ({"rearr_config": "1"},
         {
             "use_random_requests": 3000,
             "use_personal_uids": 0,
             "search_prop_filter": "'UPPER.ApplyBlender.IntentWeight/FRESH' > 0.01"},
         "REARRANGE",
         mutate_yandex_host_with_region(STABLE_PRIEMKA_YANDEX_RU, "TR"),
         mutate_yandex_host_with_region(pytest.config.option.beta, "TR"),
         "TR"),
    ]


@pytest.mark.noapache
@pytest.mark.parametrize("config_attrs, tast_ctx_custom, test_name, prod_host, beta_host, region",
                         parameters_for_tests_on_random_streams())
@pytest.metainfo.set(name="{test_name} {region}",
                     description="{prod_host} vs {beta_host}: config_attrs={config_attrs}, tast_ctx_custom={tast_ctx_custom}")
def tests_on_random_streams(tmpdir, prod_host, beta_host, config_attrs, tast_ctx_custom, region, test_name):
    test_config = Sandbox().FindLatestResource("FACTOR_CONFIG", "any", config_attrs)

    task_ctx = {"prod_host": prod_host,
                "beta_host": beta_host,
                "config": test_config['id'],
                "region": REGIONS_DATA[region]["geo_id"],
                "sandbox.gateway.get.LogFiles": "rearrange.junit.xml", }
    task_ctx = dict(DEFAULT_REARRANGE_CTX.items() + task_ctx.items() + tast_ctx_custom.items())

    description = "{prod_host} vs {beta_host}, config_attrs={config_attrs}, tast_ctx_custom={tast_ctx_custom}".format(
        prod_host=prod_host, beta_host=beta_host,
        tast_ctx_custom=tast_ctx_custom,
        config_attrs=config_attrs)
    result = task_runner.run("REARRANGE_ACCEPTANCE", task_ctx, "any", description, work_dir=tmpdir.strpath,
                             raise_for_returncode=False)

    report = filter(lambda item: item["type_name"] == "FACTOR_TESTS_RESULT", result)[0]
    try:
        report_url = "{base_url}/main.html".format(base_url=Sandbox().GetResourceHttpLinks(report['id'])[0])
        pytest.metainfo.update(report_url=report_url)
    except:
        pass
    task_id = filter(lambda item: item["type_name"] == "TASK_LOGS", result)[0]['task_id']
    try:
        task_url = 'https://sandbox.yandex-team.ru/sandbox/tasks/view?task_id={task_id}'.format(task_id=task_id)
        pytest.metainfo.update(task_url=task_url)
    except:
        pass
    status = Sandbox().GetTask(task_id)['status']

    assert status == "SUCCESS"
