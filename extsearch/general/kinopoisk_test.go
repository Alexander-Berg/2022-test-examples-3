package rules

import (
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/extsearch/video/station/starter/input"
	"a.yandex-team.ru/extsearch/video/station/starter/pusher"

	"bufio"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"os"
	"strings"
	"testing"
)

func ImplTestKinopoisk(t *testing.T, inputID string) (input.TInputData, *TCmsData) {
	apiAns, err := ioutil.ReadFile(fmt.Sprintf("%s.json", inputID))
	require.NoError(t, err)
	inputReq, err := ioutil.ReadFile(fmt.Sprintf("input_%s.json", inputID))
	require.NoError(t, err)

	var apiAnswer jsAnswerAPI
	cmsData := &apiAnswer.CmsData
	err = json.Unmarshal(apiAns, &apiAnswer)
	require.NoError(t, err)

	var inputData input.TInputData
	err = json.Unmarshal(inputReq, &inputData)
	require.NoError(t, err)

	return inputData, cmsData

}

func TestKinopoisk(t *testing.T) {
	inputData, cmsData := ImplTestKinopoisk(t, "5776167073012007198")
	assert.Equal(t, "AVOD", cmsData.OttParams["monetizationModel"])

	id, err := getVhID(inputData.VideoMessage.CanoURL)
	assert.Equal(t, "5776167073012007198", id)
	require.NoError(t, err)
	assert.Equal(t, "4a4d7a469c2ee1ceb637cfe863ba43b0", cmsData.UUID)
	assert.Equal(t, "vod-episode", cmsData.Type)
	assert.Equal(t, true, isKinopoisk(inputData.VideoMessage, cmsData))
	assert.Equal(t, false, isKinopoiskTrailer(inputData.VideoMessage, cmsData))

	outMsg := pusher.TServiceData{}
	res := fillKinopoiskData(inputData.VideoMessage, cmsData, &outMsg)
	assert.Equal(t, true, res)
	assert.Equal(t, "kinopoisk", outMsg.ProviderName)
	assert.Equal(t, "4a4d7a469c2ee1ceb637cfe863ba43b0", outMsg.ProviderItemID)
}

func TestKinopoiskOTT(t *testing.T) {
	inputData, cmsData := ImplTestKinopoisk(t, "1538975236056290344")
	assert.Equal(t, "AVOD", cmsData.OttParams["monetizationModel"])

	id, err := getVhID(inputData.VideoMessage.CanoURL)
	assert.Equal(t, "1538975236056290344", id)
	require.NoError(t, err)
	assert.Equal(t, "4c8ac4db38abb1d285a55e016da54951", cmsData.UUID)
	assert.Equal(t, "vod-episode", cmsData.Type)
	assert.Equal(t, true, isKinopoisk(inputData.VideoMessage, cmsData))
	assert.Equal(t, false, isKinopoiskTrailer(inputData.VideoMessage, cmsData))

	outMsg := pusher.TServiceData{}
	res := fillKinopoiskData(inputData.VideoMessage, cmsData, &outMsg)
	assert.Equal(t, true, res)
	assert.Equal(t, "kinopoisk", outMsg.ProviderName)
	assert.Equal(t, "4c8ac4db38abb1d285a55e016da54951", outMsg.ProviderItemID)
}

func TestKinopoiskOTT2(t *testing.T) {
	inputData, cmsData := ImplTestKinopoisk(t, "4c8ac4db38abb1d285a55e016da54951")
	assert.Equal(t, "AVOD", cmsData.OttParams["monetizationModel"])

	id, err := getVhID(inputData.VideoMessage.CanoURL)
	assert.Equal(t, "4c8ac4db38abb1d285a55e016da54951", id)
	require.NoError(t, err)
	assert.Equal(t, "4c8ac4db38abb1d285a55e016da54951", cmsData.UUID)
	assert.Equal(t, "vod-episode", cmsData.Type)
	assert.Equal(t, true, isKinopoisk(inputData.VideoMessage, cmsData))
	assert.Equal(t, false, isKinopoiskTrailer(inputData.VideoMessage, cmsData))

	outMsg := pusher.TServiceData{}
	res := fillKinopoiskData(inputData.VideoMessage, cmsData, &outMsg)
	assert.Equal(t, true, res)
	assert.Equal(t, "kinopoisk", outMsg.ProviderName)
	assert.Equal(t, "4c8ac4db38abb1d285a55e016da54951", outMsg.ProviderItemID)
}

