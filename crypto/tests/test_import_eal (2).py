import sys
import json
import luigi
import yt.wrapper as yt
import pytest


YT_DIR = "yt-data"
YT_ID = "yt-graph-test-import-eal-log"
PROXY_PORT = 9015


@pytest.mark.usefixtures("ytlocal")
@pytest.mark.usefixtures("crypta_env")
class TestParseYuidWithEal(object):
    def __prepare_cypress(self, date):
        from crypta.graph.v1.python.rtcconf import config
        from crypta.graph.v1.python.utils import mr_utils as mr

        out_date_folder = config.YT_OUTPUT_FOLDER + date + "/"
        mr.mkdir(config.YT_OUTPUT_FOLDER)
        mr.mkdir(out_date_folder)
        mr.mkdir(config.LOGFELLER_EAL_FOLDER)

    def __load_data(self, date):
        from crypta.graph.v1.python.rtcconf import config

        eal_log = config.LOGFELLER_EAL_FOLDER + date
        yt.create_table(eal_log)
        data = [
            {
                "request": "/status.xml?yasoft=punto&clid=41281&ui={8A364D94-B00B-4887-B5D4-3CFDF557C463}&ver=4.2.5.1238&os=winnt&stat=dayuse&launchesAsAdmin=0&osver=win7x64&usesAutocorrection=0&usesDiary=0&yb_installed=0&yu=2136375691466196609&yu_opch=2136375691466196609&fd=24.05.2016",
                "cookies": "uid=CmuLAldmz+SUHc8ECBLUAg==",
            },  # punto
            {
                "request": "/status.xml?yasoft=punto&clid=41281&ui={8A364D94-B00B-4887-B5D4-3CFDF557C463}&ver=4.2.5.1238&os=winnt&stat=dayuse&launchesAsAdmin=0&osver=win7x64&usesAutocorrection=0&usesDiary=0&yb_installed=0&yu=1136375691466196609&yu_opch=1136375691466196609&yu_ch=1136375691466196609&yu_yb=1136375691466196610&yu_amg=1136375691466196611&yu_yedge=1136375691466196610&fd=24.05.2016",
                "cookies": "-",
            },  # punto
            {
                "cookies": "yandexuid=1377513741465469105; fuid01=575948c02bc2a920.QyKYztvR5oHaIVV8iG-NkO7JbGXZMitQ3v2eLGqlNmdPiTDFUWL4PKMEToYuD135uTn7LJcxt6gIsR70IXg2h6a4STniw001stFiWz2usb70y2ldK2Lz5dMxCQ3M7kHo; my=YysBrM0A; yandex_gid=11469; zm=m-white_bender.flex.webp.css-https%3Awww_dwwNXt31Y0jeracAoz-HmOST0Sk%3Al; yabs-frequency=/4/0W0000lKMrS00000/paMmSDWaPm00/; _ym_uid=1466262554563078434; _ym_isad=1; yp=1466521748.clh.931353#1481825336.szm.1_00:1366x768:1366x681#1497115248.dsws.2#1497115248.dswa.0#1496597692.dwys.1#1468085692.los.1#1496597693.dwyc.1#1497030243.st_browser_s.4#2808604800.ygo.26:11469#1473412024.ww.1#1496756606.st_browser_cl.1#1466410062.gpauto.45_1095055:35_4625676:4004:3:1466237262#1497028182.st_set_s.1#1497115248.dwss.1",
                "request": "/status.xml?stat=dayuse&ui=%7BD705B5D4-BCE3-BBC5-E058-D57EE2A32E70%7D&ver=2.27.1&lang=ru&bv=51&os=winnt&yasoft=vbch&brandID=yandex&fd=2014.04.03&tl=1466237261&clid=2063708",
            },  # eal yuid
            {
                "cookies": "-",
                "request": "/status.xml?yasoft=yabrowser&clid=1959248&ui={254B47A0-29FA-4B3A-83BF-0403613D552B}&ver=50.0.2661.8149&os=win7&stat=dayuse&act=0&banerid=0500000134&bitness=32&brandID=ua-custo&build=custo&distr_yandexuid=1445727671453453661&eid=PDO.1%3bbda.1%3bbft.1%3bdb1.3%3bdkg.1%3bdkt.2%3bdnc.1%3bdzs.1%3bedn.1%3beif.1%3besb.1%3bfs3.1%3bgpf.1%3bhrl.3%3bhsh.2%3bmsp.1%3bndb.0%3bod.1%3bpop.1%3bptc.1%3bpwp.2%3brcm.1%3bsbm.2%3bsbu.2%3bsi9.0%3bsie.1%3bskf.1%3bsmb.1%3bspp.1%3bsrp.1%3bssd.0%3bsst.2%3bsxp.1%3btkc.1%3btnw.0%3btsc.1%3btuc.1%3busb.1%3bwrr.1%3bzdd.1%3bzen.1%3bzoa.1%3bzvt.1&fuid01=56bba9d3163801f8.AKVPpAbcPawKx-Lxfl_UqVibuJMXezDQIp26uPVQ8RyarjHaB-T6DydKRDnqCwOMpOWpLIaITNSrTqTf7nK_bNQRfuqubTHwXjshiZVfdqdaFUqp3oTR-Jdh8LemMmGW&install_type=1&installed=50.0.2661.8149&partner_id=&user_agent=Mozilla/5.0%20(Windows%20NT%206.1)%20AppleWebKit/537.36%20(KHTML%2c%20like%20Gecko)%20Chrome/50.0.2661.102%20YaBrowser/16.6.0.8149%20Yowser/2.5%20Safari/537.36&uv=1.2.0.1831&yandexuid=52408781455139280&fd=22.01.2016",
            },  # eal distr_yuid
            {
                "cookies": "-",
                "request": "/status.xml?yasoft=yabrowser&clid=1997623&ui={E4388A5E-0AC2-41A9-9989-ECA9C8C24044}&ver=17.1.0.2034&os=win&stat=dayuse&bitness=64&brandID=yandex-custo&bro=Firefox.GoogleChrome.GoogleChrome.GoogleChrome.GoogleChrome.OperaChrome.OperaChrome.OperaChrome.OperaChrome&build=custo&ckp=91393041451775493.1192337261451762991.2170517491453219833&df=0&distr_yandexuid=&eid=PDO.1%3bacq.2%3bant.2%3bbda.1%3bcmp.1%3bcrd.2%3bcrl.2%3bcsh.1%3bdb1.2%3bdkn.1%3bdks.1%3bdnc.1%3beaa.2%3bedn.1%3bedt.1%3beif.1%3besb.1%3bfld.1%3bfnp.3%3bfs3.1%3bfup.1%3bgpf.2%3bhps.1%3bhrl.3%3bhsh.2%3bmsp.1%3bmyl.3%3bnse.1%3bocm.1%3bod.1%3bpop.1%3bpr1.1%3bprt.1%3bpwp.2%3braf.2%3bsad.1%3bsbm.2%3bsbu.2%3bscl.1%3bsi9.0%3bsie.1%3bskf.1%3bsmb.1%3bspa.1%3bsvi.1%3bsxp.1%3bsyr.1%3bte1.8%3btnw.0%3btso.1%3bvaa.3%3bvrb.7%3bwpm.1%3bwrr.1%3byal.1%3bzav.1%3bzbl.2%3bzdd.1%3bzen.1%3bzes.1%3bzfl.1%3bzoa.1%3bzon.1%3bzsm.1%3bzss.1%3bzvt.1%3binst_date.14517",
            },  # external browserss
        ]
        yt.write_table(eal_log, [json.dumps(row) for row in data], format="json", raw=True)

    def __assert_task_result(self, date):
        from crypta.graph.v1.python.rtcconf import config

        eal_result_table = config.YT_OUTPUT_FOLDER + date + "/yuid_raw/yuid_with_" + config.ID_SOURCE_TYPE_EAL
        assert yt.exists(eal_result_table)
        eal_rows = [json.loads(row) for row in list(yt.read_table(eal_result_table, "json", raw=True))]
        assert len(eal_rows) == 2
        eal_ui_yuid_pairs = set([(row["id_value"], row["yuid"]) for row in eal_rows])
        for pair in [
            ("D705B5D4-BCE3-BBC5-E058-D57EE2A32E70", "1377513741465469105"),
            ("254B47A0-29FA-4B3A-83BF-0403613D552B", "1445727671453453661"),
        ]:
            assert pair in eal_ui_yuid_pairs

        # assert punto source
        punto_result_table = config.YT_OUTPUT_FOLDER + date + "/yuid_raw/yuid_with_" + config.ID_SOURCE_TYPE_PUNTO
        assert yt.exists(punto_result_table)
        punto_rows = [json.loads(row) for row in list(yt.read_table(punto_result_table, "json", raw=True))]
        punto_ui_yuid_pairs = set([(row["id_value"], row["yuid"]) for row in punto_rows])
        assert len(punto_ui_yuid_pairs) == 4
        for pair in [
            ("8A364D94-B00B-4887-B5D4-3CFDF557C463", "2136375691466196609"),
            ("8A364D94-B00B-4887-B5D4-3CFDF557C463", "1136375691466196609"),
            ("8A364D94-B00B-4887-B5D4-3CFDF557C463", "1136375691466196610"),
            ("8A364D94-B00B-4887-B5D4-3CFDF557C463", "1136375691466196611"),
        ]:
            assert pair in punto_ui_yuid_pairs

        # assert external browsers' cookies
        ext_bro_result_table = (
            config.YT_OUTPUT_FOLDER + date + "/yuid_raw/yuid_with_" + config.ID_SOURCE_TYPE_EXTERNAL_BROWSERS
        )
        assert yt.exists(ext_bro_result_table)
        ext_bro_rows = [json.loads(row) for row in list(yt.read_table(ext_bro_result_table, "json", raw=True))]
        ext_bro_ui_yuid_pairs = set([(row["id_value"], row["yuid"]) for row in ext_bro_rows])
        assert len(ext_bro_ui_yuid_pairs) == 3
        for pair in [
            ("E4388A5E-0AC2-41A9-9989-ECA9C8C24044", "91393041451775493"),
            ("E4388A5E-0AC2-41A9-9989-ECA9C8C24044", "1192337261451762991"),
            ("E4388A5E-0AC2-41A9-9989-ECA9C8C24044", "2170517491453219833"),
        ]:
            assert pair in ext_bro_ui_yuid_pairs

    def test_import_eal(self):
        from crypta.graph.v1.python import graph_all
        from crypta.graph.v1.python.rtcconf import config
        from crypta.graph.v1.python.data_imports.import_logs.graph_eal import ImportEalDayTask

        date = "2016-06-21"
        self.__prepare_cypress(date)
        self.__load_data(date)
        luigi.run(
            [
                "ImportEalDayTask",
                "--date",
                date,
                "--run-date",
                date,
                "--workers",
                "1",
                "--local-scheduler",
                "--no-lock",
            ]
        )
        self.__assert_task_result(date)
