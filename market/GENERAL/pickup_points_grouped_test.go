package routes

import (
	"sort"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/enums"
	"a.yandex-team.ru/market/combinator/pkg/graph"
	"a.yandex-team.ru/market/combinator/pkg/outlets"
	"a.yandex-team.ru/market/combinator/pkg/timex"
	"a.yandex-team.ru/market/combinator/pkg/util"
	cr "a.yandex-team.ru/market/combinator/proto/grpc"
)

func createDateFrom(timeStr string) time.Time {
	t, _ := time.Parse(time.RFC3339, timeStr)
	return t
}

func CreatePoints() []*PickupPointData {
	return []*PickupPointData{
		&PickupPointData{
			ServiceID:      3,
			DateFrom:       createDateFrom("2020-07-25T19:04:05Z"),
			Type:           cr.PickupPointType_POST_OFFICE,
			PaymentMethods: enums.PaymentMethodsMask(2),
			PointIDs: cr.PointIds{
				LogisticPointId: 322,
			},
		},
		&PickupPointData{
			ServiceID:      3,
			DateFrom:       createDateFrom("2020-07-25T11:04:05Z"),
			Type:           cr.PickupPointType_POST_OFFICE,
			PaymentMethods: enums.PaymentMethodsMask(2),
			PointIDs: cr.PointIds{
				LogisticPointId: 323,
			},
		},
		&PickupPointData{
			ServiceID:      3,
			DateFrom:       createDateFrom("2020-07-25T15:04:05Z"),
			PaymentMethods: enums.PaymentMethodsMask(7),
			PointIDs: cr.PointIds{
				LogisticPointId: 324,
			},
		},
		&PickupPointData{
			ServiceID:      3,
			DateFrom:       createDateFrom("2020-07-25T15:08:05Z"),
			PaymentMethods: enums.PaymentMethodsMask(7),
			PointIDs: cr.PointIds{
				LogisticPointId: 325,
			},
		},
		&PickupPointData{
			ServiceID:      3,
			DateFrom:       createDateFrom("2020-07-26T03:04:05Z"),
			PaymentMethods: enums.PaymentMethodsMask(7),
			PointIDs: cr.PointIds{
				LogisticPointId: 326,
			},
		},
		&PickupPointData{
			ServiceID:      3,
			DateFrom:       createDateFrom("2020-07-25T09:04:05Z"),
			PaymentMethods: enums.PaymentMethodsMask(3),
			Type:           cr.PickupPointType_POST_OFFICE,
			PointIDs: cr.PointIds{
				LogisticPointId: 327,
			},
		},
		&PickupPointData{
			ServiceID:      3,
			DateFrom:       createDateFrom("2020-07-24T01:01:01Z"),
			PaymentMethods: enums.PaymentMethodsMask(3),
			Type:           cr.PickupPointType_POST_OFFICE,
			PointIDs: cr.PointIds{
				LogisticPointId: 328,
			},
		},
		&PickupPointData{
			ServiceID:      3,
			DateFrom:       createDateFrom("2020-07-26T03:04:05Z"),
			PaymentMethods: enums.PaymentMethodsMask(5),
			PointIDs: cr.PointIds{
				LogisticPointId: 329,
			},
		},
		&PickupPointData{
			ServiceID:      3,
			DateFrom:       createDateFrom("2020-07-25T09:04:05Z"),
			PaymentMethods: enums.PaymentMethodsMask(3),
			PointIDs: cr.PointIds{
				LogisticPointId: 330,
			},
		},
		&PickupPointData{
			ServiceID:      3,
			DateFrom:       createDateFrom("2020-07-25T15:04:05Z"),
			PaymentMethods: enums.PaymentMethodsMask(7),
			Type:           cr.PickupPointType_POST_OFFICE,
			PointIDs: cr.PointIds{
				LogisticPointId: 331,
			},
		},
		&PickupPointData{
			ServiceID:      3,
			DateFrom:       createDateFrom("2020-07-25T00:04:05Z"),
			PaymentMethods: enums.PaymentMethodsMask(7),
			Type:           cr.PickupPointType_POST_OFFICE,
			PointIDs: cr.PointIds{
				LogisticPointId: 332,
			},
		},
		&PickupPointData{
			ServiceID: 4,
			DateFrom:  createDateFrom("2020-07-23T15:02:05Z"),
			PointIDs: cr.PointIds{
				LogisticPointId: 333,
			},
		},
		&PickupPointData{
			ServiceID: 1,
			DateFrom:  createDateFrom("2020-07-29T01:04:05Z"),
			PointIDs: cr.PointIds{
				LogisticPointId: 334,
			},
		},
	}
}

func expectedPickupPoints() [][]int64 {
	return [][]int64{
		[]int64{334},
		[]int64{328},
		[]int64{322, 323},
		[]int64{330},
		[]int64{327},
		[]int64{324, 325},
		[]int64{331, 332},
		[]int64{329},
		[]int64{326},
		[]int64{333},
	}
}

