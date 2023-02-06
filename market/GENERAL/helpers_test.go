package helpers

import (
	"strings"
	"testing"

	"a.yandex-team.ru/market/sre/tools/config-primer/src/internal/globals"
)

func TestIsTrue(t *testing.T) {
	type testCase struct {
		input  string
		output bool
	}

	var cases = []testCase{
		{"    " + globals.KeyYesRus, true},
		{strings.ToUpper(globals.KeyYes), true},
		{strings.ToTitle(globals.KeyTrue), true},
		{"Да, конечно", false},
		{"Нет", false},
		{"NOOOOOOO!!!!", false},
	}
	for _, c := range cases {
		var res = IsTrue(c.input)
		if res != c.output {
			t.Errorf("IsTrue result was incorrect, on input \"%s\" got \"%v\", want: \"%v\"", c.input, res, c.output)
		}
	}
}
