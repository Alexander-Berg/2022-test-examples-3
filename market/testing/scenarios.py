"""
This module declares and implements a $Scenario interface.
"""

import abc

from edera.coroutine import coroutine
from edera.nameable import Nameable
from edera.parameterizable import Parameterizable


class Scenario(Nameable):
    """
    A faking scenario used for testing purposes.

    Scenarios are applied to tasks (called "subjects") in order to change their behaviour
    for testing purposes, e.g. to emulate or to check something after execution.

    Attributes:
        restrictions (Optional[Mapping[Task, Scenario]]) - the restrictions imposed on stubbing
                scenarios for other tasks (usually, the ones the subject depends on)
            If $None, there are no actual dependencies, and the scenario is self-contained.
    """

    @abc.abstractmethod
    def execute(self, subject):
        """
        Stage the scenario for the subject task.

        Args:
            subject (Task)
        """

    @abc.abstractproperty
    def restrictions(self):
        pass


class DefaultScenario(Scenario, Parameterizable):
    """
    The most common and simple implementation of a scenario.

    Executes the subject itself, assuming default stubbing scenarios for all its dependencies.
    """

    @coroutine
    def execute(self, cc, subject):
        cc.embrace(subject.execute)()

    @property
    def restrictions(self):
        return {}
