__author__ = 'aokhotin'


def pytest_addoption(parser):
    parser.addoption("--prod", type="string", help="prod url", default='http://hamster.yandex.ru/search/')
    parser.addoption("--beta", type="string", help="beta url", default='http://hamster.yandex.ru/search/')
    parser.addoption("--test-data-path", type="string", help="Path to the test data", default='TestData/kubr')
