import ConfigParser
import pytest
from yatest.common import source_path

from core.loadconfig import get_config
from core.loadconfig import get_run_params


class TestLoadConfig(object):

    @staticmethod
    def test_get_config():
        config = get_config(source_path('market/sre/services/cs-dashboard/pydashie/tests/files/cs-dashboard.conf'))
        assert isinstance(config, ConfigParser.ConfigParser)
        assert config.get('golem', 'sampler') == 'samplers.custom_samplers.GolemSampler'
        assert config.get('conductor', 'conductor_age') == '96'

    @staticmethod
    def test_get_config_fail():
        with pytest.raises(IOError) as exc_info:
            get_config('files/doesnt_exists_path_ofconfig')
        assert str(exc_info.value) == 'File files/doesnt_exists_path_ofconfig doesn\'t exists!'

    @staticmethod
    def test_get_run_params():
        params = get_run_params(get_config(
            source_path('market/sre/services/cs-dashboard/pydashie/tests/files/cs-dashboard.conf')))
        assert isinstance(params, dict)
        assert len(params) == 6
        assert isinstance(params['port'], int)
        assert isinstance(params['host'], str)
        assert isinstance(params['debug'], bool)
