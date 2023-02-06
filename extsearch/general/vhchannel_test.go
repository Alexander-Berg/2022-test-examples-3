package rules

import (
	"a.yandex-team.ru/extsearch/video/station/starter/input"
	"a.yandex-team.ru/extsearch/video/station/starter/pusher"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"encoding/json"
	"io/ioutil"
	"testing"
)

func TestChannel(t *testing.T) {
	apiAns, err := ioutil.ReadFile("4678919787855744743.json")
	require.NoError(t, err)
	inputReq, err := ioutil.ReadFile("input_4678919787855744743.json")
	require.NoError(t, err)

	var apiAnswer jsAnswerAPI
	cmsData := &apiAnswer.CmsData
	err = json.Unmarshal(apiAns, &apiAnswer)
	require.NoError(t, err)

	var inputData input.TInputData
	err = json.Unmarshal(inputReq, &inputData)
	require.NoError(t, err)

	assert.Equal(t, cmsData.UUID, "4d1d6979385d28ebab8faae66cd97d72")
	assert.Equal(t, cmsData.Type, "channel")

	assert.Equal(t, isVh(inputData.VideoMessage), true)
	outMsg := pusher.TServiceData{}
	res := fillChannelData(inputData.VideoMessage, cmsData, &outMsg)
	assert.Equal(t, res, true)
	assert.Equal(t, outMsg.ProviderName, "strm")
	assert.Equal(t, outMsg.ProviderItemID, "4d1d6979385d28ebab8faae66cd97d72")
}

func TestVHVideo(t *testing.T) {
	apiAns, err := ioutil.ReadFile("11434391912841711256.json")
	require.NoError(t, err)
	inputReq, err := ioutil.ReadFile("input_11434391912841711256.json")
	require.NoError(t, err)

	var apiAnswer jsAnswerAPI
	cmsData := &apiAnswer.CmsData
	err = json.Unmarshal(apiAns, &apiAnswer)
	require.NoError(t, err)

	var inputData input.TInputData
	err = json.Unmarshal(inputReq, &inputData)
	require.NoError(t, err)

	assert.Equal(t, "403b9a820eedd1b38fff959ac7a1ddfa", cmsData.UUID)
	assert.Equal(t, "vod-episode", cmsData.Type)

	assert.Equal(t, isVh(inputData.VideoMessage), true)
	outMsg := pusher.TServiceData{}
	res := fillChannelData(inputData.VideoMessage, cmsData, &outMsg)
	assert.Equal(t, res, true)
	assert.Equal(t, "strm", outMsg.ProviderName)
	assert.Equal(t, "403b9a820eedd1b38fff959ac7a1ddfa", outMsg.ProviderItemID)
	assert.Equal(t, "null", outMsg.PlayURI)
}

func TestUGCVideo(t *testing.T) {
	apiAns, err := ioutil.ReadFile("voBV6v5xTCDU.json")
	require.NoError(t, err)
	inputReq, err := ioutil.ReadFile("input_voBV6v5xTCDU.json")
	require.NoError(t, err)

	var apiAnswer jsAnswerAPI
	cmsData := &apiAnswer.CmsData
	err = json.Unmarshal(apiAns, &apiAnswer)
	require.NoError(t, err)

	var inputData input.TInputData
	err = json.Unmarshal(inputReq, &inputData)
	require.NoError(t, err)

	assert.Equal(t, "voBV6v5xTCDU", cmsData.UUID)
	assert.Equal(t, "vod-episode", cmsData.Type)

	assert.Equal(t, isVh(inputData.VideoMessage), true)
	outMsg := pusher.TServiceData{}
	res := fillChannelData(inputData.VideoMessage, cmsData, &outMsg)
	assert.Equal(t, res, true)
	assert.Equal(t, "strm", outMsg.ProviderName)
	assert.Equal(t, "voBV6v5xTCDU", outMsg.ProviderItemID)
	assert.Equal(t, "null", outMsg.PlayURI)
}

func TestCatchupConverted(t *testing.T) {
	apiAns, err := ioutil.ReadFile("4108cd53be958d8eb8f31bfeb358b7ea.json")
	require.NoError(t, err)
	inputReq, err := ioutil.ReadFile("input_4108cd53be958d8eb8f31bfeb358b7ea.json")
	require.NoError(t, err)

	var apiAnswer jsAnswerAPI
	cmsData := &apiAnswer.CmsData
	err = json.Unmarshal(apiAns, &apiAnswer)
	require.NoError(t, err)

	var inputData input.TInputData
	err = json.Unmarshal(inputReq, &inputData)
	require.NoError(t, err)

	assert.Equal(t, cmsData.UUID, "4108cd53be958d8eb8f31bfeb358b7ea")
	assert.Equal(t, cmsData.Type, "episode")

	assert.Equal(t, isVh(inputData.VideoMessage), true)
	outMsg := pusher.TServiceData{}
	res := fillChannelData(inputData.VideoMessage, cmsData, &outMsg)
	assert.Equal(t, res, true)
	assert.Equal(t, outMsg.ProviderName, "strm")
	assert.Equal(t, outMsg.ProviderItemID, "4108cd53be958d8eb8f31bfeb358b7ea")
}

func TestCatchupFresh(t *testing.T) {
	apiAns, err := ioutil.ReadFile("427ec3f3eac2d36ca6ea7a25c2290856.json")
	require.NoError(t, err)
	inputReq, err := ioutil.ReadFile("input_427ec3f3eac2d36ca6ea7a25c2290856.json")
	require.NoError(t, err)

	var apiAnswer jsAnswerAPI
	cmsData := &apiAnswer.CmsData
	err = json.Unmarshal(apiAns, &apiAnswer)
	require.NoError(t, err)

	var inputData input.TInputData
	err = json.Unmarshal(inputReq, &inputData)
	require.NoError(t, err)

	assert.Equal(t, cmsData.UUID, "427ec3f3eac2d36ca6ea7a25c2290856")
	assert.Equal(t, cmsData.Type, "episode")

	assert.Equal(t, isVh(inputData.VideoMessage), true)
	outMsg := pusher.TServiceData{}
	res := fillChannelData(inputData.VideoMessage, cmsData, &outMsg)
	assert.Equal(t, res, true)
	assert.Equal(t, outMsg.ProviderName, "strm")
	assert.Equal(t, outMsg.ProviderItemID, "427ec3f3eac2d36ca6ea7a25c2290856")
}
