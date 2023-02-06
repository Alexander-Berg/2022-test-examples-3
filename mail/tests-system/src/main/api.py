import pytest
from helpers import decode_global_collector_id
from helpers.api import ApiException, call_service_api
from helpers.passport import get_social_task_id
from hamcrest import (
    assert_that,
    contains,
    contains_inanyorder,
    has_entry,
    has_entries,
    has_property,
    equal_to,
)


@pytest.fixture(autouse=True)
def clean_collectors(rpop_api, collectors_api):
    for src in [rpop_api, collectors_api]:
        collectors = src.list()["rpops"]
        for c in collectors:
            src.delete(c["popid"])


@pytest.fixture
def root_folder_id():
    return "1"


@pytest.fixture
def label_id():
    return "1"


def test_create_new_collector_by_oauth(rpop_api, collectors_api, social_task_id):
    create_res = collectors_api.create_yandex(social_task_id)
    list_res = collectors_api.list()["rpops"]

    assert_that(list_res, contains(has_entry("popid", create_res["popid"])))
    assert_that(list_res, contains(has_entry("login", create_res["email"])))
    assert_that(list_res, contains(has_entry("email", create_res["email"])))
    assert_that(list_res, contains(has_entry("root_folder_id", "0")))
    assert_that(list_res, contains(has_entry("label_id", "0")))
    assert_that(list_res, contains(has_entry("is_on", "1")))

    assert_that(rpop_api.list(), has_entry("rpops", []))


def test_create_collector_to_folder_by_oauth(collectors_api, social_task_id, root_folder_id):
    collectors_api.create_yandex(social_task_id, root_folder_id=root_folder_id)
    list_res = collectors_api.list()["rpops"]
    assert_that(list_res, contains(has_entry("root_folder_id", root_folder_id)))


def test_create_collector_with_label_by_oauth(collectors_api, social_task_id, label_id):
    collectors_api.create_yandex(social_task_id, label_id=label_id)
    list_res = collectors_api.list()["rpops"]
    assert_that(list_res, contains(has_entry("label_id", label_id)))


def test_create_collector_with_wrong_app_by_oauth(
    collectors_api, src_user_login, test_users_password, label_id
):
    social_task_id_by_wrong_app = get_social_task_id(
        src_user_login, test_users_password, app_name="mailru-o2-mail"
    )  # need existing app
    with pytest.raises(ApiException) as e:
        collectors_api.create_yandex(social_task_id_by_wrong_app)
    assert_that(e.value, has_property("reason", equal_to("wrong_social_task")))


def test_delete_collector(collectors_api, social_task_id):
    create_res = collectors_api.create_yandex(social_task_id)
    collectors_api.delete(create_res["popid"])
    assert_that(collectors_api.list(), has_entry("rpops", []))


def test_disable_collector(collectors_api, social_task_id):
    create_res = collectors_api.create_yandex(social_task_id)
    collectors_api.enable(create_res["popid"], False)
    list_res = collectors_api.list()["rpops"]
    assert_that(list_res, contains(has_entry("is_on", "0")))

    collectors_api.enable(create_res["popid"], True)
    list_res = collectors_api.list()["rpops"]
    assert_that(list_res, contains(has_entry("is_on", "1")))


def test_edit_root_folder(collectors_api, social_task_id, root_folder_id):
    create_res = collectors_api.create_yandex(social_task_id)
    collectors_api.edit(create_res["popid"], root_folder_id=root_folder_id)
    list_res = collectors_api.list()["rpops"]
    assert_that(list_res, contains(has_entry("root_folder_id", root_folder_id)))


def test_edit_label(collectors_api, social_task_id, label_id):
    create_res = collectors_api.create_yandex(social_task_id)
    collectors_api.edit(create_res["popid"], label_id=label_id)
    list_res = collectors_api.list()["rpops"]
    assert_that(list_res, contains(has_entry("label_id", label_id)))


def test_edit_old(rpop_api, collectors_api, src_user_login, test_users_password, label_id):
    create_res = rpop_api.create(src_user_login, test_users_password)
    collectors_api.edit(create_res["popid"], label_id=label_id)
    list_res = collectors_api.list()["rpops"]
    assert_that(list_res, contains(has_entry("label_id", label_id)))


def test_edit_wrong_login(collectors_api, dst_user, social_task_id, test_users_password):
    create_res = collectors_api.create_yandex(social_task_id)
    with pytest.raises(ApiException) as e:
        collectors_api.edit(
            create_res["popid"], login=dst_user["login"], password=test_users_password
        )
    assert_that(e.value, has_property("reason", equal_to("wrong_login")))


def test_edit_bad_password(collectors_api, social_task_id, src_user_login, test_users_password):
    create_res = collectors_api.create_yandex(social_task_id)
    with pytest.raises(ApiException):
        collectors_api.edit(create_res["popid"], login=src_user_login, password="BadPassword")


def test_edit_bad_social_task(collectors_api, social_task_id):
    create_res = collectors_api.create_yandex(social_task_id)
    with pytest.raises(ApiException):
        collectors_api.edit(create_res["popid"], social_task_id="bad_task_id")


def test_edit_wrong_login_social_task(
    collectors_api, dst_user, test_users_password, social_task_id
):
    wrong_login_social_task = get_social_task_id(dst_user["login"], test_users_password)
    create_res = collectors_api.create_yandex(social_task_id)
    with pytest.raises(ApiException) as e:
        collectors_api.edit(create_res["popid"], social_task_id=wrong_login_social_task)
    assert_that(e.value, has_property("reason", equal_to("wrong_social_task")))


