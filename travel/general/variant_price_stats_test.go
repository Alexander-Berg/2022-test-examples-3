package dbcache

import (
	"a.yandex-team.ru/travel/avia/price_prediction/internal/database"
	"a.yandex-team.ru/travel/avia/price_prediction/internal/models"
	"a.yandex-team.ru/travel/library/go/logging"
	"context"
	"fmt"
	"github.com/stretchr/testify/require"
	"testing"
	"time"
)

var tz = time.FixedZone("MyZone", 0)

func TestUpdate(t *testing.T) {
	logger, err := logging.New(&logging.DefaultConfig)
	if err != nil {
		panic(err)
	}

	testPgClient := database.GetTestPgClient()
	dbRepository := database.NewVariantsPriceStatsDBRepository(testPgClient, true)
	dbOperationTimeout := database.GetDBTestTimeout()
	withEmptyDB := database.NewDBCleaner(testPgClient)

	t.Run(
		"Test update cache", withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbOperationTimeout)
				defer cancelFunc()

				_, err := dbRepository.Create(ctx, models.VariantPriceStat{
					PointFromType:    "c",
					PointFromID:      213,
					PointToType:      "c",
					PointToID:        2,
					RouteUID:         "DP 206",
					DepartureWeekday: 1,
					DaysToFlight:     1,
					Q33:              3000,
					Q67:              6000,
				})
				require.NoError(t, err)

				repository, err := NewVariantsPriceStatsRepository(testPgClient, true, DefaultConfig, logger)
				require.NoError(t, err)

				_, exists := repository.GetByKey(&models.PriceStatKey{
					PointFromType:    "c",
					PointFromID:      213,
					PointToType:      "c",
					PointToID:        2,
					RouteUID:         "DP 206",
					DepartureWeekday: 1,
					DaysToFlight:     1,
				})
				require.True(t, exists)

				_, exists = repository.GetByKey(&models.PriceStatKey{
					PointFromType:    "c",
					PointFromID:      2,
					PointToType:      "c",
					PointToID:        213,
					RouteUID:         "DP 207",
					DepartureWeekday: 1,
					DaysToFlight:     1,
				})
				require.False(t, exists)

				err = dbRepository.DeleteAll(ctx)
				require.NoError(t, err)
				_, err = dbRepository.Create(ctx, models.VariantPriceStat{
					PointFromType:    "c",
					PointFromID:      2,
					PointToType:      "c",
					PointToID:        213,
					RouteUID:         "DP 207",
					DepartureWeekday: 1,
					DaysToFlight:     1,
					Q33:              3000,
					Q67:              6000,
				})
				require.NoError(t, err)
				err = repository.update()
				require.NoError(t, err)

				_, exists = repository.GetByKey(&models.PriceStatKey{
					PointFromType:    "c",
					PointFromID:      213,
					PointToType:      "c",
					PointToID:        2,
					RouteUID:         "DP 206",
					DepartureWeekday: 1,
					DaysToFlight:     1,
				})
				require.False(t, exists)

				_, exists = repository.GetByKey(&models.PriceStatKey{
					PointFromType:    "c",
					PointFromID:      2,
					PointToType:      "c",
					PointToID:        213,
					RouteUID:         "DP 207",
					DepartureWeekday: 1,
					DaysToFlight:     1,
				})
				require.True(t, exists)
			},
		),
	)
}

func TestNeedToUpdate(t *testing.T) {
	testCases := []struct {
		now           time.Time
		lastUpdatedAt time.Time
		updateAtHour  int
		expected      bool
	}{
		{
			now:           time.Date(2021, 4, 13, 9, 30, 0, 0, tz),
			lastUpdatedAt: time.Date(2021, 4, 11, 21, 0, 0, 0, tz),
			updateAtHour:  10,
			expected:      true,
		},
		{
			now:           time.Date(2021, 4, 13, 21, 30, 0, 0, tz),
			lastUpdatedAt: time.Date(2021, 4, 11, 21, 0, 0, 0, tz),
			updateAtHour:  10,
			expected:      true,
		},

		{
			now:           time.Date(2021, 4, 13, 9, 30, 0, 0, tz),
			lastUpdatedAt: time.Date(2021, 4, 12, 9, 0, 0, 0, tz),
			updateAtHour:  10,
			expected:      true,
		},
		{
			now:           time.Date(2021, 4, 13, 21, 30, 0, 0, tz),
			lastUpdatedAt: time.Date(2021, 4, 12, 9, 0, 0, 0, tz),
			updateAtHour:  10,
			expected:      true,
		},

		{
			now:           time.Date(2021, 4, 13, 9, 30, 0, 0, tz),
			lastUpdatedAt: time.Date(2021, 4, 12, 21, 0, 0, 0, tz),
			updateAtHour:  10,
			expected:      false,
		},
		{
			now:           time.Date(2021, 4, 13, 21, 30, 0, 0, tz),
			lastUpdatedAt: time.Date(2021, 4, 12, 21, 0, 0, 0, tz),
			updateAtHour:  10,
			expected:      true,
		},

		{
			now:           time.Date(2021, 4, 13, 9, 30, 0, 0, tz),
			lastUpdatedAt: time.Date(2021, 4, 13, 9, 0, 0, 0, tz),
			updateAtHour:  10,
			expected:      false,
		},
		{
			now:           time.Date(2021, 4, 13, 21, 30, 0, 0, tz),
			lastUpdatedAt: time.Date(2021, 4, 13, 9, 0, 0, 0, tz),
			updateAtHour:  10,
			expected:      true,
		},

		{
			now:           time.Date(2021, 4, 13, 9, 30, 0, 0, tz),
			lastUpdatedAt: time.Date(2021, 4, 13, 21, 0, 0, 0, tz),
			updateAtHour:  10,
			expected:      false,
		},
		{
			now:           time.Date(2021, 4, 13, 21, 30, 0, 0, tz),
			lastUpdatedAt: time.Date(2021, 4, 13, 21, 0, 0, 0, tz),
			updateAtHour:  10,
			expected:      false,
		},
	}
	for _, tc := range testCases {
		testName := fmt.Sprintf("need to update [%s, %s, %d, %t]",
			tc.now,
			tc.lastUpdatedAt,
			tc.updateAtHour,
			tc.expected,
		)
		t.Run(testName, func(t *testing.T) {
			r := VariantsPriceStatsRepository{
				lastUpdatedAt: tc.lastUpdatedAt,
				cfg:           Config{UpdateAtHour: tc.updateAtHour},
			}
			require.Equal(t, r.needToUpdate(tc.now), tc.expected)
		})
	}
}
