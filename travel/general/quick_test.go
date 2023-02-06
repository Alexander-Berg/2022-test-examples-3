package filtering

import (
	"context"
	"fmt"
	"testing"

	"github.com/stretchr/testify/require"
	"golang.org/x/exp/maps"

	"a.yandex-team.ru/library/go/core/log/nop"
	aviaAPI "a.yandex-team.ru/travel/app/backend/api/avia/v1"
	commonAPI "a.yandex-team.ru/travel/app/backend/api/common/v1"
	f "a.yandex-team.ru/travel/app/backend/internal/avia/search/proto/factories"
	aviaSearchProto "a.yandex-team.ru/travel/app/backend/internal/avia/search/proto/v1"
	"a.yandex-team.ru/travel/app/backend/internal/avia/search/searchcommon"
)

func TestBaggage(t *testing.T) {
	r := f.NewReference()

	bgAirport := r.CreateStation(9623271)
	zeaAirport := r.CreateStation(9635355)

	ac := r.CreateAviaCompany(50)

	partner := r.CreatePartner("some-partner")

	bgMorning := r.CreateFlight(ac, "HZ 2682", bgAirport, zeaAirport, "9:10 - 10:20")
	snippet1 := f.CreateSnippet("snippet-1", []string{bgMorning.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(9897), f.WithBaggage(21, 1)),
	})

	bgEvening := r.CreateFlight(ac, "K4 5582", bgAirport, zeaAirport, "19:10 - 20:20")
	snippet2 := f.CreateSnippet("snippet-2", []string{bgEvening.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(3329)),
	})

	snippets := map[string]*aviaSearchProto.Snippet{
		snippet1.Key: snippet1,
		snippet2.Key: snippet2,
	}

	reference := r.GetProto()

	filters := &aviaAPI.SearchFiltersReq{
		QuickBaggage: true,
	}
	searchContext, _ := searchcommon.ParseQIDToProto("220421-145911-315.travelapp.plane.c77_c11379_2022-04-26_None_economy_1_0_0_ru.ru")

	snippets, filterResponse := ApplyFilters(context.Background(), &nop.Logger{}, snippets, reference, searchContext, filters)

	snippetKeys := make(map[string]struct{})
	for key := range snippets {
		snippetKeys[key] = struct{}{}
	}
	expectedSnippetKeys := map[string]struct{}{"snippet-1": {}}
	require.True(t, maps.Equal(snippetKeys, expectedSnippetKeys), "unexpected snippets: %v != %v", snippetKeys, expectedSnippetKeys)
	require.NotNil(t, filterResponse)
	require.NotNil(t, filterResponse.QuickBaggage)
	require.EqualValues(t, &aviaAPI.SearchFiltersRsp_QuickBaggageFilter{
		State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
			Enabled: true,
			Value:   true,
		},
		MinPriceNoBaggage: &commonAPI.Price{
			Currency: "RUB",
			Value:    3329,
		},
		MinPriceWithBaggage: &commonAPI.Price{
			Currency: "RUB",
			Value:    9897,
		},
	}, filterResponse.QuickBaggage)
}

func TestBaggage_OnlyBaggage(t *testing.T) {
	r := f.NewReference()

	bgAirport := r.CreateStation(9623271)
	zeaAirport := r.CreateStation(9635355)

	ac := r.CreateAviaCompany(50)

	partner := r.CreatePartner("some-partner")

	bgMorning := r.CreateFlight(ac, "HZ 2682", bgAirport, zeaAirport, "9:10 - 10:20")
	snippet := f.CreateSnippet("snippet", []string{bgMorning.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(9897), f.WithBaggage(21, 1)),
	})

	reference := r.GetProto()
	searchContext, _ := searchcommon.ParseQIDToProto("220421-145911-315.travelapp.plane.c77_c11379_2022-04-26_None_economy_1_0_0_ru.ru")

	for _, initialFilterState := range []bool{true, false} {
		t.Run(fmt.Sprintf("TestBaggage_OnlyBaggage - initialFilterState: %t", initialFilterState), func(t *testing.T) {
			snippets := map[string]*aviaSearchProto.Snippet{snippet.Key: snippet}
			filters := &aviaAPI.SearchFiltersReq{
				QuickBaggage: initialFilterState,
			}

			snippets, filterResponse := ApplyFilters(context.Background(), &nop.Logger{}, snippets, reference, searchContext, filters)

			snippetKeys := make(map[string]struct{})
			for key := range snippets {
				snippetKeys[key] = struct{}{}
			}
			expectedSnippetKeys := map[string]struct{}{"snippet": {}}
			require.True(t, maps.Equal(snippetKeys, expectedSnippetKeys), "unexpected snippets: %v != %v", snippetKeys, expectedSnippetKeys)
			require.NotNil(t, filterResponse)
			require.NotNil(t, filterResponse.QuickBaggage)
			require.EqualValues(t, &aviaAPI.SearchFiltersRsp_QuickBaggageFilter{
				State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
					Enabled: false,
					Value:   true,
				},
				MinPriceNoBaggage: nil,
				MinPriceWithBaggage: &commonAPI.Price{
					Currency: "RUB",
					Value:    9897,
				},
			}, filterResponse.QuickBaggage)
		})
	}
}

