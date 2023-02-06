package filtering2

import (
	"context"
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"golang.org/x/exp/maps"

	"a.yandex-team.ru/library/go/core/log/nop"
	aviaAPI "a.yandex-team.ru/travel/app/backend/api/avia/v1"
	commonAPI "a.yandex-team.ru/travel/app/backend/api/common/v1"
	f "a.yandex-team.ru/travel/app/backend/internal/avia/search/proto/factories"
	aviaSearchProto "a.yandex-team.ru/travel/app/backend/internal/avia/search/proto/v1"
	"a.yandex-team.ru/travel/app/backend/internal/avia/search/searchcommon"
)

func TestTransfer(t *testing.T) {
	r := f.NewReference()
	ac := r.CreateAviaCompany(50)
	partner := r.CreatePartner("some-partner")

	moscow := r.CreateMoscow()
	blagoveschinsk := r.CreateBlagoveschinsk()
	zea := r.CreateZea()

	mskAirport := r.CreateSheremetyevo(moscow)
	bgAirport := r.CreateStation(9623271, f.WithSettlement(blagoveschinsk))
	zeaAirport := r.CreateStation(9635355, f.WithSettlement(zea))

	bzMorning := r.CreateFlight(ac, "HZ 2682", bgAirport, zeaAirport, "9:10 - 10:20")
	snippet1 := f.CreateSnippet("snippet-1", []string{bzMorning.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(2897))})

	bzEvening := r.CreateFlight(ac, "HZ 5582", bgAirport, zeaAirport, "19:10 - 20:20")
	snippet2 := f.CreateSnippet("snippet-2", []string{bzEvening.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(3329))})

	bm := r.CreateFlight(ac, "HZ 5050", bgAirport, mskAirport, "9:10 - 10:20")
	mz := r.CreateFlight(ac, "HZ 4242", mskAirport, zeaAirport, "12:10 - 13:20")
	snippet3 := f.CreateSnippet("snippet-3", []string{bm.Key, mz.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(5600))})

	reference := r.GetProto()

	snippets := map[string]*aviaSearchProto.Snippet{
		snippet1.Key: snippet1,
		snippet2.Key: snippet2,
		snippet3.Key: snippet3,
	}

	filters := &aviaAPI.SearchFiltersReq{
		QuickTransfer: false,
		Transfer: &aviaAPI.SearchFiltersReq_TransferFilter{
			NoTransfer:        false,
			OneTransferOrLess: false,
			NoNightTransfer:   true,
			NoAirportChange:   true,
			TransferDuration: &aviaAPI.SearchFiltersReq_TransferDurationInterval{
				MinimumMinutes: 90,
				MaximumMinutes: 140,
			},
			Airports: &aviaAPI.SearchFiltersReq_TransferAirportsFilter{
				Forward: []*aviaAPI.SearchFiltersReq_TransferAirport{
					{
						StationId: mskAirport.Id,
						State:     false,
					},
				},
				Backward: nil,
			},
		},
	}
	searchContext, _ := searchcommon.ParseQIDToProto("220421-145911-315.travelapp.plane.c77_c11379_2022-04-26_None_economy_1_0_0_ru.ru")

	snippets, filterResponse := ApplyFilters(context.Background(), &nop.Logger{}, snippets, reference, searchContext, filters)

	snippetKeys := make(map[string]struct{})
	for key := range snippets {
		snippetKeys[key] = struct{}{}
	}
	expectedSnippetKeys := map[string]struct{}{"snippet-1": {}, "snippet-2": {}, "snippet-3": {}}
	require.True(t, maps.Equal(snippetKeys, expectedSnippetKeys), "unexpected snippets: %v != %v", snippetKeys, expectedSnippetKeys)
	require.NotNil(t, filterResponse)
	require.NotNil(t, filterResponse.Transfer)
	require.NotNil(t, filterResponse.QuickTransfer)
	require.EqualValues(t, &aviaAPI.SearchFiltersRsp_QuickTransferFilter{
		State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
			Enabled: true,
			Value:   false,
		},
		MinPriceNoTransfer: &commonAPI.Price{
			Currency: "RUB",
			Value:    2897,
		},
		MinPriceWithTransfer: &commonAPI.Price{
			Currency: "RUB",
			Value:    5600,
		},
	}, filterResponse.QuickTransfer)
	require.EqualValues(t, &aviaAPI.SearchFiltersRsp_TransferFilter{
		NoTransfer: &aviaAPI.SearchFiltersRsp_BoolFilterState{
			Enabled: true,
			Value:   false,
		},
		OneTransferOrLess: &aviaAPI.SearchFiltersRsp_BoolFilterState{
			Enabled: true,
			Value:   false,
		},
		NoNightTransfer: &aviaAPI.SearchFiltersRsp_BoolFilterState{
			Enabled: true,
			Value:   true,
		},
		NoAirportChange: &aviaAPI.SearchFiltersRsp_BoolFilterState{
			Enabled: true,
			Value:   true,
		},
		TransferDuration: &aviaAPI.SearchFiltersRsp_TransferDurationFilter{
			All: &aviaAPI.SearchFiltersRsp_TransferDurationInterval{
				MinimumMinutes: 110,
				MaximumMinutes: 110,
			},
			Selected: &aviaAPI.SearchFiltersRsp_TransferDurationInterval{
				MinimumMinutes: 110,
				MaximumMinutes: 110,
			},
		},
		Airports: &aviaAPI.SearchFiltersRsp_TransferAirportsFilter{
			Forward: []*aviaAPI.SearchFiltersRsp_TransferAirport{
				{
					StationId: mskAirport.Id,
					State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
						Enabled: true,
						Value:   false,
					},
					StationTitle:    mskAirport.Title,
					SettlementTitle: moscow.Title,
					AviaCode:        mskAirport.AviaCode,
				},
			},
			Backward: nil,
		},
	}, filterResponse.Transfer)
}

