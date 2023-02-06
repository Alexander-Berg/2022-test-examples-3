package filtering

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

func TestTime(t *testing.T) {
	r := f.NewReference()

	blagoveschinsk := r.CreateSettlement(77)
	zea := r.CreateSettlement(11379)

	bgAirport := r.CreateStation(9623271, f.WithSettlement(blagoveschinsk))
	zeaAirport := r.CreateStation(9635355, f.WithSettlement(zea))

	ac := r.CreateAviaCompany(50)

	partner := r.CreatePartner("some-partner")

	bzMorning := r.CreateFlight(ac, "HZ 2682", bgAirport, zeaAirport, "9:10 - 10:20")
	snippet1 := f.CreateSnippet("snippet-1", []string{bzMorning.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner),
	})

	bzEvening := r.CreateFlight(ac, "K4 5582", bgAirport, zeaAirport, "19:10 - 20:20")
	snippet2 := f.CreateSnippet("snippet-2", []string{bzEvening.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("2", partner),
	})
	snippets := map[string]*aviaSearchProto.Snippet{
		snippet1.Key: snippet1,
		snippet2.Key: snippet2,
	}

	reference := r.GetProto()

	filters := &aviaAPI.SearchFiltersReq{
		DepartureAndArrival: &aviaAPI.SearchFiltersReq_DepartureAndArrivalFilter{
			ForwardDeparture: &aviaAPI.SearchFiltersReq_DepartureOrArrivalInterval{
				MinDatetime: "2022-04-26T08:00:00+03:00",
				MaxDatetime: "2022-04-26T10:00:00+03:00",
			},
			ForwardArrival: &aviaAPI.SearchFiltersReq_DepartureOrArrivalInterval{
				MinDatetime: "2022-04-26T09:00:00+03:00",
				MaxDatetime: "2022-04-26T11:00:00+03:00",
			},
			BackwardDeparture: nil,
			BackwardArrival:   nil,
		},
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
	require.NotNil(t, filterResponse.DepartureAndArrival)
	require.EqualValues(t, &aviaAPI.SearchFiltersRsp_DepartureAndArrivalFilter{
		ForwardDeparture: &aviaAPI.SearchFiltersRsp_DepartureOrArrivalState{
			SettlementId: blagoveschinsk.Id,
			StationId:    0,
			All: &aviaAPI.SearchFiltersRsp_DepartureOrArrivalInterval{
				MinDatetime: "2022-04-26T09:10:00+03:00",
				MaxDatetime: "2022-04-26T19:10:00+03:00",
			},
			Selected: &aviaAPI.SearchFiltersRsp_DepartureOrArrivalInterval{
				MinDatetime: "2022-04-26T09:10:00+03:00",
				MaxDatetime: "2022-04-26T10:00:00+03:00",
			},
			Title: "Вылет из Города 77",
		},
		ForwardArrival: &aviaAPI.SearchFiltersRsp_DepartureOrArrivalState{
			SettlementId: zea.Id,
			StationId:    0,
			All: &aviaAPI.SearchFiltersRsp_DepartureOrArrivalInterval{
				MinDatetime: "2022-04-26T10:20:00+03:00",
				MaxDatetime: "2022-04-26T20:20:00+03:00",
			},
			Selected: &aviaAPI.SearchFiltersRsp_DepartureOrArrivalInterval{
				MinDatetime: "2022-04-26T10:20:00+03:00",
				MaxDatetime: "2022-04-26T11:00:00+03:00",
			},
			Title: "Прилёт в Город 11379",
		},
		BackwardDeparture: nil,
		BackwardArrival:   nil,
	}, filterResponse.DepartureAndArrival)
}
