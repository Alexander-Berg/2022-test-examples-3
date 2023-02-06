from .http import load_json, urlencode


class ApiException(Exception):
    def __init__(self, error):
        self.context_id = error["id"] if "id" in error else error["request_id"]
        self.reason = error["reason"]
        self.description = error["description"]
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

    def check_server_with_oauth(self, social_task_id, popid=None):
        params = {"suid": self.user["suid"], "mdb": "pg", "social_task_id": social_task_id}
        if popid is not None:
            params["popid"] = popid
        return self._call_api("/api/check_server", params)

    def check_server_with_password(self, login, password, popid=None):
        params = {"suid": self.user["suid"], "mdb": "pg", "login": login, "password": password}
        if popid is not None:
            params["popid"] = popid
        return self._call_api("/api/check_server", params)

    def check_server_full(self, server, port, ssl, protocol, login, password, popid=None):
        params = {
            "suid": self.user["suid"],
            "mdb": "pg",
            "server": server,
            "port": port,
            "ssl": 1 if ssl else 0,
            "imap": 1 if protocol == "imap" else 0,
            "login": login,
            "password": password,
        }
        if popid is not None:
            params["popid"] = popid
        return self._call_api("/api/check_server", params)

    def create(self, login, password, **kwargs):
        params = {
            "suid": self.user["suid"],
            "mdb": "pg",
            "user": self.user["login"],
            "password": password,
            "login": login,
            "server": "imap.yandex.ru",
        }
        params.update(kwargs)
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

    def edit(self, id, **kwargs):
        params = {"suid": self.user["suid"], "mdb": "pg", "user": self.user["login"], "popid": id}
        params.update(kwargs)
        return self._call_api("/api/edit", params)

    def status(self, id):
        params = {"suid": self.user["suid"], "mdb": "pg", "user": self.user["login"], "popid": id}
        return self._call_api("/api/status", params)

    def info(self, id):
        params = {"suid": self.user["suid"], "mdb": "pg", "user": self.user["login"], "popid": id}
        return self._call_api("/api/info", params)

    def run(self, id):
        params = {"suid": self.user["suid"], "mdb": "pg", "synced": "yes", "popid": id}
        return self._call_api("/api/run", params)
