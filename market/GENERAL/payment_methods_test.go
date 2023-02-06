package enums

import (
	"testing"

	"github.com/stretchr/testify/require"
)

func TestGetPaymentMethod(t *testing.T) {
	require.Equal(t, MethodUnknown, GetPaymentMethod(ServiceHanding))

	require.Equal(t, MethodPrepayAllowed, GetPaymentMethod(ServicePrepayAllowed))
	require.Equal(t, 2, int(MethodCashAllowed))
	require.Equal(t, 4, int(MethodCardAllowed))
	require.Equal(t, 0, int(MethodUnknown))
}