func TestTrailer(t *testing.T) {
	inputData, cmsData := ImplTestKinopoisk(t, "11956629183503470810")
	assert.Equal(t, "AVOD", cmsData.OttParams["monetizationModel"])

	id, err := getVhID(inputData.VideoMessage.CanoURL)
	assert.Equal(t, "11956629183503470810", id)
	require.NoError(t, err)
	assert.Equal(t, "44b33e770316f98f89705178ff750de8", cmsData.UUID)
	assert.Equal(t, "vod-episode", cmsData.Type)
	assert.Equal(t, true, isKinopoisk(inputData.VideoMessage, cmsData))
	assert.Equal(t, true, isKinopoiskTrailer(inputData.VideoMessage, cmsData))

	outMsg := pusher.TServiceData{}
	res := fillKinopoiskData(inputData.VideoMessage, cmsData, &outMsg)
	assert.Equal(t, true, res)
	assert.Equal(t, "yavideo_proxy", outMsg.ProviderName)
	assert.Equal(t, "http://frontend.vh.yandex.ru/player/11956629183503470810", outMsg.ProviderItemID)
	assert.Equal(t, "https://strm.yandex.ru/vh-kp-converted/ott-content/395322987-44b33e770316f98f89705178ff750de8/master.m3u8", outMsg.PlayURI)
}

func TestTrailer2(t *testing.T) {
	inputData, cmsData := ImplTestKinopoisk(t, "8072610027815219148")
	assert.Equal(t, "AVOD", cmsData.OttParams["monetizationModel"])

	assert.Equal(t, "4eb786a4ed3848829978216203ee9e09", cmsData.UUID)
	assert.Equal(t, "vod-episode", cmsData.Type)
	assert.Equal(t, true, isKinopoisk(inputData.VideoMessage, cmsData))
	assert.Equal(t, true, isKinopoiskTrailer(inputData.VideoMessage, cmsData))

	id, err := getVhID(inputData.VideoMessage.CanoURL)
	assert.Equal(t, "8072610027815219148", id)
	require.NoError(t, err)
	assert.Equal(t, "149578", cmsData.OttParams["kpTrailerId"])

	outMsg := pusher.TServiceData{}
	res := fillKinopoiskData(inputData.VideoMessage, cmsData, &outMsg)
	assert.Equal(t, true, res)
	assert.Equal(t, "yavideo_proxy", outMsg.ProviderName)
	assert.Equal(t, "http://frontend.vh.yandex.ru/player/8072610027815219148", outMsg.ProviderItemID)
	assert.Equal(t, "https://strm.yandex.ru/vh-kp-converted/vod-content/312554741/master.m3u8", outMsg.PlayURI)
}

func TestNotKinopoisk(t *testing.T) {
	inputData, cmsData := ImplTestKinopoisk(t, "11434391912841711256")

	assert.Equal(t, "403b9a820eedd1b38fff959ac7a1ddfa", cmsData.UUID)
	assert.Equal(t, "vod-episode", cmsData.Type)
	assert.Equal(t, false, isKinopoisk(inputData.VideoMessage, cmsData))
}

