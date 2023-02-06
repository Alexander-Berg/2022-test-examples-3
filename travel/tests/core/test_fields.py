import enum
import pytest

from travel.hotels.feeders.lib.model import enums, fields, objects

S = objects.serialize  # shortcut for less boilerplate in tests code


def build(**class_fields):
    """A helper function which builds a TestClass out of provided fields and initializes an instance of it"""
    return type("TestClass", (object,), class_fields)()


def test_simple_field_assign():
    obj = build(field=fields.Field("foo"))
    obj.field = "BAR"
    assert S(obj)["foo"] == "BAR"


def test_simple_field_single_add():
    obj = build(field=fields.Field("foo"))
    obj.field.add("BAR")
    assert S(obj)["foo"] == "BAR"


def test_multivalue_field_assign_then_add():
    obj = build(field=fields.Field("foo", allow_multi=True))
    obj.field = "FOO"
    assert S(obj)["foo"] == ["FOO"]
    obj.field.add("BAR")
    assert S(obj)["foo"] == ["FOO", "BAR"]


def test_single_value_cant_add_more_then_one():
    obj = build(field=fields.Field("foo"))
    obj.field.add("FOO")
    with pytest.raises(ValueError):
        obj.field.add("BAR")


def test_single_value_set_and_reset():
    obj = build(field=fields.Field("foo"))
    obj.field.set("FOO")
    assert S(obj)["foo"] == "FOO"
    obj.field.set("BAR")
    assert S(obj)["foo"] == "BAR"


def test_multi_value_set_and_reset():
    obj = build(field=fields.Field("foo", allow_multi=True))
    obj.field.set("FOO")
    assert S(obj)["foo"] == ["FOO"]
    obj.field.set("BAR")
    assert S(obj)["foo"] == ["BAR"]


def test_multi_value_assign_multiple_and_add():
    obj = build(field=fields.Field("foo", allow_multi=True))
    obj.field.set("foo")
    obj.field.add("bar")
    assert S(obj)["foo"] == ["foo", "bar"]


def test_field_cant_pass_kwargs():
    obj = build(field=fields.Field("foo", allow_multi=True))
    with pytest.raises(TypeError):
        obj.field.set("some value", lang="bar")


def test_field_with_no_value():
    obj = build(field=fields.Field("foo"))
    assert "foo" in S(obj)
    assert S(obj)["foo"] is None


def test_two_fields_with_same_name():
    obj = build(field1=fields.Field("foo"), field2=fields.Field("foo"))
    assert S(obj)["foo"] == []
    obj.field1 = "bar"
    assert S(obj)["foo"] == ["bar"]
    obj.field2 = "baz"
    assert S(obj)["foo"].sort() == ["bar", "baz"].sort()


def test_localized_field():
    obj = build(field=fields.Field("foo", field_type=fields.Localized, allow_multi=True))
    obj.field = "foo"
    assert S(obj)["foo"] == [{"value": "foo"}]
    obj.field.add("another")
    assert S(obj)["foo"] == [{"value": "foo"}, {"value": "another"}]
    obj.field.add("localized", lang=enums.Language.RU)
    assert S(obj)["foo"] == [{"value": "foo"}, {"value": "another"}, {"value": "localized", "lang": "RU"}]


def test_localized_field_cant_accept_invalid_language():
    obj = build(field=fields.Field("foo", field_type=fields.Localized))
    with pytest.raises(ValueError):
        obj.field.set("FOO", lang="wrong")


def test_localized_field_with_overriden_value_key():
    obj = build(field=fields.Field("foo", field_type=fields.Localized, value_key="bar"))
    obj.field = "baz"
    assert S(obj)["foo"] == {"bar": "baz"}


