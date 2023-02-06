import random
import string
from typing import Hashable


__all__ = ('UserIDs', 'SupergroupIDs', 'Usernames', 'Titles')


class _ClassPool:
    use_pool = True
    values = set()

    @classmethod
    def generate(cls) -> Hashable:
        value = cls._generate()
        if cls.use_pool:
            while value in cls.values:
                value = cls._generate()

            cls.values.add(value)
        return value

    @classmethod
    def _generate(cls) -> Hashable:
        raise NotImplementedError

    @classmethod
    def insert(cls, value: Hashable) -> Hashable:
        if cls.use_pool:
            cls.values.add(value)

        return value


class _InstancePool:
    use_pool = True

    def __init__(self):
        self.values = set()

    def generate(self) -> Hashable:
        value = self._generate()
        if self.use_pool:
            while value in self.values:
                value = self._generate()

            self.values.add(value)

        return value

    def _generate(self) -> Hashable:
        raise NotImplementedError

    def insert(self, value: Hashable) -> Hashable:
        if self.use_pool:
            self.values.add(value)

        return value


class UserIDs(_ClassPool):
    @classmethod
    def _generate(cls) -> int:
        return random.randint(100000000, 999999999)


class SupergroupIDs(_ClassPool):
    @classmethod
    def _generate(cls) -> int:
        return -random.randint(100100000000, 100999999999)


class Usernames(_ClassPool):
    @classmethod
    def _generate(cls) -> str:
        return ''.join([random.choice(string.ascii_lowercase) for _ in range(10)])


class Titles(_ClassPool):
    use_pool = False

    @classmethod
    def _generate(cls) -> str:
        return ''.join([random.choice(string.ascii_lowercase) for _ in range(20)])


class MessageIDs(_InstancePool):
    def __init__(self):
        super().__init__()
        self.counter = 0

    def _generate(self) -> int:
        self.counter += 1
        return self.counter


class UpdateIDs(_ClassPool):
    counter = 1

    @classmethod
    def _generate(cls) -> int:
        cls.counter += 1
        return cls.counter
