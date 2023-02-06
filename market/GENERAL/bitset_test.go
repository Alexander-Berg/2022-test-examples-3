package bitset

import (
	"testing"

	"github.com/stretchr/testify/require"
)

func TestBitset(t *testing.T) {
	bset := New(1000)
	for i := uint(0); i < bset.Len(); i++ {
		val, err := bset.Test(i)
		require.NoError(t, err)
		require.False(t, val)

		require.NoError(t, bset.Set(i))
		val, err = bset.Test(i)
		require.NoError(t, err)
		require.True(t, val)

		require.NoError(t, bset.Clear(i))
		val, err = bset.Test(i)
		require.NoError(t, err)
		require.False(t, val)
	}
}

func TestBitsetError(t *testing.T) {
	size := uint(1000)
	bset := New(size)

	_, err := bset.Test(size)
	require.Error(t, err)
	require.Error(t, bset.Set(size))
	require.Error(t, bset.Clear(size))
}

func TestBitsetWordsNeeded(t *testing.T) {
	require.Equal(t, 0, wordsNeeded(0))
	require.Equal(t, 1, wordsNeeded(1))
	require.Equal(t, 1, wordsNeeded(64))
	require.Equal(t, 2, wordsNeeded(65))
	require.Equal(t, 16, wordsNeeded(1024))
}

func TestResizeAndClear(t *testing.T) {
	bset := New(64)
	require.Equal(t, 1, len(bset.set))
	require.NoError(t, bset.Set(63))

	bset.ResizeAndClear(128)
	require.Equal(t, 2, len(bset.set))
	require.False(t, bset.SafeTest(63))
	require.NoError(t, bset.Set(63))

	bset.ResizeAndClear(64)
	require.Equal(t, 2, len(bset.set))  // no realloc
	require.False(t, bset.SafeTest(63)) // clear
}
