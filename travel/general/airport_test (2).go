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

func TestAirport(t *testing.T) {
	r := f.NewReference()
	ac := r.CreateAviaCompany(50)
	partner := r.CreatePartner("some-partner")

	msk := r.CreateMoscow()
	ekb := r.CreateYekaterinburg()

	dme := r.CreateDomodedovo(msk)
	svo := r.CreateSheremetyevo(msk)
	ekbAirport := r.CreateKoltsovo(ekb)

	dme2ekbFlight := r.CreateFlight(ac, "HZ 2682", dme, ekbAirport, "9:00 - 10:30")
	dme2ekb := f.CreateSnippet("dme2ekb", []string{dme2ekbFlight.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner),
	})
	svo2ekbFlight := r.CreateFlight(ac, "HZ 5582", svo, ekbAirport, "18:30 - 20:00")
	svo2ekb := f.CreateSnippet("svo2ekb", []string{svo2ekbFlight.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner),
	})

	snippets := map[string]*aviaSearchProto.Snippet{
		dme2ekb.Key: dme2ekb,
		svo2ekb.Key: svo2ekb,
	}

	reference := r.GetProto()

	filters := &aviaAPI.SearchFiltersReq{
		Airport: &aviaAPI.SearchFiltersReq_AirportFilter{
			Forward: &aviaAPI.SearchFiltersReq_DirectionAirports{
				Departure: &aviaAPI.SearchFiltersReq_DepartureOrArrivalAirports{
					SettlementId: msk.Id,
					Airports: []*aviaAPI.SearchFiltersReq_Airport{
						{
							State:     false,
							StationId: svo.Id,
						},
						{
							State:     true,
							StationId: dme.Id,
						},
					},
				},
				Arrival: &aviaAPI.SearchFiltersReq_DepartureOrArrivalAirports{
					SettlementId: ekb.Id,
					Airports: []*aviaAPI.SearchFiltersReq_Airport{
						{
							State:     false,
							StationId: ekbAirport.Id,
						},
					},
				},
			},
			Backward: nil,
		},
	}
	searchContext, _ := searchcommon.ParseQIDToProto("220421-145911-315.travelapp.plane.c213_c54_2022-04-26_None_economy_1_0_0_ru.ru")

	snippets, filterResponse := ApplyFilters(context.Background(), &nop.Logger{}, snippets, reference, searchContext, filters)

	snippetKeys := make(map[string]struct{})
	for key := range snippets {
		snippetKeys[key] = struct{}{}
	}
	expectedSnippetKeys := map[string]struct{}{dme2ekb.Key: {}}
	require.True(t, maps.Equal(snippetKeys, expectedSnippetKeys), "unexpected snippets: %v != %v", snippetKeys, expectedSnippetKeys)
	require.NotNil(t, filterResponse)
	require.EqualValues(t, &aviaAPI.SearchFiltersRsp_AirportFilter{
		Forward: &aviaAPI.SearchFiltersRsp_DirectionAirports{
			Departure: &aviaAPI.SearchFiltersRsp_DepartureOrArrivalAirports{
				SettlementId: msk.Id,
				Airports: []*aviaAPI.SearchFiltersRsp_Airport{
					{
						State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
							Enabled: true,
							Value:   false,
						},
						StationId:       svo.Id,
						StationTitle:    svo.Title,
						SettlementTitle: "",
						AviaCode:        svo.AviaCode,
					},
					{
						State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
							Enabled: true,
							Value:   true,
						},
						StationId:       dme.Id,
						StationTitle:    dme.Title,
						SettlementTitle: "",
						AviaCode:        dme.AviaCode,
					},
				},
				Title: "Вылет из Москвы",
			},
			Arrival: &aviaAPI.SearchFiltersRsp_DepartureOrArrivalAirports{
				SettlementId: ekb.Id,
				Airports: []*aviaAPI.SearchFiltersRsp_Airport{
					{
						State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
							Enabled: true,
							Value:   false,
						},
						StationId:       ekbAirport.Id,
						StationTitle:    ekbAirport.Title,
						SettlementTitle: "",
						AviaCode:        ekbAirport.AviaCode,
					},
				},
				Title: "Прилёт в Екатеринбург",
			},
		},
		Backward: nil,
	}, filterResponse.Airport)
}

