import typing


class JugglerClientMock(object):
    def __init__(self, name='st_mock', *args, **kwargs):
        self._checks = {}
        self.name = name

    def set_data(self, data: dict[str, dict[str, dict[str, typing.Any]]]):
        self._checks = data

    def clear_data(self):
        self._checks = {}

    def checks(self, *args, **kwargs):
        return self._checks
