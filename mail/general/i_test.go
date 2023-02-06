package list

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"net"
	"net/http"
	"net/http/httptest"
	"net/url"
	"strconv"
	"testing"
	"time"

	"a.yandex-team.ru/mail/iex/taksa/client"
	"a.yandex-team.ru/mail/iex/taksa/errs"
	"a.yandex-team.ru/mail/iex/taksa/logger"
	"a.yandex-team.ru/mail/iex/taksa/server"
	"a.yandex-team.ru/mail/iex/taksa/tutil"
	"a.yandex-team.ru/mail/iex/taksa/types"
	"a.yandex-team.ru/mail/iex/taksa/widgets"
	"a.yandex-team.ru/mail/iex/taksa/widgets/avia"
	"a.yandex-team.ru/mail/iex/taksa/widgets/common"
)

import . "a.yandex-team.ru/mail/iex/matchers"
import . "a.yandex-team.ru/mail/iex/matchers/httpresp"

var (
	srv *httptest.Server
)

func init() {
	typs, _ := types.Parse("5")
	srv = httptest.NewServer(server.MakeAPIHandler(
		Method{
			Cfg: Config{
				IexHost: "localhost",
				IexPort: 1234,
				IexPath: "facts",
				Timeout: tutil.Timeout(time.Duration(5) * time.Second),
			},
			Wcfg: widgets.Config{Avia: avia.Config{Types: typs}}}))
	logger.StartWriter("", "")
	client.Init(client.Default())
}

func makeURL(srv *httptest.Server, method string) string {
	return fmt.Sprintf("%s%s", srv.URL, method)
}

type JSON struct {
	Value interface{}
}

func (inputJson JSON) Match(i interface{}) bool {
	resp := i.(io.ReadCloser)
	return nil == json.NewDecoder(resp).Decode(inputJson.Value)
}

func (inputJson JSON) String() string {
	return fmt.Sprintf("json %v", inputJson.Value)
}

func TestList_noUid_returnsErrorUidRequired(t *testing.T) {
	resp, _ := http.Get(makeURL(srv, "/list"))
	var err errs.BadRequest
	AssertThat(t, resp.Body, JSON{&err})
	AssertThat(t, resp, Code{C: http.StatusBadRequest})
	AssertThat(t, err.Err, Is{V: "uid required"})
}

func TestList_noTypes_noError(t *testing.T) {
	resp, _ := http.Post(makeURL(srv, "/list?uid=1&version=hound"), "application/json", bytes.NewReader([]byte(postNoTypes)))
	var w common.Response
	AssertThat(t, resp.Body, JSON{&w})
	AssertThat(t, resp, Code{C: http.StatusOK})
	AssertThat(t, len(w.Widgets), Is{V: 0})
}

func TestList_noIex_givesInternalError(t *testing.T) {
	resp, _ := http.Post(makeURL(srv, "/list?uid=1&version=hound"), "application/json", bytes.NewReader([]byte(postTypes5And16)))
	var err errs.InternalError
	AssertThat(t, resp.Body, JSON{&err})
	AssertThat(t, resp, Code{C: http.StatusInternalServerError})
	AssertThat(t, err.Err, Is{V: "iex fetch failed"})
}

func hostport(u string) string {
	uri, _ := url.Parse(u)
	return uri.Host
}

func hostportnum(u string) (string, int) {
	host, portS, _ := net.SplitHostPort(hostport(u))
	port, _ := strconv.Atoi(portS)
	return host, port
}

func mockThirdParty(code int, body string, takes time.Duration) *httptest.Server {
	mux := http.NewServeMux()
	mux.HandleFunc("/facts", func(w http.ResponseWriter, r *http.Request) {
		time.Sleep(takes)
		w.WriteHeader(code)
		_, _ = w.Write([]byte(body))
	})
	return httptest.NewServer(mux)
}

