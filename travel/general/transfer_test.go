package filtering

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

	moscow := r.CreateSettlement(213)
	blagoveschinsk := r.CreateSettlement(77)
	zea := r.CreateSettlement(11379)

	mskAirport := r.CreateStation(9600213, f.WithSettlement(moscow))
	bgAirport := r.CreateStation(9623271, f.WithSettlement(blagoveschinsk))
	zeaAirport := r.CreateStation(9635355, f.WithSettlement(zea))

	ac := r.CreateAviaCompany(50)

	partner := r.CreatePartner("some-partner")

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
			Enabled: false,
			Value:   true,
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

	moscow := r.CreateSettlement(213)
	ekaterinburg := r.CreateSettlement(214)
	blagoveschinsk := r.CreateSettlement(77)
	zea := r.CreateSettlement(11379)

	mskAirport := r.CreateStation(9600213, f.WithSettlement(moscow))
	ekbAirport := r.CreateStation(1, f.WithSettlement(ekaterinburg))
	bgAirport := r.CreateStation(9623271, f.WithSettlement(blagoveschinsk))
	zeaAirport := r.CreateStation(9635355, f.WithSettlement(zea))

	ac := r.CreateAviaCompany(50)

	partner := r.CreatePartner("some-partner")

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

	bms := r.CreateFlight(ac, "HZ 5051", bgAirport, ekbAirport, "9:10 - 10:20")
	msz := r.CreateFlight(ac, "HZ 4243", ekbAirport, zeaAirport, "12:10 - 13:20")
	snippet4 := f.CreateSnippet("snippet-4", []string{bms.Key, msz.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(5600))})

	reference := r.GetProto()

	snippets := map[string]*aviaSearchProto.Snippet{
		snippet1.Key: snippet1,
		snippet2.Key: snippet2,
		snippet3.Key: snippet3,
		snippet4.Key: snippet4,
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
						StationId: ekbAirport.Id,
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
	expectedSnippetKeys := map[string]struct{}{"snippet-1": {}, "snippet-2": {}, "snippet-3": {}, "snippet-4": {}}
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
			Enabled: false,
			Value:   true,
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
					StationId: ekbAirport.Id,
					State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
						Enabled: true,
						Value:   false,
					},
					StationTitle:    ekbAirport.Title,
					SettlementTitle: ekaterinburg.Title,
					AviaCode:        ekbAirport.AviaCode,
				},
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

