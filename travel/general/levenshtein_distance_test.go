package speller

import (
	"testing"
)

var levenshteinCases = []struct {
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
	{"ab", "ba", 2},
	{"abc", "cba", 2},
	{"екб", "кекб", 1},
	{"екб", "факеротмкекб", 9},
	{"мисква", "екатеринбург", 11},
}

func TestLevenshteinDistance(t *testing.T) {
	dist := make([]int, 30)
	for _, testCase := range levenshteinCases {
		result := levenshteinDistance(testCase.First, testCase.Second, -1, dist)
		if result != testCase.Expected {
			t.Errorf("Result is %v, for test case %+v", result, testCase)
		}
	}
}

func BenchmarkLevenshteinDistance(b *testing.B) {
	dist := make([]int, 30)
	for i := 0; i < b.N; i++ {
		levenshteinDistance("Икотиринбурк", "Екатеринбург", -1, dist)
	}
}
