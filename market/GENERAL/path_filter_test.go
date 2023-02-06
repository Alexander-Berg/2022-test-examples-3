package graph

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/enums"
	"a.yandex-team.ru/market/combinator/pkg/its"
	"a.yandex-team.ru/market/combinator/pkg/settings"
)

func getDefaultSettings() its.Settings {
	settings, _ := its.NewStringSettingsHolder("{}")
	return settings.GetSettings()
}

func TestIsGoodPathForSortingCenterByBuildPath(t *testing.T) {
	build := func(
		partnerType enums.PartnerType,
		inboundCSTime, sortTime time.Time,
		inboundTime, movementTime time.Time,
	) *Path {
		return MakePathForSortCenterServices(
			partnerType,
			inboundCSTime,
			sortTime,
			inboundTime,
			movementTime,
		).Path
	}
	defSet := settings.New(getDefaultSettings(), "")
	defSet.MaxHoursToStoreSortedBoxOnSC = 24
	ctx := settings.ContextWithSettings(context.Background(), defSet)
	pv := NewPathValidator(ctx, PathValidatorOptions{DType: DeliveryTypeUnknown})
	// Последний склад не СЦ => ok.
	// Перескок был бы в Movement'e.
	require.True(t, pv.IsGoodPathForSortingCenter(
		build(
			enums.PartnerTypeFulfillment,
			time.Date(2020, 2, 1, 0, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 1, 0, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 1, 0, 0, 0, 0, time.UTC), // <-- Проверяем перескок тут
			time.Date(2020, 2, 11, 0, 0, 0, 0, time.UTC),
		),
	))
	// Последний склад СЦ и разница меньше суток => ok.
	// Перескок проверяем в Movement'e.
	require.True(t, pv.IsGoodPathForSortingCenter(
		build(
			enums.PartnerTypeSortingCenter,
			time.Date(2020, 2, 1, 0, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 1, 0, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 1, 0, 0, 0, 0, time.UTC), // <-- Проверяем перескок тут
			time.Date(2020, 2, 1, 23, 59, 59, 0, time.UTC),
		),
	))
	// Последний склад СЦ и разница больше суток => fail.
	// Перескок в Movement'e.
	require.False(t, pv.IsGoodPathForSortingCenter(
		build(
			enums.PartnerTypeSortingCenter,
			time.Date(2020, 2, 1, 0, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 1, 0, 0, 0, 0, time.UTC),
			time.Date(2020, 12, 1, 0, 0, 0, 0, time.UTC), // <-- Проверяем перескок тут
			time.Date(2020, 12, 2, 1, 0, 0, 0, time.UTC),
		),
	))
	// Последний склад не СЦ => ok.
	// Перескок был бы в Warehouse'e.
	require.True(t, pv.IsGoodPathForSortingCenter(
		build(
			enums.PartnerTypeFulfillment,
			time.Date(2020, 2, 1, 0, 0, 0, 0, time.UTC), // <-- Проверяем перескок тут
			time.Date(2020, 2, 11, 0, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 11, 0, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 11, 0, 0, 0, 0, time.UTC),
		),
	))
	// Последний склад СЦ и разница меньше суток => ok.
	// Перескок проверяем в Warehouse'e.
	require.True(t, pv.IsGoodPathForSortingCenter(
		build(
			enums.PartnerTypeSortingCenter,
			time.Date(2020, 2, 1, 0, 0, 0, 0, time.UTC), // <-- Проверяем перескок тут
			time.Date(2020, 2, 1, 23, 59, 59, 0, time.UTC),
			time.Date(2020, 2, 1, 23, 59, 59, 0, time.UTC),
			time.Date(2020, 2, 1, 23, 59, 59, 0, time.UTC),
		),
	))
	// Последний склад СЦ и разница больше суток => fail.
	// Перескок в Warehouse'e. Однако, фича выключена.
	require.True(t, pv.IsGoodPathForSortingCenter(
		build(
			enums.PartnerTypeSortingCenter,
			time.Date(2020, 12, 1, 0, 0, 0, 0, time.UTC), // <-- Проверяем перескок тут
			time.Date(2020, 12, 2, 0, 0, 0, 0, time.UTC),
			time.Date(2020, 12, 2, 0, 0, 0, 0, time.UTC),
			time.Date(2020, 12, 2, 0, 0, 0, 0, time.UTC),
		),
	))
	// Последний склад СЦ и обе разницы больше суток => fail.
	// Перескок есть и в Warehouse'e, и в Movement'e
	// Проверяем только movement, так как фича выклюена
	require.False(t, pv.IsGoodPathForSortingCenter(
		build(
			enums.PartnerTypeSortingCenter,
			time.Date(2020, 12, 1, 0, 0, 0, 0, time.UTC), // <-- Перескок тут
			time.Date(2020, 12, 2, 1, 0, 0, 0, time.UTC),
			time.Date(2020, 12, 2, 1, 0, 0, 0, time.UTC), // <-- Перескок тут
			time.Date(2020, 12, 3, 2, 0, 0, 0, time.UTC),
		),
	))
}

