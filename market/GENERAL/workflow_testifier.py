"""
This module implements a $WorkflowTestifier class.
"""

import logging

import edera.helpers

from edera.clusterizers import GreedyClusterizer
from edera.condition import ConditionWrapper
from edera.heap import Heap
from edera.linearizers import DFSLinearizer
from edera.task import TaskWrapper
from edera.testing import AllTestSelector
from edera.testing import Stub
from edera.testing import Test
from edera.workflow.processor import WorkflowProcessor


class WorkflowTestifier(WorkflowProcessor):
    """
    A workflow processor that transforms the workflow into a workflow that auto-tests the original
    workflow.

    It also clusterizes the resulting tests into independent groups, and then assigns different
    colors to different groups.

    WARNING!
        Doesn't preserve annotations, so consider applying this processor before others.

    See also:
        $TestableTask
        $TestingWorkflowExecuter
    """

    def __init__(self, cache, selector=AllTestSelector(), clusterizer=GreedyClusterizer()):
        """
        Args:
            cache (Storage) - a storage used to store passed tests
                Can be safely shared with the one used for $TargetCacher.
            selector (TestSelector) - a test selector to use
                Default is "all available".
            clusterizer (Clusterizer) - a clusterizer used to group tests and stubs
                Default is the greedy clusterizer.
        """
        self.__Test = type.__new__(type, "Test", (Test,), {"cache": cache})
        self.__selector = selector
        self.__clusterizer = clusterizer

    def process(self, workflow):
        tests = [
            self.__Test(scenario=scenario, subject=task)
            for task in sorted(workflow)
            for scenario in self.__selector.select(workflow, task)
        ]
        logging.getLogger(__name__).debug("Collected %d tests", len(tests))
        ranks = self.__get_task_ranks(workflow)
        replacements = {test: self.__get_replacement(test, workflow, ranks) for test in tests}
        clusters = self.__clusterizer.clusterize(replacements)
        logging.getLogger(__name__).debug("Split tests into %d clusters", len(clusters))
        origin = workflow.clone()
        workflow.remove(*workflow)
        for cluster in clusters:
            self.__project_workflow(origin, cluster, workflow)

    def _generate_color(self, cluster):
        """
        Generate a "color" for a cluster of tests.

        It must be stable to permutations.

        Args:
            cluster (Cluster) - a cluster of tests to color

        Returns:
            String - the alphanumeric color identifier
        """
        return edera.helpers.sha1("\n".join(sorted(test.name for test in cluster.items)))[:6]

    def __get_replacement(self, test, workflow, ranks):
        result = {test.subject: test}
        restrictions = dict(test.scenario.restrictions)
        heap = Heap()
        collector = set()
        for parent in workflow[test.subject].parents:
            heap.push(parent, ranks[parent])
            collector.add(parent)
        while heap:
            task = heap.pop()
            scenario = restrictions.get(task, workflow[task]["stub"])
            result[task] = Stub(scenario=scenario, subject=task)
            if scenario.restrictions is None:
                continue
            restrictions.update(scenario.restrictions)
            for parent in workflow[task].parents:
                if parent in collector:
                    continue
                heap.push(parent, ranks[parent])
                collector.add(parent)
        return result

    def __get_task_ranks(self, workflow):
        return {task: rank for rank, task in enumerate(DFSLinearizer().linearize(workflow))}

    def __project_workflow(self, workflow, cluster, result):

        def incorporate(task, color):
            fake = SuffixingTaskWrapper(cluster.mapping[task], "@" + color)
            if fake in result:
                return fake
            result.add(fake)
            result[fake]["color"] = color
            for parent in workflow[task].parents:
                if parent not in cluster.mapping:
                    continue
                result.link(incorporate(parent, color), fake)
            return fake

        color = self._generate_color(cluster)
        for test in cluster.items:
            incorporate(test.subject, color)


class SuffixingTaskWrapper(TaskWrapper):
    """
    A task wrapper that appends a given suffix to the names of the base task and its target.
    """

    def __init__(self, base, suffix):
        """
        Args:
            suffix (String) - a suffix to append
        """
        TaskWrapper.__init__(self, base)
        self.__suffix = suffix

    @property
    def name(self):
        return super(SuffixingTaskWrapper, self).name + self.__suffix

    @property
    def target(self):
        if super(SuffixingTaskWrapper, self).target is None:
            return None
        return SuffixingConditionWrapper(super(SuffixingTaskWrapper, self).target, self.__suffix)


class SuffixingConditionWrapper(ConditionWrapper):
    """
    A condition wrapper that appends a given suffix to the name of the base condition.
    """

    def __init__(self, base, suffix):
        """
        Args:
            suffix (String) - a suffix to append
        """
        ConditionWrapper.__init__(self, base)
        self.__suffix = suffix

    @property
    def name(self):
        return super(SuffixingConditionWrapper, self).name + self.__suffix
