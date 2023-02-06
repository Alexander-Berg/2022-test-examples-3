package tarifficator

import (
	"testing"

	"github.com/stretchr/testify/require"
)

func TestUniq(t *testing.T) {
	type dlist []uint64
	require.Equal(t, []uint64{}, UniqDservices([]uint64{}))
	require.Equal(t, []uint64{1}, UniqDservices([]uint64{1, 1}))
	require.Equal(t, []uint64{1, 2, 3}, UniqDservices([]uint64{1, 2, 1, 3}))
	require.Equal(t, []uint64{1, 2, 3}, UniqDservices([]uint64{1, 2, 1, 3, 3}))
}
