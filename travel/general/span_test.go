package eventslogger

import (
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/travel/komod/trips/internal/models"
	"a.yandex-team.ru/yt/go/yson"
)

func TestMarshalVisit(t *testing.T) {
	loc, _ := time.LoadLocation("Europe/Moscow")
	visit := newMarshalableVisit(
		models.NewVisit(
			models.NewGeoRegionPoint(1, loc, nil),
			time.Date(2022, 02, 07, 10, 11, 0, 0, loc),
		),
	)
	bytes, err := yson.Marshal(visit)

	require.NoError(t, err)
	require.Equal(t, `{pointKey=g1;time="2022-02-07T10:11:00+03:00";}`, string(bytes))
}
