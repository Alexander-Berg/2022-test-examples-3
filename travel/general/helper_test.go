package span

import (
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/travel/komod/trips/internal/models"
	"a.yandex-team.ru/travel/komod/trips/internal/point"
	"a.yandex-team.ru/travel/komod/trips/internal/testutils"
	"a.yandex-team.ru/travel/library/go/geobase"
)

var (
	comparator = NewSpanComparator(point.NewComparator(geobase.StubGeobase{}, nil, nil))
	helper     = Helper{spanComparator: comparator}
)

func TestHelper_ExtractVisitsRemovingExtremes(t *testing.T) {
	tests := []struct {
		name  string
		spans []models.Span
		want  []models.Visit
	}{
		{
			name:  "empty slice",
			spans: []models.Span{},
		},
		{
			name: "round trip",
			spans: []models.Span{
				models.NewSpan(
					makeFakeVisit(1, "2021-12-12"),
					makeFakeVisit(2, "2021-12-13"),
					true,
				),
				models.NewSpan(
					makeFakeVisit(2, "2021-12-14"),
					makeFakeVisit(1, "2021-12-15"),
					true,
				),
			},
			want: []models.Visit{
				makeFakeVisit(2, "2021-12-13"),
				makeFakeVisit(2, "2021-12-14"),
			},
		},
		{
			name: "not round trip",
			spans: []models.Span{
				models.NewSpan(
					makeFakeVisit(1, "2021-12-12"),
					makeFakeVisit(2, "2021-12-13"),
					true,
				),
				models.NewSpan(
					makeFakeVisit(2, "2021-12-14"),
					makeFakeVisit(3, "2021-12-15"),
					true,
				),
			},
			want: []models.Visit{
				makeFakeVisit(2, "2021-12-13"),
				makeFakeVisit(2, "2021-12-14"),
				makeFakeVisit(3, "2021-12-15"),
			},
		},
		{
			name: "transfer inside the one point",
			spans: []models.Span{
				models.NewSpan(
					makeFakeVisit(1, "2021-12-12"),
					makeFakeVisit(1, "2021-12-13"),
					true,
				),
			},
			want: []models.Visit{
				makeFakeVisit(1, "2021-12-13"),
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := helper.ExtractVisitsRemovingExtremes(tt.spans)
			require.Equal(t, len(tt.want), len(got))
			for i := range got {
				require.True(t, comparator.pointComparator.SamePoints(got[i].Point(), tt.want[i].Point()))
			}
		})
	}
}

