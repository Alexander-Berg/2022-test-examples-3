class MockContext:
    x_req_id = ''
    ip = ''
    check_warn = False
    uid = None

    def log_error(self, reason, message, **kwargs):
        pass

    def log_warning(self, reason, message, **kwargs):
        pass

    def log_info(self, reason, message, **kwargs):
        pass

    def log_debug(self, reason, message, **kwargs):
        pass
