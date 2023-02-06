package indexer

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/require"
	"google.golang.org/protobuf/types/known/timestamppb"

	persqueueRecipe "a.yandex-team.ru/kikimr/public/sdk/go/persqueue/recipe"
	"a.yandex-team.ru/library/go/test/requirepb"
	"a.yandex-team.ru/travel/library/go/logbroker"
	tpb "a.yandex-team.ru/travel/proto"
	"a.yandex-team.ru/travel/proto/dicts/rasp"
	"a.yandex-team.ru/travel/trains/library/go/tariffs/models"
	api "a.yandex-team.ru/travel/trains/search_api/api/tariffs"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/date"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/errors"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/testutils"
)

const defaultTopic = "tariffs"

func TestWrite(t *testing.T) {
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	var err error
	logger := testutils.NewLogger(t)
	env := persqueueRecipe.New(t)

	producer := logbroker.NewProducerWithRecipe(env, "", defaultTopic, logger)
	consumer, err := logbroker.NewConsumerWithRecipe(env, []string{""}, defaultTopic, logger)
	require.NoError(t, err)

	require.NoError(t, producer.Run(ctx))
	require.NoError(t, consumer.Run(ctx))
	channel := consumer.NewChannel()

	indexer := New(logger, producer)

	t.Run("write empty", func(t *testing.T) {
		departureDate := date.Date(2021, 5, 10)
		departurePointExpressID := 1
		arrivalPointExpressID := 2
		require.NoError(t, indexer.Index(
			context.Background(),
			departurePointExpressID,
			arrivalPointExpressID,
			departureDate,
			[]*models.DirectionTariffTrain{},
		))

		info := new(api.DirectionTariffInfo)
		require.NoError(t, channel.Read(info))

		requirepb.Equal(t, &api.DirectionTariffInfo{
			DeparturePointExpressId: int32(departurePointExpressID),
			ArrivalPointExpressId:   int32(arrivalPointExpressID),
			DepartureDate:           date.GetProtoFromDate(departureDate),
			Data:                    []*api.DirectionTariffTrain{},
			CreatedAt:               info.CreatedAt,
			UpdatedAt:               info.UpdatedAt,
		}, info)
	})

	t.Run("write infos", func(t *testing.T) {
		now := time.Now()
		departureDate := date.Date(2021, 5, 10)
		departurePointExpressID := 1
		arrivalPointExpressID := 2
		require.NoError(t, indexer.Index(
			context.Background(),
			departurePointExpressID,
			arrivalPointExpressID,
			departureDate,
			[]*models.DirectionTariffTrain{
				{
					Arrival:            now,
					ArrivalStationID:   100,
					Departure:          now,
					DepartureStationID: 101,
					Number:             "test1",
					DisplayNumber:      "test1",
					HasDynamicPricing:  true,
					TwoStorey:          false,
					IsSuburban:         false,
					CoachOwners:        []string{"test1"},
					ElectronicTicket:   false,
					FirstCountryCode:   "test1",
					LastCountryCode:    "test1",
					Places: []*models.TrainPlace{
						{
							CoachType:            "suite",
							Count:                11,
							LowerCount:           12,
							MaxSeatsInTheSameCar: 13,
							UpperSideCount:       14,
							Price: &models.TrainPlacePrice{
								Currency: "USD",
								Value:    "10000.10",
							},
							PriceDetails: &models.TrainPlacePriceDetails{
								Fee:           "1002.12",
								ServicePrice:  "1003.0",
								SeveralPrices: false,
								TicketPrice:   "1004.4",
							},
							ServiceClass: "1Э",
							UpperCount:   15,
						},
					},
					BrokenClasses: &models.TariffBrokenClasses{
						Unknown:     []uint32{5},
						Compartment: []uint32{1, 2},
					},
					TitleDict: &models.ThreadTitle{
						Type:          "default",
						TransportType: "train",
						TitleParts: []string{
							"c2",
							"s33",
						},
						IsCombined: false,
						IsRing:     false,
					},
					Provider:     "Provider",
					RawTrainName: "RawTrainName",
				},
			},
		))

		info := new(api.DirectionTariffInfo)
		require.NoError(t, channel.Read(info))

		requirepb.Equal(t, &api.DirectionTariffInfo{
			DeparturePointExpressId: int32(departurePointExpressID),
			ArrivalPointExpressId:   int32(arrivalPointExpressID),
			DepartureDate:           date.GetProtoFromDate(departureDate),
			Data: []*api.DirectionTariffTrain{
				{
					Arrival:            timestamppb.New(now),
					ArrivalStationId:   100,
					Departure:          timestamppb.New(now),
					DepartureStationId: 101,
					Number:             "test1",
					DisplayNumber:      "test1",
					HasDynamicPricing:  true,
					TwoStorey:          false,
					IsSuburban:         false,
					CoachOwners:        []string{"test1"},
					ElectronicTicket:   false,
					FirstCountryCode:   "test1",
					LastCountryCode:    "test1",
					Places: []*api.TrainPlace{
						{
							CoachType:            "suite",
							Count:                11,
							LowerCount:           12,
							MaxSeatsInTheSameCar: 13,
							UpperSideCount:       14,
							Price: &tpb.TPrice{
								Currency:  tpb.ECurrency_C_USD,
								Amount:    1000010,
								Precision: 2,
							},
							PriceDetails: &api.TrainPlacePriceDetails{
								Fee: &tpb.TPrice{
									Currency:  tpb.ECurrency_C_USD,
									Amount:    100212,
									Precision: 2,
								},
								ServicePrice: &tpb.TPrice{
									Currency:  tpb.ECurrency_C_USD,
									Amount:    10030,
									Precision: 1,
								},
								SeveralPrices: false,
								TicketPrice: &tpb.TPrice{
									Currency:  tpb.ECurrency_C_USD,
									Amount:    10044,
									Precision: 1,
								},
							},
							ServiceClass: "1Э",
							UpperCount:   15,
						},
					},
					BrokenClasses: &api.TariffBrokenClasses{
						Unknown:     []uint32{5},
						Compartment: []uint32{1, 2},
					},
					TitleDict: &rasp.TThreadTitle{
						TitleParts: []*rasp.TThreadTitlePart{
							{
								SettlementId: 2,
							},
							{
								StationId: 33,
							},
						},
						Type:          rasp.TThreadTitle_TYPE_DEFAULT,
						TransportType: rasp.TTransport_TYPE_TRAIN,
					},
					Provider:     "Provider",
					RawTrainName: "RawTrainName",
				},
			},
			CreatedAt: info.CreatedAt,
			UpdatedAt: info.UpdatedAt,
		}, info)
	})

	t.Run("write unknown", func(t *testing.T) {
		departureDate := date.Date(2021, 5, 10)
		departurePointExpressID := 1
		arrivalPointExpressID := 2
		err := indexer.Index(
			context.Background(),
			departurePointExpressID,
			arrivalPointExpressID,
			departureDate,
			[]*models.DirectionTariffTrain{
				{
					Places: []*models.TrainPlace{
						{
							Price: &models.TrainPlacePrice{
								Currency: "",
								Value:    "1000.10",
							},
						},
					},
				},
			},
		)

		require.ErrorIs(t, err, errors.ErrUnknownValue)
	})
}