func TestAirport_Backward(t *testing.T) {
	r := f.NewReference()
	ac := r.CreateAviaCompany(50)
	partner := r.CreatePartner("some-partner")

	msk := r.CreateMoscow()
	ekb := r.CreateYekaterinburg()

	dme := r.CreateDomodedovo(msk)
	ekbAirport := r.CreateKoltsovo(ekb)

	dme2ekbFlight := r.CreateFlight(ac, "HZ 2682", dme, ekbAirport, "9:00 - 10:30")
	ekb2dmeFlight := r.CreateFlight(ac, "HZ 2683", ekbAirport, dme, "12:00 - 13:30")
	dme2ekb := f.CreateSnippet("dme2ekb", []string{dme2ekbFlight.Key}, []string{ekb2dmeFlight.Key}, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner),
	})

	snippets := map[string]*aviaSearchProto.Snippet{
		dme2ekb.Key: dme2ekb,
	}

	reference := r.GetProto()
	filters := &aviaAPI.SearchFiltersReq{}
	searchContext, _ := searchcommon.ParseQIDToProto("220421-145911-315.travelapp.plane.c213_c54_2022-04-26_2022-04-27_economy_1_0_0_ru.ru")

	snippets, filterResponse := ApplyFilters(context.Background(), &nop.Logger{}, snippets, reference, searchContext, filters)

	snippetKeys := make(map[string]struct{})
	for key := range snippets {
		snippetKeys[key] = struct{}{}
	}
	expectedSnippetKeys := map[string]struct{}{dme2ekb.Key: {}}
	require.True(t, maps.Equal(snippetKeys, expectedSnippetKeys), "unexpected snippets: %v != %v", snippetKeys, expectedSnippetKeys)
	require.NotNil(t, filterResponse)
	require.EqualValues(t, &aviaAPI.SearchFiltersRsp_DirectionAirports{
		Departure: &aviaAPI.SearchFiltersRsp_DepartureOrArrivalAirports{
			SettlementId: ekb.Id,
			Airports: []*aviaAPI.SearchFiltersRsp_Airport{
				{
					State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
						Enabled: true,
						Value:   false,
					},
					StationId:       ekbAirport.Id,
					StationTitle:    ekbAirport.Title,
					SettlementTitle: "",
					AviaCode:        ekbAirport.AviaCode,
				},
			},
			Title: "Вылет из Екатеринбурга",
		},
		Arrival: &aviaAPI.SearchFiltersRsp_DepartureOrArrivalAirports{
			SettlementId: msk.Id,
			Airports: []*aviaAPI.SearchFiltersRsp_Airport{
				{
					State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
						Enabled: true,
						Value:   false,
					},
					StationId:       dme.Id,
					StationTitle:    dme.Title,
					SettlementTitle: "",
					AviaCode:        dme.AviaCode,
				},
			},
			Title: "Прилёт в Москву",
		},
	}, filterResponse.Airport.Backward)
}

