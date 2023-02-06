package priceindexclient

import (
	"context"
	"net/http"
	"testing"

	"github.com/jarcoal/httpmock"
	"github.com/stretchr/testify/require"
	"google.golang.org/genproto/googleapis/type/date"

	"a.yandex-team.ru/library/go/core/log/nop"
	nopmetrics "a.yandex-team.ru/library/go/core/metrics/nop"
)

func buildClient() *HTTPClient {
	return NewHTTPClient(&nop.Logger{}, &DefaultConfig, false, nopmetrics.Registry{})
}

func mockResponseFromJSONString(status int, response string) *http.Response {
	resp := httpmock.NewStringResponse(status, response)
	resp.Header[http.CanonicalHeaderKey("Content-Type")] = []string{"application/json"}
	return resp
}

const exampleBatchResponse = `
{
    "status": "ok",
    "data": [
        {
            "to_id": 2,
            "forward_date": "2017-11-01",
			"backward_date": "2017-11-25",
            "min_price": {
                "currency": "RUR",
                "value": 3109,
                "baseValue": 3109
            },
            "updatedAt": null,
            "from_id": 54
        },
        {
            "to_id": 3,
            "forward_date": "2017-11-01",
            "min_price": {
                "currency": "RUR",
                "value": 3109,
                "baseValue": 3109
            },
            "updatedAt": "2017-09-01",
            "from_id": 54
        }
    ]
}
`

func TestGetPriceBatch(t *testing.T) {
	client := buildClient()

	httpmock.ActivateNonDefault(client.httpClient.GetClient())
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder("POST", "http://api-gateway.testing.avia.yandex.net/v1/price-index/search_methods/v1/min_price_batch_search/ru",
		httpmock.ResponderFromResponse(mockResponseFromJSONString(200, exampleBatchResponse)))

	req := PriceBatchReq{
		Requests: []*BatchReq{
			{
				FromID: 54,
				ToID:   2,
				DateForward: &date.Date{
					Year:  2017,
					Month: 11,
					Day:   1,
				},
				DateBackward: &date.Date{
					Year:  2017,
					Month: 11,
					Day:   25,
				},
			},
			{
				FromID: 54,
				ToID:   3,
				DateForward: &date.Date{
					Year:  2017,
					Month: 11,
					Day:   1,
				},
			},
		},
	}
	res, err := client.GetPriceBatch(context.Background(), &req)
	require.NoError(t, err)

	bd := "2017-11-25"
	updateAt := "2017-09-01"
	expected := &PriceBatchRsp{
		Status: "ok",
		Data: []PriceBatchData{
			{
				FromID:       54,
				ToID:         2,
				ForwardDate:  "2017-11-01",
				BackwardDate: &bd,
				MinPrice: MinPrice{
					Value:     3109,
					BaseValue: 3109,
					Currency:  "RUR",
				},
			},
			{
				FromID:      54,
				ToID:        3,
				ForwardDate: "2017-11-01",
				MinPrice: MinPrice{
					Value:     3109,
					BaseValue: 3109,
					Currency:  "RUR",
				},
				UpdatedAt: &updateAt,
			},
		},
	}

	require.Equal(t, expected, res)
}
