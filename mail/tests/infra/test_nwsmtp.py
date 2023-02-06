import asyncio
import os
import signal
import pytest

from datetime import datetime

from mail.nwsmtp.tests.lib import CLUSTERS
from mail.nwsmtp.tests.lib.default_conf import make_conf
from mail.nwsmtp.tests.lib.env import Stubs
from mail.nwsmtp.tests.lib.nwsmtp import NwSMTP, parse_log_line, parse_tskv_line, DATE_TIME_FMT


@pytest.mark.cluster(CLUSTERS)
async def test_run_nwsmtp(conf, users):
    async with Stubs(conf, users):
        async with NwSMTP(conf) as nw:
            await asyncio.sleep(1)
            os.kill(nw.proc._process.pid, signal.SIGINT)
            nw.proc.wait(check_exit_code=False, timeout=5.0)
            assert nw.proc.exit_code == 0
            assert "global application::stop status=ok" in nw.get_log("yplatform")[-1]


def change_gid(conf):
    conf.system.gid = "abcdefg"


async def test_raise_when_misconfiguration():
    with make_conf("mxbackout", customize_with=change_gid) as conf:
        with pytest.raises(RuntimeError) as exc_info:
            async with NwSMTP(conf):
                pass

    assert "Unable to run NwSMTP" in str(exc_info.value)


@pytest.mark.parametrize("line, expected", [
    ("[2018-11-21 11:55:09.445397] a8kHRSZoIX-RECV: ",
     (datetime.strptime("2018-11-21 11:55:09.445397", DATE_TIME_FMT),
      "11:55:09.445397 a8kHRSZoIX-RECV: ")),
    ("[2018-12-21 11:55:09.357893] global smtp worker ",
     (datetime.strptime("2018-12-21 11:55:09.357893", DATE_TIME_FMT),
      "11:55:09.357893 global smtp worker ")),
])
def test_parse_log_line(line, expected):
    assert parse_log_line(line) == expected


@pytest.mark.parametrize("line, expected", [
    ("tskv	tskv_format=mail-nwsmtp-tskv-log	thread=140640549030720	unixtime=1593935277	timestamp="
     "2020-07-05T10:47:57.364529+0300	level=notice	message=Load IP restriction file: "
     "name='ip_param.conf'	direction=in",
     (datetime.strptime("2020-07-05 10:47:57.364529", DATE_TIME_FMT),
      "10:47:57.364529 Load IP restriction file: name='ip_param.conf'")),
    ("tskv	tskv_format=mail-nwsmtp-tskv-log	thread=140213935885120	unixtime=1593951847	timestamp="
     "2020-07-05T15:24:07.500559+0300	level=notice	message=Reload aliases file: "
     "name='virtual_alias_maps'	direction=out",
     (datetime.strptime("2020-07-05 15:24:07.500559", DATE_TIME_FMT),
      "15:24:07.500559 Reload aliases file: name='virtual_alias_maps'"))
])
def test_parse_tskv_line(line, expected):
    assert parse_tskv_line(line) == expected
