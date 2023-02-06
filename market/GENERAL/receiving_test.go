package receiving

import (
	"a.yandex-team.ru/market/logistics/wms-load/go/sql"
	"net/url"
	"os"
	"testing"

	"a.yandex-team.ru/market/logistics/wms-load/go/model"
)

var webUser = "load_1"
var webPass, _ = os.LookupEnv("WMS")

func TestRunReceiving(t *testing.T) {
	//firstAccept
	connectToDB()
	RunReceiving(
		map[string]string{
			"login":           webUser,
			"tableNumber":     "STAGE",
			"printerName":     "P01",
			"numberOfPallets": "1",
			"receiptKey":      "0000000002",
		},
		model.WebUICredentials{User: webUser, Password: webPass},
		url.URL{
			Scheme: "http",
			Host:   "localhost",
		},
		"firstAccept",
	)
}

func connectToDB() {
	sql.LocalConnectToDB()
}