func TestList_500Iex_givesInternalError(t *testing.T) {
	iexSrv := mockThirdParty(500, `{}`, 0)
	defer iexSrv.Close()
	host, port := hostportnum(iexSrv.URL)
	typs, _ := types.Parse("5")
	srv2 := httptest.NewServer(server.MakeAPIHandler(
		Method{Cfg: Config{IexHost: host, IexPort: port, IexPath: "facts", Timeout: tutil.Timeout(time.Duration(5) * time.Second)}, Wcfg: widgets.Config{Avia: avia.Config{Types: typs}}}))
	resp, _ := http.Post(makeURL(srv2, "/list?uid=1&version=hound"), "application/json", bytes.NewReader([]byte(postTypes5And16)))
	var err errs.InternalError
	AssertThat(t, resp.Body, JSON{&err})
	AssertThat(t, resp, Code{C: http.StatusInternalServerError})
	AssertThat(t, err.Err, Is{V: "iex fetch failed"})
}

func TestList_notjsonIex_givesInternalError(t *testing.T) {
	iexSrv := mockThirdParty(200, `qwe`, 0)
	defer iexSrv.Close()
	host, port := hostportnum(iexSrv.URL)
	typs, _ := types.Parse("5")
	srv2 := httptest.NewServer(server.MakeAPIHandler(
		Method{Cfg: Config{IexHost: host, IexPort: port, IexPath: "facts", Timeout: tutil.Timeout(time.Duration(5) * time.Second)}, Wcfg: widgets.Config{Avia: avia.Config{Types: typs}}}))
	resp, _ := http.Post(makeURL(srv2, "/list?uid=1&version=hound"), "application/json", bytes.NewReader([]byte(postTypes5And16)))
	var err errs.InternalError
	AssertThat(t, resp.Body, JSON{&err})
	AssertThat(t, resp, Code{C: http.StatusInternalServerError})
	AssertThat(t, err.Err, Is{V: "iex fetch failed"})
}

func TestList_timeoutIex_givesInternalError(t *testing.T) {
	iexSrv := mockThirdParty(200, `qwe`, 1*time.Second)
	defer iexSrv.Close()
	host, port := hostportnum(iexSrv.URL)
	typs, _ := types.Parse("5")
	srv2 := httptest.NewServer(server.MakeAPIHandler(
		Method{Cfg: Config{IexHost: host, IexPort: port, IexPath: "facts", Timeout: tutil.Timeout(time.Duration(5) * time.Second)}, Wcfg: widgets.Config{Avia: avia.Config{Types: typs}}}))
	resp, _ := http.Post(makeURL(srv2, "/list?uid=1&version=hound"), "application/json", bytes.NewReader([]byte(postTypes5And16)))
	var err errs.InternalError
	AssertThat(t, resp.Body, JSON{&err})
	AssertThat(t, resp, Code{C: http.StatusInternalServerError})
	AssertThat(t, err.Err, Is{V: "iex fetch failed"})
}

func TestList_emptyIex_givesZeroWidgets(t *testing.T) {
	iexSrv := mockThirdParty(200, `{}`, 0)
	defer iexSrv.Close()
	host, port := hostportnum(iexSrv.URL)
	typs, _ := types.Parse("5")
	srv2 := httptest.NewServer(server.MakeAPIHandler(
		Method{Cfg: Config{IexHost: host, IexPort: port, IexPath: "facts", Timeout: tutil.Timeout(time.Duration(5) * time.Second)}, Wcfg: widgets.Config{Avia: avia.Config{Types: typs}}}))
	resp, _ := http.Post(makeURL(srv2, "/list?uid=1&version=hound"), "application/json", bytes.NewReader([]byte(postTypes5And16)))
	var w common.Response
	AssertThat(t, resp.Body, JSON{&w})
	AssertThat(t, resp, Code{C: http.StatusOK})
	AssertThat(t, len(w.Widgets), Is{V: 0})
}

