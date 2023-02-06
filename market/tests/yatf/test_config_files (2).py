import os

import pytest
import yatest.common

from market.idx.input.mdm_dumper.yatf.test_envs.test_env import LB_DUMPER_BIN
from market.idx.yatf.utils.common_proxy import validate_config_file


@pytest.mark.parametrize(
    'env, dc',
    [
        ('testing', 'sas'),
        ('testing', 'vla'),
        ('production', 'sas'),
        ('production', 'vla'),
    ]
)
def test_config_files(env, dc):
    etc_dir = yatest.common.source_path(os.path.join('market', 'idx', 'input', 'mdm_dumper', 'etc'))

    conf = os.path.join(etc_dir, 'lbdumper.cfg')
    variable_files = [os.path.join(etc_dir, 'env', env)]

    lua_variables = {
        'PORT': 12345,
        'LBDUMPER_TVM_SECRET': 'lbdumper_tvm_secret',
        'DC': dc,
    }

    env_variables = {
        'LBDUMPER_TVM_SECRET': 'lbdumper_tvm_secret',
    }

    validate_config_file(LB_DUMPER_BIN, conf, variable_files, lua_variables, env_variables, 'logs/lbdumper')
