package schedule

import (
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/test/requirepb"
	"a.yandex-team.ru/travel/proto/dicts/rasp"
)

func TestAddingStopsInOneThread(t *testing.T) {
	stops := make(pointToStops)
	stops.addThreadStation(1, 1, &rasp.TThreadStation{Id: 1})
	stops.addThreadStation(1, 1, &rasp.TThreadStation{Id: 2})

	require.Equal(t, len(stops), 1)
	require.Equal(t, len(stops[1]), 1)

	stations := stops[1][1]
	require.Equal(t, len(stations), 2)
	requirepb.Equal(t, stations[0], &rasp.TThreadStation{Id: 1})
	requirepb.Equal(t, stations[1], &rasp.TThreadStation{Id: 2})
}

func TestAddingStopsInDifferentThreads(t *testing.T) {
	stops := make(pointToStops)
	stops.addThreadStation(1, 0, &rasp.TThreadStation{Id: 0})
	stops.addThreadStation(1, 1, &rasp.TThreadStation{Id: 1})

	require.Equal(t, len(stops), 1)
	require.Equal(t, len(stops[1]), 2)

	for i := 0; i < 2; i++ {
		stations := stops[1][i]
		require.Equal(t, len(stations), 1)
		requirepb.Equal(t, stations[0], &rasp.TThreadStation{Id: int32(i)})
	}
}

func TestAddingStopsInDifferentPoints(t *testing.T) {
	stops := make(pointToStops)
	stops.addThreadStation(0, 1, &rasp.TThreadStation{Id: 0})
	stops.addThreadStation(1, 1, &rasp.TThreadStation{Id: 1})

	require.Equal(t, len(stops), 2)

	for i := 0; i < 2; i++ {
		require.Equal(t, len(stops[i]), 1)
		stations := stops[i][1]
		require.Equal(t, len(stations), 1)
		requirepb.Equal(t, stations[0], &rasp.TThreadStation{Id: int32(i)})
	}
}
