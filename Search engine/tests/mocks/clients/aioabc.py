from bot.aioabc import Member, Person, Role, Name, Scope
import typing as tp
from contextlib import contextmanager


class MockABC:
    def __init__(self):
        self.members: tp.Dict[str, tp.List[Member]] = {}

    @staticmethod
    def member(login: str, slug: str, telegram=None) -> Member:
        return Member(
            person=Person(id=0, name=None, login=login, telegram=telegram or login),
            role=Role(id=0, name=None, scope=Scope(id=0, slug=slug, name=None), code=slug)
        )

    def set_members(self, slug: str, members: tp.List[Member]):
        self.members[slug] = members

    async def list_members(self, service_slug: str = '', role_slug: str = ''):
        members = self.members.get(service_slug, [])
        if not role_slug:
            return members

        return list(filter(lambda s: s.role.scope.slug == role_slug, members))
