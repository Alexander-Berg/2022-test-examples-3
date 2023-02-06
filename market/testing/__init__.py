"""
This subpackage implements various classes to auto-test workflows.
"""

from .scenarios import DefaultScenario
from .scenarios import Scenario
from .selectors import AllTestSelector
from .selectors import RegexTestSelector
from .selectors import TestSelector
from .tasks import Stub
from .tasks import Test
from .tasks import TestableTask
