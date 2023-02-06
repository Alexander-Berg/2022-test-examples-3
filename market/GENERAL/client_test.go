package conductor

import (
	"context"
	"github.com/stretchr/testify/assert"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestClient_hosts2groups(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(rw http.ResponseWriter, req *http.Request) {
		assert.Equal(t, "/api/hosts2groups/wms-app03ht.market.yandex.net,alpha.market.yandex.net?format=json", req.URL.String())
		_, _ = rw.Write([]byte(`[{"name":"market_corba-testing-stable"},{"name":"cs_all"},{"name":"cs_deb-testing"},{"name":"cs_deb-stable"},{"name":"cs_deb"},{"name":"cs_deb-deploy"},{"name":"market_master"},{"name":"market_wms"},{"name":"market_wms-testing"}]`))
	}))
	defer server.Close()
	client := NewClient(server.URL, server.Client(), "token")
	groups, _, err := client.Hosts2groups(context.TODO(), []string{"wms-app03ht.market.yandex.net", "alpha.market.yandex.net"})
	assert.NoError(t, err)
	assert.NotEmpty(t, groups)
	assert.Len(t, groups, 9)
}

func TestClient_groups2hosts(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(rw http.ResponseWriter, req *http.Request) {
		assert.Equal(t, "/api/groups2hosts/wms-rst-app-stable-primary,wms-sof-app-stable-primary?format=json", req.URL.String())
		_, _ = rw.Write([]byte(`[{"fqdn":"market-wms-wh101-app01.market.yandex.net"},{"fqdn":"market-wms-wh301-app01.market.yandex.net"}]`))
	}))
	defer server.Close()
	client := NewClient(server.URL, server.Client(), "token")
	groups, _, err := client.Groups2hosts(context.TODO(), []string{"wms-rst-app-stable-primary", "wms-sof-app-stable-primary"})
	assert.NoError(t, err)
	assert.NotEmpty(t, groups)
	assert.Len(t, groups, 2)
}

func TestClient_GetHostTags(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(rw http.ResponseWriter, req *http.Request) {
		assert.Equal(t, "/api/get_host_tags/wms-app03ht.market.yandex.net?format=json", req.URL.String())
		_, _ = rw.Write([]byte(`["cauth_create_homes","cauth_root_keys","ipvs_tun_communal","skynet_full","yasm_monitored","yndxjumbo"]`))
	}))
	defer server.Close()
	client := NewClient(server.URL, server.Client(), "token")
	tags, _, err := client.GetHostTags(context.Background(), "wms-app03ht.market.yandex.net")
	assert.NoError(t, err)
	assert.NotEmpty(t, tags)
	assert.Len(t, tags, 6)
}

func TestNewClient(t *testing.T) {
	client := NewClient("", nil, "token")
	assert.NotNil(t, client)
}
