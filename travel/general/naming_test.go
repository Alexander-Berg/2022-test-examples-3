package utils

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestNaming_NumericFlightNumber(t *testing.T) {
	assert.Equal(t, int32(0), NumericFlightNumber(""))
	assert.Equal(t, int32(1), NumericFlightNumber("1"))
	assert.Equal(t, int32(1002), NumericFlightNumber("1002"))
	assert.Equal(t, int32(6544), NumericFlightNumber("6544"))
	assert.Equal(t, int32(6544), NumericFlightNumber("6544а"))
	assert.Equal(t, int32(0), NumericFlightNumber("12345678")) // longer than 7
}

func TestNaming_TrimZeroesFromFlightNumber(t *testing.T) {
	assert.Equal(t, "34", TrimZeroesFromFlightNumber("034"))
	assert.Equal(t, "340", TrimZeroesFromFlightNumber("0340"))
	assert.Equal(t, "3400", TrimZeroesFromFlightNumber("3400"))
	assert.Equal(t, "0", TrimZeroesFromFlightNumber("0"))
	assert.Equal(t, "0", TrimZeroesFromFlightNumber("000"))
	assert.Equal(t, "0", TrimZeroesFromFlightNumber(""))
}