func TestTransferSeveralAirports(t *testing.T) {
	r := f.NewReference()
	ac := r.CreateAviaCompany(50)
	partner := r.CreatePartner("some-partner")

	moscow := r.CreateMoscow()
	yekaterinburg := r.CreateYekaterinburg()
	blagoveschinsk := r.CreateBlagoveschinsk()
	zea := r.CreateZea()

	dme := r.CreateDomodedovo(moscow)
	svx := r.CreateKoltsovo(yekaterinburg)
	bgAirport := r.CreateBlagoveschinskAirport(blagoveschinsk)
	zeaAirport := r.CreateZeaAirport(zea)

	directMorningFlight := r.CreateFlight(ac, "HZ 2682", bgAirport, zeaAirport, "9:10 - 10:20")
	directMorning := f.CreateSnippet("direct-morning", []string{directMorningFlight.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(2897))})

	directEveningFlight := r.CreateFlight(ac, "HZ 5582", bgAirport, zeaAirport, "19:10 - 20:20")
	directEvening := f.CreateSnippet("direct-evening", []string{directEveningFlight.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(3329))})

	b2dme := r.CreateFlight(ac, "HZ 5050", bgAirport, dme, "9:10 - 10:20")
	dme2z := r.CreateFlight(ac, "HZ 4242", dme, zeaAirport, "12:10 - 13:20")
	dmeTransfer := f.CreateSnippet("dme-transfer", []string{b2dme.Key, dme2z.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(5600))})

	b2ekb := r.CreateFlight(ac, "HZ 5051", bgAirport, svx, "9:10 - 10:20")
	ekb2z := r.CreateFlight(ac, "HZ 4243", svx, zeaAirport, "12:10 - 13:20")
	ekbTransfer := f.CreateSnippet("ekb-transfer", []string{b2ekb.Key, ekb2z.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(5600))})

	reference := r.GetProto()

	snippets := map[string]*aviaSearchProto.Snippet{
		directMorning.Key: directMorning,
		directEvening.Key: directEvening,
		dmeTransfer.Key:   dmeTransfer,
		ekbTransfer.Key:   ekbTransfer,
	}

	filters := &aviaAPI.SearchFiltersReq{
		QuickTransfer: false,
		Transfer: &aviaAPI.SearchFiltersReq_TransferFilter{
			Airports: &aviaAPI.SearchFiltersReq_TransferAirportsFilter{
				Forward: []*aviaAPI.SearchFiltersReq_TransferAirport{
					{
						StationId: svx.Id,
						State:     false,
					},
				},
				Backward: nil,
			},
		},
	}
	searchContext, _ := searchcommon.ParseQIDToProto("220421-145911-315.travelapp.plane.c77_c11379_2022-04-26_None_economy_1_0_0_ru.ru")

	snippets, filterResponse := ApplyFilters(context.Background(), &nop.Logger{}, snippets, reference, searchContext, filters)

	snippetKeys := make(map[string]struct{})
	for key := range snippets {
		snippetKeys[key] = struct{}{}
	}
	expectedSnippetKeys := map[string]struct{}{directMorning.Key: {}, directEvening.Key: {}, ekbTransfer.Key: {}, dmeTransfer.Key: {}}
	require.True(t, maps.Equal(snippetKeys, expectedSnippetKeys), "unexpected snippets: %v != %v", snippetKeys, expectedSnippetKeys)
	require.NotNil(t, filterResponse)
	require.NotNil(t, filterResponse.Transfer)
	require.NotNil(t, filterResponse.QuickTransfer)
	require.EqualValues(t, &aviaAPI.SearchFiltersRsp_TransferAirportsFilter{
		Forward: []*aviaAPI.SearchFiltersRsp_TransferAirport{
			{
				StationId: dme.Id,
				State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
					Enabled: true,
					Value:   false,
				},
				StationTitle:    dme.Title,
				SettlementTitle: moscow.Title,
				AviaCode:        dme.AviaCode,
			},
			{
				StationId: svx.Id,
				State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
					Enabled: true,
					Value:   false,
				},
				StationTitle:    svx.Title,
				SettlementTitle: yekaterinburg.Title,
				AviaCode:        svx.AviaCode,
			},
		},
		Backward: nil,
	}, filterResponse.Transfer.Airports)
}

func TestTransfer_NoTransfer(t *testing.T) {
	r := f.NewReference()
	ac := r.CreateAviaCompany(50)
	partner := r.CreatePartner("some-partner")

	moscow := r.CreateMoscow()
	blagoveschinsk := r.CreateBlagoveschinsk()
	zea := r.CreateZea()

	mskAirport := r.CreateDomodedovo(moscow)
	bgAirport := r.CreateBlagoveschinskAirport(blagoveschinsk)
	zeaAirport := r.CreateZeaAirport(zea)

	directMorningFlight := r.CreateFlight(ac, "HZ 2682", bgAirport, zeaAirport, "9:10 - 10:20")
	directMorning := f.CreateSnippet("direct-morning", []string{directMorningFlight.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(2897))})

	directEveningFlight := r.CreateFlight(ac, "HZ 5582", bgAirport, zeaAirport, "19:10 - 20:20")
	directEvening := f.CreateSnippet("direct-evening", []string{directEveningFlight.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(3329))})

	b2m := r.CreateFlight(ac, "HZ 5050", bgAirport, mskAirport, "9:10 - 10:20")
	m2z := r.CreateFlight(ac, "HZ 4242", mskAirport, zeaAirport, "12:10 - 13:20")
	transfer := f.CreateSnippet("transfer", []string{b2m.Key, m2z.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(5600))})

	reference := r.GetProto()

	snippets := map[string]*aviaSearchProto.Snippet{
		directMorning.Key: directMorning,
		directEvening.Key: directEvening,
		transfer.Key:      transfer,
	}

	filters := &aviaAPI.SearchFiltersReq{
		QuickTransfer: true,
		Transfer: &aviaAPI.SearchFiltersReq_TransferFilter{
			NoTransfer: true,
		},
	}
	searchContext, _ := searchcommon.ParseQIDToProto("220421-145911-315.travelapp.plane.c77_c11379_2022-04-26_None_economy_1_0_0_ru.ru")

	snippets, filterResponse := ApplyFilters(context.Background(), &nop.Logger{}, snippets, reference, searchContext, filters)

	snippetKeys := make(map[string]struct{})
	for key := range snippets {
		snippetKeys[key] = struct{}{}
	}
	expectedSnippetKeys := map[string]struct{}{directMorning.Key: {}, directEvening.Key: {}}
	require.True(t, maps.Equal(snippetKeys, expectedSnippetKeys), "unexpected snippets: %v != %v", snippetKeys, expectedSnippetKeys)
	require.NotNil(t, filterResponse)
	require.NotNil(t, filterResponse.Transfer)
	require.NotNil(t, filterResponse.QuickTransfer)
	require.EqualValues(t, &aviaAPI.SearchFiltersRsp_QuickTransferFilter{
		State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
			Enabled: true,
			Value:   true,
		},
		MinPriceNoTransfer: &commonAPI.Price{
			Currency: "RUB",
			Value:    2897,
		},
		MinPriceWithTransfer: &commonAPI.Price{
			Currency: "RUB",
			Value:    5600,
		},
	}, filterResponse.QuickTransfer)
	require.EqualValues(t, &aviaAPI.SearchFiltersRsp_BoolFilterState{
		Enabled: true,
		Value:   true,
	}, filterResponse.Transfer.NoTransfer)
}

