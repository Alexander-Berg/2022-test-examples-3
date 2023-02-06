import itertools

from sandbox import common


class TestPlatform:

    LUCID_PLATFORM_ALIAS = "linux_ubuntu_10.04_lucid"
    LUCID_PLATFORMS = common.platform.PLATFORM_ALIASES[LUCID_PLATFORM_ALIAS]

    PRECISE_PLATFORM_ALIAS = "linux_ubuntu_12.04_precise"
    PRECISE_PLATFORMS = common.platform.PLATFORM_ALIASES[PRECISE_PLATFORM_ALIAS]

    FREEBSD8_ALIAS = "freebsd8"
    FREEBSD8_PLATFFORMS = common.platform.PLATFORM_ALIASES[FREEBSD8_ALIAS]

    FREEBSD9_ALIAS = "freebsd9"
    FREEBSD9_PLATFFORMS = common.platform.PLATFORM_ALIASES[FREEBSD9_ALIAS]

    def test__alias_naming(self):
        """
            Check linux platform aliases
        """
        for lucid_platfrom in self.LUCID_PLATFORMS:
            assert common.platform.get_platform_alias(lucid_platfrom) == self.LUCID_PLATFORM_ALIAS
            assert common.platform.get_platform_alias(lucid_platfrom.lower()) == self.LUCID_PLATFORM_ALIAS

        for precise_platfrom in self.PRECISE_PLATFORMS:
            assert common.platform.get_platform_alias(precise_platfrom) == self.PRECISE_PLATFORM_ALIAS
            assert common.platform.get_platform_alias(precise_platfrom.lower()) == self.PRECISE_PLATFORM_ALIAS
            assert common.platform.compare_platforms(precise_platfrom, self.PRECISE_PLATFORM_ALIAS)

        for freebsd8_platform in self.FREEBSD8_PLATFFORMS:
            assert common.platform.get_platform_alias(freebsd8_platform), self.FREEBSD8_ALIAS
            assert common.platform.get_platform_alias(freebsd8_platform.lower()) == self.FREEBSD8_ALIAS
            assert common.platform.compare_platforms(freebsd8_platform, self.FREEBSD8_ALIAS)

        for freebsd9_platform in self.FREEBSD9_PLATFFORMS:
            assert common.platform.get_platform_alias(freebsd9_platform) == self.FREEBSD9_ALIAS
            assert common.platform.get_platform_alias(freebsd9_platform.lower()) == self.FREEBSD9_ALIAS
            assert common.platform.compare_platforms(freebsd9_platform, self.FREEBSD9_ALIAS)

    def test__compare_platforms(self):
        """
            Check platform compare method
        """
        for platfroms_list in [
            self.LUCID_PLATFORMS, self.PRECISE_PLATFORMS, self.FREEBSD9_PLATFFORMS, self.FREEBSD8_PLATFFORMS,
        ]:
            for platfrom_one, platform_two in itertools.combinations_with_replacement(platfroms_list, 2):
                assert common.platform.compare_platforms(platfrom_one, platform_two)

        def check_for_another_platforms(platforms, another_platforms, alias, another_aliases):
            for platform in platforms:
                assert common.platform.compare_platforms(platform, alias)
                for another_alias in another_aliases:
                    assert not common.platform.compare_platforms(platform, another_alias)
                for another_platform in another_platforms:
                    assert not common.platform.compare_platforms(platform, another_platform)

        check_for_another_platforms(
            self.LUCID_PLATFORMS,
            self.PRECISE_PLATFORMS + self.FREEBSD9_PLATFFORMS + self.FREEBSD8_PLATFFORMS,
            self.LUCID_PLATFORM_ALIAS,
            [self.PRECISE_PLATFORM_ALIAS, self.FREEBSD8_ALIAS, self.FREEBSD9_ALIAS])

        check_for_another_platforms(
            self.PRECISE_PLATFORMS,
            self.LUCID_PLATFORMS + self.FREEBSD9_PLATFFORMS + self.FREEBSD8_PLATFFORMS,
            self.PRECISE_PLATFORM_ALIAS,
            [self.LUCID_PLATFORM_ALIAS, self.FREEBSD8_ALIAS, self.FREEBSD9_ALIAS])

        check_for_another_platforms(
            self.FREEBSD9_PLATFFORMS,
            self.LUCID_PLATFORMS + self.PRECISE_PLATFORMS + self.FREEBSD8_PLATFFORMS,
            self.FREEBSD9_ALIAS,
            [self.LUCID_PLATFORM_ALIAS, self.FREEBSD8_ALIAS, self.PRECISE_PLATFORM_ALIAS])

        check_for_another_platforms(
            self.FREEBSD8_PLATFFORMS,
            self.LUCID_PLATFORMS + self.PRECISE_PLATFORMS + self.FREEBSD9_PLATFFORMS,
            self.FREEBSD8_ALIAS,
            [self.LUCID_PLATFORM_ALIAS, self.FREEBSD9_ALIAS, self.PRECISE_PLATFORM_ALIAS])
