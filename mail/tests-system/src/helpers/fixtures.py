import pytest
from helpers.api import Api
from helpers.mdb import get_conninfo_by_uid
from helpers.passport import userinfo_by_login, get_social_task_id
import ticket_parser2.api.v1 as tp2
import psycopg2


@pytest.fixture(scope="session")
def rpop_host():
    return "http://collectors-ext-testing.stable.qloud-b.yandex.net"


@pytest.fixture(scope="session")
def collectors_url():
    return "http://localhost:3048"


@pytest.fixture(scope="session")
def collectors_internal_url():
    return "http://localhost:5048"


@pytest.fixture(scope="session")
def collectors_service_url():
    return "http://localhost:8080"


@pytest.fixture(scope="session")
def src_user_login(request):
    return "collectors-src-user@yandex.ru"


@pytest.fixture(scope="session")
def src_users_logins(request):
    return [
        "collectors-src-user@yandex.ru",
        "collectors-src-user2@yandex.ru",
        "collectors-src-user3@yandex.ru",
    ]


@pytest.fixture(scope="session")
def test_users_password(request):
    return "simple12345"  # for all tests users


@pytest.fixture(scope="session")
def tvm_client():
    CLIENT_ID = 2025680
    INTERNAL_API_ID = 2013998
    CLIENT_SECRET_FILE = "etc/collectors/tvm_secret_ext"

    with open(CLIENT_SECRET_FILE, "r") as f:
        secret = f.read()
    return tp2.TvmClient(
        tp2.TvmApiClientSettings(
            self_client_id=CLIENT_ID,
            self_secret=secret,
            enable_service_ticket_checking=True,
            dsts={"internal_api": INTERNAL_API_ID},
        )
    )


@pytest.fixture(scope="session")
def service_ticket(tvm_client):
    return tvm_client.get_service_ticket_for("internal_api")


@pytest.fixture(scope="module")
def dst_user(request):
    name_parts = request.node.name.split(".")[0:-1]
    name = "-".join(name_parts)
    login = "collectors-test-" + name + "@yandex.ru"
    return userinfo_by_login(login)


@pytest.fixture(scope="module")
def rpop_api(rpop_host, dst_user):
    return Api(rpop_host, dst_user)


@pytest.fixture(scope="module")
def collectors_api(collectors_url, dst_user):
    return Api(collectors_url, dst_user)


@pytest.fixture(scope="module")
def social_task_id(src_user_login, test_users_password):
    return get_social_task_id(src_user_login, test_users_password)


@pytest.fixture(scope="module")
def dst_user_db_cursor(dst_user):
    conninfo = get_conninfo_by_uid(dst_user["uid"])
    with psycopg2.connect(conninfo) as conn:
        conn.autocommit = True
        yield conn.cursor()


@pytest.fixture
def non_existing_uid():
    return "9223372036854775807"


@pytest.fixture
def user_folders():
    return [
        ["0", "1", "Inbox", "inbox"],
        ["0", "2", "Spam", "spam"],
        ["0", "3", "Trash", "trash"],
        ["0", "4", "Sent", "sent"],
        ["0", "5", "Outbox", "outbox"],
        ["0", "6", "Drafts", "draft"],
        ["0", "7", "FirstFolder", ""],
        ["0", "8", "SecondFolder", ""],
        ["7", "9", "FirstChildFolder", ""],
        ["8", "10", "SecondChildFolder", ""],
    ]


@pytest.fixture
def pop3_enabled_fids():
    return ["1"]


