import sandbox.common.types.client as ctc


class TestTag(object):
    @staticmethod
    def __to_str(values):
        return map(lambda _: tuple(set(map(str, _)) for _ in _), values)

    def test__tag_query_predicate(self):
        assert ctc.Tag.Query.predicates("") == self.__to_str([
            (
                {ctc.Tag.GENERIC},
                {ctc.Tag.MULTISLOT, ctc.Tag.CORES1, ctc.Tag.CORES4, ctc.Tag.PORTOD, ctc.Tag.Group.SERVICE}
            )
        ])
        assert ctc.Tag.Query.predicates("BROWSER") == self.__to_str([
            (
                {ctc.Tag.BROWSER},
                {ctc.Tag.MULTISLOT, ctc.Tag.CORES1, ctc.Tag.CORES4, ctc.Tag.PORTOD, ctc.Tag.Group.SERVICE}
            )
        ])
        assert ctc.Tag.Query.predicates("MULTISLOT") == self.__to_str([
            ({ctc.Tag.GENERIC, ctc.Tag.MULTISLOT}, {ctc.Tag.PORTOD, ctc.Tag.Group.SERVICE})
        ])
        assert ctc.Tag.Query.predicates("BROWSER & MULTISLOT") == self.__to_str([
            ({ctc.Tag.BROWSER, ctc.Tag.MULTISLOT}, {ctc.Tag.PORTOD, ctc.Tag.Group.SERVICE})
        ])
        assert ctc.Tag.Query.predicates("BROWSER & ~MULTISLOT") == self.__to_str([
            (
                {ctc.Tag.BROWSER},
                {ctc.Tag.MULTISLOT, ctc.Tag.CORES1, ctc.Tag.CORES4, ctc.Tag.PORTOD, ctc.Tag.Group.SERVICE}
            )
        ])
        assert ctc.Tag.Query.predicates("CUSTOM_TAG") == self.__to_str([
            ({ctc.Tag.CUSTOM_TAG}, {ctc.Tag.Group.SERVICE})
        ])
        # empty set
        assert ctc.Tag.Query.predicates("MULTISLOT & ~GENERIC") == self.__to_str([
            ({ctc.Tag.GENERIC, ctc.Tag.MULTISLOT}, {ctc.Tag.GENERIC, ctc.Tag.PORTOD, ctc.Tag.Group.SERVICE})
        ])
        # all generic multi slots
        assert ctc.Tag.Query.predicates("~BROWSER & MULTISLOT") == self.__to_str([
            ({ctc.Tag.GENERIC, ctc.Tag.MULTISLOT}, {ctc.Tag.BROWSER, ctc.Tag.PORTOD, ctc.Tag.Group.SERVICE})
        ])
        # all generic multi slots (1 core)
        assert ctc.Tag.Query.predicates("~BROWSER & CORES1") == self.__to_str([
            ({ctc.Tag.GENERIC, ctc.Tag.CORES1}, {ctc.Tag.BROWSER, ctc.Tag.PORTOD, ctc.Tag.Group.SERVICE})
        ])
        # all generic fat multi slots (4 cores)
        assert ctc.Tag.Query.predicates("~BROWSER & CORES4") == self.__to_str([
            ({ctc.Tag.GENERIC, ctc.Tag.CORES4}, {ctc.Tag.BROWSER, ctc.Tag.PORTOD, ctc.Tag.Group.SERVICE})
        ])

    def test__query_representation(self):
        samples = [
            (ctc.Tag.GENERIC, "GENERIC"),
            (ctc.Tag.Group.LINUX, "LINUX"),
            (~ctc.Tag.Group.LINUX, "~LINUX"),
            (ctc.Tag.Group.LINUX & ctc.Tag.GENERIC, "LINUX & GENERIC"),
            (ctc.Tag.GENERIC & ctc.Tag.Group.LINUX, "GENERIC & LINUX"),
            (ctc.Tag.Group.LINUX & (ctc.Tag.GENERIC | ctc.Tag.PORTO), "LINUX & (GENERIC | PORTO)"),
            (ctc.Tag.Group.LINUX & (ctc.Tag.PORTO | ctc.Tag.GENERIC), "LINUX & (PORTO | GENERIC)"),
            ((ctc.Tag.PORTO | ctc.Tag.GENERIC) & ctc.Tag.Group.LINUX, "(PORTO | GENERIC) & LINUX"),
            (~(ctc.Tag.PORTO | ctc.Tag.GENERIC) & ctc.Tag.Group.LINUX, "~(PORTO | GENERIC) & LINUX"),
            ((ctc.Tag.PORTO | ctc.Tag.GENERIC) & ~ctc.Tag.Group.LINUX, "(PORTO | GENERIC) & ~LINUX"),
            ((ctc.Tag.PORTO | ctc.Tag.GENERIC) | ctc.Tag.SERVER, "PORTO | GENERIC | SERVER"),
            (ctc.Tag.GENERIC & (ctc.Tag.MULTISLOT & ctc.Tag.Group.LINUX), "GENERIC & MULTISLOT & LINUX"),
            (ctc.Tag.GENERIC | (ctc.Tag.MULTISLOT & ctc.Tag.Group.LINUX), "GENERIC | MULTISLOT & LINUX"),
            (
                (ctc.Tag.GENERIC & ctc.Tag.SSD) | (ctc.Tag.MULTISLOT & ctc.Tag.Group.LINUX),
                "GENERIC & SSD | MULTISLOT & LINUX"
            ),
            ((ctc.Tag.GENERIC | ctc.Tag.PORTO) & (ctc.Tag.SSD | ctc.Tag.HDD), "(GENERIC | PORTO) & (SSD | HDD)"),
        ]

        for query, query_repr_ref in samples:
            assert str(query) == query_repr_ref
            assert str(ctc.Tag.Query(query_repr_ref)) == query_repr_ref

        assert str(ctc.Tag.Query("(~(GENERIC) & ((LINUX | (OSX))))")) == "~GENERIC & (LINUX | OSX)"
