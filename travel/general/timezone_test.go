package timezone

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func TestCompareOffsets_Basic(t *testing.T) {
	instant := time.Now()

	loc1, _ := time.LoadLocation("Europe/Madrid")
	loc2, _ := time.LoadLocation("Europe/Andorra")
	locMoscow, _ := time.LoadLocation("Europe/Moscow")

	assert.False(t, CompareOffsets(loc1, locMoscow, instant), "loc1 v. Mow")
	assert.False(t, CompareOffsets(loc2, locMoscow, instant), "loc2 v. Mow")
	assert.True(t, CompareOffsets(loc1, loc2, instant), "loc v. loc2")
}
