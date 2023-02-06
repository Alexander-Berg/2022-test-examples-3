package main

import (
	health_ui_client "a.yandex-team.ru/market/infra/market-health/loadtesting/internal/health-ui-client"
	"encoding/json"
	"github.com/jarcoal/httpmock"
	"github.com/stretchr/testify/require"
	"net/http"
	"os"
	"strings"
	"testing"
)

const createNewVersionURL = "https://health-testing.market.yandex-team.ru/api/public/logshatter/config/version/createNewVersion?checkDuplicate=true&publishAndActivate=true"

const oAuthToken = "some_token"

const configNotFoundResponseBody = `{
  "errors" : [ "There is no config with id some_nonexistent_config" ],
  "timestamp" : "2022-06-01T17:41:35.986945Z"
}`

const activeVersionResponseBody = `{
  "meta" : {
    "id" : {
      "configId" : "abo-bpmn",
      "versionNumber" : 0
    },
    "configSource" : "API",
    "versionStatus" : "ACTIVE",
    "author" : "robot-market-infra",
    "modifiedBy" : [ ],
    "createdTime" : 1643285878.109000000,
    "reviews" : [ ]
  },
  "content" : {
    "dataSource" : {
      "logBroker" : {
        "topics" : [ "market-health-stable--other", "market-health-prestable--other", "market-health-testing--other", "market-health-dev--other" ],
        "hostGlob" : "*",
        "pathGlob" : "**/nginx/abo-bpmn-access-tskv.log"
      }
    },
    "parser" : {
      "java" : {
        "className" : "ru.yandex.market.logshatter.parser.nginx.NginxTskvLogParser"
      },
      "params" : {
        "logbroker://market-health-stable" : "PRODUCTION",
        "logbroker://market-health-prestable" : "PRESTABLE",
        "logbroker://market-health-testing" : "TESTING",
        "logbroker://market-health-dev" : "DEVELOPMENT"
      }
    },
    "table" : {
      "table" : "abo_bpmn_nginx"
    },
    "dataRotationDays" : 14
  }
}`

const expectedCreateNewVersionRequestBody = `{
  "id": {
	"configId": "abo-bpmn__market_health_loadtesting"
  },
  "dataSource" : {
    "logBroker" : {
      "topics" : [ "market-health-stable--loadtesting" ],
      "hostGlob" : "*",
      "pathGlob" : "**/nginx/abo-bpmn-access-tskv.log"
    }
  },
  "parser" : {
    "java" : {
      "className" : "ru.yandex.market.logshatter.parser.nginx.NginxTskvLogParser"
    },
    "params" : {
      "logbroker://market-health-stable" : "PRODUCTION",
      "logbroker://market-health-prestable" : "PRESTABLE",
      "logbroker://market-health-testing" : "TESTING",
      "logbroker://market-health-dev" : "DEVELOPMENT"
    }
  },
  "table" : {
    "table" : "abo_bpmn_nginx__market_health_loadtesting"
  },
  "dataRotationDays" : 14
}`

const newVersionSuccessfullyCreatedResponseBody = `{
  "configId" : "abo-bpmn__market_health_loadtesting",
  "versionNumber" : 2
}`

const expectedCreateConfigRequestBody = `{
  "id": "abo-bpmn__market_health_loadtesting",
  "projectId": "market-health-loadtesting"
}`

var healthUIClient, _ = health_ui_client.NewHealthUIClient(health_ui_client.Testing, oAuthToken)
var configProcessor = ConfigProcessor{*healthUIClient}

func TestMain(m *testing.M) {
	httpmock.Activate()
	retCode := m.Run()
	httpmock.Deactivate()
	os.Exit(retCode)
}

func tearDown() {
	httpmock.Reset()
}

func checkRequestBody(t *testing.T, expectedBodyString string, req *http.Request) {
	var actualBody map[string]interface{}
	require.NoError(t, json.NewDecoder(req.Body).Decode(&actualBody))
	var expectedBody map[string]interface{}
	require.NoError(t, json.NewDecoder(strings.NewReader(expectedBodyString)).Decode(&expectedBody))
	require.Equal(t, expectedBody, actualBody)
}

