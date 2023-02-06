package travelapiclient

import (
	"embed"
	"net/http"
	"time"

	"github.com/jarcoal/httpmock"

	"a.yandex-team.ru/library/go/core/log/nop"
	nopmetrics "a.yandex-team.ru/library/go/core/metrics/nop"
	"a.yandex-team.ru/travel/app/backend/internal/common"
)

//go:embed testdata/*.json
var testData embed.FS

func buildClient() *HTTPClient {
	return NewHTTPClient(
		&nop.Logger{},
		common.TestingEnv,
		TravelAPIConfig{
			BaseURL: "https://example.com",
			Timeout: time.Second,
		},
		false,
		nopmetrics.Registry{},
	)
}

func mockResponseFromJSONString(status int, response string) *http.Response {
	resp := httpmock.NewStringResponse(status, response)
	resp.Header[http.CanonicalHeaderKey("Content-Type")] = []string{"application/json"}
	return resp
}

func intP(i int64) *int64 {
	return &i
}

func uintP(i uint64) *uint64 {
	return &i
}
