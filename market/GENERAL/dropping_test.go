package dropping

import (
	"net/url"
	"os"
	"testing"

	"a.yandex-team.ru/market/logistics/wms-load/go/sql"
	"a.yandex-team.ru/market/logistics/wms-load/go/util"
)

var webUser = "load_1"
var webPass, _ = os.LookupEnv("WMS")
var tokenTest = ""

func TestGenerateDrop(t *testing.T) {
	url := url.URL{
		Scheme: "http",
		Host:   "wms-load-app01e.market.yandex.net",
	}
	loginURL := util.GetURL(url, login)
	wmsAuth := util.DoAuth(loginURL, webUser, webPass)
	generateDrop(&wmsAuth, "test")
}

func TestGetPackFromPacking(t *testing.T) {
	sql.LocalConnectToDB()
	getPackFromPacking("test")
}
