package search

import (
	"context"
	"net/url"
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/core/log/nop"
	"a.yandex-team.ru/library/go/ptr"
	aviaAPI "a.yandex-team.ru/travel/app/backend/api/avia/v1"
	"a.yandex-team.ru/travel/app/backend/internal/lib/aviatdapiclient"
)

func TestBuildOrderURL(t *testing.T) {
	forward := []string{
		"2210100905SU14602210101715",
		"2210101955S750192210102025",
	}
	var backward []string
	flightReference := map[string]aviatdapiclient.Flight{
		"2210100905SU14602210101715": {
			Key:    "2210100905SU14602210101715",
			Number: "SU 1460",
			Departure: aviatdapiclient.DateTime{
				Local:    "2022-10-10T09:05:00",
				Timezone: "Europe/Moscow",
				Offset:   180,
			},
			Arrival: aviatdapiclient.DateTime{
				Local:    "2022-10-10T17:15:00",
				Timezone: "Asia/Novosibirsk",
				Offset:   420,
			},
			CompanyID:            26,
			OperatingAviaCompony: aviatdapiclient.OperatingAviaCompany{},
			CompanyTariffID:      100500,
			AviaCompanyID:        ptr.Uint64(26),
			StationFromID:        9600213,
			StationToID:          9600374,
		},
		"2210101955S750192210102025": {
			Key:    "2210101955S750192210102025",
			Number: "S7 5019",
			Departure: aviatdapiclient.DateTime{
				Local:    "2022-10-10T19:55:00",
				Timezone: "Asia/Novosibirsk",
				Offset:   420,
			},
			Arrival: aviatdapiclient.DateTime{
				Local:    "2022-10-10T20:25:00",
				Timezone: "Asia/Yekaterinburg",
				Offset:   300,
			},
			CompanyID:            23,
			OperatingAviaCompony: aviatdapiclient.OperatingAviaCompany{},
			CompanyTariffID:      200300,
			AviaCompanyID:        ptr.Uint64(23),
			StationFromID:        9600374,
			StationToID:          9600370,
		},
	}

	actual, err := BuildOrderURL(
		"220224-211309-562.travelapp.plane.c213_c54_2022-10-10_None_economy_1_0_0_ru.ru",
		forward,
		backward,
		flightReference,
		true,
		aviaAPI.Snippet_REFUND_AVAILABILITY_FREE,
	)

	require.NoError(t, err)
	parsed, err := url.Parse("https://example.com" + actual)
	require.NoError(t, err)
	require.Equal(t, "/avia/order/", parsed.Path)
	require.Equal(t, url.Values{
		"adult_seats":    {"1"},
		"baggage":        {"1"},
		"children_seats": {"0"},
		"forward":        {"SU 1460.2022-10-10T09:05,S7 5019.2022-10-10T19:55"},
		"free_refund":    {"1"},
		"fromId":         {"c213"},
		"infant_seats":   {"0"},
		"klass":          {"economy"},
		"oneway":         {"1"},
		"toId":           {"c54"},
		"when":           {"2022-10-10"},
	}, parsed.Query())
}

var (
	ss              = NewServiceSearch(&nop.Logger{})
	key1            = "key1"
	key2            = "key2"
	route1          = "2205191555SU11282205191830"
	route2          = "2205210135U660062205210525"
	companyTariffID = uint64(1)

	baggageTariff = map[string]aviatdapiclient.BaggageTariff{
		"0p0p0p": {
			Included: aviatdapiclient.BaggageTariffDetails{Count: 0, Source: "partner"},
			Pieces:   aviatdapiclient.BaggageTariffDetails{Count: 0, Source: "partner"},
			Weight:   aviatdapiclient.BaggageTariffDetails{Count: 0, Source: "partner"},
		},
		"1p1p23p": {
			Included: aviatdapiclient.BaggageTariffDetails{Count: 1, Source: "partner"},
			Pieces:   aviatdapiclient.BaggageTariffDetails{Count: 1, Source: "partner"},
			Weight:   aviatdapiclient.BaggageTariffDetails{Count: 23, Source: "partner"},
		},
	}
	fareFamilies = map[string]aviatdapiclient.FareFamily{
		key1: {
			Key: key1,
			Terms: []aviatdapiclient.FFTerm{
				createTermBaggage(1, 15),
				createTermCarryOn(1, 5),
				createTermRefunded(aviatdapiclient.AvailabilityTypeNotAvailable, ""),
			},
		},
		key2: {
			Key: key2,
			Terms: []aviatdapiclient.FFTerm{
				createTermBaggage(1, 20),
			},
		},
	}
	flights = []aviatdapiclient.Flight{
		{
			Key:             route1,
			CompanyTariffID: companyTariffID,
		},
		{
			Key:             route2,
			CompanyTariffID: companyTariffID,
		},
	}
	companyTariff = []aviatdapiclient.CompanyTariff{
		{
			ID:          companyTariffID,
			CarryOn:     true,
			CarryOnNorm: 5,
		},
	}
)

func createTermBaggage(places int, weight int) aviatdapiclient.FFTerm {
	return aviatdapiclient.FFTerm{
		Code: aviatdapiclient.FFTermCodeBaggage,
		Rule: map[string]interface{}{
			"places": places,
			"weight": weight,
		},
	}
}
func createTermCarryOn(places int, weight int) aviatdapiclient.FFTerm {
	return aviatdapiclient.FFTerm{
		Code: aviatdapiclient.FFTermCodeCarryOn,
		Rule: map[string]interface{}{
			"places": places,
			"weight": weight,
		},
	}
}
func createTermRefunded(availability aviatdapiclient.AvailabilityType, value string) aviatdapiclient.FFTerm {
	rule := map[string]interface{}{
		"availability": availability,
	}
	if value != "" {
		rule["charge"] = map[string]interface{}{
			"currency": "RUB",
			"value":    value,
		}
	}
	return aviatdapiclient.FFTerm{
		Code: aviatdapiclient.FFTermCodeRefundable,
		Rule: rule,
	}
}

