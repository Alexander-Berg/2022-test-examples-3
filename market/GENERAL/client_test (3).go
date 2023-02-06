package walle_client_test

import (
	walle_client "a.yandex-team.ru/market/sre/library/golang/walle-client"
	"a.yandex-team.ru/market/sre/library/golang/walle-client/models/hosts"
	"context"
	"encoding/json"
	"fmt"
	"github.com/go-resty/resty/v2"
	"github.com/stretchr/testify/assert"
	"net/http"
	"net/http/httptest"
	"net/url"
	"strings"
	"testing"
)

func setupTestServer(ssl bool, handler http.Handler) (*httptest.Server, *url.URL) {
	var ts *httptest.Server
	if ssl {
		ts = httptest.NewTLSServer(handler)
	} else {
		ts = httptest.NewServer(handler)
	}
	u := new(url.URL)
	u, _ = u.Parse(ts.URL)
	return ts, u
}

func pingHandler() http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
	}
}

func hostsHandler() http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		res := hosts.Result{}
		fields := r.URL.Query().Get("fields")
		fieldSet := strings.Split(fields, ",")
		for _, v := range fieldSet {
			res[v] = "test_value"
		}
		bytes, err := json.Marshal(res)
		if err != nil {
			fmt.Errorf("JSON marshalling error: %w", err)
			return
		}
		w.Header().Set("Status", "200")
		w.Write(bytes)
	}
}

func hostsYaGdeHandler() http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		res := hosts.Result{}
		fields := r.URL.Query().Get("fields")
		fieldSet := strings.Split(fields, ",")
		for _, v := range fieldSet {
			res[v] = "test_value"
		}
		bytes, err := json.Marshal(res)
		if err != nil {
			fmt.Errorf("JSON marhalling error: %w", err)
			return
		}
		w.Header().Set("Status", "200")
		w.Write(bytes)
	}
}

func TestClient_Ping(t *testing.T) {
	ctx := context.Background()
	ts, tsUrl := setupTestServer(true, pingHandler())
	ok, err := walle_client.NewCustomClient(tsUrl.String(), "", resty.NewWithClient(ts.Client())).Ping(ctx)
	t.Logf("%v", ok)
	assert.Nil(t, err, "error is not nil but %v", err)
	assert.Equal(t, true, ok, "ok is not true but: %v", ok)
}

func TestClient_HostInfoByFQDN(t *testing.T) {
	ctx := context.Background()
	ts, tsUrl := setupTestServer(true, hostsHandler())
	withFields := hosts.WithFields(hosts.Fields.Inv, hosts.Fields.Config)
	res, err := walle_client.NewCustomClient(
		tsUrl.String(),
		"",
		resty.NewWithClient(ts.Client())).
		HostInfoByFQDN(ctx, "example.com", withFields)
	assert.Nil(t, err, "err is not nil but: %v", err)
	assert.NotNil(t, res, "res is nil!")
	assert.Equal(t, 2, len(*res), "response keys count not equal to expected")
}

func TestClient_HostInfoYaGde(t *testing.T) {
	ctx := context.Background()
	ts, tsUrl := setupTestServer(true, hostsYaGdeHandler())
	withFields := hosts.WithFields(
		hosts.Fields.Name,
		hosts.Fields.LocationUnit,
		hosts.Fields.LocationDatacenter,
		hosts.Fields.LocationSwitch,
		hosts.Fields.LocationPort,
		hosts.Fields.LocationShortDatacenterName,
		hosts.Fields.LocationRack,
		hosts.Fields.LocationQueue,
		hosts.Fields.LocationCity,
		hosts.Fields.LocationNetworkSource,
		hosts.Fields.Config,
		hosts.Fields.Project,
		)
	res, err := walle_client.NewCustomClient(
		tsUrl.String(),
		"",
		resty.NewWithClient(ts.Client())).
		HostInfoByFQDN(ctx, "example.com", withFields)
	assert.Nil(t, err, "err is not nil but: %v", err)
	assert.NotNil(t, res, "res is nil!")
	assert.Equal(t, 12, len(*res), "response keys count not equal to expected")
}
