import edera.requisites

from edera import Condition
from edera import Task
from edera.workflow import WorkflowBuilder


class DemoTarget(Condition):

    def __init__(self, name):
        self.__name = name

    def check(self):
        return False

    @property
    def name(self):
        return self.__name


class DemoTask(Task):

    @property
    def name(self):
        return self.__class__.__name__

    @property
    def target(self):
        return DemoTarget(self.name)


class A(DemoTask):

    @property
    def requisite(self):
        return B()


class B(DemoTask):

    @property
    def requisite(self):
        return [C(), D()]


class C(DemoTask):

    @property
    def requisite(self):
        return {
            Z(): [B(), D()],
            E(): [A(), Z()],
        }


class D(DemoTask):

    pass


class E(DemoTask):

    pass


class Z(DemoTask):

    pass


def test_builder_expands_requisite_correctly():
    builder = WorkflowBuilder()
    graph = builder.expand(edera.requisites.include(A()))
    assert graph[A()].item == A()
    assert graph[A()].children == {E()}
    assert graph[A()].parents == {B()}
    assert graph[B()].item == B()
    assert graph[B()].children == {A(), Z()}
    assert graph[B()].parents == {C(), D()}
    assert graph[C()].item == C()
    assert graph[C()].children == {B()}
    assert not graph[C()].parents
    assert graph[D()].item == D()
    assert graph[D()].children == {B(), Z()}
    assert not graph[D()].parents
    assert graph[E()].item == E()
    assert not graph[E()].children
    assert graph[E()].parents == {A(), Z()}
    assert graph[Z()].item == Z()
    assert graph[Z()].children == {E()}
    assert graph[Z()].parents == {B(), D()}


def test_builder_builds_workflow_from_task_correctly():
    builder = WorkflowBuilder()
    workflow = builder.build(A())
    assert workflow[C()]["rank"] < workflow[B()]["rank"]
    assert workflow[D()]["rank"] < workflow[B()]["rank"]
    assert workflow[A()]["rank"] > workflow[B()]["rank"]
    assert workflow[Z()]["rank"] > workflow[B()]["rank"]