func TestTransfer_NoTransfer(t *testing.T) {
	r := f.NewReference()

	moscow := r.CreateSettlement(213)
	blagoveschinsk := r.CreateSettlement(27)
	zea := r.CreateSettlement(11379)

	mskAirport := r.CreateStation(9600213, f.WithSettlement(moscow))
	bgAirport := r.CreateStation(9623271, f.WithSettlement(blagoveschinsk))
	zeaAirport := r.CreateStation(9635355, f.WithSettlement(zea))

	ac := r.CreateAviaCompany(50)

	partner := r.CreatePartner("some-partner")

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
		QuickTransfer: true,
		Transfer: &aviaAPI.SearchFiltersReq_TransferFilter{
			NoTransfer:        true,
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
	expectedSnippetKeys := map[string]struct{}{"snippet-1": {}, "snippet-2": {}}
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
	require.EqualValues(t, &aviaAPI.SearchFiltersRsp_TransferFilter{
		NoTransfer: &aviaAPI.SearchFiltersRsp_BoolFilterState{
			Enabled: true,
			Value:   true,
		},
		OneTransferOrLess: &aviaAPI.SearchFiltersRsp_BoolFilterState{
			Enabled: false,
			Value:   true,
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

func TestQuickTransferPrice_DependingOnBaggageFilter(t *testing.T) {
	r := f.NewReference()

	moscow := r.CreateSettlement(213)
	blagoveschinsk := r.CreateSettlement(27)
	zea := r.CreateSettlement(11379)

	mskAirport := r.CreateStation(9600213, f.WithSettlement(moscow))
	bgAirport := r.CreateStation(9623271, f.WithSettlement(blagoveschinsk))
	zeaAirport := r.CreateStation(9635355, f.WithSettlement(zea))

	ac := r.CreateAviaCompany(50)

	partner := r.CreatePartner("some-partner")

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
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(5600)),
		"2": f.CreateVariant("2", partner, f.WithPriceRUB(7700), f.WithBaggage(21, 1)),
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

func TestTransfer_TransferOnly(t *testing.T) {
	r := f.NewReference()

	moscow := r.CreateSettlement(213)
	blagoveschinsk := r.CreateSettlement(77)
	zea := r.CreateSettlement(11379)

	mskAirport := r.CreateStation(9600213, f.WithSettlement(moscow))
	bgAirport := r.CreateStation(9623271, f.WithSettlement(blagoveschinsk))
	zeaAirport := r.CreateStation(9635355, f.WithSettlement(zea))

	ac := r.CreateAviaCompany(50)

	partner := r.CreatePartner("some-partner")

	bm := r.CreateFlight(ac, "HZ 5050", bgAirport, mskAirport, "9:10 - 10:20")
	mz := r.CreateFlight(ac, "HZ 4242", mskAirport, zeaAirport, "12:10 - 13:20")
	snippet := f.CreateSnippet("snippet", []string{bm.Key, mz.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(5600))})

	reference := r.GetProto()
	searchContext, _ := searchcommon.ParseQIDToProto("220421-145911-315.travelapp.plane.c77_c11379_2022-04-26_None_economy_1_0_0_ru.ru")

	for _, initialFilterState := range []bool{true, false} {
		t.Run(fmt.Sprintf("TestTransfer_TransferOnly - initialFilterState: %t", initialFilterState), func(t *testing.T) {
			filters := &aviaAPI.SearchFiltersReq{
				QuickTransfer: initialFilterState,
				Transfer: &aviaAPI.SearchFiltersReq_TransferFilter{
					NoTransfer:        initialFilterState,
					OneTransferOrLess: false,
					NoNightTransfer:   true,
					NoAirportChange:   true,
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
					Enabled: false,
					Value:   false,
				},
				MinPriceNoTransfer: nil,
				MinPriceWithTransfer: &commonAPI.Price{
					Currency: "RUB",
					Value:    5600,
				},
			}, filterResponse.QuickTransfer)
			require.EqualValues(t, &aviaAPI.SearchFiltersRsp_TransferFilter{
				NoTransfer: &aviaAPI.SearchFiltersRsp_BoolFilterState{
					Enabled: false,
					Value:   false,
				},
				OneTransferOrLess: &aviaAPI.SearchFiltersRsp_BoolFilterState{
					Enabled: false,
					Value:   true,
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
		})
	}
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
			Enabled: true,
			Value:   false,
		},
		NoAirportChange: &aviaAPI.SearchFiltersRsp_BoolFilterState{
			Enabled: true,
			Value:   false,
		},
		TransferDuration: nil,
		Airports:         nil,
	}, filterResponse.Transfer)
}

func TestTransfer_DirectOnly(t *testing.T) {
	r := f.NewReference()

	bgAirport := r.CreateStation(9623271)
	zeaAirport := r.CreateStation(9635355)

	ac := r.CreateAviaCompany(50)

	partner := r.CreatePartner("some-partner")

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
					NoNightTransfer:   true,
					NoAirportChange:   true,
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
					Enabled: false,
					Value:   true,
				},
				MinPriceNoTransfer: &commonAPI.Price{
					Currency: "RUB",
					Value:    2000,
				},
				MinPriceWithTransfer: nil,
			}, filterResponse.QuickTransfer)
			require.EqualValues(t, &aviaAPI.SearchFiltersRsp_TransferFilter{
				NoTransfer: &aviaAPI.SearchFiltersRsp_BoolFilterState{
					Enabled: false,
					Value:   true,
				},
				OneTransferOrLess: &aviaAPI.SearchFiltersRsp_BoolFilterState{
					Enabled: false,
					Value:   true,
				},
				NoNightTransfer: &aviaAPI.SearchFiltersRsp_BoolFilterState{
					Enabled: true,
					Value:   true,
				},
				NoAirportChange: &aviaAPI.SearchFiltersRsp_BoolFilterState{
					Enabled: true,
					Value:   true,
				},
				TransferDuration: nil,
				Airports:         nil,
			}, filterResponse.Transfer)
		})
	}
}

