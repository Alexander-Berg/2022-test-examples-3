package server_test

import (
	"bytes"
	"context"
	"crypto/sha1"
	"crypto/tls"
	"encoding/base64"
	"io/ioutil"
	"net"
	"net/http"
	"net/http/httptest"
	"os"
	"strings"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/extsearch/video/robot/rt_transcoder/metrics"
	"a.yandex-team.ru/extsearch/video/station/starter/answer"
	"a.yandex-team.ru/extsearch/video/station/starter/downloader"
	"a.yandex-team.ru/extsearch/video/station/starter/players"
	"a.yandex-team.ru/extsearch/video/station/starter/pusher"
	"a.yandex-team.ru/extsearch/video/station/starter/rules"
	"a.yandex-team.ru/extsearch/video/station/starter/server"
	"a.yandex-team.ru/extsearch/video/station/starter/solomon"
	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/log/zap"
)

func generateTestClient(https bool, handler http.Handler) (*http.Client, func()) {
	var ts *httptest.Server
	if https {
		ts = httptest.NewTLSServer(handler)
	} else {
		ts = httptest.NewServer(handler)
	}
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

func downloaderClientHandler(t *testing.T, logger log.Logger, w http.ResponseWriter, r *http.Request) {
	query := r.URL.Query()
	logger.Info("[downloader handler] downloading url", log.Any("host", r.URL.Host), log.Any("path", r.URL.Path), log.Any("query", query))
	var body []byte
	var err error

	assert.True(t, r.Header.Get("X-Req-Id") != "", "no reqid header provided!")

	switch r.URL.Path {
	case "/video":
		id := query.Get("id")
		hasher := sha1.New()
		_, err = hasher.Write([]byte(id))
		if err != nil {
			panic(err)
		}
		sha := base64.URLEncoding.EncodeToString(hasher.Sum(nil))
		logger.Info("url id", log.Any("id", id), log.Any("sha", sha))
		switch sha {
		case "0EPhxcL7BWfiQs5LYSuULN0Asws=":
			http.Redirect(w, r, "https://strm.yandex.ru/someid2", http.StatusFound)
		case "bRZDR81-Q9zek2UlwPZ62m_-Eb0=":
			http.Redirect(w, r, "https://strm.yandex.ru/someid", http.StatusFound)
		default:
			http.NotFound(w, r)
		}
		return
	case "/video.mp4":
		body, err = ioutil.ReadFile(r.URL.Path[1:])
	case "/video.m3u8":
		body, err = ioutil.ReadFile(r.URL.Path[1:])
	}
	require.NoError(t, err)

	_, err = w.Write(body)
	require.NoError(t, err)
}

func ruleClientHandler(t *testing.T, logger log.Logger, w http.ResponseWriter, r *http.Request) {
	logger.Info("[rule handler] downloading url", log.Any("path", r.URL.Path), log.Any("query", r.URL.RawQuery), log.Any("host", r.URL.Host))
	parts := strings.Split(r.URL.Path, "/")
	assert.True(t, 1 < len(parts), "len of parts %d <= 1", len(parts))
	assert.True(t, r.Header.Get("X-Req-Id") != "", "no reqid header provided!")
	switch parts[1] {
	case "player":
		logger.Error("[rule handler] downloading url part", log.Any("filename", parts[len(parts)-1]))
		http.ServeFile(w, r, parts[len(parts)-1])
	case "ott":
		logger.Error("[rule handler] downloading url part", log.Any("filename", r.URL.RawQuery))
		switch r.URL.RawQuery {
		case "kpFilmId=271516":
			http.ServeContent(w, r, "kpFilmId=271516.json", time.Unix(0, 0), bytes.NewReader([]byte(`{"uuid":"4a8b9447a2b2bf3da2bbfa5ffd90b8fc","status":2,"svod":false}`)))
		case "":
			http.ServeContent(w, r, ".json", time.Unix(0, 0), bytes.NewReader([]byte(`{"uuid":"4a8b9447a2b2bf3da2bbfa5ffd90b8fc","status":2,"svod":false}`)))
		default:
			http.NotFound(w, r)
		}
	default:
		http.NotFound(w, r)
	}
}

func pusherClientHandler(t *testing.T, logger log.Logger, w http.ResponseWriter, r *http.Request) {
	logger.Info("[pusher handler] downloading url", log.Any("path", r.URL.Path), log.Any("query", r.URL.RawQuery), log.Any("host", r.URL.Host))
	assert.True(t, r.Header.Get("X-Req-Id") != "", "no reqid header provided!")
	data, err := ioutil.ReadAll(r.Body)
	require.NoError(t, err)
	logger.Info("[pusher handler]", log.Any("data", string(data)))
	http.ServeContent(w, r, "answer.json", time.Unix(0, 0), bytes.NewReader([]byte(`{"status":"ok"}`)))
}

func generateGlobalObj(t *testing.T, logger *zap.Logger, clientDownloader, clientRules, clientPusher *http.Client, reg *metrics.Registry) *server.TGlobal {
	unban := "unban.json"
	xivaToken := "1"
	banKp := "banned_test.txt"
	hmacSecret := "2"

	whitelist, err := players.NewHostWhitelist(unban)
	require.NoError(t, err)

	global := &server.TGlobal{
		Players:     whitelist,
		Pusher:      &pusher.TPusher{Token: xivaToken, Client: clientPusher, ReqID: ""},
		VhProcessor: rules.NewProcessVH(banKp, logger),
		Downloader:  downloader.NewDownloader(hmacSecret, "", clientDownloader, ""),
		Rules:       rules.NewRuleHandler(clientRules),
		Logger:      logger,
		Metrics:     solomon.NewMetrics(reg, answer.CodeMax),
	}
	return global
}

func generateTestMetrics(t *testing.T) *metrics.Registry {
	reg := metrics.NewRegistry(map[string]string{
		"project": "quasar_video_proxy",
		"cluster": "testing",
		"service": "station_starter",
	})
	return reg
}

func TestBassPusher(t *testing.T) {
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
	server.CheckAndGoBass(global, w, req)
	resp := w.Result()

	ansDataBytes, err := ioutil.ReadAll(resp.Body)
	ansData := string(ansDataBytes)
	defer func() {
		if err := resp.Body.Close(); err != nil {
			panic(err)
		}
	}()

	require.NoError(t, err)

	logger.Info("answer", log.Any("ans", ansData))
	assert.Equal(t, `{"status":"error","msg":"cant play video","code":2}`+"\n", ansData)
	require.Equal(t, 200, resp.StatusCode)
	require.Equal(t, "application/json", resp.Header.Get("Content-Type"))

	assert.Equal(t, `{"commonLabels":{"cluster":"testing","project":"quasar_video_proxy","service":"station_starter"},"sensors":[{"value":1,"labels":{"code":"2","msg":"cant play video","sensor":"answerStatus","status":"error"},"kind":"RATE"},{"value":1,"labels":{"hostname":"","playerid":"","provider":"error","sensor":"requestStatus"},"kind":"RATE"}]}`, string(reg.Dump()))
}

func testQuery(t *testing.T, global *server.TGlobal, reqid string, dataInputPath string) (string, int, string) {
	f, err := os.Open(dataInputPath)
	require.NoError(t, err)

	req := httptest.NewRequest(http.MethodPost, "http://localhost/", f)
	req.Header.Add("X-Uid", "123")
	req.Header.Add("X-Req-Id", reqid)

	w := httptest.NewRecorder()
	server.CheckAndGoBass(global, w, req)
	resp := w.Result()

	ansDataBytes, err := ioutil.ReadAll(resp.Body)
	ansData := string(ansDataBytes)
	defer func() {
		if err := resp.Body.Close(); err != nil {
			panic(err)
		}
	}()

	require.NoError(t, err)

	return ansData, resp.StatusCode, resp.Header.Get("Content-Type")
}

func TestBassPusherAll(t *testing.T) {
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

	reqID := "reqid123"
	global := generateGlobalObj(t, logger, clientDownloader, clientRules, clientPusher, reg)

	// input_5776167073012007198 - kinopoisk
	// input_11956629183503470810 - trailer
	// input_8072610027815219148 - trailer2
	// input_11434391912841711256 - not kinopoisk

	testsInput := []string{"xvideos.json", "youtube.json", "vh_blogger.json", "input_5776167073012007198.json", "input_8072610027815219148.json", "input_11434391912841711256.json", "kinopoisk_271516.json", "kinopoisk_886143_128772.json"}

	for _, test := range testsInput {
		answerData, statusCode, contentHeader := testQuery(t, global, reqID, test)
		logger.Info("answer", log.Any("ans", answerData), log.Any("test", test))
		assert.Equal(t, "{\"status\":\"play\",\"msg\":\"success\",\"code\":1}\n", answerData)
		require.Equal(t, 200, statusCode)
		require.Equal(t, "application/json", contentHeader)
	}

	assert.Equal(t, `{"commonLabels":{"cluster":"testing","project":"quasar_video_proxy","service":"station_starter"},"sensors":[{"value":8,"labels":{"code":"1","msg":"success","sensor":"answerStatus","status":"play"},"kind":"RATE"},{"value":1,"labels":{"hostname":"www.xvideos.com","playerid":"xvideos","provider":"yavideo_proxy","sensor":"requestStatus"},"kind":"RATE"},{"value":1,"labels":{"hostname":"www.youtube.com","playerid":"youtube","provider":"yavideo","sensor":"requestStatus"},"kind":"RATE"},{"value":2,"labels":{"hostname":"frontend.vh.yandex.ru","playerid":"vh","provider":"strm","sensor":"requestStatus"},"kind":"RATE"},{"value":1,"labels":{"hostname":"frontend.vh.yandex.ru","playerid":"vh","provider":"kinopoisk","sensor":"requestStatus"},"kind":"RATE"},{"value":1,"labels":{"hostname":"frontend.vh.yandex.ru","playerid":"vh","provider":"yavideo_proxy","sensor":"requestStatus"},"kind":"RATE"},{"value":1,"labels":{"hostname":"www.kinopoisk.ru","playerid":"__raw_json.semantic.schema_embed_url__","provider":"kinopoisk","sensor":"requestStatus"},"kind":"RATE"},{"value":1,"labels":{"hostname":"www.kinopoisk.ru","playerid":"kinopoisk","provider":"yavideo_proxy","sensor":"requestStatus"},"kind":"RATE"}]}`, string(reg.Dump()))

}
