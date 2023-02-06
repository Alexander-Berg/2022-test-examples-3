package breaker

import "testing"

func TestStates(t *testing.T) {
	v := States{}
	if v.Closed() {
		t.Errorf("empty states are always open")
	}

	v["a"] = StateOpen
	if v.Closed() {
		t.Error()
	}

	v["a"] = StateClosed
	if !v.Closed() {
		t.Error()
	}

	v["b"] = StateOpen
	if v.Closed() {
		t.Error()
	}

	v["b"] = StateClosed
	if !v.Closed() {
		t.Error()
	}
}