func TestQuickTransferPrice_DependingOnBaggageFilter(t *testing.T) {
	r := f.NewReference()
	ac := r.CreateAviaCompany(50)
	partner := r.CreatePartner("some-partner")

	moscow := r.CreateSettlement(213)
	blagoveschinsk := r.CreateSettlement(27)
	zea := r.CreateSettlement(11379)

	mskAirport := r.CreateStation(9600213, f.WithSettlement(moscow))
	bgAirport := r.CreateStation(9623271, f.WithSettlement(blagoveschinsk))
	zeaAirport := r.CreateStation(9635355, f.WithSettlement(zea))

	bzMorning := r.CreateFlight(ac, "HZ 2682", bgAirport, zeaAirport, "9:10 - 10:20")
	snippet1 := f.CreateSnippet("snippet-1", []string{bzMorning.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"no-baggage": f.CreateVariant("no-baggage", partner, f.WithPriceRUB(2000)),
		"baggage":    f.CreateVariant("baggage", partner, f.WithPriceRUB(3000), f.WithBaggage(21, 1)),
	})

	bzEvening := r.CreateFlight(ac, "HZ 5582", bgAirport, zeaAirport, "19:10 - 20:20")
	snippet2 := f.CreateSnippet("snippet-2", []string{bzEvening.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"no-baggage": f.CreateVariant("no-baggage", partner, f.WithPriceRUB(2500)),
		"baggage":    f.CreateVariant("baggage", partner, f.WithPriceRUB(3500), f.WithBaggage(21, 1)),
	})

	// Пересадки нужны только для того, чтобы быстрый фильтр пересадок отображался
	bm := r.CreateFlight(ac, "HZ 5050", bgAirport, mskAirport, "9:10 - 10:20")
	mz := r.CreateFlight(ac, "HZ 4242", mskAirport, zeaAirport, "12:10 - 13:20")
	snippet3 := f.CreateSnippet("snippet-3", []string{bm.Key, mz.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"no-baggage": f.CreateVariant("no-baggage", partner, f.WithPriceRUB(5600)),
		"baggage":    f.CreateVariant("baggage", partner, f.WithPriceRUB(7700), f.WithBaggage(21, 1)),
	})

	reference := r.GetProto()

	t.Run("Test price with no baggage", func(t *testing.T) {
		snippets := map[string]*aviaSearchProto.Snippet{
			snippet1.Key: snippet1,
			snippet2.Key: snippet2,
			snippet3.Key: snippet3,
		}

		filters := &aviaAPI.SearchFiltersReq{
			QuickTransfer: false,
			QuickBaggage:  false,
			Transfer: &aviaAPI.SearchFiltersReq_TransferFilter{
				NoTransfer: false,
			},
		}
		searchContext, _ := searchcommon.ParseQIDToProto("220421-145911-315.travelapp.plane.c77_c11379_2022-04-26_None_economy_1_0_0_ru.ru")

		snippets, filterResponse := ApplyFilters(context.Background(), &nop.Logger{}, snippets, reference, searchContext, filters)

		snippetKeys := make(map[string]struct{})
		for key := range snippets {
			snippetKeys[key] = struct{}{}
		}
		expectedSnippetKeys := map[string]struct{}{"snippet-1": {}, "snippet-2": {}, "snippet-3": {}}
		require.True(t, maps.Equal(snippetKeys, expectedSnippetKeys), "unexpected snippets: %v != %v", snippetKeys, expectedSnippetKeys)
		require.EqualValues(t, 2000, filterResponse.QuickTransfer.MinPriceNoTransfer.Value)
	})

	t.Run("Test price with baggage", func(t *testing.T) {
		snippets := map[string]*aviaSearchProto.Snippet{
			snippet1.Key: snippet1,
			snippet2.Key: snippet2,
			snippet3.Key: snippet3,
		}

		filters := &aviaAPI.SearchFiltersReq{
			QuickTransfer: false,
			QuickBaggage:  true,
			Transfer: &aviaAPI.SearchFiltersReq_TransferFilter{
				NoTransfer: false,
			},
		}
		searchContext, _ := searchcommon.ParseQIDToProto("220421-145911-315.travelapp.plane.c77_c11379_2022-04-26_None_economy_1_0_0_ru.ru")

		snippets, filterResponse := ApplyFilters(context.Background(), &nop.Logger{}, snippets, reference, searchContext, filters)

		snippetKeys := make(map[string]struct{})
		for key := range snippets {
			snippetKeys[key] = struct{}{}
		}
		expectedSnippetKeys := map[string]struct{}{"snippet-1": {}, "snippet-2": {}, "snippet-3": {}}
		require.True(t, maps.Equal(snippetKeys, expectedSnippetKeys), "unexpected snippets: %v != %v", snippetKeys, expectedSnippetKeys)
		require.EqualValues(t, 3000, filterResponse.QuickTransfer.MinPriceNoTransfer.Value)
	})
}

func TestTransfer_TransferOnly_On(t *testing.T) {
	r := f.NewReference()
	ac := r.CreateAviaCompany(50)
	partner := r.CreatePartner("some-partner")

	mskAirport := r.CreateStation(9600213)
	bgAirport := r.CreateStation(9623271)
	zeaAirport := r.CreateStation(9635355)

	bm := r.CreateFlight(ac, "HZ 5050", bgAirport, mskAirport, "9:10 - 10:20")
	mz := r.CreateFlight(ac, "HZ 4242", mskAirport, zeaAirport, "12:10 - 13:20")
	snippet := f.CreateSnippet("snippet", []string{bm.Key, mz.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(5600))})

	reference := r.GetProto()
	searchContext, _ := searchcommon.ParseQIDToProto("220421-145911-315.travelapp.plane.c77_c11379_2022-04-26_None_economy_1_0_0_ru.ru")
	filters := &aviaAPI.SearchFiltersReq{
		QuickTransfer: true,
		Transfer: &aviaAPI.SearchFiltersReq_TransferFilter{
			NoTransfer: true,
		},
	}
	snippets := map[string]*aviaSearchProto.Snippet{snippet.Key: snippet}

	snippets, filterResponse := ApplyFilters(context.Background(), &nop.Logger{}, snippets, reference, searchContext, filters)

	snippetKeys := make(map[string]struct{})
	for key := range snippets {
		snippetKeys[key] = struct{}{}
	}
	expectedSnippetKeys := map[string]struct{}{}
	require.True(t, maps.Equal(snippetKeys, expectedSnippetKeys), "unexpected snippets: %v != %v", snippetKeys, expectedSnippetKeys)
	require.NotNil(t, filterResponse)
	require.NotNil(t, filterResponse.Transfer)
	require.NotNil(t, filterResponse.QuickTransfer)
	assert.EqualValues(t, &aviaAPI.SearchFiltersRsp_QuickTransferFilter{
		State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
			Enabled: false,
			Value:   true,
		},
		MinPriceNoTransfer: nil,
		MinPriceWithTransfer: &commonAPI.Price{
			Currency: "RUB",
			Value:    5600,
		},
	}, filterResponse.QuickTransfer)
	require.EqualValues(t, &aviaAPI.SearchFiltersRsp_BoolFilterState{
		Enabled: false,
		Value:   true,
	}, filterResponse.Transfer.NoTransfer)
}