func TestHelper_ReduceTransfers(t *testing.T) {
	tests := []struct {
		name  string
		spans []models.Span
		want  []models.Span
	}{
		{
			name: "2 is transfer point -> reduce spans",
			spans: []models.Span{
				models.NewSpan(makeFakeVisit(1, "2021-12-12"), makeFakeVisit(2, "2021-12-14"), true),
				models.NewSpan(makeFakeVisit(2, "2021-12-14T23:59:59"), makeFakeVisit(3, "2021-12-17"), true),
			},
			want: []models.Span{
				models.NewSpan(makeFakeVisit(1, "2021-12-12"), makeFakeVisit(3, "2021-12-17"), true),
			},
		},
		{
			name: "stay in 2 for 1 day -> not consider 2 as transfer",
			spans: []models.Span{
				models.NewSpan(makeFakeVisit(1, "2021-12-12"), makeFakeVisit(2, "2021-12-14"), true),
				models.NewSpan(makeFakeVisit(2, "2021-12-15"), makeFakeVisit(3, "2021-12-17"), true),
			},
			want: []models.Span{
				models.NewSpan(makeFakeVisit(1, "2021-12-12"), makeFakeVisit(2, "2021-12-14"), true),
				models.NewSpan(makeFakeVisit(2, "2021-12-15"), makeFakeVisit(3, "2021-12-17"), true),
			},
		},
		{
			name: "long stay in one point -> don't reduce",
			spans: []models.Span{
				models.NewSpan(makeFakeVisit(1, "2021-12-12"), makeFakeVisit(2, "2021-12-14"), true),
				models.NewSpan(makeFakeVisit(2, "2021-12-17"), makeFakeVisit(3, "2021-12-18"), true),
			},
			want: []models.Span{
				models.NewSpan(makeFakeVisit(1, "2021-12-12"), makeFakeVisit(2, "2021-12-14"), true),
				models.NewSpan(makeFakeVisit(2, "2021-12-17"), makeFakeVisit(3, "2021-12-18"), true),
			},
		},
		{
			name: "staying in hotel is not a transfer",
			spans: []models.Span{
				models.NewSpan(makeFakeVisit(1, "2021-12-12"), makeFakeVisit(2, "2021-12-14"), false),
				models.NewSpan(makeFakeVisit(2, "2021-12-15"), makeFakeVisit(2, "2021-12-17"), false),
				models.NewSpan(makeFakeVisit(2, "2021-12-18"), makeFakeVisit(3, "2021-12-20"), false),
			},
			want: []models.Span{
				models.NewSpan(makeFakeVisit(1, "2021-12-12"), makeFakeVisit(2, "2021-12-14"), false),
				models.NewSpan(makeFakeVisit(2, "2021-12-15"), makeFakeVisit(2, "2021-12-17"), false),
				models.NewSpan(makeFakeVisit(2, "2021-12-18"), makeFakeVisit(3, "2021-12-20"), false),
			},
		},
		{
			name: "round-trip is not a transfer",
			spans: []models.Span{
				models.NewSpan(makeFakeVisit(1, "2021-12-12"), makeFakeVisit(2, "2021-12-12"), true),
				models.NewSpan(makeFakeVisit(2, "2021-12-17"), makeFakeVisit(1, "2021-12-17"), true),
			},
			want: []models.Span{
				models.NewSpan(makeFakeVisit(1, "2021-12-12"), makeFakeVisit(2, "2021-12-12"), true),
				models.NewSpan(makeFakeVisit(2, "2021-12-17"), makeFakeVisit(1, "2021-12-17"), true),
			},
		},
		{
			name: "one-day round-trip is not a transfer",
			spans: []models.Span{
				models.NewSpan(makeFakeVisit(1, "2021-12-12"), makeFakeVisit(2, "2021-12-12"), true),
				models.NewSpan(makeFakeVisit(2, "2021-12-12"), makeFakeVisit(1, "2021-12-12"), true),
			},
			want: []models.Span{
				models.NewSpan(makeFakeVisit(1, "2021-12-12"), makeFakeVisit(2, "2021-12-12"), true),
				models.NewSpan(makeFakeVisit(2, "2021-12-12"), makeFakeVisit(1, "2021-12-12"), true),
			},
		},
		{
			name: "two-days round-trip is not a transfer",
			spans: []models.Span{
				models.NewSpan(makeFakeVisit(1, "2021-12-12"), makeFakeVisit(2, "2021-12-12"), true),
				models.NewSpan(makeFakeVisit(2, "2021-12-13"), makeFakeVisit(1, "2021-12-13"), true),
			},
			want: []models.Span{
				models.NewSpan(makeFakeVisit(1, "2021-12-12"), makeFakeVisit(2, "2021-12-12"), true),
				models.NewSpan(makeFakeVisit(2, "2021-12-13"), makeFakeVisit(1, "2021-12-13"), true),
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := helper.ReduceTransfers(tt.spans)
			require.Equal(t, len(got), len(tt.want))
			for i, gotSpan := range got {
				require.True(t, comparator.SameSpans(gotSpan, tt.want[i]))
			}
		})
	}
}