func TestPickupPointsSort(t *testing.T) {
	points := CreatePoints()

	startTime := createDateFrom("2020-07-23T15:02:05Z")
	startDay := createStartDay(startTime)
	// Сортировка проверяется автоматом в группировке
	SortPickupPoints(points, startDay)

	handler := &RequestHandler{}
	groups := GroupPickupPoints(handler, points, startDay, startTime)
	expectedGroups := expectedPickupPoints()

	for id := range groups {
		var groupIDs []int64
		for _, p := range groups[id].Points {
			groupIDs = append(groupIDs, int64(p.LogisticPointId))
		}
		// Так как внешняя сортировка - не стабильная
		sort.SliceStable(
			groupIDs,
			func(i, j int) bool {
				return groupIDs[i] < groupIDs[j]
			},
		)
		require.Equal(t, expectedGroups[id], groupIDs)
	}
}

func TestProtoToOutletTypeMask(t *testing.T) {
	specs := []struct {
		pointTypes         []cr.PickupPointType
		want               uint8
		wantIfSkipPostTerm uint8
	}{
		// pickup
		{
			pointTypes:         []cr.PickupPointType{cr.PickupPointType_SERVICE_POINT, cr.PickupPointType_PARCEL_LOCKER},
			want:               uint8(outlets.Depot) | uint8(outlets.PostTerm),
			wantIfSkipPostTerm: uint8(outlets.Depot),
		},
		// post
		{
			pointTypes:         []cr.PickupPointType{cr.PickupPointType_POST_OFFICE},
			want:               uint8(outlets.Post),
			wantIfSkipPostTerm: uint8(outlets.Post),
		},
		// all
		{
			pointTypes:         []cr.PickupPointType{cr.PickupPointType_SERVICE_POINT, cr.PickupPointType_POST_OFFICE, cr.PickupPointType_PARCEL_LOCKER},
			want:               uint8(outlets.Depot) | uint8(outlets.Post) | uint8(outlets.PostTerm),
			wantIfSkipPostTerm: uint8(outlets.Depot) | uint8(outlets.Post),
		},
		// empty
		{
			pointTypes:         []cr.PickupPointType{},
			want:               0,
			wantIfSkipPostTerm: uint8(outlets.Depot) | uint8(outlets.Post),
		},
	}
	for i, spec := range specs {
		require.Equal(t, spec.want, ProtoToOutletTypeMask(spec.pointTypes, false), i)
		require.Equal(t, spec.wantIfSkipPostTerm, ProtoToOutletTypeMask(spec.pointTypes, true), i)
	}
}

func createTwoWavePoints() []*PickupPointData {
	// 1. После дейоффа в pickup endTime обеих волн может быть смещён на одно и то же время, например 04.11.2021 00:00:00.000
	// Надо проверить что всегда будет выигрывать волна с более ранним окончанием интервала
	return []*PickupPointData{
		&PickupPointData{
			ServiceID:      3,
			DateFrom:       createDateFrom("2020-07-25T00:00:00Z"),
			Type:           cr.PickupPointType_SERVICE_POINT,
			PaymentMethods: enums.PaymentMethodsMask(2),
			PointIDs: cr.PointIds{
				LogisticPointId: 322,
			},
			InboundInterval: &graph.Interval{
				From: timex.DayTime{Hour: 12, Minute: 0},
				To:   timex.DayTime{Hour: 18, Minute: 0},
			},
		},
		&PickupPointData{
			ServiceID:      3,
			DateFrom:       createDateFrom("2020-07-25T00:00:00Z"),
			Type:           cr.PickupPointType_SERVICE_POINT,
			PaymentMethods: enums.PaymentMethodsMask(2),
			PointIDs: cr.PointIds{
				LogisticPointId: 322,
			},
			InboundInterval: &graph.Interval{
				From: timex.DayTime{Hour: 10, Minute: 0},
				To:   timex.DayTime{Hour: 12, Minute: 0},
			},
		},
		// 2. Обе волны везут в один день, а значит попадают в одну группу. Сортировка проходит по дням, отбрасывая время,
		// а значит более поздняя волна в один и тот же ПВЗ может оказаться ранее в отсортированном слайсе
		// Надо проверить что всегда, в одной группе, будет проставлен интервал от более раннего endTime.
		&PickupPointData{
			ServiceID:      3,
			DateFrom:       createDateFrom("2020-07-26T00:00:01Z"),
			Type:           cr.PickupPointType_SERVICE_POINT,
			PaymentMethods: enums.PaymentMethodsMask(2),
			PointIDs: cr.PointIds{
				LogisticPointId: 333,
			},
			InboundInterval: &graph.Interval{
				From: timex.DayTime{Hour: 10, Minute: 0},
				To:   timex.DayTime{Hour: 12, Minute: 0},
			},
		},
		&PickupPointData{
			ServiceID:      3,
			DateFrom:       createDateFrom("2020-07-26T00:00:00Z"),
			Type:           cr.PickupPointType_SERVICE_POINT,
			PaymentMethods: enums.PaymentMethodsMask(2),
			PointIDs: cr.PointIds{
				LogisticPointId: 333,
			},
			InboundInterval: &graph.Interval{
				From: timex.DayTime{Hour: 12, Minute: 0},
				To:   timex.DayTime{Hour: 18, Minute: 0},
			},
		},
		// 3. Если по каким-либо причинам оба endTime в одно время, но нельзя вычислить один из интервалов,
		// То проставляем тот который есть, тк если это более ранний интервал, то мы гарантированно не опоздаем, тк эта опция доступна,
		// а если это более поздний интервал, то не опоздаем тем более, тк должны будем привезти в более раннюю волну,
		// хотя интервал для неё не смогли рассчитать. Главная мысль: не позднее.
		&PickupPointData{
			ServiceID:      3,
			DateFrom:       createDateFrom("2020-07-27T00:00:00Z"),
			Type:           cr.PickupPointType_SERVICE_POINT,
			PaymentMethods: enums.PaymentMethodsMask(2),
			PointIDs: cr.PointIds{
				LogisticPointId: 444,
			},
			InboundInterval: nil,
		},
		&PickupPointData{
			ServiceID:      3,
			DateFrom:       createDateFrom("2020-07-27T00:00:00Z"),
			Type:           cr.PickupPointType_SERVICE_POINT,
			PaymentMethods: enums.PaymentMethodsMask(2),
			PointIDs: cr.PointIds{
				LogisticPointId: 444,
			},
			InboundInterval: &graph.Interval{
				From: timex.DayTime{Hour: 12, Minute: 0},
				To:   timex.DayTime{Hour: 18, Minute: 0},
			},
		},
	}
}

