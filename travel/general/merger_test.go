package results

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/ptr"
	"a.yandex-team.ru/travel/avia/wizard/pkg/wizard/domain/results"
	"a.yandex-team.ru/travel/avia/wizard/pkg/wizard/repositories"
	"a.yandex-team.ru/travel/avia/wizard/pkg/wizard/repositories/ydb"
	"a.yandex-team.ru/travel/proto/avia/wizard"
)

const (
	partner1 = "partner1"
	partner2 = "partner2"
	partner3 = "partner3"
)

func TestMergerByConversion_MergeByConversion(t *testing.T) {
	t.Run("nil result", func(t *testing.T) {
		doTest(t, testData{})
	})

	t.Run("one result -> same result", func(t *testing.T) {
		fare := buildFare("fare", partner1, 10, 10)
		result := buildWizardSearchResult(fare)
		doTest(t, testData{
			values:     []*ydb.WizardSearchResult{result},
			wantResult: result,
		})
	})

	t.Run("choose the most popular partner", func(t *testing.T) {
		fare1 := buildFare("fare", partner1, 100, 100)
		fare2 := buildFare("fare", partner2, 10, 100)
		doTest(t, testData{
			values: []*ydb.WizardSearchResult{
				buildWizardSearchResult(fare1),
				buildWizardSearchResult(fare2),
			},
			wantResult:   buildWizardSearchResult(fare2),
			wantMinPrice: 10,
		})
	})

	t.Run("pessimize partner rearrange", func(t *testing.T) {
		seg1partner1 := buildFare("seg1", partner1, 100, 100)
		seg1partner2 := buildFare("seg1", partner2, 10, 100)
		seg2partner1 := buildFare("seg2", partner1, 20, 10)
		seg2partner2 := buildFare("seg2", partner2, 100, 10)
		doTest(t, testData{
			values: []*ydb.WizardSearchResult{
				buildWizardSearchResult(seg1partner1, seg2partner1),
				buildWizardSearchResult(seg1partner2, seg2partner2),
			},
			wantResult:   buildWizardSearchResult(seg1partner2, seg2partner1),
			wantMinPrice: 10,
		})
	})

	t.Run("partner not found by conversion for second fare", func(t *testing.T) {
		fare1 := buildFare("seg1", partner1, 100, 100)
		fare2 := buildFare("seg1", partner2, 10, 100)
		fare3 := buildFare("seg2", partner2, 10, 10)
		doTest(t, testData{
			values: []*ydb.WizardSearchResult{
				buildWizardSearchResult(fare1),
				buildWizardSearchResult(fare2, fare3),
			},
			wantResult:   buildWizardSearchResult(fare2, fare3),
			wantMinPrice: 10,
		})
	})

	t.Run("use min price after conversion", func(t *testing.T) {
		seg1partner1 := buildFare("seg1", partner1, 100, 100)
		seg2partner2 := buildFare("seg2", partner2, 100, 99)
		seg3partner3 := buildFare("seg3", partner3, 100, 98)
		seg4partner1 := buildFare("seg4", partner1, 10, 97)
		seg4partner3 := buildFare("seg4", partner3, 100, 97)
		doTest(t, testData{
			values: []*ydb.WizardSearchResult{
				buildWizardSearchResult(seg1partner1, seg4partner1),
				buildWizardSearchResult(seg2partner2),
				buildWizardSearchResult(seg3partner3, seg4partner3),
			},
			wantResult:   buildWizardSearchResult(seg1partner1, seg2partner2, seg3partner3, seg4partner1),
			wantMinPrice: 10,
		})
	})
}

type testData struct {
	values       []*ydb.WizardSearchResult
	wantResult   *ydb.WizardSearchResult
	wantMinPrice int32
}

func doTest(t *testing.T, tt testData) {
	m := NewMergerByConversion(prepareConversionsRepository())
	got := m.MergeByConversion(tt.values)
	if tt.wantResult != nil {
		require.NotNil(t, got)
	} else {
		require.Nil(t, got)
		return
	}
	assert.Equal(t, tt.wantMinPrice, got.MinPrice)
	assert.Equal(t, tt.wantResult.DateForward, got.DateForward)
	assert.Equal(t, tt.wantResult.DateBackward, got.DateBackward)
	assert.Equal(t, tt.wantResult.SearchResult.Value, got.SearchResult.Value)
	assert.Equal(t, tt.wantResult.FilterState.Value, got.FilterState.Value)
}

type mockedConversionsRepository struct {
	conversions repositories.Conversions
}

func (r *mockedConversionsRepository) GetAll() repositories.Conversions {
	return r.conversions
}

func prepareConversionsRepository() ConversionRepository {
	conversions := repositories.Conversions{
		partner1: 1,
		partner2: 2,
		partner3: 3,
	}
	return &mockedConversionsRepository{conversions: conversions}
}

func buildWizardSearchResult(fares ...*wizard.Fare) *ydb.WizardSearchResult {
	const (
		fakeDateForward  = 1
		fakeDateBackward = 2
	)

	return &ydb.WizardSearchResult{
		DateForward:  fakeDateForward,
		DateBackward: fakeDateBackward,
		FilterState: ydb.FilterStateScanner{
			Value: results.FilterState{
				WithBaggage: ptr.Bool(true),
			},
		},
		SearchResult: ydb.SearchResultScanner{
			Value: &wizard.SearchResult{
				Qid: ptr.String("fake"),
				Flights: map[string]*wizard.Flight{
					"1": new(wizard.Flight),
				},
				Fares:         fares,
				Version:       ptr.Int32(2),
				OffersCount:   ptr.Int32(int32(len(fares))),
				PollingStatus: new(wizard.PollingStatus),
			},
		},
	}
}

func buildFare(segment string, partner string, price float64, popularity int32) *wizard.Fare {
	return &wizard.Fare{
		Route:      &wizard.RouteSegments{Forward: []string{segment}},
		Partner:    ptr.String(partner),
		Tariff:     &wizard.Tariff{Value: ptr.Float64(price)},
		Popularity: ptr.Int32(popularity),
	}
}

func Test_makeRouteKey(t *testing.T) {
	tests := []struct {
		name  string
		route *wizard.RouteSegments
		want  string
	}{
		{
			name:  "empty",
			route: new(wizard.RouteSegments),
			want:  "",
		},
		{
			name:  "nil",
			route: nil,
			want:  "",
		},
		{
			name: "one forward",
			route: &wizard.RouteSegments{
				Forward: []string{"f1"},
			},
			want: "f1|",
		},
		{
			name: "two forward",
			route: &wizard.RouteSegments{
				Forward: []string{"f1", "f2"},
			},
			want: "f1-f2|",
		},
		{
			name: "one backward",
			route: &wizard.RouteSegments{
				Backward: []string{"b1"},
			},
			want: "|b1",
		},
		{
			name: "two backward",
			route: &wizard.RouteSegments{
				Backward: []string{"b1", "b2"},
			},
			want: "|b1-b2",
		},
		{
			name: "same segments -> must not to deduplicate",
			route: &wizard.RouteSegments{
				Forward:  []string{"f1", "f1"},
				Backward: []string{"b1", "b1"},
			},
			want: "f1-f1|b1-b1",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			assert.Equalf(t, tt.want, makeRouteKey(tt.route), "makeRouteKey(%v)", tt.route)
		})
	}
}
