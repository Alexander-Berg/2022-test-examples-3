import pytest
from helpers.fixtures import *
from mocks.fixtures import *
from helpers.api import call_internal_api


class Message:
    def __init__(self, folder, labels, body):
        self.folder = folder
        self.labels = labels
        self.body = body

    def __eq__(self, other):
        return (
            self.folder == other.folder and self.labels == other.labels and self.body == other.body
        )

    def __repr__(self):
        return (
            "\n{ folder = "
            + str(self.folder)
            + ",\n"
            + "labels = "
            + str(self.labels)
            + ",\n"
            + "body = "
            + str(self.body)
            + " }\n"
        )


@pytest.fixture()
def expected_message_bodies(storage_mock):
    return storage_mock.messages


@pytest.fixture()
def expected_labels(internal_api_mock):
    return internal_api_mock.labels


def fids_from_messages(messages):
    fid_index = 1
    fids = [messages[i][fid_index] for i in range(len(messages))]
    return fids


@pytest.fixture()
def expected_folders(internal_api_mock):
    fids = fids_from_messages(internal_api_mock.messages)
    expected = fids_to_folders(fids, internal_api_mock.folders_info)
    return expected


@pytest.fixture()
def expected_messages(expected_message_bodies, expected_labels, expected_folders):
    expected = []
    for i in range(len(expected_message_bodies)):
        message = Message(expected_folders[i], expected_labels[i], expected_message_bodies[i])
        expected.append(message)
    return expected


def get_result_messages(nw, dst_user, collectors_internal_url, service_ticket):
    result_labels = nw.labels
    result_message_bodies = nw.messages
    result_folders = fids_to_folders_proxy(
        nw.fids, dst_user, collectors_internal_url, service_ticket
    )

    result = []
    for i in range(len(result_message_bodies)):
        message = Message(result_folders[i], result_labels[i], result_message_bodies[i])
        result.append(message)
    return result


def fids_to_folders(fids, folders_info):
    fid_index = 1
    name_index = 2
    info_fids = [folders_info[i][fid_index] for i in range(len(folders_info))]
    info_names = [folders_info[i][name_index] for i in range(len(folders_info))]
    fid_2_name = dict(zip(info_fids, info_names))

    folders = [fid_2_name[fid] for fid in fids]
    return folders


def fids_to_folders_proxy(fids, dst_user, collectors_internal_url, service_ticket):
    folders_req_args = {"uid": dst_user["uid"]}
    folders_info = call_internal_api(
        collectors_internal_url + "/folders", folders_req_args, service_ticket
    )
    return fids_to_folders(fids, folders_info)


@pytest.fixture()
def folders_from_messages(user_messages, user_folders):
    fid_index = 1
    msg_fids = [user_messages[i][fid_index] for i in range(len(user_messages))]
    folders = fids_to_folders(msg_fids, user_folders)
    return folders
