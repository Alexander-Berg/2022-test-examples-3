from extsearch.geo.kernel.pymod.runserver import declare_runserver_recipe
import yatest.common


def cmd():
    return [yatest.common.binary_path('extsearch/geo/kernel/pymod/runserver/test/app/app'), '--delay', 0.5, '--port', 0]


if __name__ == '__main__':
    declare_runserver_recipe('app', cmd)