func TestTransfer_TransferOnly_Off(t *testing.T) {
	r := f.NewReference()
	ac := r.CreateAviaCompany(50)
	partner := r.CreatePartner("some-partner")

	mskAirport := r.CreateStation(9600213)
	bgAirport := r.CreateStation(9623271)
	zeaAirport := r.CreateStation(9635355)

	bm := r.CreateFlight(ac, "HZ 5050", bgAirport, mskAirport, "9:10 - 10:20")
	mz := r.CreateFlight(ac, "HZ 4242", mskAirport, zeaAirport, "12:10 - 13:20")
	snippet := f.CreateSnippet("snippet", []string{bm.Key, mz.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(5600))})

	reference := r.GetProto()
	searchContext, _ := searchcommon.ParseQIDToProto("220421-145911-315.travelapp.plane.c77_c11379_2022-04-26_None_economy_1_0_0_ru.ru")

	filters := &aviaAPI.SearchFiltersReq{
		QuickTransfer: false,
		Transfer: &aviaAPI.SearchFiltersReq_TransferFilter{
			NoTransfer: false,
		},
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
	require.NotNil(t, filterResponse.Transfer)
	require.NotNil(t, filterResponse.QuickTransfer)
	assert.EqualValues(t, &aviaAPI.SearchFiltersRsp_QuickTransferFilter{
		State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
			Enabled: false,
			Value:   false,
		},
		MinPriceNoTransfer: nil,
		MinPriceWithTransfer: &commonAPI.Price{
			Currency: "RUB",
			Value:    5600,
		},
	}, filterResponse.QuickTransfer)
	require.EqualValues(t, &aviaAPI.SearchFiltersRsp_BoolFilterState{
		Enabled: false,
		Value:   false,
	}, filterResponse.Transfer.NoTransfer)
}

func TestTransfer_EmptyResult(t *testing.T) {
	r := f.NewReference()
	reference := r.GetProto()
	snippets := map[string]*aviaSearchProto.Snippet{}
	filters := &aviaAPI.SearchFiltersReq{}
	searchContext, _ := searchcommon.ParseQIDToProto("220421-145911-315.travelapp.plane.c77_c11379_2022-04-26_None_economy_1_0_0_ru.ru")

	snippets, filterResponse := ApplyFilters(context.Background(), &nop.Logger{}, snippets, reference, searchContext, filters)

	snippetKeys := make(map[string]struct{})
	for key := range snippets {
		snippetKeys[key] = struct{}{}
	}
	expectedSnippetKeys := map[string]struct{}{}
	require.True(t, maps.Equal(snippetKeys, expectedSnippetKeys), "unexpected snippets: %v != %v", snippetKeys, expectedSnippetKeys)
	require.NotNil(t, filterResponse)
	require.NotNil(t, filterResponse.Transfer)
	require.NotNil(t, filterResponse.QuickTransfer)
	require.EqualValues(t, &aviaAPI.SearchFiltersRsp_QuickTransferFilter{
		State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
			Enabled: false,
			Value:   false,
		},
		MinPriceNoTransfer:   nil,
		MinPriceWithTransfer: nil,
	}, filterResponse.QuickTransfer)
	require.EqualValues(t, &aviaAPI.SearchFiltersRsp_TransferFilter{
		NoTransfer: &aviaAPI.SearchFiltersRsp_BoolFilterState{
			Enabled: false,
			Value:   false,
		},
		OneTransferOrLess: &aviaAPI.SearchFiltersRsp_BoolFilterState{
			Enabled: false,
			Value:   false,
		},
		NoNightTransfer: &aviaAPI.SearchFiltersRsp_BoolFilterState{
			Enabled: false,
			Value:   false,
		},
		NoAirportChange: &aviaAPI.SearchFiltersRsp_BoolFilterState{
			Enabled: false,
			Value:   false,
		},
		TransferDuration: nil,
		Airports: &aviaAPI.SearchFiltersRsp_TransferAirportsFilter{
			Forward:  nil,
			Backward: nil,
		},
	}, filterResponse.Transfer)
}

func TestTransfer_DirectOnly(t *testing.T) {
	r := f.NewReference()
	ac := r.CreateAviaCompany(50)
	partner := r.CreatePartner("some-partner")

	bgAirport := r.CreateStation(9623271)
	zeaAirport := r.CreateStation(9635355)

	bz := r.CreateFlight(ac, "HZ 2682", bgAirport, zeaAirport, "9:10 - 10:20")
	snippet := f.CreateSnippet("snippet", []string{bz.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(2000))})

	reference := r.GetProto()
	searchContext, _ := searchcommon.ParseQIDToProto("220421-145911-315.travelapp.plane.c77_c11379_2022-04-26_None_economy_1_0_0_ru.ru")

	for _, initialFilterState := range []bool{true, false} {
		t.Run(fmt.Sprintf("TestTransfer_DirectOnly - initialFilterState: %t", initialFilterState), func(t *testing.T) {
			filters := &aviaAPI.SearchFiltersReq{
				QuickTransfer: initialFilterState,
				Transfer: &aviaAPI.SearchFiltersReq_TransferFilter{
					NoTransfer:        initialFilterState,
					OneTransferOrLess: false,
					NoNightTransfer:   false,
					NoAirportChange:   false,
					TransferDuration:  nil,
					Airports:          nil,
				},
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
			require.NotNil(t, filterResponse.Transfer)
			require.NotNil(t, filterResponse.QuickTransfer)
			assert.EqualValues(t, &aviaAPI.SearchFiltersRsp_QuickTransferFilter{
				State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
					Enabled: true,
					Value:   initialFilterState,
				},
				MinPriceNoTransfer: &commonAPI.Price{
					Currency: "RUB",
					Value:    2000,
				},
				MinPriceWithTransfer: nil,
			}, filterResponse.QuickTransfer)
			require.EqualValues(t, &aviaAPI.SearchFiltersRsp_TransferFilter{
				NoTransfer: &aviaAPI.SearchFiltersRsp_BoolFilterState{
					Enabled: true,
					Value:   initialFilterState,
				},
				OneTransferOrLess: &aviaAPI.SearchFiltersRsp_BoolFilterState{
					Enabled: true,
					Value:   false,
				},
				NoNightTransfer: &aviaAPI.SearchFiltersRsp_BoolFilterState{
					Enabled: true,
					Value:   false,
				},
				NoAirportChange: &aviaAPI.SearchFiltersRsp_BoolFilterState{
					Enabled: true,
					Value:   false,
				},
				TransferDuration: nil,
				Airports: &aviaAPI.SearchFiltersRsp_TransferAirportsFilter{
					Forward:  nil,
					Backward: nil,
				},
			}, filterResponse.Transfer)
		})
	}
}

