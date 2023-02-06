from copy import deepcopy
import json
import traceback
from datetime import datetime
from pytest import fixture
from unittest.mock import MagicMock, ANY, call
from tractor.crypto.fernet import Fernet
from tractor.crypto.versioned import VersionedKeyStorage
from tractor.models import Task
from tractor.mail.models import MailServerConnectionInfo, MailServerInfo, TaskType, UserInfo
from tractor.models import TaskWorkerStatus
from tractor.tests.fixtures.common import *
from tractor.util.dataclasses import construct_from_dict
from tractor.yandex_services.collectors import CollectorInfo
from tractor_mail.impl.prepare_worker import run_task, _unpack_task_input


USER_UID = "test_user_uid"
USER_SUID = "test_user_suid"
ADMIN_UID = "test_admin_uid"
POPID = "test_popid"

TASK_INPUT_FOR_NEW_USER = json.loads(
    """{
    "user": 
        {
            "login": "test_login",
            "first_name": "name",
            "last_name": "surname",
            "middle_name": "middle_name",
            "gender": "male", "birthday": "11.11.1111",
            "language": "en",
            "encrypted_external_password": "MTpjVldQNjFjRFkrVDVVMTJKZ2xrdlFBPT06pl+JsmZ2t49IDIKlg3i9lK1j8YMrb1Tf+1Qzx9eTD1g=",
            "encrypted_yandex_password": "MTo5ZE10d3JobmY4bjhYUmFheDBIeFNnPT06yrD+MZ+XvxJ/INQN6TNTjA=="
        },
    "mail_server_info": 
        {
            "provider": "google"
        },
    "admin_uid": "test_admin_uid"
}"""
)

TASK_INPUT_FOR_EXISTING_USER = json.loads(
    """{
    "user": 
        {
            "login": "test_login",
            "uid": "test_uid"
        }, 
    "mail_server_info": 
        {
            "provider": "google"
        }, 
    "admin_uid": "admin_test_uid"
}"""
)

USER_INFO = {
    "login": "test_login",
    "yandex_password": "yandex_password",
    "external_password": "external_password",
    "first_name": "name",
    "middle_name": "middle_name",
    "last_name": "surname",
    "gender": "male",
    "birthday": "11.11.1111",
    "language": "en",
}

COLLECTOR_INFO = CollectorInfo(
    bad_retries=0,
    error_status="ok",
    is_oauth=False,
    is_on="1",
    last_connect=str(datetime.today().timestamp()),
    last_msg_count="0",
    login=LOGIN,
    popid="",
    server_info=MailServerConnectionInfo(host="", port=80, ssl=False),
)


def _throw_test_exception(*args, **kwargs):
    raise RuntimeError("test exception")


def _make_collector_info(popid: str) -> CollectorInfo:
    collector_info = deepcopy(COLLECTOR_INFO)
    collector_info.popid = popid
    return collector_info


@fixture
def user_info():
    user_info = construct_from_dict(UserInfo, USER_INFO)
    user_info.uid = None
    return user_info


@fixture
def env():
    env = {
        "settings": MagicMock(),
        "db": MagicMock(),
        "logger": MagicMock(),
        "fernet": Fernet(VersionedKeyStorage(KEY_VERSIONS)),
        "directory": MagicMock(),
        "blackbox": MagicMock(),
        "collectors": MagicMock(),
    }
    env["directory"].create_user.return_value = USER_UID
    env["blackbox"].get_suid.return_value = USER_SUID
    env["collectors"].create.return_value = POPID

    yield env

    if env["logger"].exception.called:
        e = env["logger"].exception.call_args.kwargs["exception"]
        traceback.print_tb(e.__traceback__)


@fixture
def google_provider():
    return MailServerInfo(provider=GOOGLE_PROVIDER, conn_info=None)


@fixture
def task():
    return Task(
        task_id=TASK_ID,
        org_id=ORG_ID,
        domain=DOMAIN,
        created_ts=ANY,
        type=TaskType.PREPARE,
        input=TASK_INPUT_FOR_NEW_USER,
        canceled=False,
        worker_id=None,
        worker_status=TaskWorkerStatus.PENDING,
        worker_ts=ANY,
        worker_output="",
    )


def test_should_unpack_task_input_correctly(env, user_info, google_provider):
    dct_input = TASK_INPUT_FOR_NEW_USER
    (
        admin_uid_from_input,
        user_info_from_input,
        mail_server_info_from_input,
    ) = _unpack_task_input(env, dct_input)
    assert admin_uid_from_input == ADMIN_UID
    assert user_info_from_input == user_info
    assert mail_server_info_from_input == google_provider


def test_should_create_user_if_not_exists(task, env):
    run_task(task, env)
    env["directory"].create_user.assert_called_once()


def test_should_not_create_user_if_exists(task, env):
    task.input = TASK_INPUT_FOR_EXISTING_USER
    run_task(task, env)
    env["directory"].create_user.assert_not_called()


def test_should_create_new_collector_exactly_once(task, env):
    run_task(task, env)
    env["collectors"].create.assert_called_once()


def test_should_delete_old_collectors(task, env):
    env["collectors"].list.return_value = [
        _make_collector_info("1"),
        _make_collector_info("2"),
        _make_collector_info("3"),
    ]
    run_task(task, env)
    assert env["collectors"].delete.call_args_list == [
        call(ANY, ANY, "1"),
        call(ANY, ANY, "2"),
        call(ANY, ANY, "3"),
    ]


def test_trycatch_should_not_fail_task_if_no_exceptions_occurred(task, env):
    run_task(task, env)
    env["db"].fail_task.assert_not_called()


def test_should_fail_task_in_case_of_directory_error(task, env):
    env["directory"].create_user.side_effect = _throw_test_exception
    run_task(task, env)
    env["db"].fail_task.assert_called_once()


def test_should_fail_task_in_case_of_blackbox_error(task, env):
    env["blackbox"].get_suid.side_effect = _throw_test_exception
    run_task(task, env)
    env["db"].fail_task.assert_called_once()


def test_should_fail_task_in_case_of_collectors_create_error(task, env):
    env["collectors"].create.side_effect = _throw_test_exception
    run_task(task, env)
    env["db"].fail_task.assert_called_once()


def test_should_fail_task_in_case_of_collectors_list_error(task, env):
    env["collectors"].list.side_effect = _throw_test_exception
    run_task(task, env)
    env["db"].fail_task.assert_called_once()


def test_should_fail_task_in_case_of_db_finish_task_error(task, env):
    env["db"].finish_task.side_effect = _throw_test_exception
    run_task(task, env)
    env["db"].fail_task.assert_called_once()
