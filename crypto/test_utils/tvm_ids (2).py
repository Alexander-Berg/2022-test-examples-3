class TvmIds(object):
    def __init__(self, tvm_api):
        self.api = tvm_api.issue_id()

        self.full_permissions = tvm_api.issue_id()
        self.no_permissions = tvm_api.issue_id()

        self.status_only = tvm_api.issue_id()
        self.full_except_status = tvm_api.issue_id()

        self.delete_only = tvm_api.issue_id()
        self.full_except_delete = tvm_api.issue_id()

        self.ping_only = tvm_api.issue_id()
        self.full_except_ping = tvm_api.issue_id()

        self.version_only = tvm_api.issue_id()
        self.full_except_version = tvm_api.issue_id()
