import yatest.common

from pytest_bdd import scenarios
from .steps import *  # noqa


scenarios(
    'contacts.feature',
    features_base_dir=yatest.common.source_path('mail/collie/tests/integration/feature')
)
