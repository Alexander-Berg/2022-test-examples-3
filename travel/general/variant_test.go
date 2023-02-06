package searchresult

import (
	"fmt"
	"net/url"
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/avia/library/proto/common/v1"
	result "a.yandex-team.ru/travel/avia/library/proto/search_result/v1"
	travel_commons_proto "a.yandex-team.ru/travel/proto"
)

func TestOrderLinkForVariant(t *testing.T) {
	claim := assert.New(t)
	searchResult := &result.Result{
		Qid: "",
		PointFrom: &common.Point{
			Type: common.PointType_POINT_TYPE_SETTLEMENT,
			Id:   213,
		},
		PointTo: &common.Point{
			Type: common.PointType_POINT_TYPE_SETTLEMENT,
			Id:   239,
		},
		DateForward: &travel_commons_proto.TDate{
			Year:  2021,
			Month: 9,
			Day:   1,
		},
		ServiceClass: common.ServiceClass_SERVICE_CLASS_ECONOMY,
		Passengers: &common.Passengers{
			Adults:   1,
			Children: 0,
			Infants:  0,
		},
		Flights: map[string]*result.Flight{
			"flightkeyWZ125": &result.Flight{
				Key:            "flightkeyWZ125",
				Number:         "WZ 125",
				LocalDeparture: "2021-09-01T00:15:00",
			},
		},
		Variants: []*result.Variant{
			{
				PartnerCode: "",
				Charter:     false,
				SelfConnect: false,
				Forward: []*result.FlightSegment{
					{
						FlightKey: "flightkeyWZ125",
						Baggage: &common.Baggage{
							Included: true,
						},
					},
				},
			},
		},
	}
	u, err := url.Parse(NewVariantURL(searchResult, searchResult.Variants[0]).OrderURL())
	claim.NoError(err)

	claim.Equal(url.Values{
		"fromId":         {"c213"},
		"toId":           {"c239"},
		"when":           {"2021-09-01"},
		"return_date":    {""},
		"adult_seats":    {"1"},
		"children_seats": {"0"},
		"infant_seats":   {"0"},
		"klass":          {"economy"},
		"oneway":         {"1"},
		"forward":        {"WZ 125.2021-09-01T00:15"},
		"backward":       {""},
		"baggage":        {"1"},
	}, u.Query())
	claim.Equal(TravelHostProduction, u.Host)
}

func Example_onewayOrderLinkForVariant() {
	// https://travel-test.yandex.ru/avia/order/?adult_seats=1&backward=&children_seats=0&forward=WZ%20125.2021-09-01T00%3A15&fromId=c213&infant_seats=0&klass=economy&oneway=1&return_date=&toId=c239&when=2021-09-01#empty

	q := url.Values{
		"fromId":         {"c213"},
		"toId":           {"c239"},
		"when":           {"2021-09-01"},
		"return_date":    {""},
		"adult_seats":    {"1"},
		"children_seats": {"0"},
		"infant_seats":   {"0"},
		"klass":          {"economy"},
		"oneway":         {"1"},
		"forward":        {"WZ 125.2021-09-01T00:15"},
		"backward":       {""},
	}
	fmt.Println((&url.URL{
		Scheme:     "https",
		Host:       "travel-test.yandex.ru",
		Path:       "avia/order/",
		ForceQuery: false,
		RawQuery:   q.Encode(),
		Fragment:   "empty",
	}).String())
}

func Example_roundtripOrderLinkForVariant() {
	// https://travel-test.yandex.ru/avia/order/?adult_seats=1&backward=S7%202042.2021-09-08T09%3A30%2CS7%201175.2021-09-08T14%3A55&children_seats=0&forward=S7%201176.2021-09-01T20%3A05%2CS7%202051.2021-09-01T23%3A55&fromId=c54&infant_seats=0&klass=economy&oneway=2&return_date=2021-09-08&toId=c239&when=2021-09-01#empty

	q := url.Values{
		"fromId":         {"c54"},
		"toId":           {"c239"},
		"when":           {"2021-09-01"},
		"return_date":    {"2021-09-08"},
		"adult_seats":    {"1"},
		"children_seats": {"0"},
		"infant_seats":   {"0"},
		"klass":          {"economy"},
		"oneway":         {"2"},
		"forward":        {"S7 1176.2021-09-01T20:05,S7 2051.2021-09-01T23:55"},
		"backward":       {"S7 2042.2021-09-08T09:30,S7 1175.2021-09-08T14:55"},
	}
	fmt.Println((&url.URL{
		Scheme:     "https",
		Host:       "travel-test.yandex.ru",
		Path:       "avia/order/",
		ForceQuery: false,
		RawQuery:   q.Encode(),
		Fragment:   "empty",
	}).String())
}
