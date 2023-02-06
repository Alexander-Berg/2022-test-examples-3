import json

from yamarec1.data import DataQuery
from yamarec1.data import QueryableData
from yamarec1.data.storages import PermanentDataStorage


def test_preamble_tag_marks_all_preambles(factory):
    result = factory["sample_with_preamble.j2"].render()
    assert result == "<YQL-PREAMBLE>$x = 25</YQL-PREAMBLE>SELECT NOTHING<YQL-PREAMBLE>$y = 125</YQL-PREAMBLE>"


def test_attachment_tag_marks_all_attachments(factory):
    result = factory["sample_with_attachments.j2"].render()
    assert result == "<YQL-ATTACHMENT>intro</YQL-ATTACHMENT>SELECT NOTHING<YQL-ATTACHMENT>outro</YQL-ATTACHMENT>"


def test_attachment_tag_supports_expressions(factory):
    template = factory["sample_with_attachment_and_expression_as_attachment_name.j2"]
    result = template.render({"formula_id": 42})
    assert result == "<YQL-ATTACHMENT>formula_42</YQL-ATTACHMENT>SELECT SOMETHING"


def test_data_tag_respects_query_preamble(factory):

    class DummyData(QueryableData):

        @property
        def query(self):
            return DataQuery("`//storage`", preamble=("$y = 2",))

    storage = PermanentDataStorage(DummyData())
    result = factory["sample_with_data_and_preamble.j2"].render({"storage": storage})
    assert result == "<YQL-PREAMBLE>$x = 1</YQL-PREAMBLE>SELECT * FROM `//storage`<YQL-PREAMBLE>$y = 2</YQL-PREAMBLE> LIMIT 100"


def test_table_tag_expands_to_valid_json(factory):
    result = json.loads(factory["sample_with_table.j2"].render())
    assert result == {
        "schema": [
            {
                "name": "field",
                "type": "String",
            },
        ],
        "records": [
            ["field value, wow"],
        ],
    }


def test_table_tag_with_expression_as_schema_name_expands_to_valid_json(factory):
    template = factory["sample_with_table_and_expression_as_schema_name.j2"]
    result = json.loads(template.render({"table": "schema"}))
    assert result == {
        "schema": [
            {
                "name": "field",
                "type": "String",
            },
        ],
        "records": [
            ["another field value, wow"],
        ],
    }
