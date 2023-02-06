import environ
import pytest

from travel.avia.subscriptions.app.api.app import App
from travel.avia.subscriptions.app.settings.config import for_environment
from travel.avia.subscriptions.app.settings.app import AppConfig


def config_mock():
    config_dict = {
        **for_environment('dev'),
        'SENDER_CAMPAIGN_SLUG': '',
        'SENDER_CAMPAIGN_SLUG_EXTENDED': '',
        'SENDER_SINGLE_OPT_IN_CAMPAIGN_SLUG': '',
        'SENDER_DOUBLE_OPT_IN_CAMPAIGN_SLUG': '',
    }

    config: AppConfig = environ.to_config(
        AppConfig,
        config_dict,
    )
    # Ставим настройки tvm, чтобы импортировался tvm
    config.tvm_client_id = 123
    config.blackbox_client = '224'

    return config


# так как внутри App создается PointKeyResolver,
# который пытается достать ресурсы из Sandbox,
# то нужно его замокать
@pytest.mark.usefixtures('point_key_resolver')
def test_create_app_with_tvm_client_id():
    App(config_mock())
