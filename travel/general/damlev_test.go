package main

import (
	"testing"
)

var damLevCases = []struct {
	First    string
	Second   string
	Expected int
}{
	{"test", "test", 0},
	{"test", "tst", 1},
	{"tst", "test", 1},
	{"test", "tsst", 1},
	{"abc", "def", 3},
	{"test", "tst", 1},
	{"ab", "ba", 1},
	{"abc", "cba", 2},
	{"екб", "кекб", 1},
	{"екб", "факеротмкекб", 9},
	{"мисква", "екатеринбург", 11},
}

func TestDamLevDistance(t *testing.T) {
	for _, testCase := range damLevCases {
		result := DamLevDistance([]rune(testCase.First), []rune(testCase.Second))
		if result != testCase.Expected {
			t.Errorf("Result is %v, for test case %+v", result, testCase)
		}
	}
}

func BenchmarkDamLevDistance(b *testing.B) {
	for i := 0; i < b.N; i++ {
		DamLevDistance([]rune("Икотиринбурк"), []rune("Екатеринбург"))
	}
}
