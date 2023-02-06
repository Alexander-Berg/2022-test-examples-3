package sandbox

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/stretchr/testify/require"
)

func getServerWithResponse(response *ResourceResponse, status int) *httptest.Server {
	return httptest.NewServer(
		http.HandlerFunc(
			func(res http.ResponseWriter, req *http.Request) {
				res.WriteHeader(status)
				_ = json.NewEncoder(res).Encode(response)
			},
		),
	)
}

func TestCorrectSandboxAnswer(t *testing.T) {
	sandboxResponse := &ResourceResponse{
		Items: make([]ResourceItems, 1),
	}
	server := getServerWithResponse(sandboxResponse, http.StatusOK)
	defer server.Close()

	sandboxClient := NewHTTPClient(server.URL, server.Client(), "")

	response, err := sandboxClient.GetResources(
		ResourceQuery{
			Limit:  1,
			Offset: 1,
			Order:  []string{"id"},
			Type:   "SomeType",
		},
	)

	require.NoError(t, err)
	require.Equal(t, response, sandboxResponse)
}

func TestCorrectSandboxAnswer_LimitIsRequire(t *testing.T) {
	sandboxResponse := &ResourceResponse{
		Items: make([]ResourceItems, 1),
	}
	server := getServerWithResponse(sandboxResponse, http.StatusOK)
	defer server.Close()

	sandboxClient := NewHTTPClient(server.URL, server.Client(), "")

	_, err := sandboxClient.GetResources(ResourceQuery{})

	require.Error(t, err)
}

func TestIncorrectSandboxAnswer(t *testing.T) {
	server := getServerWithResponse(nil, http.StatusInternalServerError)
	defer server.Close()

	sandboxClient := NewHTTPClient(server.URL, server.Client(), "")

	_, err := sandboxClient.GetResources(ResourceQuery{})

	require.Error(t, err)
}
