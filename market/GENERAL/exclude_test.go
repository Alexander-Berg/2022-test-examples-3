package main

import (
	"github.com/stretchr/testify/assert"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestHTTPExcludeProvider_fetch(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(rw http.ResponseWriter, req *http.Request) {
		assert.Equal(t, "/market.excl", req.RequestURI)
		_, _ = rw.Write([]byte(`[
    {
        "service_pattern": ".*",
        "vulnerabilities_excludes": ["YADI-LINUX-UBUNTU-APT-2016-1252"],
        "packages_excludes": ["apt"]
    }
]
`))
	}))
	defer server.Close()
	prov := NewHTTPExcludeProvider(server.URL+"/market.excl", "test_service_name")
	prov.SetHTTPClient(server.Client())
	exclude, err := prov.PackagesExclude()
	assert.NoError(t, err)
	assert.NotEmpty(t, exclude)
}

func TestHTTPExcludeProvider_match(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(rw http.ResponseWriter, req *http.Request) {
		assert.Equal(t, "/market.excl", req.RequestURI)
		_, _ = rw.Write([]byte(`[
    {
        "service_pattern": ".*",
        "vulnerabilities_excludes": ["YADI-LINUX-UBUNTU-APT-2016-1252"],
        "packages_excludes": ["apt"]
    },
    {
        "service_pattern": "^test.*",
        "packages_excludes": ["yes"]
    },
	{
        "service_pattern": "^production.*",
        "packages_excludes": ["no"]
    }
]
`))
	}))
	defer server.Close()
	prov := NewHTTPExcludeProvider(server.URL+"/market.excl", "test_service_name")
	prov.SetHTTPClient(server.Client())
	exclude, err := prov.PackagesExclude()
	assert.NoError(t, err)
	assert.NotEmpty(t, exclude)
	assert.NotContains(t, exclude, "no")
	assert.Contains(t, exclude, "yes")
}