func TestIsGoodPathBeforeSortingCenter(t *testing.T) {
	build := func(
		partnerType enums.PartnerType,
		shipmentTime time.Time,
		inboundCSTime time.Time,
		samePartner bool,
	) *Path {
		return MakePathBeforeSortCenterServices(
			partnerType,
			shipmentTime,
			inboundCSTime,
			inboundCSTime,
			samePartner,
		).Path
	}
	defSet := settings.New(getDefaultSettings(), "")
	defSet.MaxHoursToStoreSortedBoxOnSC = 24
	ctx := settings.ContextWithSettings(context.Background(), defSet)
	pv := NewPathValidator(ctx, PathValidatorOptions{DType: DeliveryTypeUnknown})
	// Перескока нет
	require.True(t, pv.IsGoodPathForSortingCenter(
		build(
			enums.PartnerTypeSortingCenter,
			time.Date(2020, 2, 1, 0, 0, 0, 0, time.UTC), // <-- Проверяем перескок тут
			time.Date(2020, 2, 1, 0, 0, 0, 0, time.UTC),
			false,
		),
	))
	// Перескок есть, партнёры разные, поэтому путь плохой
	require.False(t, pv.IsGoodPathForSortingCenter(
		build(
			enums.PartnerTypeSortingCenter,
			time.Date(2020, 12, 1, 0, 0, 0, 0, time.UTC), // <-- Перескок тут
			time.Date(2020, 12, 2, 1, 0, 0, 0, time.UTC),
			false,
		),
	))
	// Перескока нет, но мы и не проверяем
	require.True(t, pv.IsGoodPathForSortingCenter(
		build(
			enums.PartnerTypeSortingCenter,
			time.Date(2020, 2, 1, 0, 0, 0, 0, time.UTC), // <-- Проверяем перескок тут
			time.Date(2020, 2, 1, 0, 0, 0, 0, time.UTC),
			true,
		),
	))
	// Перескок есть, но партнёр одинаковый, поэтому не проверяем
	require.True(t, pv.IsGoodPathForSortingCenter(
		build(
			enums.PartnerTypeSortingCenter,
			time.Date(2020, 12, 1, 0, 0, 0, 0, time.UTC), // <-- Перескок тут
			time.Date(2020, 12, 2, 1, 0, 0, 0, time.UTC),
			true,
		),
	))
	// Перескок есть, но склад - не сорт.центр
	require.True(t, pv.IsGoodPathForSortingCenter(
		build(
			enums.PartnerTypeFulfillment,
			time.Date(2020, 12, 1, 0, 0, 0, 0, time.UTC), // <-- Перескок тут
			time.Date(2020, 12, 2, 1, 0, 0, 0, time.UTC),
			false,
		),
	))
}

