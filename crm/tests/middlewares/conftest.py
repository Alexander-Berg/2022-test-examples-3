import pytest

from unittest.mock import AsyncMock


@pytest.fixture
async def handler():
    return AsyncMock(spec=[])
