package connector

import (
	"bytes"
	"io/ioutil"
	"net/http"
	"testing"

	"a.yandex-team.ru/library/go/test/assertpb"
	tpb "a.yandex-team.ru/travel/proto"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/travel/buses/backend/internal/common/connector/mock"
	"a.yandex-team.ru/travel/buses/backend/internal/common/logging"
	wpb "a.yandex-team.ru/travel/buses/backend/proto/worker"
)

func TestHTTPClient_GetRefundInfo(t *testing.T) {
	getRefundInfo := func(responseStatus int, responseBody string) (*wpb.TRefundInfo, *wpb.TExplanation, error) {
		logger, _ := logging.New(&logging.DefaultConfig)
		client, err := NewClientWithTransport(
			&Config{APIURL: "http://mock"},
			10,
			logger,
			mock.TransportMock(func(req *http.Request) *http.Response {
				assert.Equal(t, "GET", req.Method)
				assert.Equal(t, "/etraffic/tickets/ticketID/calc-refund", req.URL.Path)

				return &http.Response{
					StatusCode: responseStatus,
					Body:       ioutil.NopCloser(bytes.NewBufferString(responseBody)),
					Request:    req,
					// Must be set to non-nil value or it panics
					Header: make(http.Header),
				}
			}),
		)
		require.NoError(t, err)

		return client.GetRefundInfo("ticketID")
	}

	t.Run("refund_info mapping", func(t *testing.T) {
		info, _, err := getRefundInfo(http.StatusOK, `{"result": {"available": true, "price": 100}}`)

		require.NoError(t, err)
		assertpb.Equal(t, &wpb.TRefundInfo{
			Available: true,
			Price:     &tpb.TPrice{Amount: 10000, Currency: tpb.ECurrency_C_RUB, Precision: pricePrecision},
		}, info)
	})

	for _, testCase := range errorTestCases {
		t.Run(testCase.Name, func(t *testing.T) {
			info, _, err := getRefundInfo(testCase.ResponseStatus, testCase.ResponseBody)

			assert.Nil(t, info)
			assert.True(t, testCase.Check(t, err))
		})
	}
}