func TestTransfer_MoreThanOneTransfer(t *testing.T) {
	r := f.NewReference()
	ac := r.CreateAviaCompany(50)
	partner := r.CreatePartner("some-partner")

	msk := r.CreateMoscow()
	mskAirport := r.CreateDomodedovo(msk)
	ekb := r.CreateYekaterinburg()
	ekbAirport := r.CreateKoltsovo(ekb)
	bgAirport := r.CreateBlagoveschinskAirport(r.CreateBlagoveschinsk())
	zeaAirport := r.CreateZeaAirport(r.CreateZea())

	bz := r.CreateFlight(ac, "HZ 2682", bgAirport, zeaAirport, "9:10 - 10:20")
	direct := f.CreateSnippet("direct", []string{bz.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(2897))})

	bm := r.CreateFlight(ac, "HZ 5050", bgAirport, mskAirport, "9:10 - 10:20")
	mz := r.CreateFlight(ac, "HZ 4242", mskAirport, zeaAirport, "12:10 - 13:20")
	oneTransfer := f.CreateSnippet("one-transfer", []string{bm.Key, mz.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(5600))})

	bm2 := r.CreateFlight(ac, "HZ 2222", bgAirport, mskAirport, "9:20 - 10:30")
	me := r.CreateFlight(ac, "HZ 2233", mskAirport, ekbAirport, "12:00 - 14:10")
	ez := r.CreateFlight(ac, "HZ 2244", ekbAirport, zeaAirport, "16:10 - 19:20")
	twoTransfers := f.CreateSnippet("two-transfers", []string{bm2.Key, me.Key, ez.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(8000))})

	reference := r.GetProto()

	snippets := map[string]*aviaSearchProto.Snippet{
		direct.Key:       direct,
		oneTransfer.Key:  oneTransfer,
		twoTransfers.Key: twoTransfers,
	}

	filters := &aviaAPI.SearchFiltersReq{
		Transfer: &aviaAPI.SearchFiltersReq_TransferFilter{
			OneTransferOrLess: true,
		},
	}
	searchContext, _ := searchcommon.ParseQIDToProto("220421-145911-315.travelapp.plane.c77_c11379_2022-04-26_None_economy_1_0_0_ru.ru")

	snippets, filterResponse := ApplyFilters(context.Background(), &nop.Logger{}, snippets, reference, searchContext, filters)

	snippetKeys := make(map[string]struct{})
	for key := range snippets {
		snippetKeys[key] = struct{}{}
	}
	expectedSnippetKeys := map[string]struct{}{"direct": {}, "one-transfer": {}}
	require.True(t, maps.Equal(snippetKeys, expectedSnippetKeys), "unexpected snippets: %v != %v", snippetKeys, expectedSnippetKeys)
	require.NotNil(t, filterResponse)
	require.NotNil(t, filterResponse.Transfer)
	require.NotNil(t, filterResponse.QuickTransfer)
	require.EqualValues(t, &aviaAPI.SearchFiltersRsp_BoolFilterState{
		Enabled: true,
		Value:   true,
	}, filterResponse.Transfer.OneTransferOrLess)
	require.EqualValues(t, &aviaAPI.SearchFiltersRsp_TransferAirportsFilter{
		Forward: []*aviaAPI.SearchFiltersRsp_TransferAirport{
			{
				StationId: mskAirport.Id,
				State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
					Enabled: true,
					Value:   false,
				},
				StationTitle:    mskAirport.Title,
				SettlementTitle: msk.Title,
				AviaCode:        mskAirport.AviaCode,
			},
			{
				StationId: ekbAirport.Id,
				State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
					Enabled: false,
					Value:   false,
				},
				StationTitle:    ekbAirport.Title,
				SettlementTitle: ekb.Title,
				AviaCode:        ekbAirport.AviaCode,
			},
		},
		Backward: nil,
	}, filterResponse.Transfer.Airports)
}

func TestTransfer_TransferDuration(t *testing.T) {
	r := f.NewReference()
	ac := r.CreateAviaCompany(50)
	partner := r.CreatePartner("some-partner")

	mskAirport := r.CreateStation(9600213)
	bgAirport := r.CreateStation(9623271)
	zeaAirport := r.CreateStation(9635355)

	bz := r.CreateFlight(ac, "HZ 2682", bgAirport, zeaAirport, "9:10 - 10:20")
	direct := f.CreateSnippet("direct", []string{bz.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(2000))})

	bm := r.CreateFlight(ac, "HZ 5050", bgAirport, mskAirport, "9:10 - 10:20")
	mz := r.CreateFlight(ac, "HZ 4242", mskAirport, zeaAirport, "12:10 - 13:20")
	shortTransfer := f.CreateSnippet("short-transfer", []string{bm.Key, mz.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(5600))})

	mz2 := r.CreateFlight(ac, "HZ 7777", mskAirport, zeaAirport, "21:10 - 22:20")
	longTransfer := f.CreateSnippet("long-transfer", []string{bm.Key, mz2.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(4000))})

	reference := r.GetProto()

	snippets := map[string]*aviaSearchProto.Snippet{
		direct.Key:        direct,
		shortTransfer.Key: shortTransfer,
		longTransfer.Key:  longTransfer,
	}

	filters := &aviaAPI.SearchFiltersReq{
		Transfer: &aviaAPI.SearchFiltersReq_TransferFilter{
			TransferDuration: &aviaAPI.SearchFiltersReq_TransferDurationInterval{
				MinimumMinutes: 90,
				MaximumMinutes: 180,
			},
		},
	}
	searchContext, _ := searchcommon.ParseQIDToProto("220421-145911-315.travelapp.plane.c77_c11379_2022-04-26_None_economy_1_0_0_ru.ru")

	snippets, filterResponse := ApplyFilters(context.Background(), &nop.Logger{}, snippets, reference, searchContext, filters)

	snippetKeys := make(map[string]struct{})
	for key := range snippets {
		snippetKeys[key] = struct{}{}
	}
	expectedSnippetKeys := map[string]struct{}{"direct": {}, "short-transfer": {}}
	require.True(t, maps.Equal(snippetKeys, expectedSnippetKeys), "unexpected snippets: %v != %v", snippetKeys, expectedSnippetKeys)
	require.NotNil(t, filterResponse)
	require.NotNil(t, filterResponse.Transfer)
	require.NotNil(t, filterResponse.QuickTransfer)
	require.EqualValues(t, &aviaAPI.SearchFiltersRsp_TransferDurationFilter{
		All: &aviaAPI.SearchFiltersRsp_TransferDurationInterval{
			MinimumMinutes: 110,
			MaximumMinutes: 650,
		},
		Selected: &aviaAPI.SearchFiltersRsp_TransferDurationInterval{
			MinimumMinutes: 110,
			MaximumMinutes: 180,
		},
	}, filterResponse.Transfer.TransferDuration)
}