func expectedTwoWavesGroups() []*cr.PickupPointsGrouped_Group {
	return []*cr.PickupPointsGrouped_Group{
		&cr.PickupPointsGrouped_Group{
			Points: []*cr.PointIds{
				&cr.PointIds{
					LogisticPointId: 322,
				},
			},
			ServiceId:      3,
			DateFrom:       CreateDate(time.Date(2020, 7, 25, 0, 0, 0, 0, time.UTC)),
			DateTo:         CreateDate(time.Date(2020, 7, 25, 0, 0, 0, 0, time.UTC)),
			PaymentMethods: util.PaymentMethodsMaskToProto(enums.PaymentMethodsMask(2)),
			Type:           cr.PickupPointType_SERVICE_POINT,
			PointInterval: []*cr.PointDeliveryInterval{
				&cr.PointDeliveryInterval{
					LogisticPointId: 322,
					DeliveryInterval: &cr.DeliveryInterval{
						From: &cr.Time{Hour: 10, Minute: 0},
						To:   &cr.Time{Hour: 12, Minute: 0},
					},
				},
			},
		},
		&cr.PickupPointsGrouped_Group{
			Points: []*cr.PointIds{
				&cr.PointIds{
					LogisticPointId: 333,
				},
			},
			ServiceId:      3,
			DateFrom:       CreateDate(time.Date(2020, 7, 26, 0, 0, 0, 0, time.UTC)),
			DateTo:         CreateDate(time.Date(2020, 7, 26, 0, 0, 0, 0, time.UTC)),
			PaymentMethods: util.PaymentMethodsMaskToProto(enums.PaymentMethodsMask(2)),
			Type:           cr.PickupPointType_SERVICE_POINT,
			PointInterval: []*cr.PointDeliveryInterval{
				&cr.PointDeliveryInterval{
					LogisticPointId: 333,
					DeliveryInterval: &cr.DeliveryInterval{
						From: &cr.Time{Hour: 12, Minute: 0},
						To:   &cr.Time{Hour: 18, Minute: 0},
					},
				},
			},
		},
		&cr.PickupPointsGrouped_Group{
			Points: []*cr.PointIds{
				&cr.PointIds{
					LogisticPointId: 444,
				},
			},
			ServiceId:      3,
			DateFrom:       CreateDate(time.Date(2020, 7, 27, 0, 0, 0, 0, time.UTC)),
			DateTo:         CreateDate(time.Date(2020, 7, 27, 0, 0, 0, 0, time.UTC)),
			PaymentMethods: util.PaymentMethodsMaskToProto(enums.PaymentMethodsMask(2)),
			Type:           cr.PickupPointType_SERVICE_POINT,
			PointInterval: []*cr.PointDeliveryInterval{
				&cr.PointDeliveryInterval{
					LogisticPointId: 444,
					DeliveryInterval: &cr.DeliveryInterval{
						From: &cr.Time{Hour: 12, Minute: 0},
						To:   &cr.Time{Hour: 18, Minute: 0},
					},
				},
			},
		},
	}
}

func TestPickupPointsTwoWaves(t *testing.T) {
	points := createTwoWavePoints()

	startTime := createDateFrom("2020-07-23T15:02:05Z")
	startDay := createStartDay(startTime)
	// Сортировка проверяется автоматом в группировке
	SortPickupPoints(points, startDay)

	handler := &RequestHandler{}
	groups := GroupPickupPoints(handler, points, startDay, startTime)
	require.Equal(t, expectedTwoWavesGroups(), groups)
}
