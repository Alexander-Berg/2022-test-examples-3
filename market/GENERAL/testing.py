"""
This module implements a $TestingWorkflowExecuter class.
"""

import multiprocessing
import os

from edera.coroutine import coroutine
from edera.exceptions import ExcusableError
from edera.helpers import CurrentException
from edera.invokers import MultiProcessInvoker
from edera.workflow.executer import WorkflowExecuter


class TestingWorkflowExecuter(WorkflowExecuter):
    """
    A workflow executer that uses the "color" annotation to execute parts of the workflow
    in separate forks with different values of the "EDERA_TEST_CLUSTER_COLOR" environment
    variable.

    First, you need apply a $WorkflowTestifier at the building step.
    It will transform your workflow into a new workflow with only tests and stubs.
    It will also color each task, and moreover, will color conflicting tasks differently.

    Now, differently colored tasks will be executed in different forks.
    This allows you to reconfigure your application to work within a temporary (color-dependent)
    subenvironment.

    WARNING!
        In order for it to work correctly, task parameters must not depend on the environment.
        Employ environment-dependent stuff only within task execution and target checks.

    See also:
        $WorkflowTestifier
    """

    def __init__(self, base):
        """
        Args:
            base (WorkflowExecuter) - a base workflow executer
        """
        self.__base = base

    @coroutine
    def execute(self, cc, workflow):
        error = None
        for color in {workflow[task]["color"] for task in workflow}:
            foreigners = {task for task in workflow if workflow[task]["color"] != color}
            cluster_workflow = workflow.clone()
            cluster_workflow.remove(*foreigners)
            try:
                self.__fork_and_execute[cc](color, cluster_workflow)
            except ExcusableError:
                error = CurrentException()
        if error is not None:
            error.reraise()

    @coroutine
    def __fork_and_execute(self, cc, color, workflow):

        def execute():
            os.environ["EDERA_TEST_CLUSTER_COLOR"] = color
            cc.embrace(self.__base.execute)(workflow)

        name = "%s@%s" % (multiprocessing.current_process().name, color)
        MultiProcessInvoker({name: execute}).invoke[cc]()
