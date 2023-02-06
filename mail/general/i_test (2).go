package server

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	"a.yandex-team.ru/mail/iex/taksa/client"
	"a.yandex-team.ru/mail/iex/taksa/errs"
	"a.yandex-team.ru/mail/iex/taksa/logger"
	"a.yandex-team.ru/mail/iex/taksa/request"
)

import . "a.yandex-team.ru/mail/iex/matchers"
import . "a.yandex-team.ru/mail/iex/matchers/httpresp"

var (
	srv *httptest.Server
)

type dummyMethod struct {
	Response string
	Err      error
	Timeout  time.Duration
}

func (method dummyMethod) Do(r request.Interface, l logger.Interface, c client.Interface) (string, error) {
	time.Sleep(10 * time.Millisecond)
	return method.Response, method.Err
}

func (method dummyMethod) GetTimeout() time.Duration {
	return method.Timeout
}

func init() {
	logger.StartWriter("", "")
	client.Init(client.Default())
	go Launch(Config{Port: 1234})
}

func makeURL(srv *httptest.Server, method string) string {
	return fmt.Sprintf("%s%s", srv.URL, method)
}

func TestApi_zeroTimeout_returnsErrorTimeout(t *testing.T) {
	srv := httptest.NewServer(MakeAPIHandler(dummyMethod{Timeout: 0}))
	defer srv.Close()
	resp, _ := http.Get(makeURL(srv, "/list"))
	var err errs.InternalError
	e := json.NewDecoder(resp.Body).Decode(&err)
	AssertThat(t, e, Is{V: error(nil)})
	AssertThat(t, resp, Code{C: http.StatusInternalServerError})
	AssertThat(t, err.Err, Is{V: "taksa timeout"})
}

func TestApi_errorBadRequest_returns400(t *testing.T) {
	srv := httptest.NewServer(MakeAPIHandler(dummyMethod{Timeout: 100 * time.Millisecond, Err: errs.BadRequest{Err: "e"}}))
	defer srv.Close()
	resp, _ := http.Get(makeURL(srv, "/list"))
	var err errs.BadRequest
	e := json.NewDecoder(resp.Body).Decode(&err)
	AssertThat(t, e, Is{V: error(nil)})
	AssertThat(t, resp, Code{C: http.StatusBadRequest})
	AssertThat(t, err.Err, Is{V: "e"})
}

func TestApi_errorInternalServerError_returns500(t *testing.T) {
	srv := httptest.NewServer(MakeAPIHandler(dummyMethod{Timeout: 100 * time.Millisecond, Err: errs.InternalError{Err: "e"}}))
	defer srv.Close()
	resp, _ := http.Get(makeURL(srv, "/list"))
	var err errs.InternalError
	e := json.NewDecoder(resp.Body).Decode(&err)
	AssertThat(t, e, Is{V: error(nil)})
	AssertThat(t, resp, Code{C: http.StatusInternalServerError})
	AssertThat(t, err.Err, Is{V: "e"})
}

func TestApi_ok_returnsResponse(t *testing.T) {
	srv := httptest.NewServer(MakeAPIHandler(dummyMethod{Timeout: 100 * time.Millisecond, Response: "response"}))
	defer srv.Close()
	resp, _ := http.Get(makeURL(srv, "/list"))
	body, _ := ioutil.ReadAll(resp.Body)
	_ = resp.Body.Close()
	AssertThat(t, strings.TrimSpace(string(body)), Is{V: "response"})
	AssertThat(t, resp, Code{C: http.StatusOK})
}

func TestApi_NotFound_returnsResponse(t *testing.T) {
	srv := httptest.NewServer(MakeAPIHandler(NotFound{}))
	defer srv.Close()
	resp, _ := http.Get(makeURL(srv, "/notfound"))
	var err errs.BadRequest
	e := json.NewDecoder(resp.Body).Decode(&err)
	AssertThat(t, e, Is{V: error(nil)})
	AssertThat(t, err.Err, Is{V: "not found"})
	AssertThat(t, resp, Code{C: http.StatusBadRequest})
}

func TestServer_ping_pong(t *testing.T) {
	resp, err := http.Get("http://localhost:1234/ping")
	AssertThat(t, err, Is{V: error(nil)})
	body, _ := ioutil.ReadAll(resp.Body)
	_ = resp.Body.Close()
	AssertThat(t, strings.TrimSpace(string(body)), Is{V: "pong"})
	AssertThat(t, resp, Code{C: http.StatusOK})
}

func TestServer_unhandled_404(t *testing.T) {
	resp, err := http.Get("http://localhost:1234/unhandled")
	AssertThat(t, err, Is{V: error(nil)})
	AssertThat(t, resp, Code{C: http.StatusNotFound})
}

func checkChan(t *testing.T, code Ctrl) {
	c := <-Control
	AssertThat(t, c, Is{V: code})
}

func TestServer_ctrlStop_sendsStopToControlChan(t *testing.T) {
	go checkChan(t, STOP)
	_, _ = http.Get("http://localhost:1234/ctrl/stop")
}

func TestServer_ctrlReload_sendsReloadToControlChan(t *testing.T) {
	go checkChan(t, RELOAD)
	_, _ = http.Get("http://localhost:1234/ctrl/reload")
}

func TestServer_ReloadWithNewPort_listensToNewPort(t *testing.T) {
	Reload(Config{Port: 1235})
	time.Sleep(10 * time.Millisecond)
	resp, err := http.Get("http://localhost:1235/unhandled")
	AssertThat(t, err, Is{V: error(nil)})
	AssertThat(t, resp, Code{C: http.StatusNotFound})
}

func TestServer_ReloadWithSamePort_listensToSamePort(t *testing.T) {
	Reload(Config{Port: 1235})
	time.Sleep(10 * time.Millisecond)
	resp, err := http.Get("http://localhost:1235/unhandled")
	AssertThat(t, err, Is{V: error(nil)})
	AssertThat(t, resp, Code{C: http.StatusNotFound})
}

func TestServer_Stop_doesnotListen(t *testing.T) {
	Shutdown()
	time.Sleep(10 * time.Millisecond)
	_, err := http.Get("http://localhost:1235/unhandled")
	AssertThat(t, err, Not{V: Is{V: error(nil)}})
}
