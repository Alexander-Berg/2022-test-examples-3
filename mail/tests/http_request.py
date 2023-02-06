import random


def build_user(user):
    return {"uid": user.uid}


def build_storage(rcpt_id, message):
    result = {"stid": message.stids[rcpt_id] if rcpt_id in message.stids else message.stid}
    if rcpt_id in message.offset_diffs:
        result["offset"] = message.offset_diffs[rcpt_id]
    return result


def build_address(user):
    return {
        "local": user.local,
        "domain": user.domain,
        "display_name": user.display_name
    }


def build_headers(message):
    return {
        "recieved_date": message.received_date,
        "date": message.received_date,
        "subject": message.subject,
        "msg_id": message.message_id,
        "from": [build_address(message.from_user)],
        "to": [build_address(recipient) for recipient in message.recipients]
    }


def build_attachment(attachment):
    return {
        "hid": attachment.hid,
        "name": attachment.filename,
        "type": attachment.attachment_type,
        "size": attachment.size
    }


def build_mime_part(part):
    return {
        "hid": part.hid,
        "content_type": part.content_type,
        "content_subtype": part.content_subtype,
        "boundary": part.boundary,
        "name": part.name,
        "charset": part.charset,
        "encoding": part.encoding,
        "content_disposition": part.content_disposition,
        "file_name": part.filename,
        "content_id": part.cid,
        "offset": part.offset,
        "length": part.length
    }


def build_thread_info(thread_info):
    return {
        "hash": {
            "namespace": thread_info.hash_namespace,
            "value": "123"
        },
        "limits": {
            "days": 10,
            "count": 10
        },
        "rule": thread_info.merge_rule,
        "reference_hashes": thread_info.reference_hashes,
        "message_ids": [],
        "in_reply_to_hash": "",
        "message_id_hash": ""
    }


def build_message(rcpt_id, message):
    return {
        "firstline": message.firstline,
        "size": message.size,
        "lids": message.lids[rcpt_id] if rcpt_id in message.lids else list(),
        "label_symbols": message.label_symbols[rcpt_id] if rcpt_id in message.label_symbols else list(),
        "storage": build_storage(rcpt_id, message),
        "headers": build_headers(message),
        "labels" : [dict(name=label.name, type=label.label_type) for label in message.labels.get(rcpt_id, [])],
        "attachments": [build_attachment(attachment) for attachment in message.attachments],
        "mime_parts": [build_mime_part(part) for part in message.mime_parts],
        "thread_info": build_thread_info(message.thread_meta)
    }


def build_folders(rcpt_id, message):
    result = dict()

    result["original"] = dict()
    if rcpt_id in message.fid:
        result["original"]["fid"] = message.fid[rcpt_id]
    if rcpt_id in message.folder_path:
        result["original"]["path"] = {
            "path": message.folder_path[rcpt_id],
            "delimeter": "|"
        }

    # TODO: add tests for filters (MAILDLV-3165) for destination folder

    return result


def build_actions(rcpt_id, message):
    return {
        "duplicates": {
            "ignore": message.ignore_duplicates.get(rcpt_id, False),
            "remove": message.remove_duplicates.get(rcpt_id, False)
        },
        "use_filters": False,
        "disable_push": False,
        "original": {
            "store_as_deleted": False,
            "no_such_folder": message.no_such_folder_action.get(rcpt_id, "fallback_to_inbox")
        }
    }
    # TODO: add tests for filters (MAILDLV-3165) for rules_applied actions


def build_rcpt(rcpt_id, rcpt, message):
    return {
        "user": build_user(rcpt),
        "message": build_message(rcpt_id, message),
        "folders": build_folders(rcpt_id, message),
        "actions": build_actions(rcpt_id, message),
        "imap": message.imap[rcpt_id] if rcpt_id in message.imap else False
    }
    # TODO: add tests for filters (MAILDLV-3165) for added lids and symbols actions


def build_http_request_body(message):
    return {
        "rcpts": [dict(id=str(id), rcpt=build_rcpt(str(id), rcpt, message)) for id, rcpt in enumerate(message.recipients)],
        "sync": bool(random.getrandbits(1))
    }
