# coding: utf-8

from yatest import common


def test_ner_object():
    with open(common.source_path("search/wizard/entitysearch/tools/outnerobject/tests/input.ru.txt")) as stdin:
        return common.canonical_execute(
            common.binary_path("search/wizard/entitysearch/tools/outnerobject/outnerobject"),
            [
                "-d",
                common.binary_path("search/wizard/entitysearch/data/shard/search/wizard/entitysearch/data/shard_data"),
            ],
            stdin=stdin,
            check_exit_code=False,
        )
