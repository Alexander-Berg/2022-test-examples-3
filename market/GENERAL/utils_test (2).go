package main

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestSplitArgs(t *testing.T) {
	var tests = []struct {
		input    string
		expected []string
	}{
		{" -f adsasdas  -v 123 -d", []string{"-f", "adsasdas", "-v", "123", "-d"}},
		{"", []string{}},
		{"-a", []string{"-a"}},
		{"-f 'qweqw asda qwe ' -v 123", []string{"-f", "qweqw asda qwe ", "-v", "123"}},
		{"-q  \"123 123 a\"", []string{"-q", "123 123 a"}},
		{"-q  'ada \"123 \"'", []string{"-q", "ada \"123 \""}},
	}

	for _, test := range tests {
		assert.Equal(t, splitArgs(test.input), test.expected)
	}
}
