import msgpack
from .http import load_json, load_url, urlencode, HTTPError


class ApiException(Exception):
    def __init__(self, error):
        self.context_id = error["id"] if "id" in error else error["request_id"]
        self.reason = error["reason"]
        self.method = error["method"] if "method" in error else error["request"]
        super(ApiException, self).__init__(
            "{method}: {reason} ({context_id})".format(
                method=self.method, reason=self.reason, context_id=self.context_id
            )
        )


class Api:
    def __init__(self, host, user):
        self.host = host
        self.user = user

    def _call_api(self, path, args={}):
        args["json"] = 1
        res = load_json(self.host + path + urlencode(args, prefix="?"))

        if "error" in res:
            raise ApiException(res["error"])

        deletions = ["host", "id", "request"]
        for elem in deletions:
            if elem in res:
                del res[elem]
        return res

    def create(self, login, password, root_folder_id=None, label_id=None):
        params = {
            "suid": self.user["suid"],
            "mdb": "pg",
            "user": self.user["login"],
            "password": password,
            "login": login,
            "server": "imap.yandex.ru",
        }

        if root_folder_id is not None:
            params["root_folder_id"] = root_folder_id

        if label_id is not None:
            params["label_id"] = label_id

        return self._call_api("/api/create", params)

    def create_yandex(self, task_id, root_folder_id=None, label_id=None):
        params = {"uid": self.user["uid"], "social_task_id": task_id}

        if root_folder_id is not None:
            params["root_folder_id"] = root_folder_id

        if label_id is not None:
            params["label_id"] = label_id

        return self._call_api("/api/create_yandex", params)

    def list(self, popid=None):
        params = {"suid": self.user["suid"], "mdb": "pg"}
        if popid is not None:
            params["popid"] = popid
        return self._call_api("/api/list", params)

    def delete(self, id):
        return self._call_api("/api/delete", {"suid": self.user["suid"], "mdb": "pg", "popid": id})

    def enable(self, id, is_on):
        return self._call_api(
            "/api/enable",
            {"suid": self.user["suid"], "mdb": "pg", "popid": id, "is_on": 1 if is_on else 0},
        )

    def edit(
        self, id, social_task_id=None, login=None, password=None, root_folder_id=None, label_id=None
    ):
        params = {"suid": self.user["suid"], "mdb": "pg", "user": self.user["login"], "popid": id}

        if social_task_id is not None:
            params["social_task_id"] = social_task_id

        if login is not None:
            params["login"] = login

        if password is not None:
            params["password"] = password

        if root_folder_id is not None:
            params["root_folder_id"] = root_folder_id

        if label_id is not None:
            params["label_id"] = label_id

        return self._call_api("/api/edit", params)


class InternalApiException(Exception):
    def __init__(self, error):
        self.context_id = error.hdrs["Y-Context"]
        self.content = error.read().decode()
        self.code = error.code
        super(InternalApiException, self).__init__(
            "{content} ({context_id})".format(content=self.content, context_id=self.context_id)
        )


def call_internal_api(url, args, ya_service_ticket=None):
    try:
        headers = {}
        if ya_service_ticket is not None:
            headers = {"X-Ya-Service-Ticket": ya_service_ticket}
        res = load_url(url + urlencode(args, prefix="?"), add_headers=headers).read()
        return msgpack.unpackb(res)
    except HTTPError as e:
        raise InternalApiException(e)


def call_service_api(url, args):
    try:
        return load_url(url + urlencode(args, prefix="?")).read()
    except HTTPError as e:
        raise InternalApiException(e)
