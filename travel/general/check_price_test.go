package checkprice

import (
	"a.yandex-team.ru/travel/avia/price_prediction/internal/checkprice"
	"a.yandex-team.ru/travel/avia/price_prediction/internal/database"
	"a.yandex-team.ru/travel/avia/price_prediction/internal/dbcache"
	"a.yandex-team.ru/travel/avia/price_prediction/internal/models"
	"a.yandex-team.ru/travel/library/go/logging"
	"context"
	"github.com/stretchr/testify/require"
	"testing"
)

const spbID = 2

func TestService_CheckPrice(t *testing.T) {
	logger, err := logging.New(&logging.DefaultConfig)
	if err != nil {
		panic(err)
	}

	testPgClient := database.GetTestPgClient()
	dbRepository := database.NewVariantsPriceStatsDBRepository(testPgClient, true)
	dbOperationTimeout := database.GetDBTestTimeout()
	withEmptyDB := database.NewDBCleaner(testPgClient)

	t.Run(
		"Check price category", withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbOperationTimeout)
				defer cancelFunc()

				_, err := dbRepository.Create(ctx, models.VariantPriceStat{
					PointFromType:    "c",
					PointFromID:      moscowID,
					PointToType:      "c",
					PointToID:        novosibirskID,
					RouteUID:         "DP 206",
					DepartureWeekday: 1,
					DaysToFlight:     1,
					Q33:              3000,
					Q67:              6000,
				})
				require.NoError(t, err)

				repository, err := dbcache.NewVariantsPriceStatsRepository(testPgClient, true, dbcache.DefaultConfig, logger)
				require.NoError(t, err)

				checkPriceService := BuildService(logger, repository, false)

				testCases := []struct {
					request          checkprice.CheckPriceRequest
					expectedCategory checkprice.PriceCategory
				}{
					{
						request:          checkprice.CheckPriceRequest{},
						expectedCategory: checkprice.PriceCategoryUnknown,
					},
					{
						request: checkprice.CheckPriceRequest{
							PointFromType:      "c",
							PointFromID:        moscowID,
							PointToType:        "c",
							PointToID:          novosibirskID,
							RouteUID:           "DP 206",
							DepartureWeekday:   1,
							DaysToFlightBucket: 1,
							AdultSeats:         1,
							Price:              1000,
						},
						expectedCategory: checkprice.PriceCategoryGood,
					},
					{
						request: checkprice.CheckPriceRequest{
							PointFromType:      "c",
							PointFromID:        moscowID,
							PointToType:        "c",
							PointToID:          novosibirskID,
							RouteUID:           "DP 206",
							DepartureWeekday:   1,
							DaysToFlightBucket: 1,
							AdultSeats:         1,
							Price:              4000,
						},
						expectedCategory: checkprice.PriceCategoryUnknown,
					},
					{
						request: checkprice.CheckPriceRequest{
							PointFromType:      "c",
							PointFromID:        moscowID,
							PointToType:        "c",
							PointToID:          novosibirskID,
							RouteUID:           "DP 206",
							DepartureWeekday:   1,
							DaysToFlightBucket: 1,
							AdultSeats:         1,
							Price:              7000,
						},
						expectedCategory: checkprice.PriceCategoryBad,
					},
				}
				for _, tc := range testCases {
					result := checkPriceService.CheckPrice(ctx, tc.request)
					require.Equal(t, result, tc.expectedCategory)
				}
			},
		),
	)
}

