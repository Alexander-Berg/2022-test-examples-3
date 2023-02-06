package routes

import (
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/dsbs"
	"a.yandex-team.ru/market/combinator/pkg/enums"
	cr "a.yandex-team.ru/market/combinator/proto/grpc"
)

func TestDayDiffCreation(t *testing.T) {
	start, err := time.Parse(time.RFC3339, "2021-10-21T15:04:05+03:00")
	require.NoError(t, err)

	diff := createDayDiff(
		&cr.Date{Year: 2021, Month: 10, Day: 21},
		start,
	)
	require.Equal(t, uint16(0), diff)

	diff = createDayDiff(
		&cr.Date{Year: 2021, Month: 10, Day: 22},
		start,
	)
	require.Equal(t, uint16(1), diff)

	diff = createDayDiff(
		&cr.Date{Year: 2021, Month: 10, Day: 25},
		start,
	)
	require.Equal(t, uint16(4), diff)

	diff = createDayDiff(
		&cr.Date{Year: 2021, Month: 11, Day: 5},
		start,
	)
	require.Equal(t, uint16(15), diff)

	// treat day before current as 0
	diff = createDayDiff(
		&cr.Date{Year: 2021, Month: 10, Day: 20},
		start,
	)
	require.Equal(t, uint16(0), diff)
}

func TestCreateFakePartnerOption(t *testing.T) {
	start, err := time.Parse(time.RFC3339, "2021-10-21T15:04:05+03:00")
	require.NoError(t, err)

	testPM := []enums.PaymentMethodsMask{
		enums.MethodPrepayAllowed,
		enums.MethodCashAllowed,
		enums.MethodCardAllowed,
		enums.MethodCashAllowed | enums.MethodCardAllowed,
		enums.AllPaymentMethods,
	}
	for _, pm := range testPM {
		po := createFakePartnerOptions(
			&cr.DeliveryOption{
				DateFrom: &cr.Date{Year: 2021, Month: 10, Day: 22},
				DateTo:   &cr.Date{Year: 2021, Month: 11, Day: 1},
			},
			start,
			pm,
		)
		require.Len(t, po, 2)

		expOpts := dsbs.PartnerOptions{
			dsbs.PartnerOption{
				DaysFrom:       uint16(1),
				DaysTo:         uint16(11),
				OrderBefore:    24,
				IsRawData:      false,
				DeliveryMethod: enums.DeliveryMethodCourier,
				PaymentMethods: pm,
			},
			dsbs.PartnerOption{
				DaysFrom:       uint16(1),
				DaysTo:         uint16(11),
				OrderBefore:    24,
				IsRawData:      false,
				DeliveryMethod: enums.DeliveryMethodPickup,
				PaymentMethods: pm,
			},
		}
		require.Equal(t, expOpts, po)
	}

	po := createFakePartnerOptions(nil, start, enums.AllPaymentMethods)
	require.Len(t, po, 0)
}