func TestTransfer_AirportsTitle(t *testing.T) {
	r := f.NewReference()
	ac := r.CreateAviaCompany(50)
	partner := r.CreatePartner("some-partner")

	msk := r.CreateMoscow()
	bg := r.CreateBlagoveschinsk()

	svx := r.CreateSheremetyevo(r.CreateYekaterinburg())
	dme := r.CreateDomodedovo(msk)
	bgAirport := r.CreateBlagoveschinskAirport(bg)
	zea := r.CreateZeaAirport(r.CreateZea())

	svx2dme := r.CreateFlight(ac, "HZ 5050", svx, dme, "9:10 - 10:20")
	dme2zea := r.CreateFlight(ac, "HZ 4242", dme, zea, "12:10 - 13:20")
	dmeTransfer := f.CreateSnippet("dme-transfer", []string{svx2dme.Key, dme2zea.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(5600))})

	svx2bg := r.CreateFlight(ac, "HZ 5050", svx, bgAirport, "9:10 - 10:20")
	bg2zea := r.CreateFlight(ac, "HZ 4242", bgAirport, zea, "12:10 - 13:20")
	bgTransfer := f.CreateSnippet("bg-transfer", []string{svx2bg.Key, bg2zea.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(4000))})

	reference := r.GetProto()

	snippets := map[string]*aviaSearchProto.Snippet{
		bgTransfer.Key:  bgTransfer,
		dmeTransfer.Key: dmeTransfer,
	}

	filters := &aviaAPI.SearchFiltersReq{}
	searchContext, _ := searchcommon.ParseQIDToProto("220421-145911-315.travelapp.plane.c77_c11379_2022-04-26_None_economy_1_0_0_ru.ru")

	snippets, filterResponse := ApplyFilters(context.Background(), &nop.Logger{}, snippets, reference, searchContext, filters)

	snippetKeys := make(map[string]struct{})
	for key := range snippets {
		snippetKeys[key] = struct{}{}
	}
	expectedSnippetKeys := map[string]struct{}{dmeTransfer.Key: {}, bgTransfer.Key: {}}
	require.True(t, maps.Equal(snippetKeys, expectedSnippetKeys), "unexpected snippets: %v != %v", snippetKeys, expectedSnippetKeys)
	require.NotNil(t, filterResponse)
	require.NotNil(t, filterResponse.Transfer)
	require.NotNil(t, filterResponse.QuickTransfer)
	require.EqualValues(t, &aviaAPI.SearchFiltersRsp_TransferAirportsFilter{
		Forward: []*aviaAPI.SearchFiltersRsp_TransferAirport{
			{
				StationId: dme.Id,
				State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
					Enabled: true,
					Value:   false,
				},
				StationTitle:    dme.Title,
				SettlementTitle: msk.Title,
				AviaCode:        dme.AviaCode,
			},
			{
				StationId: bgAirport.Id,
				State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
					Enabled: true,
					Value:   false,
				},
				StationTitle:    bgAirport.Title,
				SettlementTitle: "", // because bgAirport.Title == bg.Title
				AviaCode:        bgAirport.AviaCode,
			},
		},
		Backward: nil,
	}, filterResponse.Transfer.Airports)
}

func TestTransfer_AirportsForwardDifferFromAirportsBackward(t *testing.T) {
	r := f.NewReference()
	ac := r.CreateAviaCompany(50)
	partner := r.CreatePartner("some-partner")

	msk := r.CreateMoscow()
	bg := r.CreateBlagoveschinsk()

	svx := r.CreateKoltsovo(r.CreateYekaterinburg())
	dme := r.CreateDomodedovo(msk)
	bgAirport := r.CreateBlagoveschinskAirport(bg)
	zea := r.CreateZeaAirport(r.CreateZea())

	svx2dme := r.CreateFlight(ac, "HZ 5555", svx, dme, "9:10 - 10:20")
	dme2zea := r.CreateFlight(ac, "HZ 6666", dme, zea, "12:10 - 13:20")
	zea2bg := r.CreateFlight(ac, "HZ 7777", zea, bgAirport, "9:10 - 10:20")
	bg2svx := r.CreateFlight(ac, "HZ 8888", bgAirport, svx, "12:10 - 13:20")

	roundTrip := f.CreateSnippet("round-trip", []string{svx2dme.Key, dme2zea.Key}, []string{zea2bg.Key, bg2svx.Key}, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(4000))})

	reference := r.GetProto()

	snippets := map[string]*aviaSearchProto.Snippet{
		roundTrip.Key: roundTrip,
	}

	filters := &aviaAPI.SearchFiltersReq{}
	searchContext, _ := searchcommon.ParseQIDToProto("220421-145911-315.travelapp.plane.c77_c11379_2022-04-26_2022-05-02_economy_1_0_0_ru.ru")

	snippets, filterResponse := ApplyFilters(context.Background(), &nop.Logger{}, snippets, reference, searchContext, filters)

	snippetKeys := make(map[string]struct{})
	for key := range snippets {
		snippetKeys[key] = struct{}{}
	}
	expectedSnippetKeys := map[string]struct{}{roundTrip.Key: {}}
	require.True(t, maps.Equal(snippetKeys, expectedSnippetKeys), "unexpected snippets: %v != %v", snippetKeys, expectedSnippetKeys)
	require.EqualValues(t, &aviaAPI.SearchFiltersRsp_TransferAirportsFilter{
		Forward: []*aviaAPI.SearchFiltersRsp_TransferAirport{
			{
				StationId: dme.Id,
				State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
					Enabled: true,
					Value:   false,
				},
				StationTitle:    dme.Title,
				SettlementTitle: msk.Title,
				AviaCode:        dme.AviaCode,
			},
		},
		Backward: []*aviaAPI.SearchFiltersRsp_TransferAirport{
			{
				StationId: bgAirport.Id,
				State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
					Enabled: true,
					Value:   false,
				},
				StationTitle:    bgAirport.Title,
				SettlementTitle: "", // because bgAirport.Title == bg.Title
				AviaCode:        bgAirport.AviaCode,
			},
		},
	}, filterResponse.Transfer.Airports)
}

