package dsbs

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestCargoTypesFilter(t *testing.T) {
	ct1 := []int32{1, 2, 900, 907, 999}
	ct2 := []int32{1, 2, 900, 998}
	ct3 := []int32{1, 2, 907, 997}
	ct4 := []int32{1, 2, 899, 902, 908, 999}

	shopID900 := uint32(1221108) // for 900
	shopID := uint32(322)

	// Only 900, 907
	ok := ValidateCargoTypes(shopID, ct1)
	assert.False(t, ok)

	ok = ValidateCargoTypes(shopID900, ct1)
	assert.False(t, ok)

	// Only 900
	ok = ValidateCargoTypes(shopID, ct2)
	assert.True(t, ok)

	ok = ValidateCargoTypes(shopID900, ct2)
	assert.False(t, ok)

	// Only 907
	ok = ValidateCargoTypes(shopID, ct3)
	assert.False(t, ok)

	ok = ValidateCargoTypes(shopID900, ct3)
	assert.False(t, ok)

	// No bad cargo types
	ok = ValidateCargoTypes(shopID, ct4)
	assert.True(t, ok)

	ok = ValidateCargoTypes(shopID900, ct4)
	assert.True(t, ok)
}
