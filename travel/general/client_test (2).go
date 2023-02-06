package aviasuggestclient

import (
	"context"
	"net/http"
	"testing"

	"github.com/jarcoal/httpmock"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/core/log/nop"
	nopmetrics "a.yandex-team.ru/library/go/core/metrics/nop"
)

func buildClient() *HTTPClient {
	config := DefaultConfig
	config.BaseURL = "https://example.com"
	return NewHTTPClient(&nop.Logger{}, config, false, nopmetrics.Registry{})
}

const successResponseWithAirports = `[
  "москв",
  [
    [
      0,
      "Москва",
      {
        "point_key": "c213",
        "point_code": "MOW",
        "region_title": "Москва и Московская область",
        "city_title": "Москва",
        "country_title": "Россия",
        "missprint": 0,
        "hidden": 0,
        "have_airport": 1,
        "added": 0,
        "have_not_hidden_airport": 1
      },
      [
        [
          1,
          "Домодедово",
          {
            "point_key": "s9600216",
            "point_code": "DME",
            "region_title": "Москва и Московская область",
            "city_title": "Москва",
            "country_title": "Россия",
            "missprint": 0,
            "hidden": 0,
            "have_airport": 1,
            "added": 1,
            "have_not_hidden_airport": 1
          },
          [],
          1234
        ],
        [
          1,
          "Шереметьево",
          {
            "point_key": "s9600213",
            "point_code": "SVO",
            "region_title": "Москва и Московская область",
            "city_title": "Москва",
            "country_title": "Россия",
            "missprint": 0,
            "hidden": 0,
            "have_airport": 1,
            "added": 1,
            "have_not_hidden_airport": 1
          },
          [],
          1234
        ]
      ],
      1234
    ]
  ]
]
`

func TestHTTPClient_Success(t *testing.T) {
	client := buildClient()

	httpmock.ActivateNonDefault(client.httpClient.GetClient())
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder(
		"GET",
		"https://example.com/v2/avia",
		httpmock.ResponderFromResponse(mockResponseFromJSONString(200, successResponseWithAirports)),
	)

	actual, err := client.Suggest(
		context.Background(),
		"ru",
		"ru",
		FieldFrom,
		"москв",
		"",
		"",
	)
	require.NoError(t, err)
	expected := &SuggestResponse{
		Query: "москв",
		Suggests: []Suggest{
			{
				Level:                0,
				Title:                "Москва",
				PointKey:             "c213",
				PointCode:            "MOW",
				RegionTitle:          "Москва и Московская область",
				CityTitle:            "Москва",
				CountryTitle:         "Россия",
				Missprint:            0,
				Hidden:               Bool{false},
				HaveAirport:          Bool{true},
				HaveNotHiddenAirport: Bool{true},
				Nested: []Suggest{
					{
						Level:                1,
						Title:                "Домодедово",
						PointKey:             "s9600216",
						PointCode:            "DME",
						RegionTitle:          "Москва и Московская область",
						CityTitle:            "Москва",
						CountryTitle:         "Россия",
						Missprint:            0,
						Hidden:               Bool{false},
						HaveAirport:          Bool{true},
						HaveNotHiddenAirport: Bool{true},
						Nested:               []Suggest{},
					},
					{
						Level:                1,
						Title:                "Шереметьево",
						PointKey:             "s9600213",
						PointCode:            "SVO",
						RegionTitle:          "Москва и Московская область",
						CityTitle:            "Москва",
						CountryTitle:         "Россия",
						Missprint:            0,
						Hidden:               Bool{false},
						HaveAirport:          Bool{true},
						HaveNotHiddenAirport: Bool{true},
						Nested:               []Suggest{},
					},
				},
			},
		},
	}
	require.Equal(t, expected, actual)

}

func mockResponseFromJSONString(status int, response string) *http.Response {
	resp := httpmock.NewStringResponse(status, response)
	resp.Header[http.CanonicalHeaderKey("Content-Type")] = []string{"application/json"}
	return resp
}
