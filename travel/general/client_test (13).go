package pathfinderproxy

import (
	"bytes"
	"context"
	"io/ioutil"
	"net/http"
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/log/zap"
	"a.yandex-team.ru/travel/library/go/httputil/client"
	"a.yandex-team.ru/travel/trains/library/go/httputil/clients/pathfinderproxy/models"
)

const IsolatedRun = true

func TestClient(t *testing.T) {
	logger, _ := zap.New(zap.ConsoleConfig(log.InfoLevel))
	ctx := context.Background()
	cfg := Config{
		BaseURL:      "https://production.pathfinder-proxy.rasp.common.yandex.net/",
		Timeout:      DefaultConfig.Timeout,
		StateTimeout: DefaultConfig.StateTimeout,
	}

	t.Run("TransferVariantsWithPrices", func(t *testing.T) {
		request := models.TransferVariantsWithPricesRequest{
			PointFrom:      "s9607699",
			PointTo:        "s9609329",
			When:           "2021-01-19",
			Language:       "ru",
			TransportTypes: []string{"train"},
			IsBot:          false,
		}

		var transport http.RoundTripper = nil
		if IsolatedRun {
			transport = client.TransportMock(func(req *http.Request) (*http.Response, error) {
				assert.Equal(t,
					"language=ru&pointFrom=s9607699&pointTo=s9609329&transportType=train&when=2021-01-19",
					req.URL.RawQuery)

				respBody, err := ioutil.ReadFile("gotest/transfers_with_prices_response.json")
				assert.NoError(t, err)

				return &http.Response{
					StatusCode: http.StatusOK,
					Body:       ioutil.NopCloser(bytes.NewBuffer(respBody)),
					Header:     make(http.Header),
				}, nil
			})
		}

		client, err := NewPathfinderProxyClientWithTransport(ctx, &cfg, logger, transport)
		assert.NoError(t, err)

		response, err := client.TransfersWithPrices(ctx, &request, http.Header{})
		assert.NoError(t, err)
		assert.NotEmpty(t, response.Status)
		assert.Len(t, response.TransferVariants, 2)
		assert.Equal(t, "RUB",
			response.TransferVariants[1].Segments[0].Tariffs.Classes.Compartment.ServicePrice.Currency)
	})
}
