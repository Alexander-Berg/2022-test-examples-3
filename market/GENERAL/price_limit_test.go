package routes

import (
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/enums"
)

func TestParseDserviceMaxPrice(t *testing.T) {
	dtmap, err := parseDserviceMaxPrice()
	require.NoError(t, err)
	require.True(t, len(dtmap) != 0)
}

func TestFilterPaymentMethodsByTotalPrice(t *testing.T) {
	allPayments := enums.AllPaymentMethods

	require.Equal(t, allPayments, FilterPaymentMethodsByTotalPrice(allPayments, 123456, 100_001))

	require.Equal(t, allPayments, FilterPaymentMethodsByTotalPrice(allPayments, 9, 9_999))
	require.Equal(t, allPayments, FilterPaymentMethodsByTotalPrice(allPayments, 9, 10_001))
	require.Equal(t, enums.MethodPrepayAllowed, FilterPaymentMethodsByTotalPrice(allPayments, 9, 100_001))
	require.Equal(t, enums.MethodPrepayAllowed, FilterPaymentMethodsByTotalPrice(enums.MethodPrepayAllowed, 9, 100_001))
	require.Equal(t, enums.PaymentMethodsMask(0), FilterPaymentMethodsByTotalPrice(enums.MethodCashAllowed, 9, 100_001))

	require.Equal(t, enums.MethodPrepayAllowed, FilterPaymentMethodsByTotalPrice(allPayments, 55741, 9_999))
	require.Equal(t, enums.MethodUnknown, FilterPaymentMethodsByTotalPrice(allPayments, 55741, 10_001))
	require.Equal(t, enums.MethodUnknown, FilterPaymentMethodsByTotalPrice(allPayments, 55741, 100_001))
}

func TestGetMaxPrice(t *testing.T) {
	require.Equal(t, 10000, int(GetMaxPriceForDS(55741)))
	require.Equal(t, 0, int(GetMaxPriceForDS(9)))
}
