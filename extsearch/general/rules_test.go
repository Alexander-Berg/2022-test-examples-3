package rules

import (
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/extsearch/video/station/starter/input"
	"a.yandex-team.ru/extsearch/video/station/starter/pusher"
	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/log/zap"

	"context"
	"crypto/tls"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

func generateTestClient(handler http.Handler) (*http.Client, func()) {
	ts := httptest.NewTLSServer(handler)
	cli := &http.Client{
		Transport: &http.Transport{
			DialContext: func(_ context.Context, network, _ string) (net.Conn, error) {
				return net.Dial(network, ts.Listener.Addr().String())
			},
			TLSClientConfig: &tls.Config{
				InsecureSkipVerify: true,
			},
		},
	}
	return cli, ts.Close
}

func handlerFrontendVhAPI(t *testing.T, logger log.Logger, w http.ResponseWriter, r *http.Request) {
	path := r.URL.Path
	pathParts := strings.Split(path, "/")
	logger.Info("requesting file:", log.Any("path", path), log.Any("p1", pathParts[1]), log.Any("p2", pathParts[2]))
	logger.Info("requesting file:", log.Any("path", path), log.Any("parts", pathParts))
	if pathParts[1] != "player" {
		if pathParts[1] != "v23" || pathParts[2] != "player" {
			http.NotFound(w, r)
			return
		}
	}
	filename := pathParts[len(pathParts)-1]
	filename = strings.Split(filename, "?")[0]
	body, err := ioutil.ReadFile(filename)
	require.NoError(t, err)
	_, _ = w.Write(body)
}

func initMessage(path string) (res input.TInputData, err error) {
	data, err := ioutil.ReadFile(path)
	if err != nil {
		return
	}
	err = json.Unmarshal(data, &res)
	return
}

func checker(t *testing.T, ruleHandler *TRuleHandler, msg input.TVideoMessage,
	id string, expectedRes bool, tag string, playType string, providerID string) bool {

	var out pusher.TServiceData
	var res bool
	var err error
	switch tag {
	case "kinopoisk":
		res, err = ruleHandler.FormKinopoiskMessage(msg, &out)
	case "channel":
		res, err = ruleHandler.FormChannelMessage(msg, &out)
	default:
		require.Fail(t, "cant reach here")
	}
	require.NoError(t, err)

	assert.Equal(t, expectedRes, res, "id: %s, expected %s", id, tag)
	if res {
		assert.Equal(t, playType, out.ProviderName, "playtype is unexpected for %s for id %s", tag, id)
		assert.Equal(t, providerID, out.ProviderItemID, "provider_item_id in unexpected %s for id %s", tag, id)
	}
	return res
}

func checkKinopoiskAndChannel(t *testing.T, ruleHandler *TRuleHandler, id string,
	kinopoisk bool, channel bool, playType string, providerID string) {
	inputData, err := initMessage(fmt.Sprintf("input_%s.json", id))
	require.NoError(t, err)

	msg := inputData.VideoMessage
	eqURLs := fmt.Sprintf("http://frontend.vh.yandex.ru/player/%s", id) == msg.CanoURL
	eqURLs = eqURLs || fmt.Sprintf("https://frontend.vh.yandex.ru/player/%s", id) == msg.CanoURL
	eqURLs = eqURLs || id == msg.CanoURL && (checkUgcID(id) || checkNumID(id))
	//require.Equal(t, msg.CanoURL, fmt.Sprintf("http://frontend.vh.yandex.ru/player/%s", id))
	require.Equal(t, true, eqURLs, "not good url %s", msg.CanoURL)

	res := checker(t, ruleHandler, msg, id, kinopoisk, "kinopoisk", playType, providerID)
	if res {
		return
	}
	checker(t, ruleHandler, msg, id, channel, "channel", playType, providerID)
}

func TestVhLogic(t *testing.T) {
	logger, err := zap.New(zap.JSONConfig(log.TraceLevel))
	require.NoError(t, err)

	client, endFunc := generateTestClient(http.HandlerFunc(func(w http.ResponseWriter, req *http.Request) {
		handlerFrontendVhAPI(t, logger, w, req)
	}))
	defer endFunc()

	ruleHandler := NewRuleHandler(client)

	checkKinopoiskAndChannel(t, ruleHandler, "4678919787855744743", false, true, "strm", "4d1d6979385d28ebab8faae66cd97d72")
	checkKinopoiskAndChannel(t, ruleHandler, "4d1d6979385d28ebab8faae66cd97d72", false, true, "strm", "4d1d6979385d28ebab8faae66cd97d72")
	checkKinopoiskAndChannel(t, ruleHandler, "11434391912841711256", false, true, "strm", "403b9a820eedd1b38fff959ac7a1ddfa")
	checkKinopoiskAndChannel(t, ruleHandler, "403b9a820eedd1b38fff959ac7a1ddfa", false, true, "strm", "403b9a820eedd1b38fff959ac7a1ddfa")
	checkKinopoiskAndChannel(t, ruleHandler, "11956629183503470810", true, false, "yavideo_proxy", "http://frontend.vh.yandex.ru/player/11956629183503470810")             // trailer
	checkKinopoiskAndChannel(t, ruleHandler, "44b33e770316f98f89705178ff750de8", true, false, "yavideo_proxy", "http://frontend.vh.yandex.ru/player/11956629183503470810") // trailer
	checkKinopoiskAndChannel(t, ruleHandler, "5776167073012007198", true, false, "kinopoisk", "4a4d7a469c2ee1ceb637cfe863ba43b0")                                          //"5776167073012007198")
	checkKinopoiskAndChannel(t, ruleHandler, "4a4d7a469c2ee1ceb637cfe863ba43b0", true, false, "kinopoisk", "4a4d7a469c2ee1ceb637cfe863ba43b0")                             //"5776167073012007198")
	checkKinopoiskAndChannel(t, ruleHandler, "8072610027815219148", true, false, "yavideo_proxy", "http://frontend.vh.yandex.ru/player/8072610027815219148")               // trailer
	checkKinopoiskAndChannel(t, ruleHandler, "4eb786a4ed3848829978216203ee9e09", true, false, "yavideo_proxy", "http://frontend.vh.yandex.ru/player/8072610027815219148")  // trailer
	checkKinopoiskAndChannel(t, ruleHandler, "1538975236056290344", true, false, "kinopoisk", "4c8ac4db38abb1d285a55e016da54951")
	checkKinopoiskAndChannel(t, ruleHandler, "4c8ac4db38abb1d285a55e016da54951", true, false, "kinopoisk", "4c8ac4db38abb1d285a55e016da54951")
	checkKinopoiskAndChannel(t, ruleHandler, "9522657658862053279", true, false, "kinopoisk", "47a38f03dce381358fa79811f98e8e9e")
	checkKinopoiskAndChannel(t, ruleHandler, "47a38f03dce381358fa79811f98e8e9e", true, false, "kinopoisk", "47a38f03dce381358fa79811f98e8e9e")
	checkKinopoiskAndChannel(t, ruleHandler, "4bdfe2119a42232e923540c5ec7ca605", false, true, "strm", "4bdfe2119a42232e923540c5ec7ca605")
	checkKinopoiskAndChannel(t, ruleHandler, "voBV6v5xTCDU", false, true, "strm", "voBV6v5xTCDU") // ugc
	checkKinopoiskAndChannel(t, ruleHandler, "voBV6v5xTCDP", false, true, "strm", "voBV6v5xTCDP") // ugc
	checkKinopoiskAndChannel(t, ruleHandler, "vH6qMYc2OZ_Q", false, true, "strm", "vH6qMYc2OZ_Q") // ugc
	checkKinopoiskAndChannel(t, ruleHandler, "2083940414919387039", true, false, "strm", "4ab7f976dd6c5d9a84ef523be725adcc")
	checkKinopoiskAndChannel(t, ruleHandler, "4ab7f976dd6c5d9a84ef523be725adcc", true, false, "strm", "4ab7f976dd6c5d9a84ef523be725adcc")
	checkKinopoiskAndChannel(t, ruleHandler, "7924147659230997085", true, false, "yavideo_proxy", "http://frontend.vh.yandex.ru/player/7924147659230997085")
	checkKinopoiskAndChannel(t, ruleHandler, "4108cd53be958d8eb8f31bfeb358b7ea", false, true, "strm", "4108cd53be958d8eb8f31bfeb358b7ea")
	checkKinopoiskAndChannel(t, ruleHandler, "427ec3f3eac2d36ca6ea7a25c2290856", false, true, "strm", "427ec3f3eac2d36ca6ea7a25c2290856")
}

func TestVhId(t *testing.T) {
	id, err := getVhID("http://frontend.vh.yandex.ru/player/4678919787855744743")
	require.NoError(t, err, "4678919787855744743")
	require.Equal(t, "4678919787855744743", id)
	id, err = getVhID("http://frontend.vh.yandex.ru/player/4d1d6979385d28ebab8faae66cd97d72")
	require.NoError(t, err, "4d1d6979385d28ebab8faae66cd97d72")
	require.Equal(t, "4d1d6979385d28ebab8faae66cd97d72", id)
	id, err = getVhID("4678919787855744743")
	require.NoError(t, err, "4678919787855744743")
	require.Equal(t, "4678919787855744743", id)
	id, err = getVhID("4d1d6979385d28ebab8faae66cd97d72")
	require.NoError(t, err, "4d1d6979385d28ebab8faae66cd97d72")
	require.Equal(t, "4d1d6979385d28ebab8faae66cd97d72", id)
	_, err = getVhID("https://frontend.vh.yandex.ru/player/4678919787855744743")
	require.NoError(t, err)
	_, err = getVhID("https://www.youtube.com/watch?v=1")
	require.Error(t, err)
	id, err = getVhID("voBV6v5xTCDP")
	require.NoError(t, err, "voBV6v5xTCDP")
	require.Equal(t, "voBV6v5xTCDP", id)
}
