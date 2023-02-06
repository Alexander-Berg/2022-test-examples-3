package util

import (
	"math"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestAtoui(t *testing.T) {
	tests := []struct {
		value string
		want  uint32
		ok    bool
	}{
		{
			value: "",
			want:  0,
			ok:    false,
		},
		{
			value: "000",
			want:  0,
			ok:    true,
		},
		{
			value: "123456789",
			want:  123456789,
			ok:    true,
		},
	}
	for _, tt := range tests {
		res, err := Atoui(tt.value)
		if tt.ok {
			require.NoError(t, err)
			require.Equal(t, tt.want, res)
		} else {
			require.Error(t, err)
		}

	}
}

func TestCheckSVNRevision(t *testing.T) {
	assert.Equal(t, false, SVNRevisionIsUpdated("0", "0"))
	assert.Equal(t, false, SVNRevisionIsUpdated("0", "1"))
	assert.Equal(t, false, SVNRevisionIsUpdated("", ""))
	assert.Equal(t, false, SVNRevisionIsUpdated("0", ""))
	assert.Equal(t, false, SVNRevisionIsUpdated("", "0"))
	assert.Equal(t, false, SVNRevisionIsUpdated("", "1"))

	assert.Equal(t, true, SVNRevisionIsUpdated("1", "0"))
	assert.Equal(t, true, SVNRevisionIsUpdated("1", ""))
}

func TestCombinatorErrorCode(t *testing.T) {
	require.Equal(t, "NO_COURIER_ROUTE", CombinatorErrorCode("no courier route, t:1 i:2 ds:3 c:4"))
	require.Equal(t, "ON_DEMAND_INTERNAL_ERROR", CombinatorErrorCode("ON_DEMAND_INTERNAL_ERROR"))
}

func TestCreateInt64Key(t *testing.T) {
	l := int64(math.MaxInt32)
	r := int64(math.MaxUint32)
	lr, err := CreateInt64Key(l, r)

	assert.NoError(t, err)
	assert.Equal(t, int64(math.MaxInt64), lr)

	l = math.MaxInt32 + 1
	lr, err = CreateInt64Key(l, r)
	assert.Error(t, err)
	assert.Equal(t, int64(0), lr)

	l = math.MaxInt32 + 1
	lr, err = CreateInt64Key(r, l)
	assert.Error(t, err)
	assert.Equal(t, int64(0), lr)

	l = 1
	r = 1
	lr, err = CreateInt64Key(r, l)
	assert.NoError(t, err)
	assert.Equal(t, int64(math.MaxUint32)+2, lr)
}