func TestBaggage_OnlyNoBaggage(t *testing.T) {
	r := f.NewReference()

	bgAirport := r.CreateStation(9623271)
	zeaAirport := r.CreateStation(9635355)

	ac := r.CreateAviaCompany(50)

	partner := r.CreatePartner("some-partner")

	bgMorning := r.CreateFlight(ac, "HZ 2682", bgAirport, zeaAirport, "9:10 - 10:20")
	snippet := f.CreateSnippet("snippet", []string{bgMorning.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(5000)),
	})

	reference := r.GetProto()
	searchContext, _ := searchcommon.ParseQIDToProto("220421-145911-315.travelapp.plane.c77_c11379_2022-04-26_None_economy_1_0_0_ru.ru")

	for _, initialFilterState := range []bool{true, false} {
		t.Run(fmt.Sprintf("TestBaggage_OnlyBaggage - initialFilterState: %t", initialFilterState), func(t *testing.T) {
			filters := &aviaAPI.SearchFiltersReq{
				QuickBaggage: initialFilterState,
			}
			snippets := map[string]*aviaSearchProto.Snippet{snippet.Key: snippet}

			snippets, filterResponse := ApplyFilters(context.Background(), &nop.Logger{}, snippets, reference, searchContext, filters)

			snippetKeys := make(map[string]struct{})
			for key := range snippets {
				snippetKeys[key] = struct{}{}
			}
			expectedSnippetKeys := map[string]struct{}{"snippet": {}}
			require.True(t, maps.Equal(snippetKeys, expectedSnippetKeys), "unexpected snippets: %v != %v", snippetKeys, expectedSnippetKeys)
			require.NotNil(t, filterResponse)
			require.NotNil(t, filterResponse.QuickBaggage)
			require.EqualValues(t, &aviaAPI.SearchFiltersRsp_QuickBaggageFilter{
				State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
					Enabled: false,
					Value:   false,
				},
				MinPriceNoBaggage: &commonAPI.Price{
					Currency: "RUB",
					Value:    5000,
				},
				MinPriceWithBaggage: nil,
			}, filterResponse.QuickBaggage)
		})
	}
}

func TestBaggage_NoSnippets(t *testing.T) {
	snippets := map[string]*aviaSearchProto.Snippet{}
	r := f.NewReference()
	reference := r.GetProto()
	filters := &aviaAPI.SearchFiltersReq{
		QuickBaggage: true,
	}
	searchContext, _ := searchcommon.ParseQIDToProto("220421-145911-315.travelapp.plane.c77_c11379_2022-04-26_None_economy_1_0_0_ru.ru")

	snippets, filterResponse := ApplyFilters(context.Background(), &nop.Logger{}, snippets, reference, searchContext, filters)

	snippetKeys := make(map[string]struct{})
	for key := range snippets {
		snippetKeys[key] = struct{}{}
	}
	require.NotNil(t, filterResponse)
	require.NotNil(t, filterResponse.QuickBaggage)
	require.EqualValues(t, &aviaAPI.SearchFiltersRsp_QuickBaggageFilter{
		State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
			Enabled: false,
			Value:   false,
		},
		MinPriceNoBaggage:   nil,
		MinPriceWithBaggage: nil,
	}, filterResponse.QuickBaggage)
}

func TestBaggage_NilFiltersInRequest(t *testing.T) {
	r := f.NewReference()

	bgAirport := r.CreateStation(9623271)
	zeaAirport := r.CreateStation(9635355)

	ac := r.CreateAviaCompany(50)

	partner := r.CreatePartner("some-partner")

	bg := r.CreateFlight(ac, "HZ 2682", bgAirport, zeaAirport, "9:10 - 10:20")
	snippet1 := f.CreateSnippet("snippet-1", []string{bg.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(9000), f.WithBaggage(21, 1)),
		"2": f.CreateVariant("2", partner, f.WithPriceRUB(6000)),
	})

	snippets := map[string]*aviaSearchProto.Snippet{
		snippet1.Key: snippet1,
	}

	reference := r.GetProto()

	filters := (*aviaAPI.SearchFiltersReq)(nil)
	searchContext, _ := searchcommon.ParseQIDToProto("220421-145911-315.travelapp.plane.c77_c11379_2022-04-26_None_economy_1_0_0_ru.ru")

	snippets, filterResponse := ApplyFilters(context.Background(), &nop.Logger{}, snippets, reference, searchContext, filters)

	snippetKeys := make(map[string]struct{})
	for key := range snippets {
		snippetKeys[key] = struct{}{}
	}
	expectedSnippetKeys := map[string]struct{}{"snippet-1": {}}
	require.True(t, maps.Equal(snippetKeys, expectedSnippetKeys), "unexpected snippets: %v != %v", snippetKeys, expectedSnippetKeys)
	require.NotNil(t, filterResponse)
	require.NotNil(t, filterResponse.QuickBaggage)
	require.EqualValues(t, &aviaAPI.SearchFiltersRsp_QuickBaggageFilter{
		State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
			Enabled: true,
			Value:   false,
		},
		MinPriceNoBaggage: &commonAPI.Price{
			Currency: "RUB",
			Value:    6000,
		},
		MinPriceWithBaggage: &commonAPI.Price{
			Currency: "RUB",
			Value:    9000,
		},
	}, filterResponse.QuickBaggage)
}
