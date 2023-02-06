package server_test

import (
	"a.yandex-team.ru/extsearch/video/station/starter/server"
	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/log/zap"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"os"
	"testing"
)

func TestXivaPusher(t *testing.T) {
	loggerCfg := zap.JSONConfig(log.TraceLevel)
	logger, err := zap.New(loggerCfg)
	require.NoError(t, err)

	clientDownloader, closeFuncDownloader := generateTestClient(true, http.HandlerFunc(func(w http.ResponseWriter, req *http.Request) {
		downloaderClientHandler(t, logger, w, req)
	}))
	defer closeFuncDownloader()
	clientRules, closeFuncRules := generateTestClient(true, http.HandlerFunc(func(w http.ResponseWriter, req *http.Request) {
		ruleClientHandler(t, logger, w, req)
	}))
	defer closeFuncRules()
	clientPusher, closeFuncPusher := generateTestClient(false, http.HandlerFunc(func(w http.ResponseWriter, req *http.Request) {
		pusherClientHandler(t, logger, w, req)
	}))
	defer closeFuncPusher()

	reg := generateTestMetrics(t)

	reqid := "reqid12"
	global := generateGlobalObj(t, logger, clientDownloader, clientRules, clientPusher, reg)

	f, err := os.Open("msg.json")
	require.NoError(t, err)

	req := httptest.NewRequest(http.MethodPost, "http://localhost/", f)
	req.Header.Add("X-Uid", "123")
	req.Header.Add("X-Req-Id", reqid)

	w := httptest.NewRecorder()
	server.CheckAndGoXiva(global, w, req)
	resp := w.Result()

	ansData, err := ioutil.ReadAll(resp.Body)
	defer func() {
		if err := resp.Body.Close(); err != nil {
			panic(err)
		}
	}()

	require.NoError(t, err)

	logger.Info("answer", log.Any("ans", string(ansData)))
	assert.Equal(t, []byte(`{"status":"error","msg":"cant play video","code":2}`+"\n"), ansData)
	require.Equal(t, 200, resp.StatusCode)
	require.Equal(t, "application/json", resp.Header.Get("Content-Type"))

	assert.Equal(t, `{"commonLabels":{"cluster":"testing","project":"quasar_video_proxy","service":"station_starter"},"sensors":[{"value":1,"labels":{"code":"2","msg":"cant play video","sensor":"answerStatus","status":"error"},"kind":"RATE"}]}`, string(reg.Dump()))
}
