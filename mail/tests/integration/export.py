import yatest.common

from pytest_bdd import scenarios
from .steps import *  # noqa


scenarios(
    'export.feature',
    features_base_dir=yatest.common.source_path('mail/sheltie/tests/integration/feature')
)
