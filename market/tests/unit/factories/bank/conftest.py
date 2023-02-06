import pytest

import yamarec1.config.loader

from yamarec1.factories.bank import BankFactory


@pytest.fixture
def bank_config():
    return yamarec1.config.loader.load_from_string(
        "my.items:\n"
        " kind = 'partitioned'\n"
        " layout:\n"
        "  kind = 'explicit'\n"
        "  elements:\n"
        "   model: __extend__(_partition)\n"
        "    layout.elements:\n"
        "     category: __extend__(_factor)\n"
        " _partition:\n"
        "   kind = 'factorized'\n"
        "   key = ('object_type', 'object_id')\n"
        "   mode = 'inner'\n"
        "   layout:\n"
        "    kind = 'explicit'\n"
        "   _factor:\n"
        "    kind = 'versioned'\n"
        "    layout:\n"
        "     kind = 'basic'\n"
        "     prefix = 'items'\n"
        "     suffix = '%s/%s' % (__up__(4).__key__, __up__.__key__)\n"
        "     mode = 'table'\n")


@pytest.fixture
def bank_factory(bank_config):
    return BankFactory(bank_config)
