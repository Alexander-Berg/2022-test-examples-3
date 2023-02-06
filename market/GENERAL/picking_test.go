package picking

import (
	headers2 "a.yandex-team.ru/library/go/httputil/headers"
	"a.yandex-team.ru/market/logistics/wms-load/go/sql"
	"a.yandex-team.ru/market/logistics/wms-load/go/util"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"os"
	"strconv"
	"testing"
)

var webUser = "load_1"
var webPass, _ = os.LookupEnv("WMS")
var tokenTest = ""

//генерация линий предконсолидации, нужен токен для авторизации
func TestGenerateLineConsolidation(t *testing.T) {
	url := url.URL{
		Scheme: "http",
		Host:   "wms-load-app01e.market.yandex.net",
	}

	for i := 0; i < 15; i++ {
		for j := 1; j <= 12; j++ {
			zone := "zone" + strconv.Itoa(j)
			locDTO := locDTO{
				LocationFlag:   "NONE",
				LocationStatus: "OK",
				LocationType:   "CONSOLIDATION",
				LoseId:         "false",
				Prefix:         "CL",
				Zone:           zone,
			}
			body, err := json.Marshal(locDTO)
			util.CheckError(err)

			response := util.MakeHTTPRequest(string(body), http.MethodPost, getHeaders(), util.GetURL(url, "datacreator/location/loc"), "", "GEN", "test", false, &util.WMSAuth{})
			util.Sleep(1,1)
			fmt.Printf("%v %v\n", response.StatusCode, response.Body)
		}
	}
}

func TestGenerateSortStationLoc(t *testing.T) {
	sql.LocalConnectToDB()

	sortStationPrefix := "s"
	addwho := "createData"
	var sortStation string
	var sortStationLoc string
	for i := 10; i <= 16; i++ {
		sortStation = sortStationPrefix + strconv.Itoa(i)
		for j := 21; j <= 35; j++ {
			sortStationLoc = sortStation + "-" + strconv.Itoa(j)
			fmt.Printf("%v\n", sortStationLoc)
			sql.CreateSortStationLoc(sortStation, sortStationLoc, addwho)
		}
	}
}

func TestGetCartsFromLines(t *testing.T) {
	sql.LocalConnectToDB()
	line := "S10"
	carts := sql.GetCartsFromConsolidationLine(line)
	fmt.Println(carts)
}

func TestGetSerialsFromCart(t *testing.T) {
	sql.LocalConnectToDB()
	cart := "CART00633"
	serials := sql.GetSkusAndSerialsFromCart(cart)

	for _, v := range serials {
		fmt.Printf("%v %v\n", v.Sku, v.Serial)
	}

}

type locDTO struct {
	LocationFlag   string `json:"locationFlag"`
	LocationStatus string `json:"locationStatus"`
	LocationType   string `json:"locationType"`
	LoseId         string `json:"loseId"`
	Prefix         string `json:"prefix"`
	Zone           string `json:"zone"`
}

func getHeaders() map[string]string {
	headers := map[string]string{
		headers2.ContentTypeKey: headers2.TypeApplicationJSON.String(),
		"X-Token":               tokenTest,
	}
	return headers
}