func TestOldKinopoisk(t *testing.T) {
	inputReq, err := ioutil.ReadFile("kinopoisk_271516.json")
	require.NoError(t, err)
	apiAns := []byte(`{"uuid":"4a8b9447a2b2bf3da2bbfa5ffd90b8fc","status":2,"svod":false}`)

	var ottData TOttData
	require.NoError(t, json.Unmarshal(apiAns, &ottData))

	var inputData input.TInputData
	require.NoError(t, json.Unmarshal(inputReq, &inputData))

	assert.Equal(t, true, isOldKinopoisk(inputData.VideoMessage))
	id, err := getOldKinopoiskID(inputData.VideoMessage)
	assert.Equal(t, uint64(271516), id)
	require.NoError(t, err)

	assert.Equal(t, "4a8b9447a2b2bf3da2bbfa5ffd90b8fc", ottData.UUID)
	assert.Equal(t, uint64(2), ottData.Status)
	assert.Equal(t, false, ottData.Svod)

	outMsg := pusher.TServiceData{}
	res := fillOldKinopoiskMessage(inputData.VideoMessage, &ottData, &outMsg)

	assert.Equal(t, true, res)
	assert.Equal(t, "kinopoisk", outMsg.ProviderName)
	assert.Equal(t, "4a8b9447a2b2bf3da2bbfa5ffd90b8fc", outMsg.ProviderItemID)
}

func TestOldKinopoiskTrailer(t *testing.T) {
	inputReq, err := ioutil.ReadFile("kinopoisk_886143_128772.json")
	require.NoError(t, err)

	var inputData input.TInputData
	require.NoError(t, json.Unmarshal(inputReq, &inputData))
	assert.Equal(t, false, isOldKinopoisk(inputData.VideoMessage))
	assert.Equal(t, true, isOldKpTrailer(inputData.VideoMessage))

	inputReq = []byte(`{"msg":{"play_uri":"<iframe src=\"//desktop.kinopoisk.ru/trailer/player/share/142910/?share=true&amp;from_src=yandex&amp;from=yavideo\" frameborder=\"0\" scrolling=\"no\" allowfullscreen=\"1\" allow=\"autoplay; fullscreen; accelerometer; gyroscope; picture-in-picture\" aria-label=\"Video\"></iframe>","provider_item_id":"http://www.kinopoisk.ru/film/588550/video/142910/","provider_name":"kinopoisk.ru","source_host":"www.kinopoisk.ru","player_id":"kinopoisk","visible_url":"http://www.kinopoisk.ru/film/588550/video/142910/","type":"video"},"device":"210d8400657fa461e4b14716191a6903"}`)

	require.NoError(t, json.Unmarshal(inputReq, &inputData))
	assert.Equal(t, false, isOldKinopoisk(inputData.VideoMessage))
	assert.Equal(t, true, isOldKpTrailer(inputData.VideoMessage))
}

func TestAllForPlayer(t *testing.T) {
	inputReqs, err := os.Open("test_player.txt")
	require.NoError(t, err)
	linesReqs := bufio.NewReader(inputReqs)

	var line string
	for line != "" && err == nil {
		line, err = linesReqs.ReadString('\n')
		if line == "" {
			continue
		}
		var inputData input.TInputData
		require.NoError(t, json.Unmarshal([]byte(line), &inputData))
		tmp := strings.Split(inputData.VideoMessage.CanoURL, "/")
		var apiAnswer jsAnswerAPI
		cmsData := &apiAnswer.CmsData
		f, err := ioutil.ReadFile(tmp[len(tmp)-1] + ".json")
		require.NoError(t, err)
		require.NoError(t, json.Unmarshal(f, &apiAnswer))
		assert.Equal(t, true, isKinopoisk(inputData.VideoMessage, cmsData))
	}

}

