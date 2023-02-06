import pytest_bdd
import yatest.common
from .steps import *  # noqa

pytest_bdd.scenarios(yatest.common.source_path('mail/sharpei/tests/integration/feature/conninfo_blackbox.feature'))
