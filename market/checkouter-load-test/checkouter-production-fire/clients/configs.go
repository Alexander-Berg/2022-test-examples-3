package clients

import (
	"a.yandex-team.ru/library/go/yandex/tvm"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/abstractions"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/util"
	"fmt"
	"net/url"
)

type ClientConfig struct {
	url              url.URL
	headerSetters    []func(r map[string]string) error
	rpsLimiter       RpsLimiter
	requestProcessor abstractions.RequestProcessor
}

func getAnonymousClient(url url.URL, limiter RpsLimiter, requestProcessor abstractions.RequestProcessor) ClientConfig {
	return getAuthenticatedClient(url, limiter, requestProcessor, func(r map[string]string) error {
		return nil
	})
}

func getAuthenticatedClient(url url.URL, limiter RpsLimiter, requestProcessor abstractions.RequestProcessor, headerSetter func(r map[string]string) error) ClientConfig {
	setters := []func(r map[string]string) error{headerSetter, func(r map[string]string) error {
		id, ok := util.GetRequestID()
		if ok {
			r[RequestID] = *id
			return nil
		} else {
			return fmt.Errorf("request-id generation failed")
		}
	}}
	return ClientConfig{
		url:              url,
		headerSetters:    setters,
		rpsLimiter:       limiter,
		requestProcessor: requestProcessor,
	}

}

const tvmTestID tvm.ClientID = 2028634
const tvmProdID tvm.ClientID = 2028636

func (c *ClientConfig) enrichHeaders(headers map[string]string) error {
	for _, setter := range c.headerSetters {
		err := setter(headers)
		if err != nil {
			return err
		}
	}
	return nil
}

//http://checkouter.tst.vs.market.yandex.net:39011
func getCheckouterTestURL() url.URL {
	return url.URL{Scheme: "https", Host: "checkouter.tst.vs.market.yandex.net:39011"}
}

//https://checkouter.market.http.yandex.net:39011
func getCheckouterProdURL() url.URL {
	return url.URL{Scheme: "https", Host: "checkouter.market.http.yandex.net:39011"}
}

//http://report.tst.vs.market.yandex.net:17051
func getReportTestURL() url.URL {
	return url.URL{Scheme: "http", Host: "report.tst.vs.market.yandex.net:17051"}
}

//http://int-report.vs.market.yandex.net:17151
func getReportProdURL() url.URL {
	return url.URL{Scheme: "http", Host: "int-report.vs.market.yandex.net:17151"}
}

//https://bos.tst.vs.market.yandex.net
func getStockStorageTestURL() url.URL {
	return url.URL{Scheme: "https", Host: "bos.tst.vs.market.yandex.net"}
}

//https://bos.vs.market.yandex.net
func getStockStorageProdURL() url.URL {
	return url.URL{Scheme: "https", Host: "bos.vs.market.yandex.net"}
}

//http://active.idxapi.tst.vs.market.yandex.net:29334
func getOTraceTestURL() url.URL {
	return url.URL{Scheme: "http", Host: "active.idxapi.tst.vs.market.yandex.net:29334"}
}

//http://active.idxapi.vs.market.yandex.net:29334
func getOTraceProdURL() url.URL {
	return url.URL{Scheme: "http", Host: "active.idxapi.vs.market.yandex.net:29334"}
}

//https://market-loyalty-ssl.vs.market.yandex.net:35816
func getLoyaltyProdURL() url.URL {
	return url.URL{Scheme: "https", Host: "market-loyalty-ssl.vs.market.yandex.net:35816"}
}

//https://market-loyalty-ssl.tst.vs.market.yandex.net:35816
func getLoyaltyTestURL() url.URL {
	return url.URL{Scheme: "https", Host: "market-loyalty-ssl.tst.vs.market.yandex.net:35816"}
}

type YQLConfig struct {
	StocksTable           string
	PartnersTable         string
	SuppliersTable        string
	LogisticPartnersTable string
}

var prodYqlConfig = YQLConfig{
	"home/market/production/mstat/dictionaries/stock_sku/1d/latest",
	"home/market/production/mbi/dictionaries/partner_biz_snapshot/latest",
	"home/market/production/mstat/dictionaries/suppliers/latest",
	"home/market/production/mstat/dictionaries/logistics-management-service/partner/latest",
}

var testYqlConfig = YQLConfig{
	"home/market/prestable/fulfillment/utilized_stocks/1h/latest",
	"home/market/testing/mbi/dictionaries/partner_biz_snapshot/latest",
	"home/market/prestable/mstat/dictionaries/suppliers/latest",
	"home/market/prestable/mstat/dictionaries/logistics-management-service/partner/latest",
}
