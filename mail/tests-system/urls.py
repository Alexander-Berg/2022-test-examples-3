import urllib

XENO_HOST = "http://localhost:4080"
XENO_INTERNAL_HOST = "http://localhost:8080"
SHARPEI_HOST = "http://sharpei-testing.mail.yandex.net:80"
PASSPORT_HOST = "https://pass-test.yandex.ru"
PASSPORT_INTERNAL_HOST = "https://passport-test-internal.yandex.ru"
SOCIAL_API_HOST = "https://api.social-test.yandex.ru"
SOCIAL_HOST = "https://social-test.yandex.ru"

OAUTH_HOST = "https://oauth-test.yandex.ru"
STATPORT = 8091
CACHE_PORT = 8093

FAKE_NODE_INFO = {"mobile_api_port": 14080, "internal_port": 18080}


def sharpei_url(uid):
    return "{}/conninfo?format=json&mode=master&uid={}".format(SHARPEI_HOST, uid)


def get_or_create_mailish_url():
    return "{}/1/bundle/account/get_or_create/mailish/?consumer=mail".format(PASSPORT_INTERNAL_HOST)


def auth_by_password_url(user, password, client_id, client_secret, device_id):
    return "{}/api/mobile/v1/auth_by_password?email={}&password={}&client_id={}&client_secret={}&device_id={}".format(
        XENO_HOST, user, password, client_id, client_secret, device_id
    )


def auth_by_oauth_url(task_id, client_id, client_secret, device_id):
    return "{}/api/mobile/v1/auth_by_oauth?social_task_id={}&client_id={}&client_secret={}&device_id={}".format(
        XENO_HOST, task_id, client_id, client_secret, device_id
    )


def create_task_url():
    return "{}/brokerapi/test/create_task?consumer=mail".format(SOCIAL_API_HOST)


def get_access_token_url(application):
    return "{}/proxy2/application/{}/refresh_token".format(SOCIAL_HOST, application)


def auth_by_password_ex_url(user):
    params = {
        "email": user["email"],
        "imap_login": user["imap_login"],
        "imap_password": user["imap_password"],
        "imap_host": user["imap_host"],
        "imap_port": user["imap_port"],
        "imap_ssl": user["imap_ssl"],
        "client_id": user["client_id"],
        "client_secret": user["client_secret"],
        "device_id": user["device_id"],
    }

    if "smtp_login" in user:
        params["smtp_login"] = user["smtp_login"]

    if "smtp_password" in user:
        params["smtp_password"] = user["smtp_password"]

    if "smtp_host" in user:
        params["smtp_host"] = user["smtp_host"]

    if "smtp_port" in user:
        params["smtp_port"] = user["smtp_port"]

    if "smtp_ssl" in user:
        params["smtp_ssl"] = user["smtp_ssl"]

    return "{}/api/mobile/v1/auth_by_password_ex?{}".format(XENO_HOST, urllib.urlencode(params))


def mark_read_url(mids, tids):
    return "{}/api/mobile/v1/mark_read?mids={}&tids={}".format(XENO_HOST, mids, tids)


def unload_user_url(uid):
    return "{}/unload_user?uid={}".format(XENO_INTERNAL_HOST, uid)


def load_user_url(uid):
    return "{}/load_user?uid={}".format(XENO_INTERNAL_HOST, uid)


def invalidate_auth_url(uid):
    return "{}/invalidate_auth?uid={}".format(XENO_INTERNAL_HOST, uid)


def auth_token_url():
    return "{}/token".format(OAUTH_HOST)


def store_url():
    return "{}/api/mobile/v1/store".format(XENO_HOST)


def send_url():
    return "{}/api/mobile/v1/send".format(XENO_HOST)


def mark_with_label_url(mids, tids, lid, mark):
    return "{}/api/mobile/v1/mark_with_label?mids={}&tids={}&lid={}&mark={}".format(
        XENO_HOST, mids, tids, lid, int(mark)
    )


def sync_status_url():
    return "{}/api/mobile/v1/sync_status".format(XENO_HOST)


def delete_items_v1_url(mids, tids):
    # current folder is unused parameter in xeno, but required in API
    return "{}/api/mobile/v1/delete_items?mids={}&tids={}&current_folder=1".format(
        XENO_HOST, mids, tids
    )


def delete_items_v2_url():
    return "{}/api/mobile/v2/delete_items".format(XENO_HOST)


def purge_items_url():
    return "{}/api/mobile/v2/purge_items".format(XENO_HOST)


def settings_url():
    return "{}/api/mobile/v1/settings".format(XENO_HOST)


