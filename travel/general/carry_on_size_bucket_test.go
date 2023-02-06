package storage

import (
	"testing"

	"github.com/stretchr/testify/require"
)

func TestGetCarryOnSizeBucket(t *testing.T) {
	t.Run(
		"TestGetCarryOnSizeBucket", func(t *testing.T) {
			require.EqualValues(t, UnknownCarryOnBucketSize, GetCarryOnSizeBucket(""))
			require.EqualValues(t, UnknownCarryOnBucketSize, GetCarryOnSizeBucket("49xqqqx47"))
			require.EqualValues(t, RegularCarryOnBucketSize, GetCarryOnSizeBucket("55x40x25"))
			require.EqualValues(t, SmallCarryOnBucketSize, GetCarryOnSizeBucket("49x48x47"))
		},
	)
}
