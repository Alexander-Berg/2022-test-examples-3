
async def test_ping():
    assert True


# /// [Functional test]
async def test_hello(service_client):
    response = await service_client.get('/hello')
    assert response.status == 200
    assert response.content == b'Hello world!\n'
    # /// [Functional test]
