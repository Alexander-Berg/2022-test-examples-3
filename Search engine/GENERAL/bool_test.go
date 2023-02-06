package types

import "testing"

func assertBoolean(t *testing.T, b Boolean, isTrue bool) {
	if b.True() != isTrue {
		t.Error(!isTrue)
	}
	if b.False() == isTrue {
		t.Error(isTrue)
	}
}

func TestTrueByDefault(t *testing.T) {
	{
		var b TrueByDefault
		assertBoolean(t, &b, true)
	}
	{
		var b *TrueByDefault
		assertBoolean(t, b, true)
	}
	{
		var inner *Bool
		b := TrueByDefault{inner}
		assertBoolean(t, &b, false)
	}
	{
		var inner Bool
		b := TrueByDefault{&inner}
		assertBoolean(t, &b, false)
	}
	{
		inner := Bool{true}
		b := TrueByDefault{&inner}
		assertBoolean(t, &b, true)
	}
}
