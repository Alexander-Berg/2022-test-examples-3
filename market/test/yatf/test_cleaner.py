import yatest
import subprocess

from mapreduce.yt.python.yt_stuff import YtStuff, YtConfig

YT_SERVER = None


def setup_module(module):
    module.YT_SERVER = YtStuff(YtConfig(wait_tablet_cell_initialization=True))
    module.YT_SERVER.start_local_yt()


def create_table(yt, table_name):
    yt.create(
        'table',
        table_name,
        ignore_existing=True,
        recursive=True
    )


def run_cleaner(cmdlist, raise_log=False):
    try:
        subprocess.check_call(cmdlist)
    except:
        raise Exception(open('promo-cleaner.log').read())

    if raise_log:
        raise Exception(open('promo-cleaner.log').read())


def test_history_cleanup():
    global YT_SERVER
    yt = YT_SERVER.get_yt_client()

    paths2check = ('prices', 'prices/pricedrops_input/clicks', 'prices/hprices', 'prices/hprices_pricedrops', 'prices/hprices_pricedrops_partner_report')

    for path in paths2check:
        create_table(yt, '//home/history/%s/20010601' % path)  # del
        create_table(yt, '//home/history/%s/20010602' % path)  # del
        create_table(yt, '//home/history/%s/20010603' % path)  # thin
        create_table(yt, '//home/history/%s/20010604' % path)  # thin
        create_table(yt, '//home/history/%s/20010605' % path)  # thin
        create_table(yt, '//home/history/%s/20010606' % path)  # store
        create_table(yt, '//home/history/%s/20010607' % path)  # store
        create_table(yt, '//home/history/%s/omg' % path)

    cmdlist = [
        yatest.common.binary_path('market/idx/promos/promo-cleaner/bin/promo-cleaner'),
        "--yt-idx-path", "//home",
        "--yt-server", YT_SERVER.get_server(),
        "--thinning-days", '2',
        "--thinning-period", '2',
        "--max-allowed", '5',
    ]

    run_cleaner(cmdlist)

    tables = yt.list('//home/history/prices')
    assert 'omg' in tables
    assert 'hprices_pricedrops' in tables
    assert 'hprices' in tables
    assert '20010601' not in tables
    assert '20010602' not in tables
    assert '20010603' in tables
    assert '20010604' in tables
    assert '20010605' in tables
    assert '20010606' in tables
    assert '20010607' in tables

    tables = yt.list('//home/history/prices/pricedrops_input/clicks')
    assert 'omg' in tables
    assert '20010601' not in tables
    assert '20010602' not in tables
    assert '20010603' in tables
    assert '20010604' in tables
    assert '20010605' in tables
    assert '20010606' in tables
    assert '20010607' in tables

    for path in paths2check[2:]:
        tables = yt.list('//home/history/%s' % path)
        assert 'omg' in tables
        assert '20010601' not in tables
        assert '20010602' not in tables
        assert '20010603' not in tables
        assert '20010604' in tables
        assert '20010605' not in tables
        assert '20010606' in tables
        assert '20010607' in tables

    # running cleaner second time - nothing should change

    run_cleaner(cmdlist)

    tables = yt.list('//home/history/prices')
    assert 'omg' in tables
    assert 'hprices_pricedrops' in tables
    assert 'hprices' in tables
    assert '20010601' not in tables
    assert '20010602' not in tables
    assert '20010603' in tables
    assert '20010604' in tables
    assert '20010605' in tables
    assert '20010606' in tables
    assert '20010607' in tables

    tables = yt.list('//home/history/prices/pricedrops_input/clicks')
    assert 'omg' in tables
    assert '20010601' not in tables
    assert '20010602' not in tables
    assert '20010603' in tables
    assert '20010604' in tables
    assert '20010605' in tables
    assert '20010606' in tables
    assert '20010607' in tables

    for path in paths2check[2:]:
        tables = yt.list('//home/history/%s' % path)
        assert 'omg' in tables
        assert '20010601' not in tables
        assert '20010602' not in tables
        assert '20010603' not in tables
        assert '20010604' in tables
        assert '20010605' not in tables
        assert '20010606' in tables
        assert '20010607' in tables


def test_blue_history_cleanup():
    '''
    Проверяем что клинер отработает при не совпадении стурктуры папок на белом и синем
    '''
    global YT_SERVER
    yt = YT_SERVER.get_yt_client()

    paths2check = ('blue/prices', 'blue/prices/hprices', 'blue/prices/hprices_pricedrops')

    for path in paths2check:
        create_table(yt, '//home/history/%s/20010601' % path)  # del
        create_table(yt, '//home/history/%s/20010602' % path)  # del
        create_table(yt, '//home/history/%s/20010603' % path)  # thin
        create_table(yt, '//home/history/%s/20010604' % path)  # thin
        create_table(yt, '//home/history/%s/20010605' % path)  # thin
        create_table(yt, '//home/history/%s/20010606' % path)  # store
        create_table(yt, '//home/history/%s/20010607' % path)  # store
        create_table(yt, '//home/history/%s/omg' % path)

    cmdlist = [
        yatest.common.binary_path('market/idx/promos/promo-cleaner/bin/promo-cleaner'),
        "--yt-history-price-path", "//home/history/blue/prices",
        "--yt-server", YT_SERVER.get_server(),
        "--thinning-days", '2',
        "--thinning-period", '2',
        "--max-allowed", '5',
    ]

    run_cleaner(cmdlist)

    tables = yt.list('//home/history/prices')
    assert 'omg' in tables
    assert 'hprices_pricedrops' in tables
    assert 'hprices' in tables
    assert '20010601' not in tables
    assert '20010602' not in tables
    assert '20010603' in tables
    assert '20010604' in tables
    assert '20010605' in tables
    assert '20010606' in tables
    assert '20010607' in tables

    for path in paths2check[1:]:
        tables = yt.list('//home/history/%s' % path)
        assert 'omg' in tables
        assert '20010601' not in tables
        assert '20010602' not in tables
        assert '20010603' not in tables
        assert '20010604' in tables
        assert '20010605' not in tables
        assert '20010606' in tables
        assert '20010607' in tables