@pytest.fixture
def user_labels():
    return [
        ["1", "", "", "12", "so"],
        ["10", "", "", "55", "so"],
        ["11", "", "encrypted_label", "encrypted", "system"],
        ["12", "3262267", "", "GreenUserLabel", "user"],
        ["13", "16727856", "", "RedUserLabel", "user"],
        ["14", "5275879", "", "BlueUserLabel", "user"],
        ["15", "", "delayed_message", "delayed_message", "system"],
        ["16", "", "undo_message", "undo_message", "system"],
        ["2", "", "answered_label", "answered", "system"],
        ["3", "", "draft_label", "draft", "system"],
        ["4", "", "forwarded_label", "forwarded", "system"],
        ["5", "", "mute_label", "mute", "system"],
        ["6", "", "pinned_label", "pinned", "system"],
        ["7", "", "important_label", "priority_high", "system"],
        ["8", "", "remindNoAnswer_label", "remindme_threadabout:mark", "system"],
        ["9", "", "", "4", "so"],
        ["FAKE_APPEND_LBL", "", "append_label", "FAKE_APPEND_LBL", "system"],
        ["FAKE_ATTACHED_LBL", "", "attached_label", "FAKE_ATTACHED_LBL", "system"],
        ["FAKE_COPY_LBL", "", "copy_label", "FAKE_COPY_LBL", "system"],
        ["FAKE_DELETED_LBL", "", "deleted_label", "FAKE_DELETED_LBL", "system"],
        ["FAKE_MULCA_SHARED_LBL", "", "mulcaShared_label", "FAKE_MULCA_SHARED_LBL", "system"],
        ["FAKE_POSTMASTER_LBL", "", "postmaster_label", "FAKE_POSTMASTER_LBL", "system"],
        ["FAKE_RECENT_LBL", "", "recent_label", "FAKE_RECENT_LBL", "system"],
        ["FAKE_SEEN_LBL", "", "seen_label", "FAKE_SEEN_LBL", "system"],
        ["FAKE_SPAM_LBL", "", "spam_label", "FAKE_SPAM_LBL", "system"],
        ["FAKE_SYNCED_LBL", "", "synced_label", "FAKE_SYNCED_LBL", "system"],
    ]


@pytest.fixture
def user_messages():
    return [
        [
            "169729410956525569",
            "1",
            "320.66466005.E898810:235582362097107726624968151582",
            1563193219,
            [
                ["1", "", "", "12", "so"],
                ["FAKE_RECENT_LBL", "", "recent_label", "FAKE_RECENT_LBL", "system"],
                ["FAKE_SEEN_LBL", "", "seen_label", "FAKE_SEEN_LBL", "system"],
            ],
            "<20110815165837.A26162B2802A@yaback1.mail.yandex.net>",
        ],
        [
            "169729410956525570",
            "1",
            "320.66466005.E371039:3098331666134196037936401057644",
            1563193219,
            [
                ["1", "", "", "12", "so"],
                ["FAKE_RECENT_LBL", "", "recent_label", "FAKE_RECENT_LBL", "system"],
                ["FAKE_SEEN_LBL", "", "seen_label", "FAKE_SEEN_LBL", "system"],
            ],
            "<236710bb-3652-4b96-9d0f-f30a09da9bad@robots.yandex.ru>",
        ],
        [
            "169729410956525571",
            "1",
            "320.mail:0.E4192:3118342638122113136722065208406",
            1563193632,
            [
                ["10", "", "", "55", "so"],
                ["9", "", "", "4", "so"],
                [
                    "FAKE_MULCA_SHARED_LBL",
                    "",
                    "mulcaShared_label",
                    "FAKE_MULCA_SHARED_LBL",
                    "system",
                ],
                ["FAKE_RECENT_LBL", "", "recent_label", "FAKE_RECENT_LBL", "system"],
                ["FAKE_SEEN_LBL", "", "seen_label", "FAKE_SEEN_LBL", "system"],
            ],
            "<98301563193632@myt6-0cc77672de49.qloud-c.yandex.net>",
        ],
        [
            "169729410956525573",
            "4",
            "320.mail:0.E3960:3118342638150318306323444685000",
            1563209150,
            [
                ["10", "", "", "55", "so"],
                ["9", "", "", "4", "so"],
                [
                    "FAKE_MULCA_SHARED_LBL",
                    "",
                    "mulcaShared_label",
                    "FAKE_MULCA_SHARED_LBL",
                    "system",
                ],
                ["FAKE_RECENT_LBL", "", "recent_label", "FAKE_RECENT_LBL", "system"],
                ["FAKE_SEEN_LBL", "", "seen_label", "FAKE_SEEN_LBL", "system"],
            ],
            "<107271563209150@myt6-0cc77672de49.qloud-c.yandex.net>",
        ],
    ]


@pytest.fixture
def user_messages_bodies():
    return ["message 1", "message 2", "message 3", "message 4"]


@pytest.fixture
def pop3_messages(user_messages, pop3_enabled_fids):
    return [msg for msg in user_messages if msg[1] in pop3_enabled_fids]


def format_labels(labels):
    formatted = {"imap": [], "lids": [], "symbol": [], "system": []}
    lid_index = 0
    symbol_index = 2

    for label in labels:
        if label[symbol_index]:
            formatted["symbol"].append(label[symbol_index])
        else:
            formatted["lids"].append(label[lid_index])

    return formatted


@pytest.fixture()
def labels_from_messages(user_messages):
    labels_index = 4
    labels = [user_messages[i][labels_index] for i in range(len(user_messages))]
    formatted = []
    for per_msg_labels in labels:
        formatted.append(format_labels(per_msg_labels))
    return formatted