func TestIsGoodPathAfterDropship(t *testing.T) {
	build := func(
		partnerType enums.PartnerType,
		processingTime time.Time,
		inboundTime time.Time,
		samePartner bool,
	) *Path {
		return MakePathAfterDropshipServices(
			partnerType,
			processingTime,
			inboundTime,
			samePartner,
		).Path
	}
	defSet := settings.New(getDefaultSettings(), "")
	defSet.MaxHoursToStoreSortedBoxOnSC = 24
	ctx := settings.ContextWithSettings(context.Background(), defSet)
	pv := NewPathValidator(ctx, PathValidatorOptions{DType: DeliveryTypeUnknown})
	// Перескока нет
	require.True(t, pv.IsGoodPathForSortingCenter(
		build(
			enums.PartnerTypeDropship,
			time.Date(2020, 2, 1, 0, 0, 0, 0, time.UTC), // <-- Проверяем перескок тут
			time.Date(2020, 2, 1, 0, 0, 0, 0, time.UTC),
			false,
		),
	))
	// Перескок есть, партнёры разные, поэтому путь плохой
	require.False(t, pv.IsGoodPathForSortingCenter(
		build(
			enums.PartnerTypeDropship,
			time.Date(2020, 12, 1, 0, 0, 0, 0, time.UTC), // <-- Перескок тут
			time.Date(2020, 12, 2, 1, 0, 0, 0, time.UTC),
			false,
		),
	))
	// Перескока нет, но мы и не проверяем
	require.True(t, pv.IsGoodPathForSortingCenter(
		build(
			enums.PartnerTypeDropship,
			time.Date(2020, 2, 1, 0, 0, 0, 0, time.UTC), // <-- Проверяем перескок тут
			time.Date(2020, 2, 1, 0, 0, 0, 0, time.UTC),
			true,
		),
	))
	// Перескок есть, но партнёр одинаковый, поэтому не проверяем
	require.True(t, pv.IsGoodPathForSortingCenter(
		build(
			enums.PartnerTypeDropship,
			time.Date(2020, 12, 1, 0, 0, 0, 0, time.UTC), // <-- Перескок тут
			time.Date(2020, 12, 2, 1, 0, 0, 0, time.UTC),
			true,
		),
	))
	// Перескок есть, но склад - не дропшип
	require.True(t, pv.IsGoodPathForSortingCenter(
		build(
			enums.PartnerTypeFulfillment,
			time.Date(2020, 12, 1, 0, 0, 0, 0, time.UTC), // <-- Перескок тут
			time.Date(2020, 12, 2, 1, 0, 0, 0, time.UTC),
			false,
		),
	))
}

func TestIsGoodPathForDelivery(t *testing.T) {
	build := func(
		partnerType enums.PartnerType,
		processingTime time.Time,
		inboundTime time.Time,
		samePartner bool,
	) *Path {
		return MakePathAfterDropshipServices(
			partnerType,
			processingTime,
			inboundTime,
			samePartner,
		).Path
	}
	defSet := settings.New(getDefaultSettings(), "")
	defSet.MaxHoursToStoreSortedBoxOnSC = 24
	defSet.MaxHoursToStoreSortedBoxOnDv = 25
	ctx := settings.ContextWithSettings(context.Background(), defSet)
	pv := NewPathValidator(ctx, PathValidatorOptions{DType: DeliveryTypeUnknown})
	//Прверяем, что адекватно работаем с плохим вводом для Delivery
	require.True(t, pv.IsGoodPathForSortingCenter(
		build(
			enums.PartnerTypeDelivery,
			time.Date(2020, 2, 1, 0, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 1, 0, 0, 0, 0, time.UTC),
			true,
		),
	))
	//время < 25 ч => путь хороший
	require.True(t, pv.IsGoodPathForSortingCenter(
		MakePathBeforeAndAfterDeliveryServices(
			enums.PartnerTypeDelivery,
			time.Date(2020, 2, 1, 0, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 1, 0, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 1, 0, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 1, 0, 0, 0, 0, time.UTC),
			true,
		).Path,
	))
	//время == 25 ч => путь хороший
	require.True(t, pv.IsGoodPathForSortingCenter(
		MakePathBeforeAndAfterDeliveryServices(
			enums.PartnerTypeDelivery,
			time.Date(2020, 2, 1, 0, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 1, 0, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 1, 1, 12, 0, 0, time.UTC),
			time.Date(2020, 2, 2, 1, 0, 0, 0, time.UTC),
			true,
		).Path,
	))
	//время > 25 ч => путь плохой
	require.False(t, pv.IsGoodPathForSortingCenter(
		MakePathBeforeAndAfterDeliveryServices(
			enums.PartnerTypeDelivery,
			time.Date(2020, 2, 1, 0, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 1, 0, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 2, 0, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 3, 1, 1, 0, 0, time.UTC),
			true,
		).Path,
	))
	// время < 25 ч => путь хороший
	require.True(t, pv.IsGoodPathForSortingCenter(
		MakePathBeforeAndAfterDeliveryServices(
			enums.PartnerTypeDelivery,
			time.Date(2020, 2, 1, 11, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 1, 19, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 1, 19, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 2, 19, 0, 0, 0, time.UTC),
			true,
		).Path,
	))
	// время < 25 ч => путь хороший
	require.False(t, pv.IsGoodPathForSortingCenter(
		MakePathBeforeAndAfterDeliveryServices(
			enums.PartnerTypeDelivery,
			time.Date(2020, 2, 1, 11, 0, 0, 0, time.UTC),
			time.Time{}, // Нет Shipment сервиса
			time.Date(2020, 2, 1, 19, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 2, 19, 0, 0, 0, time.UTC),
			true,
		).Path,
	))
	// большой diff между Movement и Shipment => плохой путь
	require.False(t, pv.IsGoodPathForSortingCenter(
		MakePathBeforeAndAfterDeliveryServices(
			enums.PartnerTypeDelivery,
			time.Date(2020, 2, 1, 0, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 2, 1, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 2, 1, 12, 0, 0, time.UTC),
			time.Date(2020, 2, 2, 1, 12, 0, 0, time.UTC),
			true,
		).Path,
	))

}

