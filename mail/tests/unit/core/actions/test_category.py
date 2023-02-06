import pytest

from hamcrest import assert_that, contains_inanyorder

from mail.payments.payments.core.actions.category import GetCategoryListAction


@pytest.mark.asyncio
async def test_get_service_list(storage, create_category):
    categories = (
        await create_category(),
        await create_category(),
    )
    assert_that(
        await GetCategoryListAction().run(),
        contains_inanyorder(*categories),
    )
