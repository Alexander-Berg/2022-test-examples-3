# -*- coding: utf-8 -*-

from test_utils import TestParser
from base_parsers import JSONSerpParser

ComponentTypes = JSONSerpParser.MetricsMagicNumbers.ComponentTypes
WizardTypes = JSONSerpParser.MetricsMagicNumbers.WizardTypes


class TestYandexImagesAliceTagsJSONParser(TestParser):

    def test_parse(self):
        components = self.parse_file('serp.json')['components']
        assert len(components) == 1

        true_component = {
            'type': 'COMPONENT',
            'text.relatedQuery': 'дайкон',
            'componentInfo': {
                'type': ComponentTypes.WIZARD,
                'wizardType': WizardTypes.METRICS_UNKNOWN_RELATED_QUERIES
            },
            'componentFilter.wizardOnly': 0,
        }

        assert components[0] == true_component