func TestServiceInTesting_CheckPrice(t *testing.T) {
	logger, err := logging.New(&logging.DefaultConfig)
	if err != nil {
		panic(err)
	}

	testPgClient := database.GetTestPgClient()
	dbRepository := database.NewVariantsPriceStatsDBRepository(testPgClient, true)
	dbOperationTimeout := database.GetDBTestTimeout()
	withEmptyDB := database.NewDBCleaner(testPgClient)

	t.Run(
		"Check price category", withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbOperationTimeout)
				defer cancelFunc()

				_, err := dbRepository.Create(ctx, models.VariantPriceStat{
					PointFromType:    "c",
					PointFromID:      moscowID,
					PointToType:      "c",
					PointToID:        novosibirskID,
					RouteUID:         "DP 206",
					DepartureWeekday: 1,
					DaysToFlight:     1,
					Q33:              3000,
					Q67:              6000,
				})
				require.NoError(t, err)

				_, err = dbRepository.Create(ctx, models.VariantPriceStat{
					PointFromType:    "c",
					PointFromID:      moscowID,
					PointToType:      "c",
					PointToID:        novosibirskID,
					RouteUID:         "S7 3185",
					DepartureWeekday: 1,
					DaysToFlight:     1,
					Q33:              3000,
					Q67:              6000,
				})
				require.NoError(t, err)

				repository, err := dbcache.NewVariantsPriceStatsRepository(testPgClient, true, dbcache.DefaultConfig, logger)
				require.NoError(t, err)

				checkPriceService := BuildService(logger, repository, true)

				testCases := []struct {
					request          checkprice.CheckPriceRequest
					expectedCategory checkprice.PriceCategory
				}{
					{
						request:          checkprice.CheckPriceRequest{},
						expectedCategory: checkprice.PriceCategoryUnknown,
					},
					{
						request: checkprice.CheckPriceRequest{
							PointFromType:      "c",
							PointFromID:        moscowID,
							PointToType:        "c",
							PointToID:          novosibirskID,
							RouteUID:           "DP 206",
							DepartureWeekday:   1,
							DaysToFlightBucket: 1,
							AdultSeats:         1,
							Price:              1000,
						},
						expectedCategory: checkprice.PriceCategoryGood,
					},
					{
						request: checkprice.CheckPriceRequest{
							PointFromType:      "c",
							PointFromID:        moscowID,
							PointToType:        "c",
							PointToID:          novosibirskID,
							RouteUID:           "DP 206",
							DepartureWeekday:   1,
							DaysToFlightBucket: 1,
							AdultSeats:         1,
							Price:              4000,
						},
						expectedCategory: checkprice.PriceCategoryGood,
					},
					{
						request: checkprice.CheckPriceRequest{
							PointFromType:      "c",
							PointFromID:        moscowID,
							PointToType:        "c",
							PointToID:          novosibirskID,
							RouteUID:           "DP 206",
							DepartureWeekday:   1,
							DaysToFlightBucket: 1,
							AdultSeats:         1,
							Price:              7000,
						},
						expectedCategory: checkprice.PriceCategoryGood,
					},
					{
						request: checkprice.CheckPriceRequest{
							PointFromType:      "c",
							PointFromID:        moscowID,
							PointToType:        "c",
							PointToID:          novosibirskID,
							RouteUID:           "S7 3185", // Not a pobeda
							DepartureWeekday:   1,
							DaysToFlightBucket: 1,
							AdultSeats:         1,
							Price:              4000,
						},
						expectedCategory: checkprice.PriceCategoryUnknown,
					},
					{
						request: checkprice.CheckPriceRequest{
							PointFromType:      "c",
							PointFromID:        spbID, // Another direction
							PointToType:        "c",
							PointToID:          novosibirskID,
							RouteUID:           "DP 206",
							DepartureWeekday:   1,
							DaysToFlightBucket: 1,
							AdultSeats:         1,
							Price:              4000,
						},
						expectedCategory: checkprice.PriceCategoryUnknown,
					},
					{
						request: checkprice.CheckPriceRequest{
							PointFromType:      "c",
							PointFromID:        moscowID,
							PointToType:        "c",
							PointToID:          spbID, // Another direction
							RouteUID:           "DP 206",
							DepartureWeekday:   1,
							DaysToFlightBucket: 1,
							AdultSeats:         1,
							Price:              4000,
						},
						expectedCategory: checkprice.PriceCategoryUnknown,
					},
				}
				for _, tc := range testCases {
					result := checkPriceService.CheckPrice(ctx, tc.request)
					require.Equal(t, result, tc.expectedCategory)
				}
			},
		),
	)
}
