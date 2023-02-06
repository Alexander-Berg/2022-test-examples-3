import pytest


class BaseTestSyncAction:
    @pytest.fixture
    def all_generated_numbers(self):
        return set()

    @pytest.fixture
    def generate_set_of_numbers(self, randn, all_generated_numbers):
        async def _inner(*args, **kwargs):
            affected_uids = {randn() for _ in range(5)}
            all_generated_numbers.update(affected_uids)
            return affected_uids

        return _inner
