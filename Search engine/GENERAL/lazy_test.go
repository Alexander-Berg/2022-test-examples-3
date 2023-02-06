package lazy

import "testing"

type Callable struct{ Called bool }

func (c *Callable) Call() { c.Called = true }

func TestCallAfter(t *testing.T) {
	c := new(Callable)
	f := CallAfter(0, c.Call)
	f()
	if !c.Called {
		t.Errorf("must be called")
	}

	c = new(Callable)
	f = CallAfter(1, c.Call)
	f()
	if !c.Called {
		t.Errorf("must be called")
	}

	c = new(Callable)
	f = CallAfter(2, c.Call)
	f()
	if c.Called {
		t.Errorf("must NOT be called")
	}
	f()
	if !c.Called {
		t.Errorf("must be called")
	}
}
