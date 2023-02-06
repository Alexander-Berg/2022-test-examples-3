package aviabackendclient

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

func TestGeoLookupSuccess(t *testing.T) {
	client := buildClient()

	httpmock.ActivateNonDefault(client.httpClient.GetClient())
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder(
		"POST",
		"https://example.com/?lang=ru&name=geoLookup&national_version=ru",
		httpmock.ResponderFromResponse(mockResponseFromJSONString(
			200,
			`{"status":"success","data":[{"searchCity":{"title":"Москва","geoId":213,"key":"c213","id":213,"code":"MOW","iataCode":"MOW","sirenaCode":"МОВ"}}]}`,
		)),
	)

	actual, err := client.GeoLookup(context.Background(), "ru", "ru", 213)
	require.NoError(t, err)
	expected := &GeoLookupRsp{
		Status: SuccessResponseStatus,
		Data: []ResponseDataElement{
			{
				SearchCity: SearchCityResult{
					Title:      "Москва",
					GeoID:      213,
					Key:        "c213",
					ID:         213,
					Code:       "MOW",
					IATACode:   "MOW",
					SirenaCode: "МОВ",
				},
			},
		},
	}
	require.Equal(t, expected, actual)
}

func TestTopFlightsSuccess(t *testing.T) {
	client := buildClient()

	httpmock.ActivateNonDefault(client.httpClient.GetClient())
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder(
		"POST",
		"https://example.com/?lang=ru&name=topFlights&national_version=ru",
		httpmock.ResponderFromResponse(mockResponseFromJSONString(
			200,
			`{"status":"success","data":[[{"redirects": 2263,"numbers": "DP 435"}]]}`,
		)),
	)

	actual, err := client.TopFlights(context.Background(), "ru", "ru", "c213", "c172", "2022-06-03", 100)
	require.NoError(t, err)
	expected := &TopFlightsRsp{
		Status: SuccessResponseStatus,
		Data: [][]TopFlightElem{{
			{
				Redirects: 2263,
				Numbers:   "DP 435",
			},
		}},
	}
	require.Equal(t, expected, actual)
}

func mockResponseFromJSONString(status int, response string) *http.Response {
	resp := httpmock.NewStringResponse(status, response)
	resp.Header[http.CanonicalHeaderKey("Content-Type")] = []string{"application/json"}
	return resp
}