def test_typed_localized():
    obj = build(field=fields.Field("foo", field_type=fields.TypedLocalized),
                another=fields.Field("bar", field_type=fields.TypedLocalized, type_class=dict),
                third=fields.Field("baz", field_type=fields.TypedLocalized, type_key="key"))
    obj.field = "FOO"
    assert S(obj)["foo"] == {"value": "FOO"}
    obj.field.set("FOO", type="footype")
    assert S(obj)["foo"] == {"value": "FOO", "type": "footype"}
    obj.field.set("FOO", type="footype", lang=enums.Language.RU)
    assert S(obj)["foo"] == {"value": "FOO", "type": "footype", "lang": "RU"}
    assert S(obj)["bar"] is None
    obj.another.set("BAR", type=dict(k="v"))
    assert S(obj)["bar"] == {"value": "BAR", "type": {"k": "v"}}
    with pytest.raises(ValueError):
        obj.another.set("BAR", type="wrong")
    obj.third.set("BAZ", type=42)
    assert S(obj)["baz"] == {"value": "BAZ", "key": 42}


def test_float_field():
    obj = build(field=fields.Field("foo", field_type=fields.Float))
    obj.field = 0.5
    assert S(obj)["foo"] == 0.5
    obj.field = 42
    assert S(obj)["foo"] == 42.0
    obj.field = "123.45"
    assert S(obj)["foo"] == 123.45
    with pytest.raises(ValueError):
        obj.field = "non-numeric string"


def test_int_field():
    obj = build(field=fields.Field("foo", field_type=fields.Int))
    obj.field = 1
    assert S(obj)["foo"] == 1
    obj.field = 0.3
    assert S(obj)["foo"] == 0
    obj.field = "15"
    assert S(obj)["foo"] == 15
    with pytest.raises(ValueError):
        obj.field = "non-numeric string"


def test_enum_field_as_name():
    class SomeEnum(enum.Enum):
        Foo = 1
        Bar = 2

    obj = build(field=fields.Field("foo", type_class=SomeEnum))
    obj.field = SomeEnum.Foo
    assert S(obj) == {"foo": "Foo"}
    with pytest.raises(ValueError):
        obj.field = "Foo"
    with pytest.raises(ValueError):
        obj.field = 1


def test_enum_field_as_value():
    class SomeEnum(enum.Enum):
        Foo = 1
        Bar = 2

    obj = build(field=fields.Field("foo", type_class=SomeEnum, enum_as_value=True))
    obj.field = SomeEnum.Foo
    assert S(obj) == {"foo": 1}
    obj.field = SomeEnum.Bar
    assert S(obj) == {"foo": 2}
    with pytest.raises(ValueError):
        obj.field = "Foo"
    with pytest.raises(ValueError):
        obj.field = 1


def test_boolean_features():
    obj = build(f1=fields.Field("feature", feature_name="has_something", field_type=fields.Feature,
                                type_class=bool),
                f2=fields.Field("feature", feature_name="is_good", field_type=fields.Feature,
                                type_class=bool))
    assert S(obj) == {"feature": []}
    obj.f1 = True
    assert S(obj) == {"feature": [{"id": "has_something", "value": True}]}
    obj.f2 = True
    obj.f1 = False
    assert S(obj) == {"feature": [{"id": "has_something", "value": False}, {"id": "is_good", "value": True}]}


def test_boolean_features_as_aliases():
    obj = build(f1=fields.BooleanFeatureField("has_something"), f2=fields.BooleanFeatureField("is_good"))
    assert S(obj) == {"feature": []}
    obj.f1 = True
    assert S(obj) == {"feature": [{"id": "has_something", "value": True}]}
    obj.f2 = True
    obj.f1 = False
    assert S(obj) == {"feature": [{"id": "has_something", "value": False}, {"id": "is_good", "value": True}]}


def test_enum_features():
    class SomeEnum(enum.Enum):
        Foo = 1
        Bar = 2
        Baz = 3

    obj = build(field1=fields.Field("feature", field_type=fields.EnumFeature,
                                    type_class=SomeEnum, feature_name="SingleTypeOfSomething"),
                field2=fields.Field("feature", field_type=fields.EnumFeature,
                                    type_class=SomeEnum, feature_name="MultipleTypeOfSomething", allow_multi=True))
    obj.field1 = SomeEnum.Foo
    obj.field2.add(SomeEnum.Bar)
    obj.field2.add(SomeEnum.Baz)
    actual = S(obj)
    actual["feature"].sort()
    target = {"feature": [{"id": "SingleTypeOfSomething", "enum_id": "Foo"},
                          {"id": "MultipleTypeOfSomething", "enum_id": "Bar"},
                          {"id": "MultipleTypeOfSomething", "enum_id": "Baz"}]}
    target["feature"].sort()
    assert target == actual


