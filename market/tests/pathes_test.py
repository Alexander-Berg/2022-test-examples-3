from core.pathes import get_path


class TestPathes(object):

    @staticmethod
    def test_get_path():
        assert get_path('template').endswith('pydashie/templates')
        assert get_path('fubbbar') == ''
