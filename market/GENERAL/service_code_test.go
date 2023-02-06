package enums

import (
	"testing"

	"github.com/stretchr/testify/require"
)

func TestGetLmsServiceCode(t *testing.T) {
	require.Equal(t, ServiceUnknown, GetLmsServiceCode("foo"))
	require.Equal(t, ServiceUnknown, GetLmsServiceCode("TRANSPORT_MANAGER_MOVEMENT"))

	require.Equal(t, ServiceLastMile, GetLmsServiceCode("LAST_MILE"))
	require.Equal(t, "LAST_MILE", ServiceLastMile.String())
}
