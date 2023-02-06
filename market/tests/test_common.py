import yatest
from pathlib import Path
from unittest.mock import patch
import market.sre.tools.market_alerts_configurator.lib.common as common


def test_get_config_content(args):
    config_file = yatest.common.source_path(
        'market/sre/tools/market_alerts_configurator/tests/data/configs/config.yaml')
    args.globals_dir = Path(config_file).parent.parent.joinpath('globals')
    config = common.get_config_content(args, config_file)
    return config


def test_get_config_content_with_subdir(args):
    with patch('market.sre.tools.market_alerts_configurator.lib.juggler_config.JugglerApi', return_value=None):
        managed_hosts = {}
        args.conf_dir = yatest.common.source_path(
            'market/sre/tools/market_alerts_configurator/tests/data/configs')
        args.globals_dir = Path(args.conf_dir).parent.joinpath('globals')
        common.process_configs(args, managed_hosts)
        assert managed_hosts == {'market.sre': {'market_sandbox', 'market_sandbox_dirs'}}
