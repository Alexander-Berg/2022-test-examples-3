import pytest

import yamarec1.config.loader

from yamarec1.factories.template import TemplateFactory


@pytest.fixture
def config():
    return yamarec1.config.loader.load_from_string(
        "loader:\n"
        " class = 'PackageLoader'\n"
        " package_name = 'factories.template'\n"
        " package_path = 'examples'\n"
        "extensions = \\\n"
        " [\n"
        "  'yamarec1.factories.template.extensions.PreambleTagSupport',\n"
        "  'yamarec1.factories.template.extensions.AttachmentTagSupport',\n"
        "  'yamarec1.factories.template.extensions.UDFTagSupport',\n"
        "  'yamarec1.factories.template.extensions.DataTagSupport',\n"
        "  'yamarec1.factories.template.extensions.TableTagSupport',\n"
        " ]\n")


@pytest.fixture
def factory(config):
    return TemplateFactory.from_config(config)
