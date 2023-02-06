import pytest


@pytest.fixture(scope="session")
def conf(request):
    import crypta.lib.python.bt.conf.conf as conf
    import crypta.lib.python.bt.conf.resource_conf as resource_conf
    conf.use(resource_conf.find('/config'))
    return conf


@pytest.yield_fixture(scope="session")
def local_yt(request, conf):
    from mapreduce.yt.python.yt_stuff import YtStuff

    yt = YtStuff()
    yt.start_local_yt()

    url = "{}:{}".format("localhost", yt.yt_proxy_port)

    conf.Yt._I_know_what_I_do_set(
        "proxy",
        dict(
            url=url,
            name="local_yt",
        )
    )

    try:
        yield yt
    finally:
        yt.stop_local_yt()


@pytest.yield_fixture(scope="function")
def clean_local_yt(local_yt):
    import yt.wrapper as yt
    for each in yt.list('/', absolute=True):
        if each in ('//sys', ):
            continue
        yt.remove(each, recursive=True)
    try:
        yield local_yt
    except:
        pass
