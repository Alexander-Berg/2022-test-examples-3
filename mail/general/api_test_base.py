

class APITestBase(object):
    def set_args(self, **request_args):
        self._good_args = {}
        for key in request_args:
            self._good_args[key] = request_args[key]

    def args(self, **kwargs):
        args = self._good_args.copy()
        for key in kwargs:
            args[key] = kwargs[key]
        return args
