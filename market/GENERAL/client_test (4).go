package juggler

import (
	"context"
	"github.com/stretchr/testify/assert"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestClient_GetCheckStatus(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(rw http.ResponseWriter, req *http.Request) {
		assert.Equal(t, "/v2/checks/get_checks_state", req.URL.String())
		_, _ = rw.Write([]byte(`{
  "items": [
    {
      "status": "CRIT",
      "aggregation_time": 1573751229.457589,
      "downtime_ids": [],
      "description": "Critical: 170 fresh core dumps (fslb01vt.market.yandex.net)",
      "service": "core_dumps",
      "change_time": 1573741382.525044,
      "tags": [
        "market_slb",
        "market",
        "_market_",
        "a_mark_market_slb"
      ],
      "namespace": "market.sre",
      "host": "market_slb",
      "meta": "{}",
      "state_kind": "ACTUAL"
    }
  ],
  "meta": {
    "_backend": "man-prod.juggler.search.yandex.net"
  },
  "response_too_large": false,
  "limit": 200,
  "total": 1,
  "statuses": [
    {
      "status": "CRIT",
      "count": 1
    }
  ]
}`))
	}))
	defer server.Close()
	client := NewClient(server.URL, server.Client(), "token")
	status, err := client.GetCheckStatus(context.TODO(), "market_common", "check_dns")
	assert.NoError(t, err)
	assert.NotNil(t, status)
	assert.NotEqual(t, status, &CheckStatus{})
}
