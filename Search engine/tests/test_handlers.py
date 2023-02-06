import pytest
import yatest.common

from mock import Mock

from search.tools.woland.lib.application import WolandApplication
from search.tools.woland.lib import handlers

panels_path = yatest.common.source_path('search/tools/woland/panels')

application = WolandApplication(panels_path, '', '')


class MockRequest():
    def __init__(self, path, method='GET'):
        self.path = path
        self.method = method
        self.connection = Mock()


@pytest.mark.parametrize('panel_id', application.panels_loader.get_panels_without_snippets().keys())
def test_panel_handler(panel_id):
    request = MockRequest(panel_id)
    panel_handler = handlers.PanelHandler(application, request)

    panel_handler.render = Mock()

    panel_handler.get(panel_id)
    assert len(panel_handler.render.call_args_list) == 1, f"Panel {panel_id} can't be rendered. Maybe it's a snippet, not a panel?"
