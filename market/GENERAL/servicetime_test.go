package graph

import (
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/timex"
	"a.yandex-team.ru/market/combinator/pkg/util"
)

func TestCompactTime(t *testing.T) {
	var ct CompactTime
	require.Equal(t, time.Time{}, ct.Time())
	require.Equal(t, int64(-1), ct.Unix()) // особенность реализации

	loc := timex.FixedZone("UTC+3", util.MskTZOffset)
	times := []time.Time{
		time.Unix(0, 0).UTC(),
		time.Date(2022, 6, 26, 10, 20, 30, 0, time.UTC),
		time.Date(2022, 6, 26, 10, 20, 30, 0, time.Local).In(loc), // need change locale
		time.Now().Round(time.Second).UTC(),                       // need round nanoseconds
		time.Now().Round(time.Second).In(loc),                     // need change locale
	}
	for _, tt := range times {
		var ct CompactTime
		ct.SetTime(tt)
		require.Equal(t, tt, ct.Time())
		require.Equal(t, tt.Unix(), ct.Unix())
	}
}
