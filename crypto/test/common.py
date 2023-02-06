class MockYtClient(object):
    def __init__(self, *args, **kwargs):
        self.removed = []
        self.COMMAND_PARAMS = {}

    def remove(self, path, **kwargs):
        self.removed.append(path)

    def exists(self, *args, **kwargs):
        return False

    def create(self, *args, **kwargs):
        pass

    class Transaction(object):
        def __init__(self, *args, **kwargs):
            self.transaction_id = "id"

        def __enter__(self):
            return self

        def __exit__(self, exc_type, exc_val, exc_tb):
            pass
