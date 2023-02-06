package exprs

import (
	"testing"
)

type testScope struct {
	values   map[string]interface{}
	callable map[string]func(...interface{}) interface{}
}

func (s *testScope) Value(name string) interface{} {
	return s.values[name]
}

func (s *testScope) Call(name string, args ...interface{}) interface{} {
	return s.callable[name](args...)
}

func TestLogicalOp(t *testing.T) {
	if v, ok := (logicalOp{orLogicalOp, []Operator{
		constValueOp{0}, constValueOp{false}, constValueOp{""},
	}}).Exec(nil).(bool); !ok || v {
		t.Fatal("Invalid value")
	}
	if v, ok := (logicalOp{orLogicalOp, []Operator{
		constValueOp{1},
	}}).Exec(nil).(bool); !ok || !v {
		t.Fatal("Invalid value")
	}
	if v, ok := (logicalOp{orLogicalOp, []Operator{
		constValueOp{true},
	}}).Exec(nil).(bool); !ok || !v {
		t.Fatal("Invalid value")
	}
	if v, ok := (logicalOp{orLogicalOp, []Operator{
		constValueOp{"test"},
	}}).Exec(nil).(bool); !ok || !v {
		t.Fatal("Invalid value")
	}
}

func TestUnaryOp(t *testing.T) {
	s := testScope{
		values: map[string]interface{}{
			"name1": 123,
			"name2": "value2",
		},
	}
	if v, ok := (unaryOp{
		isNilOp, valueOp{"name1"},
	}).Exec(&s).(bool); !ok || v {
		t.Fatal("Invalid value")
	}
	if v, ok := (unaryOp{
		isNotNilOp, valueOp{"name1"},
	}).Exec(&s).(bool); !ok || !v {
		t.Fatal("Invalid value")
	}
}

func TestBinaryOp(t *testing.T) {
	s := testScope{
		values: map[string]interface{}{
			"name1": 123,
			"name2": "value2",
		},
	}
	fn := func(op binaryOpKind, name string, value interface{}) binaryOp {
		return binaryOp{op, valueOp{name}, constValueOp{value}}
	}
	if v, ok := fn(equalOp, "name1", 123).Exec(&s).(bool); !ok || !v {
		t.Fatal("Invalid value")
	}
	if v, ok := fn(notEqualOp, "name1", 123).Exec(&s).(bool); !ok || v {
		t.Fatal("Invalid value")
	}
	if v, ok := fn(equalOp, "name1", 228).Exec(&s).(bool); !ok || v {
		t.Fatal("Invalid value")
	}
	if v, ok := fn(lessOp, "name1", 123).Exec(&s).(bool); !ok || v {
		t.Fatal("Invalid value")
	}
	if v, ok := fn(lessEqualOp, "name1", 123).Exec(&s).(bool); !ok || !v {
		t.Fatal("Invalid value")
	}
	if v, ok := fn(lessOp, "name1", 228).Exec(&s).(bool); !ok || !v {
		t.Fatal("Invalid value")
	}
	if v, ok := fn(lessEqualOp, "name1", 228).Exec(&s).(bool); !ok || !v {
		t.Fatal("Invalid value")
	}
	if v, ok := fn(greaterOp, "name1", 123).Exec(&s).(bool); !ok || v {
		t.Fatal("Invalid value")
	}
	if v, ok := fn(greaterEqualOp, "name1", 123).Exec(&s).(bool); !ok || !v {
		t.Fatal("Invalid value")
	}
	if v, ok := fn(greaterOp, "name1", 228).Exec(&s).(bool); !ok || v {
		t.Fatal("Invalid value")
	}
	if v, ok := fn(greaterEqualOp, "name1", 228).Exec(&s).(bool); !ok || v {
		t.Fatal("Invalid value")
	}
	if v, ok := fn(lessOp, "name2", 321).Exec(&s).(bool); !ok || v {
		t.Fatal("Invalid value")
	}
	if v, ok := fn(lessEqualOp, "name2", 321).Exec(&s).(bool); !ok || v {
		t.Fatal("Invalid value")
	}
	if v, ok := fn(greaterOp, "name2", 321).Exec(&s).(bool); !ok || v {
		t.Fatal("Invalid value")
	}
	if v, ok := fn(greaterEqualOp, "name2", 321).Exec(&s).(bool); !ok || v {
		t.Fatal("Invalid value")
	}
}

func TestCallOp(t *testing.T) {
	s := testScope{
		callable: map[string]func(...interface{}) interface{}{
			"test1": func(args ...interface{}) interface{} {
				if len(args) == 1 {
					return args[0]
				}
				return nil
			},
		},
	}
	if v, ok := (callOp{"test1", []Operator{
		constValueOp{true},
	}}).Exec(&s).(bool); !ok || !v {
		t.Fatal("Invalid value")
	}
	if v, ok := (callOp{"test1", []Operator{
		constValueOp{false},
	}}).Exec(&s).(bool); !ok || v {
		t.Fatal("Invalid value")
	}
}
