package aviatdapiclient

import (
	"context"
	"net/http"
	"testing"
	"time"

	"github.com/go-resty/resty/v2"
	"github.com/jarcoal/httpmock"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/core/log/nop"
	nopmetrics "a.yandex-team.ru/library/go/core/metrics/nop"
)

func buildClient() (*resty.Client, *HTTPClient) {
	restyClient := resty.New()
	config := DefaultConfig
	config.BaseURL = "https://example.com"
	client := NewHTTPClient(&nop.Logger{}, config, false, nopmetrics.Registry{})
	client.restyClientBuilder = func(*HTTPClient) *resty.Client {
		return restyClient
	}
	return restyClient, client
}

func TestHTTPClient_InitSearch_Success(t *testing.T) {
	restyClient, client := buildClient()

	httpmock.ActivateNonDefault(restyClient.GetClient())
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder(
		"GET",
		"https://example.com/jsendapi/init_search/",
		httpmock.ResponderFromResponse(mockResponseFromJSONString(
			200,
			`{"status":"success","data":{"qid":"220119-234926-681.travelapp.plane.s9600215_c2_2022-10-10_None_economy_1_0_0_ru.ru","queries":[{"partners":["ozon","agent"],"qid":"220119-234926-681.travelapp.plane.c213_c2_2022-10-10_None_economy_1_0_0_ru.ru"}],"partners":["kiwi","pobeda"]}}`,
		)),
	)

	actual, err := client.InitSearch(
		context.Background(),
		"ru",
		"ru",
		1,
		0,
		0,
		Date{Time: time.Date(2022, 10, 10, 0, 0, 0, 0, time.UTC)},
		nil,
		EconomyServiceClass,
		"s9600215",
		"c2",
	)
	require.NoError(t, err)
	expected := &InitSearchRsp{
		Status: SuccessResponseStatus,
		Data: InitSearchData{
			QID: "220119-234926-681.travelapp.plane.s9600215_c2_2022-10-10_None_economy_1_0_0_ru.ru",
		},
	}
	require.EqualValues(t, expected, actual)
	require.EqualValues(t, 1, httpmock.GetTotalCallCount())
}

func TestInitSearch_SuccessAfterRetry(t *testing.T) {
	restyClient, client := buildClient()

	httpmock.ActivateNonDefault(restyClient.GetClient())
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder(
		"GET",
		"https://example.com/jsendapi/init_search/",
		httpmock.ResponderFromMultipleResponses(
			[]*http.Response{
				mockResponseFromJSONString(499, "{}"),
				mockResponseFromJSONString(
					200,
					`{"status":"success","data":{"qid":"220119-234926-681.travelapp.plane.c213_c2_2022-10-10_None_economy_1_0_0_ru.ru","queries":[{"partners":["ozon","agent"],"qid":"220119-234926-681.travelapp.plane.c213_c2_2022-10-10_None_economy_1_0_0_ru.ru"}],"partners":["kiwi","pobeda"]}}`,
				),
			},
		),
	)

	actual, err := client.InitSearch(
		context.Background(),
		"ru",
		"ru",
		1,
		0,
		0,
		Date{Time: time.Date(2022, 10, 10, 0, 0, 0, 0, time.UTC)},
		nil,
		EconomyServiceClass,
		"c213",
		"c2",
	)

	require.NoError(t, err)
	expected := &InitSearchRsp{
		Status: SuccessResponseStatus,
		Data: InitSearchData{
			QID: "220119-234926-681.travelapp.plane.c213_c2_2022-10-10_None_economy_1_0_0_ru.ru",
		},
	}
	require.EqualValues(t, expected, actual)
	require.EqualValues(t, 2, httpmock.GetTotalCallCount())
}

func mockResponseFromJSONString(status int, response string) *http.Response {
	resp := httpmock.NewStringResponse(status, response)
	resp.Header[http.CanonicalHeaderKey("Content-Type")] = []string{"application/json"}
	return resp
}
