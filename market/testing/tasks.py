"""
This module implements testing-related tasks.
"""

import abc

import edera.requisites

from edera.condition import Condition
from edera.coroutine import coroutine
from edera.helpers import Phony
from edera.parameterizable import Parameter
from edera.parameterizable import Parameterizable
from edera.qualifiers import Instance
from edera.task import Task
from edera.testing.scenarios import DefaultScenario
from edera.testing.scenarios import Scenario


class TestableTask(Task):
    """
    A task class that automatically annotates itself with "stub" and "tests".

    The "stub" annotation is taken from the "stub" attribute and defines the default stubbing
    scenario for the task.
    The "tests" annotation is taken from the "tests" attribute and defines the list of available
    testing scenarios for the task.
    In both cases, $DefaultScenario will be used if you don't provide a custom value.

    Attributes:
        stub (Scenario) - the default stubbing scenario
        tests (Iterable[Scenario]) - the list of available testing scenarios

    See also:
        $Stub
        $Test
    """

    @property
    def requisite(self):
        return [
            edera.requisites.annotate("stub", self.stub),
            edera.requisites.annotate("tests", self.tests),
        ]

    @property
    def stub(self):
        return DefaultScenario()

    @property
    def tests(self):
        return [] if self.execute is Phony else [DefaultScenario()]


class Test(Task, Parameterizable):
    """
    An abstract class for tests used to check the correctness of the task.

    Executes the $scenario for the $subject, and registers itself in the $cache if no errors
    occurred (meaning, the test passed).

    Attributes:
        cache (Storage) - a storage used to store passed tests
    """

    subject = Parameter(Instance[Task])
    scenario = Parameter(Instance[Scenario])

    @abc.abstractproperty
    def cache(self):
        pass

    @coroutine
    def execute(self, cc):
        cc.embrace(self.scenario.execute)(self.subject)
        if self.cache.get(self.name, limit=1):
            return
        self.cache.put(self.name, "!")

    @property
    def target(self):
        return TestPassed(test=self)


class TestPassed(Condition, Parameterizable):

    test = Parameter(Instance[Test])

    def check(self):
        return bool(self.test.cache.get(self.test.name, limit=1))


class Stub(Task, Parameterizable):
    """
    A stub for a task used to emulate the behaviour of the task.

    Executes the $scenario for the $subject.
    Shares its target with the $subject.

    Attributes:
        cache (Storage) - a storage used to store passed tests
    """

    subject = Parameter(Instance[Task])
    scenario = Parameter(Instance[Scenario])

    @coroutine
    def execute(self, cc):
        cc.embrace(self.scenario.execute)(self.subject)

    @property
    def target(self):
        return self.subject.target