func checkAuthHeader(t *testing.T, req *http.Request) {
	require.Equal(t, req.Header.Values("Authorization"), []string{"OAuth " + oAuthToken})
}

func TestResultConfigExists(t *testing.T) {
	defer tearDown()
	httpmock.RegisterResponder(
		"GET",
		"https://health-testing.market.yandex-team.ru/api/public/logshatter/config/version/active?configId=abo-bpmn",
		httpmock.NewStringResponder(http.StatusOK, activeVersionResponseBody))
	httpmock.RegisterResponder(
		"POST",
		createNewVersionURL,
		func(req *http.Request) (*http.Response, error) {
			checkRequestBody(t, expectedCreateNewVersionRequestBody, req)
			checkAuthHeader(t, req)
			return httpmock.NewStringResponse(http.StatusOK, newVersionSuccessfullyCreatedResponseBody), nil
		})

	err := configProcessor.processConfig("abo-bpmn")

	require.NoError(t, err)
	callCountInfo := httpmock.GetCallCountInfo()
	require.Equal(t, 1,
		callCountInfo["GET https://health-testing.market.yandex-team.ru/api/public/logshatter/config/version/active?configId=abo-bpmn"])
	require.Equal(t, 1, callCountInfo["POST "+createNewVersionURL])
}

func TestResultConfigNotExists(t *testing.T) {
	defer tearDown()
	httpmock.RegisterResponder(
		"GET",
		"https://health-testing.market.yandex-team.ru/api/public/logshatter/config/version/active?configId=abo-bpmn",
		httpmock.NewStringResponder(http.StatusOK, activeVersionResponseBody))
	httpmock.RegisterResponder(
		"POST",
		createNewVersionURL,
		func(req *http.Request) (*http.Response, error) {
			checkRequestBody(t, expectedCreateNewVersionRequestBody, req)
			checkAuthHeader(t, req)
			return httpmock.NewStringResponse(http.StatusNotFound, ""), nil
		})
	httpmock.RegisterResponder(
		"POST",
		"https://health-testing.market.yandex-team.ru/api/public/logshatter/config/create",
		func(req *http.Request) (*http.Response, error) {
			checkRequestBody(t, expectedCreateConfigRequestBody, req)
			checkAuthHeader(t, req)
			callCountInfo := httpmock.GetCallCountInfo()
			require.Equal(t, 1, callCountInfo["POST "+createNewVersionURL])
			httpmock.RegisterResponder(
				"POST",
				createNewVersionURL,
				func(req *http.Request) (*http.Response, error) {
					checkRequestBody(t, expectedCreateNewVersionRequestBody, req)
					checkAuthHeader(t, req)
					return httpmock.NewStringResponse(http.StatusOK, newVersionSuccessfullyCreatedResponseBody), nil
				})
			return httpmock.NewStringResponse(http.StatusOK, ""), nil
		})

	err := configProcessor.processConfig("abo-bpmn")

	require.NoError(t, err)
	callCountInfo := httpmock.GetCallCountInfo()
	require.Equal(t, 1,
		callCountInfo["GET https://health-testing.market.yandex-team.ru/api/public/logshatter/config/version/active?configId=abo-bpmn"])
	require.Equal(t, 1,
		callCountInfo["POST "+createNewVersionURL])
	require.Equal(t, 1,
		callCountInfo["POST https://health-testing.market.yandex-team.ru/api/public/logshatter/config/create"])
}

func TestSourceConfigNotExists(t *testing.T) {
	defer tearDown()
	httpmock.RegisterResponder(
		"GET",
		"https://health-testing.market.yandex-team.ru/api/public/logshatter/config/version/active?configId=some_nonexistent_config",
		httpmock.NewStringResponder(http.StatusNotFound, configNotFoundResponseBody))

	err := configProcessor.processConfig("some_nonexistent_config")

	require.Error(t, err)
	callCountInfo := httpmock.GetCallCountInfo()
	require.Equal(t, 1,
		callCountInfo["GET https://health-testing.market.yandex-team.ru/api/public/logshatter/config/version/active?configId=some_nonexistent_config"])
}
