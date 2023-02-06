package containers

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func predicate(f bool) bool {
	return f
}

func TestFilterBy(t *testing.T) {
	tests := []struct {
		name           string
		values         []bool
		expectedLength int
	}{
		{
			name:           "empty",
			values:         []bool{},
			expectedLength: 0,
		},
		{
			name:           "one true -> return same",
			values:         []bool{true},
			expectedLength: 1,
		},
		{
			name:           "many trues -> return same",
			values:         []bool{true, true},
			expectedLength: 2,
		},
		{
			name:           "one false -> return empty",
			values:         []bool{false},
			expectedLength: 0,
		},
		{
			name:           "many false -> return empty",
			values:         []bool{false, false},
			expectedLength: 0,
		},
		{
			name:           "mixed -> return trues only",
			values:         []bool{true, false, true, true, false},
			expectedLength: 3,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			assert.Equal(t, tt.expectedLength, len(FilterBy(tt.values, predicate)))
		})
	}
}

func TestCountIf(t *testing.T) {
	tests := []struct {
		name          string
		values        []bool
		expectedCount int
	}{
		{
			name:          "empty",
			values:        []bool{},
			expectedCount: 0,
		},
		{
			name:          "one true -> return same",
			values:        []bool{true},
			expectedCount: 1,
		},
		{
			name:          "many trues -> return same",
			values:        []bool{true, true},
			expectedCount: 2,
		},
		{
			name:          "one false -> return empty",
			values:        []bool{false},
			expectedCount: 0,
		},
		{
			name:          "many false -> return empty",
			values:        []bool{false, false},
			expectedCount: 0,
		},
		{
			name:          "mixed -> return trues only",
			values:        []bool{true, false, true, true, false},
			expectedCount: 3,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			assert.Equal(t, tt.expectedCount, CountIf(tt.values, predicate))
		})
	}
}

func TestIterateByWindow(t *testing.T) {
	tests := []struct {
		name     string
		values   []int
		n        int
		expected [][]int
	}{
		{
			name:     "empty window",
			values:   []int{1, 2, 3, 4},
			n:        0,
			expected: [][]int{},
		},
		{
			name:     "too big window",
			values:   []int{1, 2, 3, 4},
			n:        10,
			expected: [][]int{},
		},
		{
			name:     "n=1",
			values:   []int{1, 2, 3, 4},
			n:        1,
			expected: [][]int{{1}, {2}, {3}, {4}},
		},
		{
			name:     "n=3",
			values:   []int{1, 2, 3, 4},
			n:        3,
			expected: [][]int{{1, 2, 3}, {2, 3, 4}},
		},
		{
			name:     "no values",
			values:   []int{},
			n:        3,
			expected: [][]int{},
		},
		{
			name:     "no values, no window",
			values:   []int{},
			n:        0,
			expected: [][]int{},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			assert.Equal(t, tt.expected, IterateByWindow(tt.values, tt.n))
		})
	}
}
