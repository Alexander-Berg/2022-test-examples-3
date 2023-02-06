package tariffmatcher

import (
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/core/log"
	farefamiliesstructs "a.yandex-team.ru/travel/avia/fare_families/internal/services/fare_families/data_structs/fare_families"
	"a.yandex-team.ru/travel/avia/fare_families/internal/services/fare_families/data_structs/payloads"
	"a.yandex-team.ru/travel/avia/fare_families/internal/services/fare_families/dicts"
	storage_pkg "a.yandex-team.ru/travel/avia/fare_families/internal/services/fare_families/storage"
	"a.yandex-team.ru/travel/library/go/testutil"
	"a.yandex-team.ru/travel/proto/dicts/rasp"
)

func TestGetHash(t *testing.T) {
	t.Run(
		"TestGetHash", func(t *testing.T) {
			testKeys := [][]string{
				{"a", "b", ""},
				nil,
			}
			result := getHash(testKeys)
			require.EqualValues(t, "ecf1993b4ab81c36cf929472ea34cb26", result)
		},
	)
}

func TestMatchesDomesticInternational(t *testing.T) {
	tariffMatcher, err := newTestTariffMatcher(t)
	require.NoError(t, err)

	flightDomesticRU := payloads.VariantsFlight{
		From: 54,
		To:   2,
	}
	require.True(t, tariffMatcher.matchesDomesticRU(flightDomesticRU))
	require.False(t, tariffMatcher.matchesInternational(flightDomesticRU))

	flightInternational1 := payloads.VariantsFlight{
		From: 157,
		To:   2,
	}
	require.False(t, tariffMatcher.matchesDomesticRU(flightInternational1))
	require.True(t, tariffMatcher.matchesInternational(flightInternational1))

	flightInternational2 := payloads.VariantsFlight{
		From: 54,
		To:   157,
	}
	require.False(t, tariffMatcher.matchesDomesticRU(flightInternational2))
	require.True(t, tariffMatcher.matchesInternational(flightInternational2))
}

func TestMatchesCountriesFromTo(t *testing.T) {
	tariffMatcher, err := newTestTariffMatcher(t)
	require.NoError(t, err)

	flight := payloads.VariantsFlight{
		From: 54,
		To:   157,
	}
	require.True(t, tariffMatcher.matchesCountriesFrom([]string{"RU"}, flight))
	require.False(t, tariffMatcher.matchesCountriesFrom([]string{"UA"}, flight))

	require.True(t, tariffMatcher.matchesCountriesTo([]string{"BY", "UA"}, flight))
	require.False(t, tariffMatcher.matchesCountriesTo([]string{"RU", "KZ"}, flight))

	require.True(t, tariffMatcher.matchesCountriesOnly([]string{"RU"}, flight))
	require.True(t, tariffMatcher.matchesCountriesOnly([]string{"UA", "BY"}, flight))
	require.False(t, tariffMatcher.matchesCountriesOnly([]string{"UA", "KZ"}, flight))
}

func TestMatchesAirports(t *testing.T) {
	tariffMatcher, err := newTestTariffMatcher(t)
	require.NoError(t, err)

	flight := payloads.VariantsFlight{
		From: 54,
		To:   157,
	}
	require.True(t, tariffMatcher.matchesAirportsOnly([]string{"SVX"}, flight))
	require.True(t, tariffMatcher.matchesAirportsOnly([]string{"MSQ"}, flight))
	require.False(t, tariffMatcher.matchesAirportsOnly([]string{"CEK"}, flight))
}

func TestMatchesAirportsBetween(t *testing.T) {
	tariffMatcher, err := newTestTariffMatcher(t)
	require.NoError(t, err)

	flight := payloads.VariantsFlight{
		From: 54,
		To:   157,
	}
	require.True(t, tariffMatcher.matchesAirportsBetween(
		farefamiliesstructs.Between{
			PointsA: []string{"SVX"},
			PointsB: []string{"MSQ"},
		},
		flight,
	))
	require.True(t, tariffMatcher.matchesAirportsBetween(
		farefamiliesstructs.Between{
			PointsA: []string{"MSQ"},
			PointsB: []string{"SVX"},
		},
		flight,
	))
	require.False(t, tariffMatcher.matchesAirportsBetween(
		farefamiliesstructs.Between{
			PointsA: []string{"MSQ"},
			PointsB: []string{"CEK"},
		},
		flight,
	))
}

func TestMatchesDepartBefore(t *testing.T) {
	tariffMatcher, err := newTestTariffMatcher(t)
	require.NoError(t, err)

	flight := payloads.VariantsFlight{
		From: 54,
		To:   157,
		Departure: payloads.FlightTime{
			LocalTime: "2022-01-31T12:34:56",
			Offset:    180,
			TZName:    "Europe/Moscow",
		},
	}
	require.True(t, tariffMatcher.matchesDepartBefore("2022-02-01", flight))
	require.False(t, tariffMatcher.matchesDepartBefore("2022-01-31", flight))
}