func TestIsGoodPathForSCBeforeAndAfter(t *testing.T) {
	defSet := settings.New(getDefaultSettings(), "")
	defSet.MaxHoursToStoreSortedBoxOnSC = 25
	ctx := settings.ContextWithSettings(context.Background(), defSet)
	pv := NewPathValidator(ctx, PathValidatorOptions{DType: DeliveryTypeUnknown})
	// время < 25 ч => путь хороший
	require.True(t, pv.IsGoodPathForSortingCenter(
		MakePathBeforeAndAfterDeliveryServices(
			enums.PartnerTypeSortingCenter,
			time.Date(2020, 2, 1, 11, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 1, 19, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 1, 19, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 2, 19, 0, 0, 0, time.UTC),
			true,
		).Path,
	))
	// время > 25 ч => путь плохой
	require.False(t, pv.IsGoodPathForSortingCenter(
		MakePathBeforeAndAfterDeliveryServices(
			enums.PartnerTypeSortingCenter,
			time.Date(2020, 2, 1, 11, 0, 0, 0, time.UTC),
			time.Time{}, // Нет Shipment сервиса
			time.Date(2020, 2, 1, 19, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 2, 19, 0, 0, 0, time.UTC),
			true,
		).Path,
	))

	// время == 25 ч => путь хороший
	require.True(t, pv.IsGoodPathForSortingCenter(
		MakePathBeforeAndAfterDeliveryServices(
			enums.PartnerTypeSortingCenter,
			time.Date(2020, 2, 1, 0, 0, 0, 0, time.UTC),
			time.Time{}, // Нет Shipment сервиса
			time.Date(2020, 2, 1, 1, 12, 0, 0, time.UTC),
			time.Date(2020, 2, 2, 1, 0, 0, 0, time.UTC),
			true,
		).Path,
	))

	// большой diff между Movement и Shipment => плохой путь
	require.False(t, pv.IsGoodPathForSortingCenter(
		MakePathBeforeAndAfterDeliveryServices(
			enums.PartnerTypeSortingCenter,
			time.Date(2020, 2, 1, 0, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 2, 1, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 2, 1, 12, 0, 0, time.UTC),
			time.Date(2020, 2, 2, 1, 12, 0, 0, time.UTC),
			true,
		).Path,
	))

	// время > 25 ч => путь плохой
	require.False(t, pv.IsGoodPathForSortingCenter(
		MakePathBeforeAndAfterDeliveryServices(
			enums.PartnerTypeSortingCenter,
			time.Date(2020, 2, 1, 0, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 1, 0, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 2, 0, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 3, 1, 1, 0, 0, time.UTC),
			true,
		).Path,
	))
	// время > 25 ч => путь плохой
	require.False(t, pv.IsGoodPathForSortingCenter(
		MakePathBeforeAndAfterDeliveryServices(
			enums.PartnerTypeSortingCenter,
			time.Date(2020, 2, 1, 0, 0, 0, 0, time.UTC),
			time.Time{}, // Нет Shipment сервиса
			time.Date(2020, 2, 2, 0, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 3, 1, 1, 0, 0, time.UTC),
			true,
		).Path,
	))
}

