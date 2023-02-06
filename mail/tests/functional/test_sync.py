import pytest


@pytest.mark.asyncio
async def test_sync(app, org, rands):
    r = await app.post(f'/api/v1/sync/{rands()}', json={'org_id': org.org_id})
    assert r.status == 200


@pytest.mark.asyncio
async def test_not_found(app, randn, rands):
    r = await app.post(f'/api/v1/sync/{rands()}', json={'org_id': randn()})
    assert r.status == 200