func TestList_goodIexAvia_givesWidgets(t *testing.T) {
	iexSrv := mockThirdParty(200, goodIexAviaAnswer, 0)
	defer iexSrv.Close()
	host, port := hostportnum(iexSrv.URL)

	wcfg := widgets.Default()
	wcfg.Avia.Types, _ = types.Parse("5&16")

	srv2 := httptest.NewServer(server.MakeAPIHandler(
		Method{Cfg: Config{IexHost: host, IexPort: port, IexPath: "facts", Timeout: tutil.Timeout(time.Duration(5) * time.Second)}, Wcfg: wcfg}))
	resp, _ := http.Post(makeURL(srv2, "/list?uid=1&version=hound"), "application/json", bytes.NewReader([]byte(postTypes5And16)))
	var w common.Response
	AssertThat(t, resp.Body, JSON{&w})
	AssertThat(t, resp, Code{C: http.StatusOK})
	AssertThat(t, len(w.Widgets), Is{V: 1})
}

func TestList_goodIexHotel_givesWidgets(t *testing.T) {
	iexSrv := mockThirdParty(200, goodIexHotelAnswer, 0)
	defer iexSrv.Close()
	host, port := hostportnum(iexSrv.URL)

	wcfg := widgets.Default()
	wcfg.Hotels.Types, _ = types.Parse("19&35")

	srv2 := httptest.NewServer(server.MakeAPIHandler(
		Method{Cfg: Config{IexHost: host, IexPort: port, IexPath: "facts", Timeout: tutil.Timeout(time.Duration(5) * time.Second)}, Wcfg: wcfg}))
	resp, _ := http.Post(makeURL(srv2, "/list?uid=1&version=hound"), "application/json", bytes.NewReader([]byte(postTypes19And35)))
	var w common.Response
	AssertThat(t, resp.Body, JSON{&w})
	AssertThat(t, resp, Code{C: http.StatusOK})
	AssertThat(t, len(w.Widgets), Is{V: 1})
}

func TestList_goodIexBounce_givesWidgets(t *testing.T) {
	iexSrv := mockThirdParty(200, goodIexBounceAnswer, 0)
	defer iexSrv.Close()
	host, port := hostportnum(iexSrv.URL)

	wcfg := widgets.Default()
	wcfg.Bounce.Types, _ = types.Parse("8")

	srv2 := httptest.NewServer(server.MakeAPIHandler(
		Method{Cfg: Config{IexHost: host, IexPort: port, IexPath: "facts", Timeout: tutil.Timeout(time.Duration(5) * time.Second)}, Wcfg: wcfg}))
	resp, _ := http.Post(makeURL(srv2, "/list?uid=1&version=hound"), "application/json", bytes.NewReader([]byte(postTypes8)))
	var w common.Response
	AssertThat(t, resp.Body, JSON{&w})
	AssertThat(t, resp, Code{C: http.StatusOK})
	AssertThat(t, len(w.Widgets), Is{V: 1})
}

func TestList_goodIexOneLink_givesWidgets(t *testing.T) {
	iexSrv := mockThirdParty(200, goodIexOneLinkAnswer, 0)
	defer iexSrv.Close()
	host, port := hostportnum(iexSrv.URL)

	wcfg := widgets.Default()
	wcfg.OneLink.Types, _ = types.Parse("2")

	srv2 := httptest.NewServer(server.MakeAPIHandler(
		Method{Cfg: Config{IexHost: host, IexPort: port, IexPath: "facts", Timeout: tutil.Timeout(time.Duration(5) * time.Second)}, Wcfg: wcfg}))
	resp, _ := http.Post(makeURL(srv2, "/list?uid=1&version=hound"), "application/json", bytes.NewReader([]byte(postTypes2)))
	var w common.Response
	AssertThat(t, resp.Body, JSON{&w})
	AssertThat(t, resp, Code{C: http.StatusOK})
	AssertThat(t, len(w.Widgets), Is{V: 1})
}

