from mail.python.theatre.utils.derived_job import derived_class_in_qualname


class Base:
    async def coro(self):
        pass


class Derived(Base):
    async def derived(self):
        pass


def test_derived_job_base_coroutine_method():
    coro = derived_class_in_qualname(Derived().coro)
    try:
        assert repr(coro).startswith("<coroutine object Derived :: Base.coro at")
    finally:
        coro.close()


def test_derived_job_derived_coroutine_method():
    coro = derived_class_in_qualname(Derived().derived)
    try:
        assert repr(coro).startswith("<coroutine object Derived :: Derived.derived at")
    finally:
        coro.close()
