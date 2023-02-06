package calendarclient

import (
	"context"
	"net/http"
	"testing"

	"github.com/jarcoal/httpmock"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/core/log/nop"
	nopmetrics "a.yandex-team.ru/library/go/core/metrics/nop"
)

const exampleResponse = `
{
  "holidays": [
	{
      "date": "2020-12-27",
      "type": "weekend",
      "name": "День спасателя"
    },
    {
      "date": "2020-12-28",
      "type": "weekday"
    },
    {
      "date": "2020-12-29",
      "type": "weekday"
    },
    {
      "date": "2020-12-30",
      "type": "weekday"
    },
    {
      "date": "2020-12-31",
      "type": "holiday",
      "name": "Новый год"
    },
    {
      "date": "2021-01-01",
      "type": "holiday",
      "name": "Новогодние каникулы"
    }
  ]
}
`

func buildClient() *HTTPClient {
	return NewHTTPClient(&nop.Logger{}, Config{
		BaseURL: "https://example.com",
	}, nopmetrics.Registry{})
}

func mockResponseFromJSONString(status int, response string) *http.Response {
	resp := httpmock.NewStringResponse(status, response)
	resp.Header[http.CanonicalHeaderKey("Content-Type")] = []string{"application/json"}
	return resp
}

func TestGetHolidays(t *testing.T) {
	client := buildClient()

	httpmock.ActivateNonDefault(client.httpClient.GetClient())
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder("GET", "https://example.com/internal/get-holidays",
		httpmock.ResponderFromResponse(mockResponseFromJSONString(200, exampleResponse)))

	res, err := client.GetHolidays(context.Background(), Date{}, Date{})
	require.NoError(t, err)

	newDay := func(dateStr, typeStr string) Holiday {
		return Holiday{
			Date: dateStr,
			Type: typeStr,
		}
	}
	expected := &HolidaysRsp{
		Holidays: []Holiday{
			newDay("2020-12-27", "weekend"),
			newDay("2020-12-28", "weekday"),
			newDay("2020-12-29", "weekday"),
			newDay("2020-12-30", "weekday"),
			newDay("2020-12-31", "holiday"),
			newDay("2021-01-01", "holiday"),
		},
	}

	assert.Equal(t, expected, res)
}