func TestMatchesDepartAfter(t *testing.T) {
	tariffMatcher, err := newTestTariffMatcher(t)
	require.NoError(t, err)

	flight := payloads.VariantsFlight{
		From: 54,
		To:   157,
		Departure: payloads.FlightTime{
			LocalTime: "2022-01-31T12:34:56",
			Offset:    180,
			TZName:    "Europe/Moscow",
		},
	}
	require.True(t, tariffMatcher.matchesDepartAfter("2022-01-30", flight))
	require.True(t, tariffMatcher.matchesDepartAfter("2022-01-31", flight))
	require.False(t, tariffMatcher.matchesDepartAfter("2022-02-01", flight))
}

func TestMatchFareFamilies(t *testing.T) {
	tariffMatcher, err := newTestTariffMatcher(t)
	require.NoError(t, err)

	variants := payloads.VariantsFromPartner{
		Fares: map[string]payloads.VariantsFare{
			"test_fare": payloads.VariantsFare{
				FareCodes: [][]string{{"Q"}, {}},
				Route:     [][]string{{"SVX-MSQ"}, {}},
			},
		},
		Flights: map[string]payloads.VariantsFlight{
			"SVX-MSQ": payloads.VariantsFlight{
				From: 157,
				To:   54,
				Departure: payloads.FlightTime{
					LocalTime: "2022-01-31T12:34:56",
					Offset:    180,
					TZName:    "Europe/Moscow",
				},
				Operating: payloads.OperatingFlightTitle{
					Company: 26,
					Title:   "SU 198",
				},
			},
		},
	}
	result, err := tariffMatcher.MatchFareFamilies(&variants, nil)
	require.NoError(t, err)
	expectedFareFamily := map[string]farefamiliesstructs.FareFamilyForVariant{
		"ff_index=0;carrier=26;dog-walking=1": farefamiliesstructs.FareFamilyForVariant{
			BaseClass:         "Q",
			Brand:             "Economy",
			TariffCodePattern: "^Q\\w*\\d*$",
			Terms: []farefamiliesstructs.FareFamilyTermForVariant{
				{
					Code: "dog-walking",
					ID:   1,
					Rule: farefamiliesstructs.FareFamilyTermRule{
						Availability: "FROM_BY",
						Conditions: []farefamiliesstructs.Condition{
							{
								CountriesFrom: []string{"BY"},
							},
						},
					},
				},
			},
			Key: "ff_index=0;carrier=26;dog-walking=1",
		},
	}
	require.Equal(t, expectedFareFamily, result.FareFamiliesReference)
	expectedFare := payloads.FareFamiliesEntry{
		FareFamilyKeys:   [][]string{{"ff_index=0;carrier=26;dog-walking=1"}, {}},
		FareFamiliesHash: "97bd411c00b1f6cba863ea5682449807",
	}
	require.Equal(t, expectedFare, result.VariantsMap["test_fare"])
}

func newTestTariffMatcher(t *testing.T) (*tariffMatcherImpl, error) {
	logger := testutil.NewLogger(t)
	return newTestTariffMatcherWithLogger(logger)
}

func newTestTariffMatcherWithLogger(logger log.Logger) (*tariffMatcherImpl, error) {
	storage := storage_pkg.NewEmptyStorage(logger)
	err := storage.(storage_pkg.StorageForTests).AddFareFamily(26, farefamiliesstructs.FareFamily{
		BaseClass:         "Q",
		Brand:             "Economy",
		TariffCodePattern: "^Q\\w*\\d*$",
		Terms: []farefamiliesstructs.FareFamilyTerm{
			{
				Code: "dog-walking",
				Rules: []farefamiliesstructs.FareFamilyTermRule{
					{
						Availability: "DOMESTIC",
						Conditions: []farefamiliesstructs.Condition{
							{
								IsDomesticRU: true,
							},
						},
					},
					{
						Availability: "FROM_BY",
						Conditions: []farefamiliesstructs.Condition{
							{
								CountriesFrom: []string{"BY"},
							},
						},
					},
					{
						Availability: "NOT_AVAILABLE",
					},
				},
			},
		},
	})
	if err != nil {
		return nil, err
	}

	dicts := dicts.NewRegistryForTests()
	dicts.Stations().Add(&rasp.TStation{
		Id:           54,
		CountryId:    225,
		TitleDefault: "SVX",
		StationCodes: map[int32]string{
			int32(rasp.ECodeSystem_CODE_SYSTEM_IATA): "SVX",
		},
	})
	dicts.Stations().Add(&rasp.TStation{
		Id:           2,
		CountryId:    225,
		TitleDefault: "LED",
	})
	dicts.Stations().Add(&rasp.TStation{
		Id:           157,
		CountryId:    149,
		TitleDefault: "MSQ",
		StationCodes: map[int32]string{
			int32(rasp.ECodeSystem_CODE_SYSTEM_IATA): "MSQ",
		},
	})
	dicts.Countries().Add(&rasp.TCountry{
		Id:   225,
		Code: "RU",
	})
	dicts.Countries().Add(&rasp.TCountry{
		Id:   149,
		Code: "BY",
	})

	tm, err := NewTariffMatcher(storage, dicts, logger)
	if err != nil {
		return nil, err
	}
	tariffMatcher := tm.(*tariffMatcherImpl)
	return tariffMatcher, nil
}