func TestAirport_NeedSettlementTitleForAirports(t *testing.T) {
	r := f.NewReference()
	ac := r.CreateAviaCompany(50)
	partner := r.CreatePartner("some-partner")

	msk := r.CreateMoscow()
	ekb := r.CreateYekaterinburg()
	zia := r.CreateZhukovskiy()

	dme := r.CreateDomodedovo(msk)
	svo := r.CreateSheremetyevo(msk)
	ziaAirport := r.CreateZhukovskiyAirport(zia)
	ekbAirport := r.CreateKoltsovo(ekb)

	dme2ekbFlight := r.CreateFlight(ac, "HZ 2682", dme, ekbAirport, "9:00 - 10:30")
	dme2ekb := f.CreateSnippet("dme2ekb", []string{dme2ekbFlight.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner),
	})
	svo2ekbFlight := r.CreateFlight(ac, "HZ 5582", svo, ekbAirport, "18:30 - 20:00")
	svo2ekb := f.CreateSnippet("svo2ekb", []string{svo2ekbFlight.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner),
	})
	zia2ekbFlight := r.CreateFlight(ac, "HZ 2222", ziaAirport, ekbAirport, "9:20 - 10:50")
	zia2ekb := f.CreateSnippet("zia2ekb", []string{zia2ekbFlight.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner),
	})

	snippets := map[string]*aviaSearchProto.Snippet{
		dme2ekb.Key: dme2ekb,
		svo2ekb.Key: svo2ekb,
		zia2ekb.Key: zia2ekb,
	}

	reference := r.GetProto()

	filters := &aviaAPI.SearchFiltersReq{}
	searchContext, _ := searchcommon.ParseQIDToProto("220421-145911-315.travelapp.plane.c213_c54_2022-04-26_None_economy_1_0_0_ru.ru")

	snippets, filterResponse := ApplyFilters(context.Background(), &nop.Logger{}, snippets, reference, searchContext, filters)

	snippetKeys := make(map[string]struct{})
	for key := range snippets {
		snippetKeys[key] = struct{}{}
	}
	expectedSnippetKeys := map[string]struct{}{dme2ekb.Key: {}, svo2ekb.Key: {}, zia2ekb.Key: {}}
	require.True(t, maps.Equal(snippetKeys, expectedSnippetKeys), "unexpected snippets: %v != %v", snippetKeys, expectedSnippetKeys)
	require.NotNil(t, filterResponse)
	require.EqualValues(t, &aviaAPI.SearchFiltersRsp_AirportFilter{
		Forward: &aviaAPI.SearchFiltersRsp_DirectionAirports{
			Departure: &aviaAPI.SearchFiltersRsp_DepartureOrArrivalAirports{
				SettlementId: msk.Id,
				Airports: []*aviaAPI.SearchFiltersRsp_Airport{
					{
						State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
							Enabled: true,
							Value:   false,
						},
						StationId:       svo.Id,
						StationTitle:    svo.Title,
						SettlementTitle: msk.Title,
						AviaCode:        svo.AviaCode,
					},
					{
						State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
							Enabled: true,
							Value:   false,
						},
						StationId:       dme.Id,
						StationTitle:    dme.Title,
						SettlementTitle: msk.Title,
						AviaCode:        dme.AviaCode,
					},
					{
						State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
							Enabled: true,
							Value:   false,
						},
						StationId:       ziaAirport.Id,
						StationTitle:    ziaAirport.Title,
						SettlementTitle: zia.Title,
						AviaCode:        ziaAirport.AviaCode,
					},
				},
				Title: "Вылет из Москвы",
			},
			Arrival: &aviaAPI.SearchFiltersRsp_DepartureOrArrivalAirports{
				SettlementId: ekb.Id,
				Airports: []*aviaAPI.SearchFiltersRsp_Airport{
					{
						State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
							Enabled: true,
							Value:   false,
						},
						StationId:       ekbAirport.Id,
						StationTitle:    ekbAirport.Title,
						SettlementTitle: "",
						AviaCode:        ekbAirport.AviaCode,
					},
				},
				Title: "Прилёт в Екатеринбург",
			},
		},
		Backward: nil,
	}, filterResponse.Airport)
}