func TestTransfer_MoreThanOneTransfer(t *testing.T) {
	r := f.NewReference()

	mskAirport := r.CreateStation(9600213)
	ekbAirport := r.CreateStation(9600066)
	bgAirport := r.CreateStation(9623271)
	zeaAirport := r.CreateStation(9635355)

	ac := r.CreateAviaCompany(50)

	partner := r.CreatePartner("some-partner")

	bz := r.CreateFlight(ac, "HZ 2682", bgAirport, zeaAirport, "9:10 - 10:20")
	direct := f.CreateSnippet("direct", []string{bz.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(2897))})

	bm := r.CreateFlight(ac, "HZ 5050", bgAirport, mskAirport, "9:10 - 10:20")
	mz := r.CreateFlight(ac, "HZ 4242", mskAirport, zeaAirport, "12:10 - 13:20")
	oneTransfer := f.CreateSnippet("one-transfer", []string{bm.Key, mz.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(5600))})

	bm2 := r.CreateFlight(ac, "HZ 2222", bgAirport, mskAirport, "9:20 - 10:30")
	me := r.CreateFlight(ac, "HZ 2233", mskAirport, ekbAirport, "12:00 - 14:10")
	ez := r.CreateFlight(ac, "HZ 2244", mskAirport, zeaAirport, "16:10 - 19:20")
	twoTransfers := f.CreateSnippet("two-transfers", []string{bm2.Key, me.Key, ez.Key}, nil, r, map[string]*aviaSearchProto.Variant{
		"1": f.CreateVariant("1", partner, f.WithPriceRUB(8000))})

	reference := r.GetProto()

	snippets := map[string]*aviaSearchProto.Snippet{
		direct.Key:       direct,
		oneTransfer.Key:  oneTransfer,
		twoTransfers.Key: twoTransfers,
	}

	filters := &aviaAPI.SearchFiltersReq{
		QuickTransfer: false,
		Transfer: &aviaAPI.SearchFiltersReq_TransferFilter{
			NoTransfer:        false,
			OneTransferOrLess: true,
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
	expectedSnippetKeys := map[string]struct{}{"direct": {}, "one-transfer": {}}
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
			Value:   true,
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
				MinimumMinutes: 90,
				MaximumMinutes: 120,
			},
			Selected: &aviaAPI.SearchFiltersRsp_TransferDurationInterval{
				MinimumMinutes: 90,
				MaximumMinutes: 120,
			},
		},
		Airports: &aviaAPI.SearchFiltersRsp_TransferAirportsFilter{
			Forward: []*aviaAPI.SearchFiltersRsp_TransferAirport{
				{
					StationId: ekbAirport.Id,
					State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
						Enabled: true,
						Value:   false,
					},
					StationTitle:    ekbAirport.Title,
					SettlementTitle: "",
					AviaCode:        ekbAirport.AviaCode,
				},
				{
					StationId: mskAirport.Id,
					State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
						Enabled: true,
						Value:   false,
					},
					StationTitle:    mskAirport.Title,
					SettlementTitle: "",
					AviaCode:        mskAirport.AviaCode,
				},
			},
			Backward: nil,
		},
	}, filterResponse.Transfer)
}

func TestTransfer_TransferDuration(t *testing.T) {
	r := f.NewReference()

	mskAirport := r.CreateStation(9600213)
	bgAirport := r.CreateStation(9623271)
	zeaAirport := r.CreateStation(9635355)

	ac := r.CreateAviaCompany(50)

	partner := r.CreatePartner("some-partner")

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
		QuickTransfer: false,
		Transfer: &aviaAPI.SearchFiltersReq_TransferFilter{
			NoTransfer:        false,
			OneTransferOrLess: false,
			NoNightTransfer:   true,
			NoAirportChange:   true,
			TransferDuration: &aviaAPI.SearchFiltersReq_TransferDurationInterval{
				MinimumMinutes: 90,
				MaximumMinutes: 180,
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
	expectedSnippetKeys := map[string]struct{}{"direct": {}, "short-transfer": {}}
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
			Value:    2000,
		},
		MinPriceWithTransfer: &commonAPI.Price{
			Currency: "RUB",
			Value:    4000,
		},
	}, filterResponse.QuickTransfer)
	require.EqualValues(t, &aviaAPI.SearchFiltersRsp_TransferFilter{
		NoTransfer: &aviaAPI.SearchFiltersRsp_BoolFilterState{
			Enabled: true,
			Value:   false,
		},
		OneTransferOrLess: &aviaAPI.SearchFiltersRsp_BoolFilterState{
			Enabled: false,
			Value:   true,
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
				MaximumMinutes: 650,
			},
			Selected: &aviaAPI.SearchFiltersRsp_TransferDurationInterval{
				MinimumMinutes: 110,
				MaximumMinutes: 180,
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
					SettlementTitle: "",
					AviaCode:        mskAirport.AviaCode,
				},
			},
			Backward: nil,
		},
	}, filterResponse.Transfer)
}
