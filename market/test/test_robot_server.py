def test_success_post_inventory(unset_robot_server, fastapi_client, inventory_task):
    response = fastapi_client.post("/task/inventory", json=inventory_task)
    assert response.status_code == 200
    assert response.json() == {'id': '0'}


def test_unsuccess_post_inventory(set_robot_server, fastapi_client, inventory_task):
    response = fastapi_client.post("/task/inventory", json=inventory_task)
    assert response.status_code == 400
    assert response.json() == {'detail': 'The robot already has task'}
