package dsbs

import (
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/enums"
)

func TestPaymentTypesConversion(t *testing.T) {
	res := ConvertPaymentTypes(nil)
	require.Equal(t, enums.AllPaymentMethods, res)

	res = ConvertPaymentTypes([]string{
		"COURIER_CARD",
		"COURIER_CASH",
		"PREPAYMENT_CARD",
		"PREPAYMENT_OTHER",
	})
	require.Equal(t, enums.AllPaymentMethods, res)

	res = ConvertPaymentTypes([]string{
		"COURIER_CARD",
		"COURIER_CASH",
		"PREPAYMENT_OTHER",
	})
	require.Equal(t, enums.AllPaymentMethods, res)

	res = ConvertPaymentTypes([]string{
		"COURIER_CARD",
		"PREPAYMENT_OTHER",
	})
	require.Equal(t, enums.MethodCardAllowed|enums.MethodPrepayAllowed, res)

	res = ConvertPaymentTypes([]string{
		"MY_CARD",
		"YOUR_CASH",
	})
	require.Equal(t, enums.MethodUnknown, res)
}
