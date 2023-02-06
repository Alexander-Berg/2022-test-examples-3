from market.backbone.offers_store.yt_sync.settings.tables import get_schema_generator


def test_generate_schema():
    get_schema = get_schema_generator(
        keys=[("A", "string")],
        columns=[("B", "string"), ("C", "uint64"), ("D", "uint64", "sum")],
    )
    assert [
        {
            "name": "Hash",
            "expression": "farm_hash(A) % 100",
            "type": "uint64",
            "sort_order": "ascending",
        },
        {"name": "A", "type": "string", "sort_order": "ascending"},
        {"name": "B", "type": "string"},
        {"name": "C", "type": "uint64"},
        {"name": "D", "type": "uint64", "aggregate": "sum"},
    ] == get_schema()


def test_generate_schema_with_hunks():
    get_schema = get_schema_generator(
        keys=[("Key", "string")],
        columns=[("String", "string"), ("Uint", "uint64"), ("Aggregate", "uin64", "sum")],
    )

    schema = get_schema(max_inline_hunk_size=128)

    hunks = []
    for name in ("Hash", "Key", "String", "Uint", "Aggregate"):
        column = next(c for c in schema if c["name"] == name)
        hunks.append(column.get("max_inline_hunk_size"))

    assert [None, None, 128, None, None] == hunks
