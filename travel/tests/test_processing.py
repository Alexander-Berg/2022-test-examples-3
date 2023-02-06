from unittest.case import TestCase

from yaml import load as load_yaml, SafeLoader

from library.python import resource

from travel.hotels.tools.text_builder.renderer.renderer.features import create_features
from travel.hotels.tools.text_builder.renderer.renderer.processing import TextRenderer
from travel.hotels.tools.text_builder.renderer.renderer.templates import HotelTemplates


class TestTextRenderer(TestCase):
    def test_full_template(self):
        raw_templates = load_yaml(resource.find('full-template.yaml'), Loader=SafeLoader)
        templates = HotelTemplates(raw_templates)
        text_randerer = TextRenderer(templates)
        raw_features = load_yaml(resource.find('full-features.yaml'), Loader=SafeLoader)
        features = create_features(raw_features)
        text = text_randerer.render("Default", features[123456789])
        assert text == resource.find('full-rendered.txt').decode('utf-8').strip('\n')
