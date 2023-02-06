package utils

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestP2PKey_GetFlightP2PKey(t *testing.T) {
	assert.Equal(t, uint64(1), GetFlightP2PKey(0, 1))
	assert.Equal(t, uint64(1<<32), GetFlightP2PKey(1, 0))
}

func TestP2PKey_ParseFlightP2PKey(t *testing.T) {
	departureStation, arrivalStation := ParseFlightP2PKey(GetFlightP2PKey(27376, 90851))
	assert.Equal(t, int32(27376), departureStation)
	assert.Equal(t, int32(90851), arrivalStation)
}
