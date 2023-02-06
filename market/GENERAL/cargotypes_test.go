package graph

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestCargoIntersetion(t *testing.T) {
	ct1 := CargoTypes{1, 2, 3}
	ct2 := CargoTypes{2, 3, 4, 5}
	ct3 := CargoTypes{5, 6}
	ct4 := CargoTypes{}

	has := HasCargoIntersection(ct1, ct2)
	assert.True(t, has)

	has = HasCargoIntersection(ct2, ct3)
	assert.True(t, has)

	has = HasCargoIntersection(ct1, ct3)
	assert.False(t, has)

	for _, ct := range []CargoTypes{ct1, ct2, ct3} {
		has = HasCargoIntersection(ct, ct)
		assert.True(t, has)

		has = HasCargoIntersection(ct, ct4)
		assert.False(t, has)

		has = HasCargoIntersection(ct4, ct)
		assert.False(t, has)
	}
}
