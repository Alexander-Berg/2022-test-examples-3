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

func TestAirport(t *testing.T) {
	r := f.NewReference()

	blagoveschinsk := r.CreateSettlement(77)
	zea := r.CreateSettlement(11379)

	bgAirport := r.CreateStation(9623271, f.WithSettlement(blagoveschinsk))
	bg2Airport := r.CreateStation(9623272, f.WithSettlement(blagoveschinsk))
	zeaAirport := r.CreateStation(9635355, f.WithSettlement(zea))

	ac := r.CreateAviaCompany(50)

	partner := r.CreatePartner("some-partner")

	bzMorning := r.CreateFlight(ac, "HZ 2682", bgAirport, zeaAirport, "9:00 - 10:30")
	snippet1 := f.CreateSnippet("snippet-1", []string{bzMorning.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner),
	})
	b2zEvening := r.CreateFlight(ac, "HZ 5582", bg2Airport, zeaAirport, "18:30 - 20:00")
	snippet2 := f.CreateSnippet("snippet-2", []string{b2zEvening.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner),
	})

	snippets := map[string]*aviaSearchProto.Snippet{
		snippet1.Key: snippet1,
		snippet2.Key: snippet2,
	}

	reference := r.GetProto()

	filters := &aviaAPI.SearchFiltersReq{
		Airport: &aviaAPI.SearchFiltersReq_AirportFilter{
			Forward: &aviaAPI.SearchFiltersReq_DirectionAirports{
				Departure: &aviaAPI.SearchFiltersReq_DepartureOrArrivalAirports{
					SettlementId: blagoveschinsk.Id,
					Airports: []*aviaAPI.SearchFiltersReq_Airport{
						{
							State:     false,
							StationId: bgAirport.Id,
						},
						{
							State:     true,
							StationId: bg2Airport.Id,
						},
					},
				},
				Arrival: &aviaAPI.SearchFiltersReq_DepartureOrArrivalAirports{
					SettlementId: zea.Id,
					Airports: []*aviaAPI.SearchFiltersReq_Airport{
						{
							State:     false,
							StationId: zeaAirport.Id,
						},
					},
				},
			},
			Backward: nil,
		},
	}
	searchContext, _ := searchcommon.ParseQIDToProto("220421-145911-315.travelapp.plane.c77_c11379_2022-04-26_None_economy_1_0_0_ru.ru")

	snippets, filterResponse := ApplyFilters(context.Background(), &nop.Logger{}, snippets, reference, searchContext, filters)

	snippetKeys := make(map[string]struct{})
	for key := range snippets {
		snippetKeys[key] = struct{}{}
	}
	expectedSnippetKeys := map[string]struct{}{"snippet-2": {}}
	require.True(t, maps.Equal(snippetKeys, expectedSnippetKeys), "unexpected snippets: %v != %v", snippetKeys, expectedSnippetKeys)
	require.NotNil(t, filterResponse)
	require.EqualValues(t, &aviaAPI.SearchFiltersRsp_AirportFilter{
		Forward: &aviaAPI.SearchFiltersRsp_DirectionAirports{
			Departure: &aviaAPI.SearchFiltersRsp_DepartureOrArrivalAirports{
				SettlementId: blagoveschinsk.Id,
				Airports: []*aviaAPI.SearchFiltersRsp_Airport{
					{
						State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
							Enabled: true,
							Value:   false,
						},
						StationId:       bgAirport.Id,
						StationTitle:    bgAirport.Title,
						SettlementTitle: "",
						AviaCode:        bgAirport.AviaCode,
					},
					{
						State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
							Enabled: true,
							Value:   true,
						},
						StationId:       bg2Airport.Id,
						StationTitle:    bg2Airport.Title,
						SettlementTitle: "",
						AviaCode:        bg2Airport.AviaCode,
					},
				},
				Title: "Вылет из Города 77",
			},
			Arrival: &aviaAPI.SearchFiltersRsp_DepartureOrArrivalAirports{
				SettlementId: zea.Id,
				Airports: []*aviaAPI.SearchFiltersRsp_Airport{
					{
						State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
							Enabled: true,
							Value:   false,
						},
						StationId:       zeaAirport.Id,
						StationTitle:    zeaAirport.Title,
						SettlementTitle: "",
						AviaCode:        zeaAirport.AviaCode,
					},
				},
				Title: "Прилёт в Город 11379",
			},
		},
		Backward: nil,
	}, filterResponse.Airport)
}
