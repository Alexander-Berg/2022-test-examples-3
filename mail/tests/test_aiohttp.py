try:
    import aiohttp  # NOQA

    has_aiohttp = True
except ImportError:
    has_aiohttp = False

if has_aiohttp:
    from sendr_qlog.tests._test_aiohttp import *  # NOQA