func TestIsGoodPickupConnection(t *testing.T) {
	defSet := settings.New(getDefaultSettings(), "")
	defSet.MaxHoursPickupGap = 24
	ctx := settings.ContextWithSettings(context.Background(), defSet)
	pv := NewPathValidator(ctx, PathValidatorOptions{DType: DeliveryTypeUnknown})
	// время < 24 ч => путь хороший
	require.True(t, pv.IsGoodPathForSortingCenter(
		MakePathWithLinehaulAndPickup(
			time.Date(2020, 2, 1, 11, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 1, 11, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 1, 19, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 1, 19, 0, 0, 0, time.UTC),
		).Path,
	))
	// время < 24 ч => путь хороший
	require.True(t, pv.IsGoodPathForSortingCenter(
		MakePathWithLinehaulAndPickup(
			time.Date(2020, 2, 1, 11, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 1, 11, 0, 0, 0, time.UTC),
			time.Time{}, // нет Inbound
			time.Date(2020, 2, 1, 19, 0, 0, 0, time.UTC),
		).Path,
	))
	// время >= 24 ч => путь плохой
	require.False(t, pv.IsGoodPathForSortingCenter(
		MakePathWithLinehaulAndPickup(
			time.Date(2020, 2, 1, 11, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 1, 11, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 2, 11, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 2, 11, 0, 0, 0, time.UTC),
		).Path,
	))
	// время >= 24 ч => путь плохой
	require.False(t, pv.IsGoodPathForSortingCenter(
		MakePathWithLinehaulAndPickup(
			time.Date(2020, 2, 1, 11, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 1, 11, 0, 0, 0, time.UTC),
			time.Time{}, // нет Inbound
			time.Date(2020, 2, 2, 11, 0, 0, 0, time.UTC),
		).Path,
	))
	// время >= 24 ч, но нет Shipment сервиса => True
	require.True(t, pv.IsGoodPathForSortingCenter(
		MakePathWithLinehaulAndPickup(
			time.Date(2020, 2, 1, 11, 0, 0, 0, time.UTC),
			time.Time{}, // нет Shipment
			time.Date(2020, 2, 2, 11, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 2, 11, 0, 0, 0, time.UTC),
		).Path,
	))
}

func TestIsGoodHandingConnection(t *testing.T) {
	defSet := settings.New(getDefaultSettings(), "")
	defSet.MaxHoursHandingGap = 24
	ctx := settings.ContextWithSettings(context.Background(), defSet)
	pv := NewPathValidator(ctx, PathValidatorOptions{DType: DeliveryTypeUnknown})
	// время < 24 ч => путь хороший
	require.True(t, pv.IsGoodPathForSortingCenter(
		MakePathWithLinehaulAndHanding(
			time.Date(2020, 2, 1, 11, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 1, 11, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 1, 19, 0, 0, 0, time.UTC),
		).Path,
	))
	// время >= 24 ч => путь плохой
	require.False(t, pv.IsGoodPathForSortingCenter(
		MakePathWithLinehaulAndHanding(
			time.Date(2020, 2, 1, 11, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 1, 11, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 2, 11, 0, 0, 0, time.UTC),
		).Path,
	))
	// время >= 24 ч, но нет Shipment сервиса => True
	require.True(t, pv.IsGoodPathForSortingCenter(
		MakePathWithLinehaulAndHanding(
			time.Date(2020, 2, 1, 11, 0, 0, 0, time.UTC),
			time.Time{}, // нет Shipment
			time.Date(2020, 2, 2, 11, 0, 0, 0, time.UTC),
		).Path,
	))
}

func TestIsGoodLinehaulConnection(t *testing.T) {
	build := func(
		shipmentTime time.Time,
		deliveryTime time.Time,
	) *Path {
		return MakePathWithMovementAndLinehaul(
			shipmentTime,
			deliveryTime,
		).Path
	}

	defSet := settings.New(getDefaultSettings(), "")
	ctx := settings.ContextWithSettings(context.Background(), defSet)
	pv := NewPathValidator(ctx, PathValidatorOptions{DType: DeliveryTypeUnknown})

	require.False(t, pv.IsGoodPathForSortingCenter(
		build(
			time.Date(2020, 2, 1, 0, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 11, 0, 0, 0, 0, time.UTC),
		),
	))
	require.True(t, pv.IsGoodPathForSortingCenter(
		build(
			time.Date(2020, 2, 11, 0, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 11, 0, 0, 0, 0, time.UTC),
		),
	))
	require.True(t, pv.IsGoodPathForSortingCenter(
		build(
			time.Date(2020, 2, 11, 0, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 11, 5, 59, 0, 0, time.UTC),
		),
	))
	require.False(t, pv.IsGoodPathForSortingCenter(
		build(
			time.Date(2020, 2, 11, 0, 0, 0, 0, time.UTC),
			time.Date(2020, 2, 11, 7, 0, 0, 0, time.UTC),
		),
	))
	require.False(t, pv.IsGoodPathForSortingCenter(
		build(
			time.Date(2022, 5, 25, 10, 0, 0, 0, time.UTC),
			time.Date(2022, 5, 26, 0, 0, 0, 0, time.UTC),
		),
	))
}
