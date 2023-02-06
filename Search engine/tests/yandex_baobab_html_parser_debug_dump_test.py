from test_utils import TestParser
from yandex_baobab.yandex_baobab_html_parser_debug_dump import YandexBaobabHTMLParserDebugDump


class TestYandexBaobabHTMLParserDebugDump(TestParser):
    _parser_class = YandexBaobabHTMLParserDebugDump

    def test_desktop(self):
        parsed_serp = self.parse_file("desktop_debug_data.html")
        components = parsed_serp["components"]

        assert len(components) == 13
        debug_info = parsed_serp["debug"]

        # Rearr checks
        assert debug_info["REARR.proto"] == "https"
        assert debug_info["REARR.browser_name"] == "Unknown"
        assert debug_info["REARR.send_platformtype_as_relev_param"] == "da"
        assert debug_info["REARR.yaplus"] == "0"
        assert debug_info["REARR.yaplus_type"] == "null"
        assert debug_info["REARR.scheme_Local/VideoSvodUpper/SvodMaxCount"] == "1"
        assert "REARR.VideoAdsMiddle_off" not in debug_info

        # Relev checks
        assert debug_info["RELEV.fresh_flow"] == "0"
        assert debug_info["RELEV.frqcls"] == "strong_tail"
        assert debug_info["RELEV.log_dt_bigrams_query_cluster"] == "1291246"

        # RequestId checks
        assert debug_info["RequestId"] == "1585052619158084-526842817799918166000095-hamster-app-host-sas-web-yp-22"

        # Upper checks
        assert debug_info["UPPER.Unanswer_QUERYSEARCH"] == "1/0"
        assert debug_info["UPPER.VideoBlenderFmls.ERROR-GLOBAL-incorrect-config-for-quick-xussr-rearr-"
                          "is_mobile-with-type-default"] == "simple_patcher_can_not_be_used"

        # Web checks
        assert debug_info["WEB.Antispam.formula_value_20"] == "www.lavazza.ru|Z53EA8471F0011DD5|0.09926509747" \
                                                              "|0.2649919653"
        assert debug_info["WEB.Unanswer_RTMR_FACTORS_API_TTL_0_TIMEOUT_140"] == "0/0"

    def test_mobile(self):
        parsed_serp = self.parse_file("mobile_debug_data.html")
        components = parsed_serp["components"]
        assert len(components) == 11

        debug_info = parsed_serp["debug"]

        # Rearr checks
        assert debug_info["REARR.device_name"] == "SM-G935V"
        assert debug_info["REARR.browser_name"] == "ChromeMobile"
        assert debug_info["REARR.send_platformtype_as_relev_param"] == "da"

        # Relev checks
        assert debug_info["RELEV.frqcls"] == "strong_tail"
        assert debug_info["RELEV.CompressedDssmRandomLogFactors"] == "AB4AAAAKwa9NAwAAI1oB8RyaAQAAdvMJD0cUDwIVFAtCfSo="
        assert debug_info["RELEV.QueryDssmEmbeddingPca1"] == "0.5453906059"

        # RequestId checks
        assert debug_info["RequestId"] == "1585052942699723-1792501178013640125300105-hamster-app-host-sas-web-yp-4"

        # Upper checks
        assert debug_info["UPPER.Unanswer_QUERYSEARCH"] == "1/0"
        assert debug_info["UPPER.BlenderNeocortexFeatures.CLF_ether_binary_classifiers_LABEL_COUNT"] == "0"

        # Web checks
        assert debug_info["WEB.ProximaPredict.Dcg5Grouping"] == "1.82743"
        assert debug_info["WEB.OrgWizardMiddle.AddressSnippetSource_GroupPos"] == "10"
