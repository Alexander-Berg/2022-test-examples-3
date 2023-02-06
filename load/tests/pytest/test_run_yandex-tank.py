import yatest.common
import load.projects.yatool_perf_test.lib.yandex_tank_module as yandex_tank_module


def write_html_report(upload_id):
    report_html = '''
    <html>
    <p>You will be redirected to Datalens report</p>
    <script>
       window.location.replace("https://datalens.yandex-team.ru/u4wjkycfj2lup-perfreport?reportId=%d")
    </script>
    </html>
    '''
    path = yatest.common.output_path("datalens.html")
    with open(path, "w") as f:
        f.write(report_html % upload_id)

    return path


def test_const(links):
    config = yatest.common.source_path("load/projects/tank_finder/tests/pytest/load.yaml")
    sla_conf = yatest.common.source_path("load/projects/tank_finder/tests/pytest/sla.yaml")

    res_code, sla, upload_id = yandex_tank_module.run_yandex_tank(config, sla_conf)

    if upload_id is not None:
        links.set("Datalens", write_html_report(upload_id))

    assert res_code == 0
    assert sla


def test_pandora_const(links):
    config = yatest.common.source_path("load/projects/tank_finder/tests/pytest/pandora.yaml")
    sla_conf = yatest.common.source_path("load/projects/tank_finder/tests/pytest/sla.yaml")
    gun = 'pandora'

    res_code, sla, upload_id = yandex_tank_module.run_yandex_tank(config, sla_conf, gun)

    if upload_id is not None:
        links.set("Datalens", write_html_report(upload_id))

    assert res_code == 0
    assert sla


def test_pandora_alone_const(links):
    config = yatest.common.source_path("load/projects/tank_finder/tests/pytest/load_alone.yaml")
    pandora_config = yatest.common.source_path("load/projects/tank_finder/tests/pytest/pandora_alone.yaml")
    sla_conf = yatest.common.source_path("load/projects/tank_finder/tests/pytest/sla.yaml")

    pandora_path = 'load/projects/yandex-tank-package/pandora'
    gun = 'pandora'

    res_code, sla, upload_id = yandex_tank_module.run_yandex_tank(config, sla_conf, gun, pandora_path, pandora_config)

    if upload_id is not None:
        links.set("Datalens", write_html_report(upload_id))

    assert res_code == 0
    assert sla
