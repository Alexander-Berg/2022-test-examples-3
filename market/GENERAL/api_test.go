package api

import (
	"encoding/xml"
	"fmt"
	"net/url"
	"strconv"
	"testing"

	"github.com/stretchr/testify/require"
)

var token = "xxxxxxxxxxxxxxxxxxxxDimaKotovProstoKosmosLoad1Testxxxxxxxxxxxxxx"
var uri = url.URL{Scheme: "http", Host: "wms-load-app01e.market.yandex.net"}

func DisabledTestData(t *testing.T) {
	params := getParams()
	inboundData, err := CreateInboundData("token", params)
	checkError(err, t)
	checkData(inboundData, CreateInboundScenarioStep, t)
}

func getParams() map[string]string {
	params := make(map[string]string)
	params["storer"] = "2020070311"
	params["count"] = strconv.Itoa(1)
	params["manufacturer_sku"] = "KD00007"
	return params
}

func checkData(data string, requestType string, t *testing.T) {
	root := Root{}
	err := xml.Unmarshal([]byte(data), &root)
	checkError(err, t)

	fmt.Printf("data: \n%v\nRequest:\n%v\n", data, root.Request)

	require.NotEqual(t, nil, root.Request)
	require.Equal(t, requestType, root.Request.Type)
}

func checkError(err error, t *testing.T) {
	if err != nil {
		t.Errorf(err.Error())
	}
}

func DisabledTestIntegrationCreateOrder(t *testing.T) {
	params := getParams()

	resp, err := CreateResource(uri, token, params, nil, "test", CreateOrderScenarioStep)
	checkError(err, t)
	require.Equal(t, false, resp.RequestState.IsError)
	require.NotEqual(t, CommonID{}, resp.Response.OrderID)
	require.Equal(t, CommonID{}, resp.Response.InboundID)
	fmt.Printf("API:Response.OrderID{%v}\n", resp.Response.OrderID)
}

func DisabledTestIntegrationCreateInbound(t *testing.T) {
	params := map[string]string{
		"storer":    "777",
		"count":     strconv.Itoa(10),
		"barcodeID": "105",
	}

	resp, err := CreateResource(uri, token, params, nil, "test", CreateInboundScenarioStep)
	checkError(err, t)
	require.Equal(t, false, resp.RequestState.IsError)
	require.Equal(t, CommonID{}, resp.Response.OrderID)
	require.NotEqual(t, CommonID{}, resp.Response.InboundID)
	fmt.Printf("API:Response.InboundID{%v}\n", resp.Response.InboundID)
}