func TestBuildBaggage_Simple(t *testing.T) {
	fareKeys := [][]string{{key1}, {key2}}
	baggage := [][]string{{"0p0p0p"}, {"0p0p0p"}}

	ctx := context.Background()
	result := ss.BuildBaggage(ctx, fareKeys, fareFamilies, baggage, baggageTariff)

	require.True(t, result.Included)
	require.Equal(t, uint32(1), result.GetPieces())
	require.Equal(t, uint32(15), result.GetWeight())
}

func TestBuildCarryOn_Simple(t *testing.T) {
	fareKeys := [][]string{{key1}, {key2}}
	route := [][]string{{route1}, {route2}}

	ctx := context.Background()
	result := ss.BuildCarryOn(ctx, fareKeys, fareFamilies, route, flights, companyTariff)

	require.True(t, result.Included)
	require.Equal(t, uint32(5), result.GetWeight())
}

func TestBuildRefunded_Simple(t *testing.T) {
	fareKeys := [][]string{{key1}, {key2}}

	ctx := context.Background()
	result := ss.BuildRefund(ctx, fareKeys, fareFamilies)

	require.Equal(t, aviaAPI.Snippet_REFUND_AVAILABILITY_NOT_AVAILABLE, result.Availability)
	require.Nil(t, result.Price)
}

var fareFamiliesOne = map[string]aviatdapiclient.FareFamily{
	key1: {
		Key: key1,
		Terms: []aviatdapiclient.FFTerm{
			createTermBaggage(1, 15),
			createTermCarryOn(1, 5),
			createTermRefunded(aviatdapiclient.AvailabilityTypeCharge, "1000"),
		},
	},
}

func TestBuildBaggage_WithOneFareFamily(t *testing.T) {
	fareKeys := [][]string{{key1}, {key2}}
	baggage := [][]string{{"1p1p23p"}, {"1p1p23p"}}

	ctx := context.Background()
	result := ss.BuildBaggage(ctx, fareKeys, fareFamiliesOne, baggage, baggageTariff)

	require.True(t, result.Included)
	require.Equal(t, uint32(1), result.GetPieces())
	require.Equal(t, uint32(15), result.GetWeight())
}

func TestBuildBaggage_WithoutFareFamily(t *testing.T) {
	fareKeys := [][]string{{""}, {}}
	baggage := [][]string{{"1p1p23p"}, {}}

	ctx := context.Background()
	result := ss.BuildBaggage(ctx, fareKeys, fareFamiliesOne, baggage, baggageTariff)

	require.True(t, result.Included)
	require.Equal(t, uint32(1), result.GetPieces())
	require.Equal(t, uint32(23), result.GetWeight())
}

func TestBuildCarryOn_WithoutOneFareFamily(t *testing.T) {
	fareKeys := [][]string{{key1}, {key2}}
	route := [][]string{{route1}, {route2}}

	ctx := context.Background()
	result := ss.BuildCarryOn(ctx, fareKeys, fareFamiliesOne, route, flights, companyTariff)

	require.True(t, result.Included)
	require.Equal(t, uint32(5), result.GetWeight())
}

func TestBuildRefunded_WithoutOneFareFamily(t *testing.T) {
	fareKeys := [][]string{{key1}, {key2}}

	ctx := context.Background()
	result := ss.BuildRefund(ctx, fareKeys, fareFamiliesOne)

	require.Equal(t, aviaAPI.Snippet_REFUND_AVAILABILITY_NOT_AVAILABLE, result.Availability)
	require.Nil(t, result.Price)
}

var fareFamiliesNotAvailable = map[string]aviatdapiclient.FareFamily{
	key1: {
		Key: key1,
		Terms: []aviatdapiclient.FFTerm{
			createTermBaggage(0, 0),
			createTermCarryOn(0, 0),
			createTermRefunded(aviatdapiclient.AvailabilityTypeNotAvailable, ""),
		},
	},
}

func TestBuildBaggage_NotAvailable(t *testing.T) {
	fareKeys := [][]string{{key1}, {key2}}
	baggage := [][]string{{"0p0p0p"}, {"0p0p0p"}}

	ctx := context.Background()
	result := ss.BuildBaggage(ctx, fareKeys, fareFamiliesNotAvailable, baggage, baggageTariff)

	require.False(t, result.Included)
	require.Equal(t, uint32(0), result.GetPieces())
	require.Equal(t, uint32(0), result.GetWeight())
}

func TestBuildCarryOn_NotAvailable(t *testing.T) {
	fareKeys := [][]string{{key1}, {key2}}
	route := [][]string{{route1}, {route2}}

	ctx := context.Background()
	result := ss.BuildCarryOn(ctx, fareKeys, fareFamiliesNotAvailable, route, flights, companyTariff)

	require.False(t, result.Included)
	require.Equal(t, uint32(0), result.GetWeight())
}

func TestBuildRefunded_NotAvailable(t *testing.T) {
	fareKeys := [][]string{{key1}, {key2}}

	ctx := context.Background()
	result := ss.BuildRefund(ctx, fareKeys, fareFamiliesNotAvailable)

	require.Equal(t, aviaAPI.Snippet_REFUND_AVAILABILITY_NOT_AVAILABLE, result.Availability)
	require.Nil(t, result.Price)
}
