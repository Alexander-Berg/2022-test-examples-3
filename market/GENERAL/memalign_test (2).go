package tarifficator

import (
	"testing"
	"unsafe"

	"github.com/stretchr/testify/assert"
)

// Этот тест нужен для понимания сколько занимают структуры в памяти.
// Можно смело менять значения в случае fail)
func TestMemAlign(t *testing.T) {
	assert.Equal(t, 8, int(unsafe.Alignof(TariffRT{})))
	assert.Equal(t, 120, int(unsafe.Sizeof(TariffRT{})))
}
