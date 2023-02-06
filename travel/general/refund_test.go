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

func TestHTTPClient_PostRefund(t *testing.T) {
	postRefund := func(responseStatus int, responseBody string) (*wpb.TRefund, *wpb.TExplanation, error) {
		logger, _ := logging.New(&logging.DefaultConfig)
		client, err := NewClientWithTransport(
			&Config{APIURL: "http://mock"},
			10,
			logger,
			mock.TransportMock(func(req *http.Request) *http.Response {
				assert.Equal(t, "POST", req.Method)
				assert.Equal(t, "/etraffic/tickets/ticketID/refund", req.URL.Path)

				body, err := ioutil.ReadAll(req.Body)
				assert.NoError(t, err)
				assert.Equal(t, []byte{}, body)

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

		return client.PostRefund("ticketID")
	}

	t.Run("refund mapping", func(t *testing.T) {
		refund, _, err := postRefund(http.StatusOK, `{"result": {"price": 100}}`)

		require.NoError(t, err)
		assertpb.Equal(t, &wpb.TRefund{
			Price: &tpb.TPrice{Amount: 10000, Currency: tpb.ECurrency_C_RUB, Precision: pricePrecision},
		}, refund)
	})

	for _, testCase := range errorTestCases {
		t.Run(testCase.Name, func(t *testing.T) {
			refund, _, err := postRefund(testCase.ResponseStatus, testCase.ResponseBody)

			assert.Nil(t, refund)
			assert.True(t, testCase.Check(t, err))
		})
	}
}