def test_enum_features_as_aliases():
    class SomeEnum(enum.Enum):
        Foo = 1
        Bar = 2
        Baz = 3

    obj = build(field1=fields.EnumFeatureField("SingleTypeOfSomething", SomeEnum),
                field2=fields.EnumFeatureField("MultipleTypeOfSomething", SomeEnum, allow_multi=True))
    obj.field1 = SomeEnum.Foo
    obj.field2.add(SomeEnum.Bar)
    obj.field2.add(SomeEnum.Baz)
    actual = S(obj)
    actual["feature"].sort()
    target = {"feature": [{"id": "SingleTypeOfSomething", "enum_id": "Foo"},
                          {"id": "MultipleTypeOfSomething", "enum_id": "Bar"},
                          {"id": "MultipleTypeOfSomething", "enum_id": "Baz"}]}
    target["feature"].sort()
    assert target == actual


def test_enum_value_feature():
    class SomeEnum(str, enum.Enum):
        spaced_field = "spaced field"

    obj = build(field1=fields.Field("feature", field_type=fields.EnumValueFeature,
                                    type_class=SomeEnum, feature_name="SingleTypeOfSomething"))
    obj.field1 = SomeEnum.spaced_field
    actual = S(obj)
    target = {
        "feature": {
            "id": "SingleTypeOfSomething",
            "enum_id": "spaced field"
        },
    }
    assert target == actual


def test_min_max_features():
    obj = build(field=fields.Field("feature", field_type=fields.MinMaxFeature, feature_name="price_range"))
    with pytest.raises(ValueError):
        obj.field = 0
    obj.field = 100, 500
    assert S(obj) == {"feature": {"id": "price_range", "min": 100, "max": 500}}
    obj.field = "few", "a lot"
    assert S(obj) == {"feature": {"id": "price_range", "min": "few", "max": "a lot"}}


def test_min_max_features_as_alias():
    obj = build(field=fields.MinMaxFeatureField("price_range"))
    with pytest.raises(ValueError):
        obj.field = 0
    obj.field = 100, 500
    assert S(obj) == {"feature": {"id": "price_range", "min": 100, "max": 500}}
    obj.field = "few", "a lot"
    assert S(obj) == {"feature": {"id": "price_range", "min": "few", "max": "a lot"}}


def test_typed_min_max_features_as_alias():
    obj = build(field=fields.MinMaxFeatureField("price_range", type_class=int))
    with pytest.raises(ValueError):
        obj.field = "few", "a lot"


def test_yt_schema_inference_1():
    obj = build(a=fields.Field("field_a"),
                b=fields.Field("field_b", allow_multi=True))
    schema = objects.build_schema(obj.__class__)
    assert schema == [{"name": "field_a", "type": "string"},
                      {"name": "field_b", "type": "any"}]


def test_yt_schema_inference_2():
    obj = build(a=fields.Field("field_a", schema_priority=100),
                b=fields.Field("field_b", allow_multi=True, schema_priority=50),
                c=fields.BooleanFeatureField("awesome_feature"))
    schema = objects.build_schema(obj.__class__)
    assert schema == [{"name": "field_b", "type": "any"},
                      {"name": "field_a", "type": "string"},
                      {"name": "feature", "type": "any"}]


def test_rubric_field():
    obj = build(rubric=fields.Field("rubric", field_type=fields.Rubric, allow_multi=True))
    obj.rubric = enums.HotelRubric.HOTEL
    assert {"rubric": [{"value": str(enums.HotelRubric.HOTEL.value)}]} == S(obj)

    with pytest.raises(ValueError):
        obj.rubric = 123