func TestHelper_RemoveDuplicatedVisits(t *testing.T) {
	tests := []struct {
		name   string
		visits []models.Visit
		want   []models.Visit
	}{
		{
			name: "one visit",
			visits: []models.Visit{
				makeFakeVisit(1, "2021-12-12"),
			},
			want: []models.Visit{
				makeFakeVisit(1, "2021-12-12"),
			},
		},
		{
			name: "all visit with one point -> one visit",
			visits: []models.Visit{
				makeFakeVisit(1, "2021-12-12"),
				makeFakeVisit(1, "2021-12-13"),
				makeFakeVisit(1, "2021-12-14"),
			},
			want: []models.Visit{
				makeFakeVisit(1, "2021-12-12"),
			},
		},
		{
			name: "all visit with different points -> without changes",
			visits: []models.Visit{
				makeFakeVisit(1, "2021-12-12"),
				makeFakeVisit(2, "2021-12-13"),
				makeFakeVisit(3, "2021-12-14"),
			},
			want: []models.Visit{
				makeFakeVisit(1, "2021-12-12"),
				makeFakeVisit(2, "2021-12-13"),
				makeFakeVisit(3, "2021-12-14"),
			},
		},
		{
			name: "one point in two visits -> remove one of these",
			visits: []models.Visit{
				makeFakeVisit(1, "2021-12-12"),
				makeFakeVisit(2, "2021-12-13"),
				makeFakeVisit(1, "2021-12-14"),
				makeFakeVisit(2, "2021-12-15"),
				makeFakeVisit(3, "2021-12-16"),
				makeFakeVisit(3, "2021-12-17"),
				makeFakeVisit(4, "2021-12-18"),
			},
			want: []models.Visit{
				makeFakeVisit(1, "2021-12-12"),
				makeFakeVisit(2, "2021-12-13"),
				makeFakeVisit(3, "2021-12-16"),
				makeFakeVisit(4, "2021-12-18"),
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := helper.RemoveDuplicatedVisits(tt.visits)
			require.Equal(t, len(got), len(tt.want))
			for i, gotVisit := range got {
				require.True(t, comparator.pointComparator.SamePoints(gotVisit.Point(), tt.want[i].Point()))
				require.True(t, gotVisit.When() == tt.want[i].When())
			}
		})
	}
}

func TestHelper_MakeGaps(t *testing.T) {
	tests := []struct {
		name  string
		spans []models.Span
		want  []models.Span
	}{
		{
			name:  "no spans -> no gaps",
			spans: []models.Span{},
			want:  []models.Span{},
		},
		{
			name: "one span -> no gaps",
			spans: []models.Span{
				models.NewSpan(makeFakeVisit(1, "2021-12-12"), makeFakeVisit(2, "2021-12-14"), true),
			},
			want: []models.Span{},
		},
		{
			name: "two crossed spans -> no gaps",
			spans: []models.Span{
				models.NewSpan(makeFakeVisit(1, "2021-12-12"), makeFakeVisit(2, "2021-12-14"), true),
				models.NewSpan(makeFakeVisit(2, "2021-12-13"), makeFakeVisit(3, "2021-12-17"), true),
			},
			want: []models.Span{},
		},
		{
			name: "two spans -> one gap",
			spans: []models.Span{
				models.NewSpan(makeFakeVisit(1, "2021-12-12"), makeFakeVisit(2, "2021-12-14"), true),
				models.NewSpan(makeFakeVisit(2, "2021-12-17"), makeFakeVisit(3, "2021-12-18"), true),
			},
			want: []models.Span{
				models.NewSpan(makeFakeVisit(2, "2021-12-14"), makeFakeVisit(2, "2021-12-17"), false),
			},
		},
		{
			name: "unordered three spans -> two gaps",
			spans: []models.Span{
				models.NewSpan(makeFakeVisit(1, "2021-12-12"), makeFakeVisit(2, "2021-12-14"), true),
				models.NewSpan(makeFakeVisit(3, "2021-12-21"), makeFakeVisit(3, "2021-12-23"), true),
				models.NewSpan(makeFakeVisit(2, "2021-12-17"), makeFakeVisit(3, "2021-12-18"), true),
			},
			want: []models.Span{
				models.NewSpan(makeFakeVisit(2, "2021-12-14"), makeFakeVisit(2, "2021-12-17"), false),
				models.NewSpan(makeFakeVisit(3, "2021-12-18"), makeFakeVisit(3, "2021-12-21"), false),
			},
		},
		{
			name: "three crossed spans -> one gap",
			spans: []models.Span{
				models.NewSpan(makeFakeVisit(1, "2021-12-12"), makeFakeVisit(2, "2021-12-14"), true),
				models.NewSpan(makeFakeVisit(2, "2021-12-13"), makeFakeVisit(3, "2021-12-18"), true),
				models.NewSpan(makeFakeVisit(3, "2021-12-21"), makeFakeVisit(3, "2021-12-23"), true),
			},
			want: []models.Span{
				models.NewSpan(makeFakeVisit(3, "2021-12-18"), makeFakeVisit(3, "2021-12-21"), false),
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := helper.MakeGaps(tt.spans)
			require.Equal(t, len(got), len(tt.want))
			require.True(t, comparator.CompareSlices(got, tt.want))
		})
	}
}

func makeFakeVisit(id int, t string) models.Visit {
	return models.NewFakeVisit(id, testutils.ParseTime(t))
}
