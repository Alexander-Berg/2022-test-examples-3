package config

import (
	"rex/common/types"
	"testing"
)

var AnyStringTests = []struct {
	V []string
	S string
}{
	{[]string{}, ""},
	{[]string{"a"}, "a"},
	{[]string{"", "b"}, "b"},
	{[]string{"a", "b"}, "a"},
}

func TestAnyString(t *testing.T) {
	for _, tt := range AnyStringTests {
		if s := AnyString(tt.V...); s != tt.S {
			t.Errorf("%v: want %q, got %q", tt.V, tt.S, s)
		}
	}
}

var AnyBooleanTests = []struct {
	V []types.Boolean
	B types.Boolean
}{
	{[]types.Boolean{}, nil},
	{[]types.Boolean{types.True}, types.True},
	{[]types.Boolean{nil, types.False}, types.False},
	{[]types.Boolean{types.True, types.False}, types.True},
}

func TestAnyBool(t *testing.T) {
	for _, tt := range AnyBooleanTests {
		if b := AnyBoolean(tt.V...); b != tt.B {
			t.Errorf("%v: want %v, got %v", tt.V, tt.B, b)
		}
	}
}