def test_list_old_collectors(rpop_api, collectors_api, src_user_login, test_users_password):
    popid = rpop_api.create(src_user_login, test_users_password)["popid"]
    for api in [rpop_api, collectors_api]:
        list_res = api.list()["rpops"]
        assert_that(list_res, contains(has_entry("popid", popid)))


def test_list_new_collectors(rpop_api, collectors_api, social_task_id):
    popid = collectors_api.create_yandex(social_task_id)["popid"]
    list_res = collectors_api.list()["rpops"]
    assert_that(list_res, contains(has_entry("popid", popid)))

    assert_that(rpop_api.list(), has_entry("rpops", []))


def test_list_mixed_collectors(
    rpop_api, collectors_api, src_user_login, test_users_password, social_task_id
):
    first_popid = rpop_api.create(src_user_login, test_users_password)["popid"]
    second_popid = collectors_api.create_yandex(social_task_id)["popid"]

    old_list_res = rpop_api.list()["rpops"]
    assert_that(old_list_res, contains(has_entry("popid", first_popid)))

    new_list_res = collectors_api.list()["rpops"]
    assert_that(
        new_list_res,
        contains_inanyorder(has_entry("popid", first_popid), has_entry("popid", second_popid)),
    )


def test_list_concrete_new_collector(collectors_api, src_users_logins, test_users_password):
    popids = []
    for login in src_users_logins:
        social_task_id = get_social_task_id(login, test_users_password)
        popids.append(collectors_api.create_yandex(social_task_id)["popid"])

    for popid in popids:
        list_res = collectors_api.list(popid)["rpops"]
        assert_that(list_res, contains(has_entry("popid", popid)))


def test_list_concrete_old_collector(
    rpop_api, collectors_api, src_users_logins, test_users_password
):
    popids = []
    for login in src_users_logins:
        popids.append(rpop_api.create(login, test_users_password)["popid"])

    for popid in popids:
        list_res = collectors_api.list(popid)["rpops"]
        assert_that(list_res, contains(has_entry("popid", popid)))


def reload_collector(collectors_service_url, collectors_api, uid):
    call_service_api(collectors_service_url + "/unload_user", {"uid": uid})
    collectors_api.list()  # implicitly loads user on owner node


def reset_auth_token(db_cursor, popid):
    uid, collector_id = decode_global_collector_id(popid)
    db_cursor.execute(
        """
        SELECT code.update_collectors_metadata(
            %s::code.uid,
            %s::code.collector_id,
            jsonb_build_object('auth_token', ''),
            false) as revision
        """,
        (uid, collector_id),
    )


def update_src_uid_in_maildb(db_cursor, popid, new_src_uid):
    uid, collector_id = decode_global_collector_id(popid)
    db_cursor.execute(
        """
        SELECT code.update_collectors_metadata(
            %s::code.uid,
            %s::code.collector_id,
            jsonb_build_object('src_uid', to_jsonb(%s::bigint)),
            false) as revision
    """,
        (uid, collector_id, new_src_uid),
    )


def test_delete_collector_from_non_existing_account(
    dst_user,
    dst_user_db_cursor,
    collectors_service_url,
    collectors_api,
    social_task_id,
    non_existing_uid,
):
    create_res = collectors_api.create_yandex(social_task_id)
    update_src_uid_in_maildb(dst_user_db_cursor, create_res["popid"], non_existing_uid)
    reload_collector(collectors_service_url, collectors_api, dst_user["uid"])

    collectors_api.delete(create_res["popid"])
    assert_that(collectors_api.list(), has_entry("rpops", []))


def test_list_collector_from_non_existing_account(
    dst_user,
    dst_user_db_cursor,
    collectors_service_url,
    collectors_api,
    social_task_id,
    non_existing_uid,
):
    create_res = collectors_api.create_yandex(social_task_id)
    update_src_uid_in_maildb(dst_user_db_cursor, create_res["popid"], non_existing_uid)
    reload_collector(collectors_service_url, collectors_api, dst_user["uid"])

    list_res = collectors_api.list()["rpops"]
    assert_that(list_res, contains(has_entries({"popid": create_res["popid"], "email": "DELETED"})))


def test_list_with_no_auth_data(
    dst_user,
    dst_user_db_cursor,
    collectors_service_url,
    collectors_api,
    social_task_id,
):
    create_res = collectors_api.create_yandex(social_task_id)
    reset_auth_token(dst_user_db_cursor, create_res["popid"])
    reload_collector(collectors_service_url, collectors_api, dst_user["uid"])

    list_res = collectors_api.list()["rpops"]
    assert_that(list_res, contains(has_entry("is_on", "3")))


def test_create_collector_from_himself(rpop_api, collectors_api, dst_user, test_users_password):
    with pytest.raises(ApiException) as e:
        task_id = get_social_task_id(dst_user["login"], test_users_password)
        collectors_api.create_yandex(task_id)
    assert_that(e.value, has_property("reason", equal_to("collector_from_himself")))


def test_create_duplicate_collector(rpop_api, collectors_api, social_task_id, test_users_password):
    collectors_api.create_yandex(social_task_id)
    with pytest.raises(ApiException) as e:
        collectors_api.create_yandex(social_task_id)
    assert_that(e.value, has_property("reason", equal_to("duplicate_collector")))
