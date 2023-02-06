package fashion

import (
	"context"
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/its"
	"a.yandex-team.ru/market/combinator/pkg/settings"
	cr "a.yandex-team.ru/market/combinator/proto/grpc"
)

func TestIsTryingAvailableCourierOption(t *testing.T) {
	ctx := context.Background()
	// TODO remove with flag partial_delivery_flag_courier_options
	st := settings.New(its.GetSettingsHolder().GetSettings(), "")
	st.PartialDeliveryFlagCourierOptions = true
	ctx = settings.ContextWithSettings(ctx, st)

	isTrying := IsTryingAvailableCourierOption(
		ctx,
		cr.DeliverySubtype_ON_DEMAND,
		true,
		false,
		true,
		10,
	)

	require.False(t, isTrying)

	isTrying = IsTryingAvailableCourierOption(
		ctx,
		cr.DeliverySubtype_DEFERRED_COURIER,
		true,
		false,
		true,
		10,
	)

	require.False(t, isTrying)

	isTrying = IsTryingAvailableCourierOption(
		ctx,
		cr.DeliverySubtype_ORDINARY,
		true,
		false,
		true,
		0,
	)

	require.True(t, isTrying)

	isTrying = IsTryingAvailableCourierOption(
		ctx,
		cr.DeliverySubtype_ORDINARY,
		true,
		false,
		false,
		0,
	)

	require.False(t, isTrying)

	isTrying = IsTryingAvailableCourierOption(
		ctx,
		cr.DeliverySubtype_ORDINARY,
		true,
		true,
		true,
		0,
	)

	require.False(t, isTrying)

	PartialDeliveryServicesForTests = map[uint64]bool{
		10: true,
	}

	isTrying = IsTryingAvailableCourierOption(
		ctx,
		cr.DeliverySubtype_ORDINARY,
		true,
		false,
		true,
		5,
	)

	require.False(t, isTrying)

	isTrying = IsTryingAvailableCourierOption(
		ctx,
		cr.DeliverySubtype_ORDINARY,
		true,
		false,
		true,
		10,
	)

	require.True(t, isTrying)
}