func TestMusicClip1(t *testing.T) {
	inputData, cmsData := ImplTestKinopoisk(t, "2083940414919387039")
	assert.Equal(t, "AVOD", cmsData.OttParams["monetizationModel"])
	assert.Equal(t, "music", cmsData.SuperTag)

	id, err := getVhID(inputData.VideoMessage.CanoURL)
	assert.Equal(t, "2083940414919387039", id)
	require.NoError(t, err)
	assert.Equal(t, "4ab7f976dd6c5d9a84ef523be725adcc", cmsData.UUID)
	assert.Equal(t, "vod-episode", cmsData.Type)
	assert.Equal(t, true, isKinopoisk(inputData.VideoMessage, cmsData))
	assert.Equal(t, false, isKinopoiskTrailer(inputData.VideoMessage, cmsData))
	assert.Equal(t, true, isMusicClip(inputData.VideoMessage, cmsData))

	outMsg := pusher.TServiceData{}
	res := fillKinopoiskData(inputData.VideoMessage, cmsData, &outMsg)
	assert.Equal(t, true, res)
	assert.Equal(t, "strm", outMsg.ProviderName)
	assert.Equal(t, "4ab7f976dd6c5d9a84ef523be725adcc", outMsg.ProviderItemID)
	assert.Equal(t, "null", outMsg.PlayURI)
}

func TestMusicClip2(t *testing.T) {
	inputData, cmsData := ImplTestKinopoisk(t, "4ab7f976dd6c5d9a84ef523be725adcc")
	assert.Equal(t, "AVOD", cmsData.OttParams["monetizationModel"])
	assert.Equal(t, "music", cmsData.SuperTag)

	id, err := getVhID(inputData.VideoMessage.CanoURL)
	assert.Equal(t, "4ab7f976dd6c5d9a84ef523be725adcc", id)
	require.NoError(t, err)
	assert.Equal(t, "4ab7f976dd6c5d9a84ef523be725adcc", cmsData.UUID)
	assert.Equal(t, "vod-episode", cmsData.Type)
	assert.Equal(t, true, isKinopoisk(inputData.VideoMessage, cmsData))
	assert.Equal(t, false, isKinopoiskTrailer(inputData.VideoMessage, cmsData))
	assert.Equal(t, true, isMusicClip(inputData.VideoMessage, cmsData))

	outMsg := pusher.TServiceData{}
	res := fillKinopoiskData(inputData.VideoMessage, cmsData, &outMsg)
	assert.Equal(t, true, res)
	assert.Equal(t, "strm", outMsg.ProviderName)
	assert.Equal(t, "4ab7f976dd6c5d9a84ef523be725adcc", outMsg.ProviderItemID)
	assert.Equal(t, "null", outMsg.PlayURI)
}

func TestFreshTrailer(t *testing.T) {
	inputData, cmsData := ImplTestKinopoisk(t, "7924147659230997085")
	assert.Equal(t, "AVOD", cmsData.OttParams["monetizationModel"])

	id, err := getVhID(inputData.VideoMessage.CanoURL)
	assert.Equal(t, "7924147659230997085", id)
	require.NoError(t, err)
	assert.Equal(t, "43ae4eef8b333a28a3dc483b256aece6", cmsData.UUID)
	assert.Equal(t, "vod-episode", cmsData.Type)
	assert.Equal(t, true, isKinopoisk(inputData.VideoMessage, cmsData))
	assert.Equal(t, true, isKinopoiskTrailer(inputData.VideoMessage, cmsData))
	assert.Equal(t, false, isMusicClip(inputData.VideoMessage, cmsData))

	outMsg := pusher.TServiceData{}
	res := fillKinopoiskData(inputData.VideoMessage, cmsData, &outMsg)
	assert.Equal(t, true, res)
	assert.Equal(t, "yavideo_proxy", outMsg.ProviderName)
	assert.Equal(t, "http://frontend.vh.yandex.ru/player/7924147659230997085", outMsg.ProviderItemID)
	assert.Equal(t, "https://strm.yandex.ru/vh-kp-converted/ott-content/616961155-43ae4eef8b333a28a3dc483b256aece6/master.m3u8", outMsg.PlayURI)
}
