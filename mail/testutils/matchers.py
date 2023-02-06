import re
from rest_framework import status


def assert_doesnt_contain_opens_counter(body):
    assert "{% opens_counter %}" not in body, "body contains opens_counter"


def assert_html_contains_attachments_publish_paths(letter):
    attachments = letter.attachments.all()
    for attachment in attachments:
        assert attachment.publish_path in letter.html_body
    assert len(attachments) == letter.html_body.count("https://avatars.mdst.yandex.net/")


def assert_lists_are_matching(actual_list, expected_list):
    assert len(actual_list) == len(expected_list)
    for dict_item in expected_list:
        assert_dict_in_list(dict_item, actual_list)


def assert_dict_in_list(dict_item, list_of_dicts):
    assert any([is_subdict(dict_item, list_dict) for list_dict in list_of_dicts])


def assert_validation_error(response, field, code):
    response_data = response.json()
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response_data["error"] == "validation"
    assert response_data["detail"][field] == code


def assert_resource_does_not_exist(response, field, code):
    response_data = response.json()
    assert response.status_code == status.HTTP_404_NOT_FOUND
    assert response_data["error"] == "resource_does_not_exist"
    assert response_data["detail"][field] == code


def assert_wrong_state_error(response, actual_state, required_state):
    response_data = response.json()
    assert response.status_code == status.HTTP_409_CONFLICT
    assert response_data["error"] == "wrong_state"
    assert response_data["detail"] == {
        "actual_state": actual_state,
        "required_state": required_state,
    }


def assert_not_ready_error(response, code):
    response_data = response.json()
    assert response.status_code == status.HTTP_409_CONFLICT
    assert response_data["error"] == "not_ready"
    assert response_data["detail"] == code


def assert_render_error(response, code, description=None):
    response_data = response.json()
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response_data["error"] == "render"
    assert response_data["detail"]["code"] == code
    if description is not None:
        assert response_data["detail"]["description"] == description


def assert_wrong_domain_error(response, code):
    response_data = response.json()
    assert response.status_code == status.HTTP_409_CONFLICT
    assert response_data["error"] == "wrong_domain"
    assert response_data["detail"] == code


def assert_wrong_login_error(response, code):
    response_data = response.json()
    assert response.status_code == status.HTTP_409_CONFLICT
    assert response_data["error"] == "wrong_login"
    assert response_data["detail"] == code


def assert_not_authenticated_error(response, code):
    response_data = response.json()
    assert response.status_code == status.HTTP_401_UNAUTHORIZED
    assert response_data["error"] == "not_authenticated"
    assert response_data["detail"] == code


def assert_forbidden_error(response, code):
    response_data = response.json()
    assert response.status_code == status.HTTP_403_FORBIDDEN
    assert response_data["error"] == "forbidden"
    assert response_data["detail"] == code


def assert_limit_reached_error(response, code):
    response_data = response.json()
    assert response.status_code == status.HTTP_429_TOO_MANY_REQUESTS
    assert response_data["error"] == "limit_reached"
    assert response_data["detail"] == code


def assert_status_code(response, code):
    assert response.status_code == code


def assert_invalid_emails_error(response, invalid_emails):
    response_data = response.json()
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response_data["error"] == "invalid_emails"
    assert response_data["detail"] == invalid_emails


def assert_contains_stat_pixel(body):
    assert re.search(
        '<img src="http.+" width="1" height="1" />', body
    ), "body doesn't contain stat_pixel"


def is_subdict(inner_dict, outer_dict):
    return inner_dict.items() <= outer_dict.items()
