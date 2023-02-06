import pytest

from test_utils import TestParser
from base_parsers import SerpParser

Alignments = SerpParser.MetricsMagicNumbers.Alignments
WizardTypes = SerpParser.MetricsMagicNumbers.WizardTypes

ORG1 = WizardTypes.WIZARD_ADRESA
ORGMN = WizardTypes.WIZARD_ORGMN
A_TOP = Alignments.TOP
A_LEFT = Alignments.LEFT
A_RIGHT = Alignments.RIGHT


class TestYandexSearchGeoWizardBaobabHtmlParser(TestParser):

    @pytest.mark.parametrize("input_filename,wizard_type,alignment,baobab_subtype,page_url_check", [
        ("cafe_pushkin_desktop.html", ORG1, A_RIGHT, "company", True),
        ("cafe_pushkin_touch.html", ORG1, A_LEFT, "company", True),
        ("radisson_sonya_desktop.html", ORG1, A_RIGHT, "travel_company", True),
        ("radisson_sonya_touch.html", ORG1, A_LEFT, "travel_company", True),
        ("cafe_spb_carousel_desktop.html", ORGMN, A_TOP, "map", False),
        ("cafe_spb_desktop.html", ORGMN, A_RIGHT, "map", False),
        ("cafe_spb_short_touch.html", ORGMN, A_LEFT, "map", False),
        ("hotels_with_pool_spb_carousel_desktop.html", ORGMN, A_TOP, "travel_map", False),
        ("hotels_with_pool_spb_desktop.html", ORGMN, A_RIGHT, "travel_map", False),
        ("hotels_with_pool_spb_short_touch.html", ORGMN, A_LEFT, "travel_map", False),
    ])
    def test_orgwizard_component(self, input_filename, wizard_type, alignment, baobab_subtype, page_url_check):
        parsed_serp = self.parse_file(input_filename)
        expected_data_filename = input_filename \
            .replace('carousel_desktop.html', 'data.json') \
            .replace('desktop.html', 'data.json') \
            .replace('touch.html', 'data.json')

        expected_data = self._read_json_file(expected_data_filename)
        for component in parsed_serp.get('components', ''):
            if component['componentInfo'].get('wizardType') in (ORG1, ORGMN):
                assert component['componentInfo'].get('wizardType') == wizard_type
                assert component['componentInfo'].get('alignment') == alignment
                assert component['text.baobabWizardName'] == 'companies'
                assert component['text.baobabWizardSubtype'] == baobab_subtype
                if page_url_check:
                    assert component['componentUrl'].get('pageUrl') == expected_data[0]['maps_url']

                if wizard_orgs_data := component.get("json.wizardOrgsData", []):
                    assert len(wizard_orgs_data) == len(expected_data)
                    assert wizard_orgs_data == expected_data
