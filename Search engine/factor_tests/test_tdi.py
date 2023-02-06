__author__ = 'aokhotin'

import pytest
from devtools.fleur.util.sandbox import Sandbox

from runtime.simcity import task_runner


@pytest.mark.noapache
@pytest.mark.parametrize("test_id,config_attrs,test_name,task_params", [
    (3213, {"tdi_config": "1"}, "TDI", {}),
    (11760, {"tdi_config": "1"}, "TDI", {}),
    (11055, {"tdi_config": "1"}, "TDI", {}),
    (12690, {"tdi_external_config": "1"}, "TDI_EXTERNAL", {"timeout_responses": 3000}),
])
@pytest.mark.parametrize("beta_url", [
    pytest.config.option.beta,
])
@pytest.metainfo.set(name="{test_name}",
                     description="TDI Test {test_id} on {beta_url}")
def test_tdi(tmpdir, test_id, config_attrs, test_name, beta_url, task_params):
    test_config = Sandbox().FindLatestResource("FACTOR_CONFIG", "any", config_attrs)
    tdi_test_config = {
        "test_id": test_id,
        "beta_host": beta_url,
        "beta_cgi_params": "&exp_confs=testing&test-id={test_id}&exp_confs=testing&json_dump=unanswer_data".format(
            test_id=test_id),
        "config": test_config["id"],
        "use_random_requests": 3000,
        "req_resource_id": 0,
        "sandbox.gateway.get.LogFiles": "tdi.junit.xml",
    }
    tdi_test_config.update(task_params)
    description = "Test {test_id} on {beta_url}".format(test_id=test_id,
                                                        beta_url=beta_url)
    result = task_runner.run("TEST_TDI", tdi_test_config, "any", description, work_dir=tmpdir.strpath,
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