func TestAirport_OtherFilterDidNotAffectPresenseOfAirport(t *testing.T) {
	r := f.NewReference()
	ac := r.CreateAviaCompany(50)
	partner := r.CreatePartner("some-partner")

	msk := r.CreateMoscow()
	ekb := r.CreateYekaterinburg()

	dme := r.CreateDomodedovo(msk)
	svo := r.CreateSheremetyevo(msk)
	zea := r.CreateZeaAirport(r.CreateZea())
	ekbAirport := r.CreateKoltsovo(ekb)

	directFromDME := r.CreateFlight(ac, "HZ 2682", dme, ekbAirport, "9:00 - 10:30")
	directToDME := r.CreateFlight(ac, "HZ 2683", ekbAirport, dme, "9:00 - 10:30")
	directDME := f.CreateSnippet("direct-dme", []string{directToDME.Key}, []string{directFromDME.Key}, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner),
	})

	transferFromSVO := r.CreateFlight(ac, "HZ 4482", svo, zea, "9:30 - 10:00")
	transferFromSVO2 := r.CreateFlight(ac, "HZ 4483", zea, ekbAirport, "18:30 - 20:00")
	transferToSVO := r.CreateFlight(ac, "HZ 6682", ekbAirport, zea, "9:30 - 10:00")
	transferToSVO2 := r.CreateFlight(ac, "HZ 6683", zea, svo, "18:30 - 20:00")
	transferSVO := f.CreateSnippet("transfer-svo", []string{transferToSVO.Key, transferToSVO2.Key}, []string{transferFromSVO.Key, transferFromSVO2.Key}, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner),
	})

	snippets := map[string]*aviaSearchProto.Snippet{
		directDME.Key:   directDME,
		transferSVO.Key: transferSVO,
	}

	reference := r.GetProto()

	// Оставляем только рейсы без пересадок
	filters := &aviaAPI.SearchFiltersReq{
		QuickTransfer: true,
		Transfer:      &aviaAPI.SearchFiltersReq_TransferFilter{NoTransfer: true},
		Airport: &aviaAPI.SearchFiltersReq_AirportFilter{
			Forward: &aviaAPI.SearchFiltersReq_DirectionAirports{
				Arrival: &aviaAPI.SearchFiltersReq_DepartureOrArrivalAirports{
					Airports: []*aviaAPI.SearchFiltersReq_Airport{
						{
							State:     true,
							StationId: svo.Id,
						},
						{
							State:     true,
							StationId: dme.Id,
						},
					},
				},
			},
			Backward: &aviaAPI.SearchFiltersReq_DirectionAirports{
				Departure: &aviaAPI.SearchFiltersReq_DepartureOrArrivalAirports{
					Airports: []*aviaAPI.SearchFiltersReq_Airport{
						{
							State:     true,
							StationId: svo.Id,
						},
						{
							State:     true,
							StationId: dme.Id,
						},
					},
				},
			},
		},
	}
	searchContext, _ := searchcommon.ParseQIDToProto("220421-145911-315.travelapp.plane.c213_c54_2022-04-26_2022-04-30_economy_1_0_0_ru.ru")

	snippets, filterResponse := ApplyFilters(context.Background(), &nop.Logger{}, snippets, reference, searchContext, filters)

	snippetKeys := make(map[string]struct{})
	for key := range snippets {
		snippetKeys[key] = struct{}{}
	}
	expectedSnippetKeys := map[string]struct{}{directDME.Key: {}}
	require.True(t, maps.Equal(snippetKeys, expectedSnippetKeys), "unexpected snippets: %v != %v", snippetKeys, expectedSnippetKeys)
	require.NotNil(t, filterResponse)

	// Проверяем, что SVO остался в фильтрах
	forwardDepartureSVO := filterResponse.Airport.Forward.Arrival.Airports[0]
	require.EqualValues(t, svo.Id, forwardDepartureSVO.StationId, forwardDepartureSVO.StationTitle)
	require.False(t, forwardDepartureSVO.State.Enabled)
	require.True(t, forwardDepartureSVO.State.Value)

	forwardDepartureDME := filterResponse.Airport.Forward.Arrival.Airports[1]
	require.EqualValues(t, dme.Id, forwardDepartureDME.StationId, forwardDepartureDME.StationTitle)
	require.True(t, forwardDepartureDME.State.Enabled)
	require.True(t, forwardDepartureDME.State.Value)

	BackwardArrivalSVO := filterResponse.Airport.Backward.Departure.Airports[0]
	require.EqualValues(t, svo.Id, BackwardArrivalSVO.StationId, BackwardArrivalSVO.StationTitle)
	require.False(t, BackwardArrivalSVO.State.Enabled)
	require.True(t, BackwardArrivalSVO.State.Value)

	BackwardArrivalDME := filterResponse.Airport.Backward.Departure.Airports[1]
	require.EqualValues(t, dme.Id, BackwardArrivalDME.StationId, BackwardArrivalDME.StationTitle)
	require.True(t, BackwardArrivalDME.State.Enabled)
	require.True(t, BackwardArrivalDME.State.Value)
}