func TestList_goodIexEshop_givesWidgets(t *testing.T) {
	iexSrv := mockThirdParty(200, goodIexEshopAnswer, 0)
	defer iexSrv.Close()
	host, port := hostportnum(iexSrv.URL)

	wcfg := widgets.Default()
	wcfg.Eshop.Types, _ = types.Parse("6")

	srv2 := httptest.NewServer(server.MakeAPIHandler(
		Method{Cfg: Config{IexHost: host, IexPort: port, IexPath: "facts", Timeout: tutil.Timeout(time.Duration(5) * time.Second)}, Wcfg: wcfg}))
	resp, _ := http.Post(makeURL(srv2, "/list?uid=1&version=hound"), "application/json", bytes.NewReader([]byte(postTypes6)))
	var w common.Response
	AssertThat(t, resp.Body, JSON{&w})
	AssertThat(t, resp, Code{C: http.StatusOK})
	AssertThat(t, len(w.Widgets), Is{V: 1})
}

func TestList_goodIexSnippet_givesWidgets(t *testing.T) {
	iexSrv := mockThirdParty(200, goodIexSnippetAnswer, 0)
	defer iexSrv.Close()
	host, port := hostportnum(iexSrv.URL)

	wcfg := widgets.Default()
	wcfg.Snippet.Types, _ = types.Parse("4")

	srv2 := httptest.NewServer(server.MakeAPIHandler(
		Method{Cfg: Config{IexHost: host, IexPort: port, IexPath: "facts", Timeout: tutil.Timeout(time.Duration(5) * time.Second)}, Wcfg: wcfg}))
	resp, _ := http.Post(makeURL(srv2, "/list?uid=1&version=hound"), "application/json", bytes.NewReader([]byte(postTypes4)))
	var w common.Response
	AssertThat(t, resp.Body, JSON{&w})
	AssertThat(t, resp, Code{C: http.StatusOK})
	AssertThat(t, len(w.Widgets), Is{V: 1})
}

func TestList_goodIexTracker_givesWidgets(t *testing.T) {
	iexSrv := mockThirdParty(200, goodIexTrackerAnswer, 0)
	defer iexSrv.Close()
	host, port := hostportnum(iexSrv.URL)

	wcfg := widgets.Default()
	wcfg.Tracker.Types, _ = types.Parse("61")

	srv2 := httptest.NewServer(server.MakeAPIHandler(
		Method{Cfg: Config{IexHost: host, IexPort: port, IexPath: "facts", Timeout: tutil.Timeout(time.Duration(5) * time.Second)}, Wcfg: wcfg}))
	resp, _ := http.Post(makeURL(srv2, "/list?uid=1&version=hound"), "application/json", bytes.NewReader([]byte(postTypes61)))
	var w common.Response
	AssertThat(t, resp.Body, JSON{&w})
	AssertThat(t, resp, Code{C: http.StatusOK})
	AssertThat(t, len(w.Widgets), Is{V: 1})
}

func TestList_goodIexCalendar_givesWidgets(t *testing.T) {
	iexSrv := mockThirdParty(200, goodIexCalendarAnswer, 0)
	defer iexSrv.Close()
	host, port := hostportnum(iexSrv.URL)

	wcfg := widgets.Default()
	wcfg.Calendar.Types, _ = types.Parse("42")

	srv2 := httptest.NewServer(server.MakeAPIHandler(
		Method{Cfg: Config{IexHost: host, IexPort: port, IexPath: "facts", Timeout: tutil.Timeout(time.Duration(5) * time.Second)}, Wcfg: wcfg}))
	resp, _ := http.Post(makeURL(srv2, "/list?uid=1&version=hound"), "application/json", bytes.NewReader([]byte(postTypes42)))
	var w common.Response
	AssertThat(t, resp.Body, JSON{&w})
	AssertThat(t, resp, Code{C: http.StatusOK})
	AssertThat(t, len(w.Widgets), Is{V: 1})
}