func TestTransfer_OneOrLess_OnlyMoreThanOneTransfer(t *testing.T) {
	r := f.NewReference()
	ac := r.CreateAviaCompany(50)
	partner := r.CreatePartner("some-partner")

	msk := r.CreateMoscow()
	bg := r.CreateBlagoveschinsk()

	svx := r.CreateKoltsovo(r.CreateYekaterinburg())
	dme := r.CreateDomodedovo(msk)
	bgAirport := r.CreateBlagoveschinskAirport(bg)
	zea := r.CreateZeaAirport(r.CreateZea())

	svx2dme := r.CreateFlight(ac, "HZ 5555", svx, dme, "9:10 - 10:20")
	dme2zea := r.CreateFlight(ac, "HZ 6666", dme, zea, "12:10 - 13:20")
	zea2bg := r.CreateFlight(ac, "HZ 7777", zea, bgAirport, "9:10 - 10:20")

	twoTransfers := f.CreateSnippet("two-transfers", []string{svx2dme.Key, dme2zea.Key, zea2bg.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(4000))})

	reference := r.GetProto()

	snippets := map[string]*aviaSearchProto.Snippet{
		twoTransfers.Key: twoTransfers,
	}

	filters := &aviaAPI.SearchFiltersReq{}
	searchContext, _ := searchcommon.ParseQIDToProto("220421-145911-315.travelapp.plane.c213_c77_2022-04-26_None_economy_1_0_0_ru.ru")

	snippets, filterResponse := ApplyFilters(context.Background(), &nop.Logger{}, snippets, reference, searchContext, filters)

	snippetKeys := make(map[string]struct{})
	for key := range snippets {
		snippetKeys[key] = struct{}{}
	}
	expectedSnippetKeys := map[string]struct{}{twoTransfers.Key: {}}
	require.True(t, maps.Equal(snippetKeys, expectedSnippetKeys), "unexpected snippets: %v != %v", snippetKeys, expectedSnippetKeys)
	require.Equal(t, false, filterResponse.Transfer.OneTransferOrLess.Enabled)
}

func TestTransfer_OneOrLess_OnlyOneOrLessTransfer(t *testing.T) {
	r := f.NewReference()
	ac := r.CreateAviaCompany(50)
	partner := r.CreatePartner("some-partner")

	msk := r.CreateMoscow()
	bg := r.CreateBlagoveschinsk()

	svx := r.CreateKoltsovo(r.CreateYekaterinburg())
	dme := r.CreateDomodedovo(msk)
	bgAirport := r.CreateBlagoveschinskAirport(bg)

	svx2dme := r.CreateFlight(ac, "HZ 5555", svx, dme, "9:10 - 10:20")
	dme2bg := r.CreateFlight(ac, "HZ 6666", dme, bgAirport, "12:10 - 13:20")

	oneTransfer := f.CreateSnippet("one-transfer", []string{svx2dme.Key, dme2bg.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(4000))})

	reference := r.GetProto()

	snippets := map[string]*aviaSearchProto.Snippet{
		oneTransfer.Key: oneTransfer,
	}

	filters := &aviaAPI.SearchFiltersReq{}
	searchContext, _ := searchcommon.ParseQIDToProto("220421-145911-315.travelapp.plane.c213_c77_2022-04-26_None_economy_1_0_0_ru.ru")

	snippets, filterResponse := ApplyFilters(context.Background(), &nop.Logger{}, snippets, reference, searchContext, filters)

	snippetKeys := make(map[string]struct{})
	for key := range snippets {
		snippetKeys[key] = struct{}{}
	}
	expectedSnippetKeys := map[string]struct{}{oneTransfer.Key: {}}
	require.True(t, maps.Equal(snippetKeys, expectedSnippetKeys), "unexpected snippets: %v != %v", snippetKeys, expectedSnippetKeys)
	require.Equal(t, true, filterResponse.Transfer.OneTransferOrLess.Enabled)
}

func TestTransfer_Night_OnlyDayTransfers(t *testing.T) {
	r := f.NewReference()
	ac := r.CreateAviaCompany(50)
	partner := r.CreatePartner("some-partner")

	msk := r.CreateMoscow()
	bg := r.CreateBlagoveschinsk()

	svx := r.CreateKoltsovo(r.CreateYekaterinburg())
	dme := r.CreateDomodedovo(msk)
	bgAirport := r.CreateBlagoveschinskAirport(bg)

	svx2dme := r.CreateFlight(ac, "HZ 5555", svx, dme, "9:10 - 10:20")
	dme2bg := r.CreateFlight(ac, "HZ 6666", dme, bgAirport, "12:10 - 13:20")

	dayTransfer := f.CreateSnippet("day-transfer", []string{svx2dme.Key, dme2bg.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(4000))})

	reference := r.GetProto()

	snippets := map[string]*aviaSearchProto.Snippet{
		dayTransfer.Key: dayTransfer,
	}

	filters := &aviaAPI.SearchFiltersReq{}
	searchContext, _ := searchcommon.ParseQIDToProto("220421-145911-315.travelapp.plane.c213_c77_2022-04-26_None_economy_1_0_0_ru.ru")

	snippets, filterResponse := ApplyFilters(context.Background(), &nop.Logger{}, snippets, reference, searchContext, filters)

	snippetKeys := make(map[string]struct{})
	for key := range snippets {
		snippetKeys[key] = struct{}{}
	}
	expectedSnippetKeys := map[string]struct{}{dayTransfer.Key: {}}
	require.True(t, maps.Equal(snippetKeys, expectedSnippetKeys), "unexpected snippets: %v != %v", snippetKeys, expectedSnippetKeys)
	require.Equal(t, true, filterResponse.Transfer.NoNightTransfer.Enabled)
}

func TestTransfer_Night_OnlyNightTransfers(t *testing.T) {
	r := f.NewReference()
	ac := r.CreateAviaCompany(50)
	partner := r.CreatePartner("some-partner")

	msk := r.CreateMoscow()
	bg := r.CreateBlagoveschinsk()

	svx := r.CreateKoltsovo(r.CreateYekaterinburg())
	dme := r.CreateDomodedovo(msk)
	bgAirport := r.CreateBlagoveschinskAirport(bg)

	svx2dme := r.CreateFlight(ac, "HZ 5555", svx, dme, "9:10 - 10:20")
	dme2bg := r.CreateFlight(ac, "HZ 6666", dme, bgAirport, "12:10 - 13:20", f.WithDepartureDayShift(1))

	nightTransfer := f.CreateSnippet("night-transfer", []string{svx2dme.Key, dme2bg.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(4000))})

	reference := r.GetProto()

	snippets := map[string]*aviaSearchProto.Snippet{
		nightTransfer.Key: nightTransfer,
	}

	filters := &aviaAPI.SearchFiltersReq{}
	searchContext, _ := searchcommon.ParseQIDToProto("220421-145911-315.travelapp.plane.c213_c77_2022-04-26_None_economy_1_0_0_ru.ru")

	snippets, filterResponse := ApplyFilters(context.Background(), &nop.Logger{}, snippets, reference, searchContext, filters)

	snippetKeys := make(map[string]struct{})
	for key := range snippets {
		snippetKeys[key] = struct{}{}
	}
	expectedSnippetKeys := map[string]struct{}{nightTransfer.Key: {}}
	require.True(t, maps.Equal(snippetKeys, expectedSnippetKeys), "unexpected snippets: %v != %v", snippetKeys, expectedSnippetKeys)
	require.Equal(t, false, filterResponse.Transfer.NoNightTransfer.Enabled)
}

