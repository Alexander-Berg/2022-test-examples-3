package main

import (
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestFilterTags(t *testing.T) {
	in := []string{
		"ABC13:test",
		"ABC:test_2",
		"ABC:test!2",
		"table",
		"market_untable",
		"production",
		"market_unstable",
		"market_unstable1",
	}
	expected := []string{
		"ABC:test!2",
		"table",
		"market_untable",
		"market_unstable1",
	}
	out := filterTags(in)

	assert.Equal(t, expected, out)
}
