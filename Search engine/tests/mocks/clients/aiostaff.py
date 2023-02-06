import typing as tp
from contextlib import contextmanager


class MockStaff:
    def __init__(self):
        self.data: tp.List[dict] = dict()
        self.chiefs: tp.Dict[str, str] = dict()

    def set_data(self, persons: tp.List[dict]):
        self.data = {p['login']: p for p in persons}

    def set_chief(self, employee: str, chief: str):
        self.chiefs[employee] = chief

    async def list_chiefs(self, login: str) -> tp.List[str]:
        chiefs = []
        chief = self.chiefs.get(login)
        while chief:
            chiefs.append(chief)
            chief = self.chiefs.get(chief)

        return chiefs

    async def get_person(self, *args, **kwargs):
        pass

    async def get_all_persons(self, *args, **kwargs):
        pass

    async def get_department(self, *args, **kwargs):
        pass
