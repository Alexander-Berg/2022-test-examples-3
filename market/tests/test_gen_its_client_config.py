from hamcrest import assert_that, has_entry

import yaml
import yatest.common


def run_script(args=None, env=None):
    cmd = [
        'bash',
        yatest.common.source_path('market/idx/datacamp/dev/its-yd/gen_its_client_config.sh')
    ]
    if args:
        cmd.extend(args)

    yatest.common.canonical_execute(cmd, env=env, cwd=yatest.common.output_path(), save_locally=True)

    with open(yatest.common.output_path('app/conf/its_client/cfg.yaml'), 'r') as fd:
        return yaml.load(fd)


def test_gen_service_id_by_env():
    cfg = run_script(env={'DEPLOY_STAGE_ID': 'testing_market_datacamp-miner', 'DEPLOY_NODE_DC': 'sas'})
    assert_that(cfg, has_entry('its_client', has_entry('service_id', 'testing_market_datacamp_miner_sas')))


def test_service_id_from_arg():
    cfg = run_script(args=['--its-service-id', 'hardcoded_service_id'])
    assert_that(cfg, has_entry('its_client', has_entry('service_id', 'hardcoded_service_id')))
