package carrier

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestPutCarrier_SingleValue(t *testing.T) {
	c := NewCarrierStorage()

	c.PutCarrier(1, "ZZ", "", "", "")
	assert.Equal(t, "ZZ", c.GetCarrierByID(1))
	assert.Equal(t, []int32{1}, c.GetCarriersByCode("ZZ"))
}

func TestPutCarrier_EmptyIata(t *testing.T) {
	c := NewCarrierStorage()

	c.PutCarrier(1, "", "", "", "")
	assert.Equal(t, "", c.GetCarrierByID(1))
}

func TestPutCarrier_MultipleValues(t *testing.T) {
	c := NewCarrierStorage()

	c.PutCarrier(1, "ZZ", "", "", "")
	c.PutCarrier(5, "ZZ", "", "", "")
	assert.Equal(t, "ZZ", c.GetCarrierByID(1))
	assert.Equal(t, "ZZ", c.GetCarrierByID(5))
	assert.Equal(t, []int32{1, 5}, c.GetCarriersByCode("ZZ"))
}

func TestPutCarrier_SameValueMoreThanOnce(t *testing.T) {
	c := NewCarrierStorage()

	c.PutCarrier(1, "ZZ", "", "", "")
	c.PutCarrier(1, "ZZ", "", "", "")
	assert.Equal(t, "ZZ", c.GetCarrierByID(1))
	assert.Equal(t, []int32{1}, c.GetCarriersByCode("ZZ"))
}

func TestGetCode_ByID(t *testing.T) {
	c := NewCarrierStorage()

	c.PutCarrier(1, "XX", "YY", "ZZ", "")
	c.PutCarrier(2, "", "YY", "ZZ", "")
	c.PutCarrier(3, "", "", "ZZ", "")
	assert.Equal(t, "XX", c.GetCarrierCodeByID(1))
	assert.Equal(t, "YY", c.GetCarrierCodeByID(2))
	assert.Equal(t, "ZZ", c.GetCarrierCodeByID(3))
}
