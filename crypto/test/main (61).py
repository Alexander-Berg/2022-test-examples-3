import collections

from crypta.profile.lib.socdem_helpers import socdem_groups


def api_get_group_tree():
    Group = collections.namedtuple("Group", ["id", "children", ])

    return Group(
        socdem_groups.ROOT_GROUP,
        [
            Group(
                socdem_groups.CRYPTA_ROOT_GROUP,
                [
                    Group("foo", [Group("bar", [])]),
                    Group(
                        socdem_groups.SOCDEM_ROOT_GROUP,
                        [
                            Group(
                                socdem_groups.AGE_GROUP,
                                []
                            ),
                            Group(
                                socdem_groups.INCOME_GROUP,
                                []
                            )
                        ]
                    ),
                ]
            ),
        ]
    )


def test_socdem_groups():
    assert ({socdem_groups.SOCDEM_ROOT_GROUP, socdem_groups.AGE_GROUP, socdem_groups.INCOME_GROUP} == socdem_groups.socdem_groups(api_get_group_tree()))
