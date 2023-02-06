class Callback:
    def __init__(self):
        self.is_called = False

    def __call__(self, *args, **kwds):
        self.is_called = True

    def called(self):
        return self.is_called

    def reset(self):
        self.is_called = False
