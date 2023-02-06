package featureflag

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/library/go/core/log"
	aviaLogging "a.yandex-team.ru/travel/avia/library/go/logging"
	"a.yandex-team.ru/travel/library/go/containers"
)

var (
	logger, _ = aviaLogging.NewLogger(&aviaLogging.Config{
		Level: log.InfoLevel,
	})
)

func getServerWithResponse(response *APIResponse) *httptest.Server {
	return httptest.NewServer(http.HandlerFunc(func(res http.ResponseWriter, req *http.Request) {
		res.WriteHeader(http.StatusOK)
		_ = json.NewEncoder(res).Encode(response)
	}))
}

func getServerWithRawResponse(status int, response string) *httptest.Server {
	return httptest.NewServer(http.HandlerFunc(func(res http.ResponseWriter, _ *http.Request) {
		res.WriteHeader(status)
		_, _ = res.Write([]byte(response))
	}))
}

func getServerWithFailResponse() *httptest.Server {
	return httptest.NewServer(http.HandlerFunc(func(res http.ResponseWriter, req *http.Request) {
		res.WriteHeader(http.StatusInternalServerError)
		_ = json.NewEncoder(res).Encode(map[string]string{"error": "some error text"})
	}))
}

func TestCreateFlags_FeatureFlagApiResponseWithoutABFlags_ShouldReturnsFlags(t *testing.T) {
	featureFlagAPIResponse := &APIResponse{
		Flags:   containers.SetOf("flag1", "flag2"),
		ABFlags: nil,
	}
	server := getServerWithResponse(featureFlagAPIResponse)
	defer server.Close()

	featureFlagClient := NewClient(server.URL, "serviceCode", logger, server.Client())

	flags, err := featureFlagClient.CreateFlags()

	assert.NoError(t, err)
	assert.True(t, flags.IsFlagEnabled("flag1"))
	assert.True(t, flags.IsFlagEnabled("flag2"))
}

func TestCreateFlags_FeatureFlagApiResponseWithoutABFlags_ShouldReturnsFlags_Raw(t *testing.T) {
	server := getServerWithRawResponse(
		200,
		`{"flags":["flag1", "flag2"],"abFlags":[],"lastUpdate":"2022-01-19T13:52:43+03:00"}`,
	)
	defer server.Close()

	featureFlagClient := NewClient(server.URL, "serviceCode", logger, server.Client())

	flags, err := featureFlagClient.CreateFlags()

	assert.NoError(t, err)
	assert.True(t, flags.IsFlagEnabled("flag1"))
	assert.True(t, flags.IsFlagEnabled("flag2"))
}

func TestCreateFlags_FeatureFlagApiResponseWithABFlags_ShouldReturnsFlags(t *testing.T) {
	abFlagsSet := containers.SetOf("flag1", "flag2", "flag4")
	featureFlagAPIResponse := &APIResponse{
		Flags:   containers.SetOf("flag1", "flag2", "flag3"),
		ABFlags: &abFlagsSet,
	}
	ctx := WithABFlags(context.Background(), "flag1", "flag4")
	server := getServerWithResponse(featureFlagAPIResponse)
	defer server.Close()

	featureFlagClient := NewClient(server.URL, "serviceCode", logger, server.Client())

	flags, err := featureFlagClient.CreateFlags()

	assert.NoError(t, err)

	assert.True(t, flags.IsFlagEnabled("flag1"))
	assert.True(t, flags.IsFlagEnabled("flag2"))
	assert.True(t, flags.IsFlagEnabled("flag3"))
	assert.False(t, flags.IsFlagEnabled("flag4"))

	assert.True(t, flags.IsFlagEnabledWithAB(ctx, "flag1"))
	assert.False(t, flags.IsFlagEnabledWithAB(ctx, "flag2"))
	assert.True(t, flags.IsFlagEnabledWithAB(ctx, "flag3"))
	assert.True(t, flags.IsFlagEnabledWithAB(ctx, "flag4"))
}

func TestCreateFlags_FeatureFlagApiResponseWithABFlagsOldWay_ShouldReturnsFlags(t *testing.T) {
	abFlagsSet := containers.SetOf("flag1", "flag2", "flag4")
	featureFlagAPIResponse := &APIResponse{
		Flags:   containers.SetOf("flag1", "flag2", "flag3"),
		ABFlags: &abFlagsSet,
	}
	ab := NewABFlagsKV(map[string]string{
		"flag1": "1",
		"flag2": "0",
		"flag3": "0",
		"flag4": "1",
	})
	server := getServerWithResponse(featureFlagAPIResponse)
	defer server.Close()

	featureFlagClient := NewClient(server.URL, "serviceCode", logger, server.Client())

	flags, err := featureFlagClient.CreateFlags()

	assert.NoError(t, err)

	assert.True(t, flags.IsFlagEnabled("flag1"))
	assert.True(t, flags.IsFlagEnabled("flag2"))
	assert.True(t, flags.IsFlagEnabled("flag3"))
	assert.False(t, flags.IsFlagEnabled("flag4"))

	assert.True(t, flags.IsFlagEnabledABOverride("flag1", ab))
	assert.False(t, flags.IsFlagEnabledABOverride("flag2", ab))
	assert.True(t, flags.IsFlagEnabledABOverride("flag3", ab))
	assert.True(t, flags.IsFlagEnabledABOverride("flag4", ab))
}

func TestCreateFlags_FeatureFlagApiFail_ShouldReturnsError(t *testing.T) {
	server := getServerWithFailResponse()
	defer server.Close()
	featureFlagClient := NewClient(server.URL, "serviceCode", logger, server.Client())

	_, err := featureFlagClient.CreateFlags()

	assert.Error(t, err)
}
