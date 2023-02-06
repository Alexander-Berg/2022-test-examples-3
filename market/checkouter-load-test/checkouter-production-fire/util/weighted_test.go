package util

import (
	"github.com/stretchr/testify/require"
	"testing"
)

func TestZeroOnSmallWeights(t *testing.T) {
	index, err := ChooseWeightedIndex(0, []int{})
	require.Zero(t, index)
	require.Errorf(t, err, "chosen more than total")
}

func TestZeroOnEmptyWeights(t *testing.T) {
	index, err := ChooseWeightedIndex(10, []int{10})
	require.Errorf(t, err, "chosen more than total")
	require.Zero(t, index)
}

func TestHugeWeightOnSmallWeights(t *testing.T) {
	for i := 0; i < 20; i++ {
		index, err := ChooseWeightedIndex(i, []int{10, 10})

		require.NoError(t, err)
		require.EqualValues(t, i/10, index)
	}
}
