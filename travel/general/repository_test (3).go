package schedule

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/test/requirepb"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/clock"
	dictfactories "a.yandex-team.ru/travel/trains/search_api/internal/pkg/dict/factories"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/dict/registry"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/points"
	schedulefactories "a.yandex-team.ru/travel/trains/search_api/internal/pkg/schedule/factories"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/testutils"
)

func TestFindSegments(t *testing.T) {
	logger := testutils.NewLogger(t)
	repoRegistry := registry.NewRepositoryRegistry(logger)

	departureStation := dictfactories.NewStationFactory(repoRegistry).Create()
	arrivalStation := dictfactories.NewStationFactory(repoRegistry).Create()
	thread := schedulefactories.NewScheduleFactory(repoRegistry).
		AddStop(departureStation, time.Hour).
		AddStop(arrivalStation, time.Hour).
		Create()

	scheduleRepository := NewRepository(logger, repoRegistry)
	require.NoError(t, scheduleRepository.UpdateCache())

	segments := scheduleRepository.FindSegments(
		context.Background(),
		points.NewStation(departureStation),
		points.NewStation(arrivalStation),
		clock.Now(),
	)
	require.GreaterOrEqual(t, len(segments), 1)
	requirepb.Equal(t, thread, segments[0].Thread)
	require.Equal(t, departureStation.Id, segments[0].Departure.StationId)
	require.Equal(t, arrivalStation.Id, segments[0].Arrival.StationId)
}
