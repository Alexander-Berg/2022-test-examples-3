package runner

import (
	"fmt"
	"github.com/go-resty/resty/v2"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"net/http"
	"net/http/httptest"
	"net/url"
	"testing"
	"time"
)

func TestCustomHosts(t *testing.T) {
	var err error
	ts := httptest.NewTLSServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		_, _ = w.Write([]byte("done"))
	}))
	u := new(url.URL)
	u, _ = u.Parse(ts.URL)
	tlsConfig := ts.Client().Transport.(*http.Transport).TLSClientConfig.Clone()

	h := Hosts{}
	h["example.com"] = u.Hostname()

	client := resty.NewWithClient(ts.Client()).
		SetTimeout(time.Second).
		SetTransport(setCustomHosts(h)).
		SetTLSClientConfig(tlsConfig)

	response, err := client.R().Get(u.String())
	require.NoError(t, err)
	assert.Equal(t, "done", response.String())

	u.Host = fmt.Sprintf("example.com:%s", u.Port())

	response, err = client.R().Get(u.String())
	require.NoError(t, err)
	assert.Equal(t, "done", response.String())
}
