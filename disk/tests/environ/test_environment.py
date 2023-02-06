# encoding: UTF-8

import unittest

from ws_properties.environ.environment import Environment
from ws_properties.environ.properties import DictPropertySource


class EnvironmentTestCase(unittest.TestCase):
    def test_get_inline_profiles(self):
        source = DictPropertySource({'application.profiles.active': '1,2, 3'})
        environment = Environment()
        environment.property_sources.append(source)

        self.assertListEqual(
            environment.profiles,
            ['1', '2', ' 3'],
        )

    def test_get_profiles(self):
        source = DictPropertySource({
            'application.profiles.active': ['1', '2', '3'],
        })
        environment = Environment()
        environment.property_sources.append(source)

        self.assertListEqual(
            environment.profiles,
            ['1', '2', '3'],
        )

    def test_get_profiles_invalid(self):
        source = DictPropertySource({
            'application.profiles.active': ['1', '2', '3', ''],
        })
        environment = Environment()
        environment.property_sources.append(source)

        with self.assertRaisesRegexp(
                ValueError,
                'Invalid profile \'\': must contains text',
        ):
            _ = environment.profiles

    def test_get_profiles_placeholders(self):
        source1 = DictPropertySource({
            'custom_profiles': '4',
        })
        source2 = DictPropertySource({
            'application.profiles.active': ['1', '2', '3', '${custom_profiles}'],
        })
        environment = Environment()
        environment.property_sources.append(source1)
        environment.property_sources.append(source2)

        self.assertListEqual(
            environment.profiles,
            ['1', '2', '3', '4'],
        )