func TestAirport_InterDependence(t *testing.T) {
	r := f.NewReference()
	ac := r.CreateAviaCompany(50)
	partner := r.CreatePartner("some-partner")

	msk := r.CreateMoscow()
	ekb := r.CreateYekaterinburg()

	dme := r.CreateDomodedovo(msk)
	svo := r.CreateSheremetyevo(msk)
	ekbAirport := r.CreateKoltsovo(ekb)

	dme2ekbFlight := r.CreateFlight(ac, "HZ 2682", dme, ekbAirport, "9:00 - 10:30")
	ekb2dmeFlight := r.CreateFlight(ac, "HZ 2683", ekbAirport, dme, "12:00 - 13:30")
	dme2ekb := f.CreateSnippet("dme2ekb", []string{dme2ekbFlight.Key}, []string{ekb2dmeFlight.Key}, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner),
	})
	svo2ekbFlight := r.CreateFlight(ac, "HZ 5582", svo, ekbAirport, "18:30 - 20:00")
	ekb2svoFlight := r.CreateFlight(ac, "HZ 5582", ekbAirport, svo, "21:30 - 23:00")
	svo2ekb := f.CreateSnippet("svo2ekb", []string{svo2ekbFlight.Key}, []string{ekb2svoFlight.Key}, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner),
	})

	snippets := map[string]*aviaSearchProto.Snippet{
		dme2ekb.Key: dme2ekb,
		svo2ekb.Key: svo2ekb,
	}

	reference := r.GetProto()
	searchContext, _ := searchcommon.ParseQIDToProto("220421-145911-315.travelapp.plane.c213_c54_2022-04-26_2022-04-27_economy_1_0_0_ru.ru")

	// выбираем аэропорт вылета туда dme
	// ожидаем что можно выбрать только аэропорт прилета обратно dme
	filters := &aviaAPI.SearchFiltersReq{
		Airport: &aviaAPI.SearchFiltersReq_AirportFilter{
			Forward: &aviaAPI.SearchFiltersReq_DirectionAirports{
				Departure: &aviaAPI.SearchFiltersReq_DepartureOrArrivalAirports{
					Airports: []*aviaAPI.SearchFiltersReq_Airport{
						{
							State:     true,
							StationId: dme.Id,
						},
					},
				},
			},
		},
	}

	snippets, filterResponse := ApplyFilters(context.Background(), &nop.Logger{}, snippets, reference, searchContext, filters)

	snippetKeys := make(map[string]struct{})
	for key := range snippets {
		snippetKeys[key] = struct{}{}
	}
	expectedSnippetKeys := map[string]struct{}{dme2ekb.Key: {}}
	require.True(t, maps.Equal(snippetKeys, expectedSnippetKeys), "unexpected snippets: %v != %v", snippetKeys, expectedSnippetKeys)
	require.NotNil(t, filterResponse)
	require.EqualValues(t, dme.Id, filterResponse.Airport.Backward.Arrival.Airports[1].StationId)
	require.True(t, filterResponse.Airport.Backward.Arrival.Airports[1].State.Enabled)
	require.EqualValues(t, svo.Id, filterResponse.Airport.Backward.Arrival.Airports[0].StationId)
	require.False(t, filterResponse.Airport.Backward.Arrival.Airports[0].State.Enabled)
}