def messages_url():
    return "{}/api/mobile/v1/messages".format(XENO_HOST)


def move_to_folder_url(mids, fid):
    return "{}/api/mobile/v1/move_to_folder?mids={}&fid={}".format(XENO_HOST, mids, fid)


def api_mops_mark_url():
    return "{}/mops/mark".format(XENO_HOST)


def api_mops_move_url():
    return "{}/mops/move".format(XENO_HOST)


def api_mops_remove_url():
    return "{}/mops/remove".format(XENO_HOST)


def api_mops_create_folder_url():
    return "{}/mops/create_folder".format(XENO_HOST)


def api_mops_update_folder_url():
    return "{}/mops/update_folder".format(XENO_HOST)


def api_mops_delete_folder_url():
    return "{}/mops/delete_folder".format(XENO_HOST)


def api_mops_get_or_create_label_url():
    return "{}/mops/get_or_create_label".format(XENO_HOST)


def api_mops_create_label_url():
    return "{}/mops/create_label".format(XENO_HOST)


def api_mops_get_or_create_label_by_symbol_url():
    return "{}/mops/get_or_create_label_by_symbol".format(XENO_HOST)


def api_mops_create_label_by_symbol_url():
    return "{}/mops/create_label_by_symbol".format(XENO_HOST)


def api_mops_update_label_url():
    return "{}/mops/update_label".format(XENO_HOST)


def api_mops_delete_label_url():
    return "{}/mops/delete_label".format(XENO_HOST)


def api_mops_label_url():
    return "{}/mops/label".format(XENO_HOST)


def api_mops_unlabel_url():
    return "{}/mops/unlabel".format(XENO_HOST)


def api_mops_set_folder_symbol_url():
    return "{}/mops/set_folder_symbol".format(XENO_HOST)


def api_sendbernar_save_url(uid, fid, old_mid):
    params = {"uid": uid, "fid": fid}
    if old_mid:
        params["old_mid"] = old_mid
    return "{}/sendbernar/save?{}".format(XENO_HOST, urllib.urlencode(params))


def blackbox_url(method="oauth"):
    return "{}/blackbox?method={}".format(PASSPORT_HOST, method)


def xlist_url():
    return "{}/api/mobile/v1/xlist".format(XENO_HOST)


def upload_url(uuid):
    return "{}/api/mobile/v1/upload?uuid={}".format(XENO_HOST, uuid)


def create_folder_url(name, parent_fid=None):
    params = {"name": name}
    if parent_fid:
        params["parent_fid"] = parent_fid
    return "{}/api/mobile/v1/create_folder?{}".format(XENO_HOST, urllib.urlencode(params))


def delete_folder_url(fid):
    return "{}/api/mobile/v1/delete_folder?fid={}".format(XENO_HOST, fid)


def update_folder_url(fid, new_name):
    return "{}/api/mobile/v1/update_folder?fid={}&name=".format(XENO_HOST, fid, new_name)


def clear_folder_url(fid):
    return "{}/api/mobile/v1/clear_folder?fid={}".format(XENO_HOST, fid)


def process_passport_events_url():
    return "{}/process_passport_events".format(XENO_INTERNAL_HOST)


def acquired_buckets_url():
    return "{}/acquired_buckets_info".format(XENO_INTERNAL_HOST)


def release_buckets_url(buckets_ids, duration):
    return "{}/release_buckets?buckets_ids={}&duration={}".format(
        XENO_INTERNAL_HOST, ",".join(buckets_ids), duration
    )


def add_bucket_url(bucket_id, shard_ids):
    ret = "{}/add_bucket?bucket_id={}".format(XENO_INTERNAL_HOST, bucket_id)
    if len(shard_ids):
        ret += "&shard_ids={}".format(",".join(shard_ids))
    return ret


def del_bucket_url(bucket_id):
    return "{}/del_bucket?bucket_id={}".format(XENO_INTERNAL_HOST, bucket_id)


def add_shards_to_bucket_url(bucket_id, shard_ids):
    return "{}/add_shards_to_bucket?bucket_id={}&shard_ids={}".format(
        XENO_INTERNAL_HOST, bucket_id, ",".join(shard_ids)
    )


def del_shards_from_bucket_url(bucket_id, shard_ids):
    return "{}/del_shards_from_bucket?bucket_id={}&shard_ids={}".format(
        XENO_INTERNAL_HOST, bucket_id, ",".join(shard_ids)
    )


def list_controllers_url():
    return "{}/list_controllers".format(XENO_INTERNAL_HOST)
