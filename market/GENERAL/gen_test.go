package main

import (
	"a.yandex-team.ru/market/logistics/wms-load/go/gen/common"
	"testing"

	"github.com/stretchr/testify/require"
)

func DisabledTestGenInventoriesSeq(t *testing.T) {
	require.Equal(t, []int{1, 1, 1}, common.GenInventorySizingSequence([]string{"abc", "def", "xyz"}, 5, 100))
	require.Equal(t, []int{1, 1, 1}, common.GenInventorySizingSequence([]string{"abc", "def", "xyz"}, 5, 10))
	require.Equal(t, []int{3}, common.GenInventorySizingSequence([]string{"abc", "def", "xyz"}, 1, 1))
	require.Equal(t, []int{1, 1, 1, 1, 1}, common.GenInventorySizingSequence([]string{"abc", "def", "xyz", "456", "789"}, 1, 10))
	require.Equal(t, []int{1, 2}, common.GenInventorySizingSequence([]string{"abc", "def", "xyz"}, 1, 2))
	require.Equal(t, []int{1, 2, 2}, common.GenInventorySizingSequence([]string{"abc", "def", "xyz", "tst", "111"}, 1, 3))
}
