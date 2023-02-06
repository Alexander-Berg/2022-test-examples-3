import os.path

import pytest

from edera import Condition
from edera import Parameter
from edera import Parameterizable
from edera import Task


class FileExists(Condition, Parameterizable):

    path = Parameter()

    def check(self):
        return os.path.exists(self.path)


class CreateFile(Task, Parameterizable):

    path = Parameter()

    def execute(self):
        with open(self.path, "w") as stream:
            stream.write("OK")

    @property
    def target(self):
        return FileExists(path=self.path)


class GetGravicapa(CreateFile):

    root = Parameter()

    @property
    def path(self):
        return os.path.join(self.root, "gravicapa.txt")


class GetPepelac(CreateFile):

    root = Parameter()

    @property
    def path(self):
        return os.path.join(self.root, "pepelac.txt")


class Fly(CreateFile):

    destination = Parameter()
    root = Parameter()

    def execute(self):
        with open(os.path.join(self.root, "gravicapa.txt"), "r") as stream:
            assert stream.read() == "OK"
        with open(os.path.join(self.root, "pepelac.txt"), "r") as stream:
            assert stream.read() == "OK"
        assert self.destination == "Earth"
        with open(self.path, "w") as stream:
            stream.write("KU")

    @property
    def path(self):
        return os.path.join(self.root, "flight.log")

    @property
    def requisite(self):
        yield GetGravicapa(root=self.root)
        yield GetPepelac(root=self.root)


class Prepare(CreateFile):

    destination = Parameter()
    root = Parameter()

    def execute(self):
        assert self.destination == "Earth"
        with open(self.path, "w") as stream:
            stream.write("QU")

    @property
    def path(self):
        return os.path.join(self.root, "preparation.log")


@pytest.fixture
def correct_fly_task(tmpdir):
    return Fly(root=str(tmpdir), destination="Earth")


@pytest.fixture
def correct_prepare_task(tmpdir):
    return Prepare(root=str(tmpdir), destination="Earth")


@pytest.fixture
def incorrect_fly_task(tmpdir):
    return Fly(root=str(tmpdir), destination="Mars")


@pytest.fixture
def incorrect_prepare_task(tmpdir):
    return Prepare(root=str(tmpdir), destination="Mars")
