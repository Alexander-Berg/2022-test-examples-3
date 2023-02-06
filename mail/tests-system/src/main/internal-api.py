from hamcrest import assert_that, contains_inanyorder, contains
from helpers.api import call_internal_api


def load_all_messages_by_chunks(uid, chunk_size, url, service_ticket):
    messages = []
    has_more_messages = True
    while has_more_messages:
        args = {"uid": uid, "mid": messages[-1][0] if len(messages) > 0 else 0, "count": chunk_size}
        data = call_internal_api(url, args, service_ticket)
        messages += data
        has_more_messages = len(data) == 2
    return messages


def test_folder(collectors_internal_url, dst_user, user_folders, service_ticket):
    args = {"uid": dst_user["uid"]}
    data = call_internal_api(collectors_internal_url + "/folders", args, service_ticket)
    assert_that(data, contains_inanyorder(*[folder + [False] for folder in user_folders]))


def test_pop3_folders(
    collectors_internal_url, dst_user, user_folders, pop3_enabled_fids, service_ticket
):
    args = {"uid": dst_user["uid"]}
    data = call_internal_api(collectors_internal_url + "/pop3_folders", args, service_ticket)
    assert_that(
        data,
        contains_inanyorder(
            *[folder + [folder[1] not in pop3_enabled_fids] for folder in user_folders]
        ),
    )


def test_labels(collectors_internal_url, dst_user, user_labels, service_ticket):
    args = {"uid": dst_user["uid"]}
    data = call_internal_api(collectors_internal_url + "/labels", args, service_ticket)
    assert_that(data, contains_inanyorder(*user_labels))


def test_messages(collectors_internal_url, dst_user, user_messages, service_ticket):
    messages = load_all_messages_by_chunks(
        dst_user["uid"], 2, collectors_internal_url + "/next_message_chunk", service_ticket
    )
    assert_that(messages, contains(*user_messages))


def test_pop3_messages(collectors_internal_url, dst_user, pop3_messages, service_ticket):
    messages = load_all_messages_by_chunks(
        dst_user["uid"], 2, collectors_internal_url + "/pop3_next_message_chunk", service_ticket
    )
    assert_that(messages, contains(*pop3_messages))
