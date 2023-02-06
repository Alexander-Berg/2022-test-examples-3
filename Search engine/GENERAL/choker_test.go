package choker

import "testing"

var ChokerTests = []struct {
	Name   string
	Choker Choker
	States []State
}{
	{
		"New(0, 1)",
		New(0, 1),
		[]State{
			StateFail, StateFail,
			StateOK, StateOK,
		},
	},
	{
		"New(2, 1)",
		New(2, 1),
		[]State{
			StateFail, StateOK,
			StateFail, StateOK,
			StateFail, StateFail,
			StateOK, StateFail,
			StateOK, StateOK,
			StateOK, StateOK,
		},
	},
}

func TestChoker(t *testing.T) {
	for _, tt := range ChokerTests {
		if len(tt.States)%2 != 0 {
			t.Fatal("States field holds a list of states pairs (input + output).")
		}

		t.Run(tt.Name, func(t *testing.T) {
			for i := 0; i < len(tt.States); i += 2 {
				in, out := tt.States[i], tt.States[i+1]
				if s := tt.Choker.Choke(in); s != out {
					t.Fatalf("%v iteration: want %v, got %v", i/2, out, s)
				}
			}
		})
	}
}

var NewPanicsTests = []struct {
	Name             string
	Base, Hysteresis int64
}{
	{"Base is negative", -1, 1},
	{"Hysteresis is < 1", 1, 0},
}

func TestNew_panicWithNegativeBase(t *testing.T) {
	for _, tt := range NewPanicsTests {
		t.Run(tt.Name, func(t *testing.T) {
			defer func() {
				if e := recover(); e == nil {
					t.Error("panic is expected")
				}
			}()
			New(tt.Base, tt.Hysteresis)
		})
	}
}
