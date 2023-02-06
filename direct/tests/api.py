import datetime as dt

import pytest

from sandbox import common


class TestApiInputOutput(object):
    def test__string(self):
        param = common.api.String()
        param_default = common.api.String(default="asdf")
        param_required = common.api.String(required=True)
        param_required_default = common.api.String(required=True, default="qwer")

        assert not param
        assert param.decode(None) is None
        assert param.encode(None) is None
        assert param.decode("qwer") == "qwer"
        assert param.encode("qwer") == "qwer"
        assert param.decode(123) == "123"
        assert param.encode(123) == "123"

        assert not param_default
        assert param_default.decode(None) == "asdf"
        assert param_default.decode("") == ""
        assert param_default.decode("qwer") == "qwer"

        assert param_required
        with pytest.raises(ValueError):
            param_required.decode(None)
        with pytest.raises(ValueError):
            param_required.decode(None)

        assert param_required_default
        with pytest.raises(ValueError):
            param_required_default.decode(None)
        with pytest.raises(ValueError):
            param_required_default.encode(None)

    def test__datetime(self):
        value = dt.datetime.utcnow()
        value_str = value.isoformat() + "Z"
        param = common.api.DateTime()
        param_default = common.api.DateTime(default=value)
        param_required = common.api.DateTime(required=True)

        assert not param
        assert param.decode(None) is None
        assert param.decode(value_str) == value
        assert param.encode(None) is None
        assert param.encode(value) == value_str
        with pytest.raises(ValueError):
            param.encode(value_str)

        assert not param_default
        assert param_default.decode(None) == value
        assert param_default.encode(None) == value_str

        assert param_required
        with pytest.raises(ValueError):
            param_required.decode(None)
        with pytest.raises(ValueError):
            param_required.encode(None)
        assert param_required.decode(value_str) == value

    def test__datetimerange(self):
        value = [dt.datetime.utcnow(), dt.datetime.now()]
        value_str = "{}Z..{}Z".format(value[0].isoformat(), value[1].isoformat())
        param = common.api.DateTimeRange()
        param_default = common.api.DateTimeRange(default=value)
        param_required = common.api.DateTimeRange(required=True)

        assert not param
        assert param.decode(None) is None
        assert param.decode(value_str) == value
        assert param.encode(None) is None
        assert param.encode(value) == value_str
        with pytest.raises(ValueError):
            param.encode(value_str)

        assert not param_default
        assert param_default.decode(None) == value
        assert param_default.encode(None) == value_str

        assert param_required
        with pytest.raises(ValueError):
            param_required.decode(None)
        with pytest.raises(ValueError):
            param_required.encode(None)
        assert param_required.decode(value_str) == value

    def test__enum(self):
        class Enum(common.utils.Enum):
            A = None
            B = None
            C = None

        param_empty = common.api.Enum()
        param = common.api.Enum(values=Enum)
        param_default = common.api.Enum(default=Enum.A, values=Enum)
        param_required = common.api.Enum(required=True, values=Enum)

        assert not param_empty
        assert param_empty.decode(None) is None
        assert param_empty.encode(None) is None
        with pytest.raises(ValueError):
            param_empty.decode(Enum.A)
        with pytest.raises(ValueError):
            param_empty.encode(Enum.A)

        assert not param
        assert param.decode(None) is None
        # noinspection PyTypeChecker
        for item in Enum:
            assert param.decode(item) == item
        with pytest.raises(ValueError):
            param.decode("D")
        assert param.encode(None) is None
        # noinspection PyTypeChecker
        for item in Enum:
            assert param.encode(item) == item
        with pytest.raises(ValueError):
            param.decode("D")

        assert not param_default
        assert param_default.decode(None) == Enum.A
        assert param_default.encode(None) == Enum.A

        assert param_required
        with pytest.raises(ValueError):
            param_required.decode(None)
        with pytest.raises(ValueError):
            param_required.encode(None)
        assert param_required.decode(Enum.B) == Enum.B
        assert param_required.encode(Enum.C) == Enum.C

    def test__integer(self):
        param = common.api.Integer()
        param_default = common.api.Integer(default=123)
        param_required = common.api.Integer(required=True)
        param_required_default = common.api.Integer(required=True, default=321)

        assert not param
        assert param.decode(None) is None
        assert param.encode(None) is None
        assert param.decode(111) == 111
        assert param.encode(222) == 222
        assert param.decode("333") == 333
        assert param.encode("444") == 444
        for v in ("", "qwer"):
            with pytest.raises(ValueError):
                param.decode(v)
            with pytest.raises(ValueError):
                param.encode(v)

        assert not param_default
        assert param_default.decode(None) == 123
        assert param_default.encode(None) == 123

        assert param_required
        with pytest.raises(ValueError):
            param_required.decode(None)
        with pytest.raises(ValueError):
            param_required.encode(None)

        assert param_required_default
        with pytest.raises(ValueError):
            param_required_default.decode(None)
        with pytest.raises(ValueError):
            param_required_default.encode(None)

    def test__id(self):
        param = common.api.Id(required=False)
        param_default = common.api.Id(required=False, default=123)
        param_required = common.api.Id()
        param_required_default = common.api.Id(default=321)

        assert not param
        assert param.decode(None) is None
        assert param.encode(None) is None
        assert param.decode(111) == 111
        assert param.encode(222) == 222
        assert param.decode("333") == 333
        assert param.encode("444") == 444
        for v in ("", "qwer", 0, -123):
            with pytest.raises(ValueError):
                param.decode(v)
            with pytest.raises(ValueError):
                param.encode(v)

        assert not param_default
        assert param_default.decode(None) == 123
        assert param_default.encode(None) == 123

        assert param_required
        with pytest.raises(ValueError):
            param_required.decode(None)
        with pytest.raises(ValueError):
            param_required.encode(None)

        assert param_required_default
        with pytest.raises(ValueError):
            param_required_default.decode(None)
        with pytest.raises(ValueError):
            param_required_default.encode(None)

    def test__number(self):
        param = common.api.Number()
        param_default1 = common.api.Number(default=123)
        param_default2 = common.api.Number(default=123.456)
        param_required = common.api.Number(required=True)
        param_required_default = common.api.Number(required=True, default=321.654)

        assert not param
        assert param.decode(None) is None
        assert param.encode(None) is None
        assert param.decode(111) == 111
        assert param.encode(111) == 111
        assert param.decode(222.333) == 222.333
        assert param.encode(222.333) == 222.333
        assert param.decode("333.222") == 333.222
        assert param.encode("333.222") == 333.222
        assert param.decode("444") == 444
        assert param.encode("444") == 444
        for v in ("", "qwer"):
            with pytest.raises(ValueError):
                param.decode(v)
            with pytest.raises(ValueError):
                param.encode(v)

        assert not param_default1
        assert param_default1.decode(None) == 123

        assert not param_default2
        assert param_default2.decode(None) == 123.456

        assert param_required
        with pytest.raises(ValueError):
            param_required.decode(None)
        with pytest.raises(ValueError):
            param_required.encode(None)

        assert param_required_default
        with pytest.raises(ValueError):
            param_required_default.decode(None)
        with pytest.raises(ValueError):
            param_required_default.encode(None)

    def test__boolean(self):
        param = common.api.Boolean()
        param_default1 = common.api.Boolean(default=False)
        param_default2 = common.api.Boolean(default=True)
        param_required = common.api.Boolean(required=True)
        param_required_default = common.api.Boolean(required=True, default=True)

        assert not param
        assert param.decode(None) is None
        assert param.encode(None) is None
        for v in (False, 0, "0", "false", "no", "off"):
            assert param.decode(v) is False
        for v in (False, 0, ""):
            assert param.encode(v) is False
        for v in (True, 1, "1", "true", "yes", "on"):
            assert param.decode(v) is True
        for v in (True, 1, "a"):
            assert param.encode(v) is True
        for v in ("", "qwer", "2", 123):
            with pytest.raises(ValueError):
                param.decode(v)

        assert not param_default1
        assert param_default1.decode(None) is False
        assert param_default1.encode(None) is False

        assert not param_default2
        assert param_default2.decode(None) is True
        assert param_default2.encode(None) is True

        assert param_required
        with pytest.raises(ValueError):
            param_required.decode(None)
        with pytest.raises(ValueError):
            param_required.encode(None)

        assert param_required_default
        with pytest.raises(ValueError):
            param_required_default.decode(None)
        with pytest.raises(ValueError):
            param_required_default.encode(None)

    def test__object(self):
        param = common.api.Object()
        value = {"qwer": 123, 321: "asdf"}
        param_default1 = common.api.Object(default={})
        param_default2 = common.api.Object(default=value)
        param_required = common.api.Object(required=True)
        param_required_default = common.api.Object(required=True, default=value)

        assert not param
        assert param.decode(None) is None
        assert param.encode(None) is None
        assert param.decode({}) == {}
        assert param.encode({}) == {}
        assert param.decode(value) == value
        assert param.encode(value) == value
        for v in ("", "qwer", 123, []):
            with pytest.raises(ValueError):
                param.decode(v)
            with pytest.raises(ValueError):
                param.encode(v)

        assert not param_default1
        assert param_default1.decode(None) == {}
        assert param_default1.encode(None) == {}

        assert not param_default2
        assert param_default2.decode(None) == value
        assert param_default2.encode(None) == value

        assert param_required
        with pytest.raises(ValueError):
            param_required.decode(None)
        with pytest.raises(ValueError):
            param_required.encode(None)

        assert param_required_default
        with pytest.raises(ValueError):
            param_required_default.decode(None)
        with pytest.raises(ValueError):
            param_required_default.encode(None)

    def test__array(self):
        with pytest.raises(AssertionError):
            common.api.Array()
        param1 = common.api.Array(common.api.String)
        param2 = common.api.Array(common.api.Integer)
        param3 = common.api.Array(common.api.Integer, collection_format=common.api.ArrayFormat.MULTI)
        value1 = ["a", "b", "c"]
        value2 = [1, 2, 3]
        param_default1 = common.api.Array(common.api.String, default=value1)
        param_default2 = common.api.Array(common.api.Integer, default=value2)
        param_default3 = common.api.Array(common.api.String, default=[])
        param_required = common.api.Array(common.api.String, required=True)
        param_required_default = common.api.Array(common.api.String, required=True, default=value1)

        assert not param1
        assert param1.decode(None) is None
        assert param1.encode(None) is None
        assert param1.decode("") == []
        assert param1.encode([]) == []
        assert param1.decode(",".join(value1)) == value1
        assert param1.encode(value1) == value1
        for v in (123, {}, []):
            with pytest.raises(ValueError):
                param1.decode(v)

        assert not param2
        assert param2.decode(",".join(map(str, value2))) == value2
        assert param2.encode(value2) == value2
        for v in ("qwer", 123, {}, ",".join(value1)):
            with pytest.raises(ValueError):
                param2.decode(v)
        for v in ("qwer", ",".join(value1), ",".join(map(str, value2))):
            with pytest.raises(ValueError):
                param2.encode(v)

        assert not param3
        assert param3.decode(None) is None
        assert param3.encode(None) is None
        assert param3.decode("123") == [123]
        assert param3.encode([]) == []
        assert param3.decode(value2) == value2
        assert param3.encode(value2) == value2
        for v in ("", "qwer", value1, ",".join(value1)):
            with pytest.raises(ValueError):
                param3.decode(v)

        assert not param_default1
        assert param_default1.decode(None) == value1
        assert param_default1.encode(None) == value1

        assert not param_default2
        assert param_default2.decode(None) == value2
        assert param_default2.encode(None) == value2

        assert not param_default3
        assert param_default3.decode(None) == []
        assert param_default3.encode(None) == []

        assert param_required
        with pytest.raises(ValueError):
            param_required.decode(None)
        with pytest.raises(ValueError):
            param_required.encode(None)

        assert param_required_default
        with pytest.raises(ValueError):
            param_required_default.decode(None)
        with pytest.raises(ValueError):
            param_required_default.encode(None)

    def test__schema(self):
        pass  # TODO


class TestRequest(object):
    def test__params_order(self):
        pass  # TODO
