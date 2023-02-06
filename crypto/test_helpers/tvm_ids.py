class TvmIds(object):
    def __init__(self, tvm_api):
        self.api = tvm_api.issue_id()

        self.full_permissions = tvm_api.issue_id()
        self.version_only = tvm_api.issue_id()
