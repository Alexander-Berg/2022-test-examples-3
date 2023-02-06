import pytest

from hamcrest import assert_that, contains, ends_with, has_entries


@pytest.mark.asyncio
async def test__create(trust_client, uid, acquirer):
    data = {'key': 'value'}
    await trust_client._partner_create(uid=uid, acquirer=acquirer, partner_data=data)
    assert_that(
        (trust_client.call_args[1], trust_client.call_args[2], trust_client.call_kwargs),
        contains(
            'POST',
            ends_with('/partners'),
            has_entries({
                'json': data,
                'uid': uid,
                'acquirer': acquirer,
            }),
        )
    )


@pytest.mark.asyncio
async def test__get(trust_client, uid, acquirer, randn):
    partner_id = randn()
    await trust_client._partner_get(uid=uid, acquirer=acquirer, partner_id=partner_id)
    assert_that(
        (trust_client.call_args[1], trust_client.call_args[2], trust_client.call_kwargs),
        contains(
            'GET',
            ends_with(f'/partners/{partner_id}'),
            has_entries({
                'uid': uid,
                'acquirer': acquirer,
            })
        ),
    )