func TestTransfer_AirportChange_NoVariantsWithAirportChange(t *testing.T) {
	r := f.NewReference()
	ac := r.CreateAviaCompany(50)
	partner := r.CreatePartner("some-partner")

	msk := r.CreateMoscow()
	bg := r.CreateBlagoveschinsk()

	svx := r.CreateKoltsovo(r.CreateYekaterinburg())
	dme := r.CreateDomodedovo(msk)
	bgAirport := r.CreateBlagoveschinskAirport(bg)

	svx2dme := r.CreateFlight(ac, "HZ 5555", svx, dme, "9:10 - 10:20")
	dme2bg := r.CreateFlight(ac, "HZ 6666", dme, bgAirport, "12:10 - 13:20")

	sameAirportTransfer := f.CreateSnippet("same-airport", []string{svx2dme.Key, dme2bg.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(4000))})

	reference := r.GetProto()

	snippets := map[string]*aviaSearchProto.Snippet{
		sameAirportTransfer.Key: sameAirportTransfer,
	}

	filters := &aviaAPI.SearchFiltersReq{}
	searchContext, _ := searchcommon.ParseQIDToProto("220421-145911-315.travelapp.plane.c213_c77_2022-04-26_None_economy_1_0_0_ru.ru")

	snippets, filterResponse := ApplyFilters(context.Background(), &nop.Logger{}, snippets, reference, searchContext, filters)

	snippetKeys := make(map[string]struct{})
	for key := range snippets {
		snippetKeys[key] = struct{}{}
	}
	expectedSnippetKeys := map[string]struct{}{sameAirportTransfer.Key: {}}
	require.True(t, maps.Equal(snippetKeys, expectedSnippetKeys), "unexpected snippets: %v != %v", snippetKeys, expectedSnippetKeys)
	require.Equal(t, true, filterResponse.Transfer.NoAirportChange.Enabled)
}

func TestTransfer_AirportChange_OnlyVariantsWithAirportChange(t *testing.T) {
	r := f.NewReference()
	ac := r.CreateAviaCompany(50)
	partner := r.CreatePartner("some-partner")

	msk := r.CreateMoscow()
	bg := r.CreateBlagoveschinsk()

	ekbAirport := r.CreateKoltsovo(r.CreateYekaterinburg())
	dme := r.CreateDomodedovo(msk)
	svo := r.CreateSheremetyevo(msk)
	bgAirport := r.CreateBlagoveschinskAirport(bg)

	ekb2dme := r.CreateFlight(ac, "HZ 5555", ekbAirport, dme, "9:10 - 10:20")
	svo2bg := r.CreateFlight(ac, "HZ 6666", svo, bgAirport, "12:10 - 13:20")

	sameAirportTransfer := f.CreateSnippet("same-airport", []string{ekb2dme.Key, svo2bg.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(4000))})

	reference := r.GetProto()

	snippets := map[string]*aviaSearchProto.Snippet{
		sameAirportTransfer.Key: sameAirportTransfer,
	}

	filters := &aviaAPI.SearchFiltersReq{}
	searchContext, _ := searchcommon.ParseQIDToProto("220421-145911-315.travelapp.plane.c213_c77_2022-04-26_None_economy_1_0_0_ru.ru")

	snippets, filterResponse := ApplyFilters(context.Background(), &nop.Logger{}, snippets, reference, searchContext, filters)

	snippetKeys := make(map[string]struct{})
	for key := range snippets {
		snippetKeys[key] = struct{}{}
	}
	expectedSnippetKeys := map[string]struct{}{sameAirportTransfer.Key: {}}
	require.True(t, maps.Equal(snippetKeys, expectedSnippetKeys), "unexpected snippets: %v != %v", snippetKeys, expectedSnippetKeys)
	require.Equal(t, false, filterResponse.Transfer.NoAirportChange.Enabled)
}

func TestTransfer_InterDependence(t *testing.T) {
	r := f.NewReference()
	ac := r.CreateAviaCompany(50)
	partner := r.CreatePartner("some-partner")

	msk := r.CreateMoscow()
	bg := r.CreateBlagoveschinsk()

	ekbAirport := r.CreateKoltsovo(r.CreateYekaterinburg())
	dme := r.CreateDomodedovo(msk)
	svo := r.CreateSheremetyevo(msk)
	bgAirport := r.CreateBlagoveschinskAirport(bg)

	ekb2dme := r.CreateFlight(ac, "HZ 5555", ekbAirport, dme, "9:10 - 10:20")
	dme2bg := r.CreateFlight(ac, "HZ 6666", dme, bgAirport, "12:10 - 13:20", f.WithDepartureDayShift(1))
	dmeNightTransfer := f.CreateSnippet("dmeNightTransfer", []string{ekb2dme.Key, dme2bg.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(4000))})

	ekb2svo := r.CreateFlight(ac, "HZ 5555", ekbAirport, svo, "9:10 - 10:20")
	svo2bg := r.CreateFlight(ac, "HZ 6666", svo, bgAirport, "12:10 - 13:20")
	svoDayTransfer := f.CreateSnippet("svoDayTransfer", []string{ekb2svo.Key, svo2bg.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(4000))})

	snippets := map[string]*aviaSearchProto.Snippet{
		dmeNightTransfer.Key: dmeNightTransfer,
		svoDayTransfer.Key:   svoDayTransfer,
	}

	reference := r.GetProto()
	searchContext, _ := searchcommon.ParseQIDToProto("220421-145911-315.travelapp.plane.c54_c77_2022-04-26_None_economy_1_0_0_ru.ru")

	// Убираем ночные пересадки
	// Ожидаем, что можно будет выбрать только svo
	filters := &aviaAPI.SearchFiltersReq{
		Transfer: &aviaAPI.SearchFiltersReq_TransferFilter{
			NoNightTransfer: true,
		},
	}

	snippets, filterResponse := ApplyFilters(context.Background(), &nop.Logger{}, snippets, reference, searchContext, filters)

	snippetKeys := make(map[string]struct{})
	for key := range snippets {
		snippetKeys[key] = struct{}{}
	}
	expectedSnippetKeys := map[string]struct{}{svoDayTransfer.Key: {}}
	require.True(t, maps.Equal(snippetKeys, expectedSnippetKeys), "unexpected snippets: %v != %v", snippetKeys, expectedSnippetKeys)
	require.Equal(t, svo.Id, filterResponse.Transfer.Airports.Forward[0].StationId)
	require.True(t, filterResponse.Transfer.Airports.Forward[0].State.Enabled)
	require.Equal(t, dme.Id, filterResponse.Transfer.Airports.Forward[1].StationId)
	require.False(t, filterResponse.Transfer.Airports.Forward[1].State.Enabled)
}
