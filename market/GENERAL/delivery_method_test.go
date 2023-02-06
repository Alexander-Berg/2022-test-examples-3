package enums

import (
	"testing"

	"github.com/stretchr/testify/require"
)

func TestDeliveryMethod(t *testing.T) {
	require.Equal(t, DeliveryMethodUnknown, GetDeliveryMethod("foo"))

	require.Equal(t, DeliveryMethodCourier, GetDeliveryMethod("COURIER"))
	require.Equal(t, "COURIER", DeliveryMethodCourier.String())
}
