package filtering2

import (
	"context"
	"testing"

	"github.com/stretchr/testify/require"
	"golang.org/x/exp/maps"

	"a.yandex-team.ru/library/go/core/log/nop"
	aviaAPI "a.yandex-team.ru/travel/app/backend/api/avia/v1"
	f "a.yandex-team.ru/travel/app/backend/internal/avia/search/proto/factories"
	aviaSearchProto "a.yandex-team.ru/travel/app/backend/internal/avia/search/proto/v1"
	"a.yandex-team.ru/travel/app/backend/internal/avia/search/searchcommon"
)

func TestAviacompany(t *testing.T) {
	r := f.NewReference()

	mskAirport := r.CreateStation(9600213)
	bgAirport := r.CreateStation(9623271)
	zeaAirport := r.CreateStation(9635355)

	ac := r.CreateAviaCompany(50)
	ac42 := r.CreateAviaCompany(42)

	partner := r.CreatePartner("some-partner")

	bzDirect := r.CreateFlight(ac, "HZ 2682", bgAirport, zeaAirport, "9:10 - 10:20")
	snippetDirect := f.CreateSnippet("snippet-direct", []string{bzDirect.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner),
	})

	bz42Direct := r.CreateFlight(ac42, "K4 5582", bgAirport, zeaAirport, "19:10 - 20:20")
	snippet42Direct := f.CreateSnippet("snippet-42-direct", []string{bz42Direct.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner),
	})

	bm := r.CreateFlight(ac, "HZ 5050", bgAirport, mskAirport, "9:10 - 10:20")
	mz42 := r.CreateFlight(ac42, "K4 4242", mskAirport, zeaAirport, "19:10 - 20:20")
	snippetMix := f.CreateSnippet("snippet-mix", []string{bm.Key, mz42.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner),
	})

	snippets := map[string]*aviaSearchProto.Snippet{
		snippetDirect.Key:   snippetDirect,
		snippet42Direct.Key: snippet42Direct,
		snippetMix.Key:      snippetMix,
	}

	reference := r.GetProto()

	filters := &aviaAPI.SearchFiltersReq{
		Aviacompany: &aviaAPI.SearchFiltersReq_AviacompanyFilter{
			AviacompanyCombinations: false,
			Aviacompanies: []*aviaAPI.SearchFiltersReq_AviacompanyState{
				{
					AviacompanyId: ac.Id,
					State:         false,
				},
				{
					AviacompanyId: ac42.Id,
					State:         true,
				},
			},
		},
	}
	searchContext, _ := searchcommon.ParseQIDToProto("220421-145911-315.travelapp.plane.c77_c11379_2022-04-26_None_economy_1_0_0_ru.ru")

	snippets, filterResponse := ApplyFilters(context.Background(), &nop.Logger{}, snippets, reference, searchContext, filters)

	snippetKeys := make(map[string]struct{})
	for key := range snippets {
		snippetKeys[key] = struct{}{}
	}
	expectedSnippetKeys := map[string]struct{}{snippet42Direct.Key: {}}
	require.True(t, maps.Equal(snippetKeys, expectedSnippetKeys), "unexpected snippets: %v != %v", snippetKeys, expectedSnippetKeys)
	require.NotNil(t, filterResponse)
	require.NotNil(t, filterResponse.Aviacompany)
	require.EqualValues(t, &aviaAPI.SearchFiltersRsp_AviacompanyFilter{
		AviacompanyCombinations: &aviaAPI.SearchFiltersRsp_BoolFilterState{
			Enabled: true,
			Value:   false,
		},
		Aviacompanies: []*aviaAPI.SearchFiltersRsp_AviacompanyState{
			{
				AviacompanyId: ac42.Id,
				State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
					Enabled: true,
					Value:   true,
				},
			},
			{
				AviacompanyId: ac.Id,
				State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
					Enabled: true,
					Value:   false,
				},
			},
		},
	}, filterResponse.Aviacompany)
}

func TestAviacompanyCombinations(t *testing.T) {
	r := f.NewReference()

	mskAirport := r.CreateStation(9600213)
	bgAirport := r.CreateStation(9623271)
	zeaAirport := r.CreateStation(9635355)

	ac := r.CreateAviaCompany(50)
	ac42 := r.CreateAviaCompany(42)

	partner := r.CreatePartner("some-partner")

	bzDirect := r.CreateFlight(ac, "HZ 2682", bgAirport, zeaAirport, "9:10 - 10:20")
	snippetDirect := f.CreateSnippet("snippet-direct", []string{bzDirect.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner),
	})

	bz42Direct := r.CreateFlight(ac42, "K4 5582", bgAirport, zeaAirport, "19:10 - 20:20")
	snippet42Direct := f.CreateSnippet("snippet-42-direct", []string{bz42Direct.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner),
	})

	bm := r.CreateFlight(ac, "HZ 5050", bgAirport, mskAirport, "9:10 - 10:20")
	mz42 := r.CreateFlight(ac42, "K4 4242", mskAirport, zeaAirport, "19:10 - 20:20")
	snippetMix := f.CreateSnippet("snippet-mix", []string{bm.Key, mz42.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner),
	})

	snippets := map[string]*aviaSearchProto.Snippet{
		snippetDirect.Key:   snippetDirect,
		snippet42Direct.Key: snippet42Direct,
		snippetMix.Key:      snippetMix,
	}

	reference := r.GetProto()

	filters := &aviaAPI.SearchFiltersReq{
		Aviacompany: &aviaAPI.SearchFiltersReq_AviacompanyFilter{
			AviacompanyCombinations: true,
			Aviacompanies: []*aviaAPI.SearchFiltersReq_AviacompanyState{
				{
					AviacompanyId: ac.Id,
					State:         false,
				},
				{
					AviacompanyId: ac42.Id,
					State:         true,
				},
			},
		},
	}
	searchContext, _ := searchcommon.ParseQIDToProto("220421-145911-315.travelapp.plane.c77_c11379_2022-04-26_None_economy_1_0_0_ru.ru")

	snippets, filterResponse := ApplyFilters(context.Background(), &nop.Logger{}, snippets, reference, searchContext, filters)

	snippetKeys := make(map[string]struct{})
	for key := range snippets {
		snippetKeys[key] = struct{}{}
	}
	expectedSnippetKeys := map[string]struct{}{snippet42Direct.Key: {}, snippetMix.Key: {}}
	require.True(t, maps.Equal(snippetKeys, expectedSnippetKeys), "unexpected snippets: %v != %v", snippetKeys, expectedSnippetKeys)
	require.NotNil(t, filterResponse)
	require.NotNil(t, filterResponse.Aviacompany)
	require.EqualValues(t, &aviaAPI.SearchFiltersRsp_AviacompanyFilter{
		AviacompanyCombinations: &aviaAPI.SearchFiltersRsp_BoolFilterState{
			Enabled: true,
			Value:   true,
		},
		Aviacompanies: []*aviaAPI.SearchFiltersRsp_AviacompanyState{
			{
				AviacompanyId: ac42.Id,
				State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
					Enabled: true,
					Value:   true,
				},
			},
			{
				AviacompanyId: ac.Id,
				State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
					Enabled: true,
					Value:   false,
				},
			},
		},
	}, filterResponse.Aviacompany)
}
