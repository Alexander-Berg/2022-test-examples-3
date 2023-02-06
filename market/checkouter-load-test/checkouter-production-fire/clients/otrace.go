package clients

import (
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/abstractions"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/configs"
	"net/url"
	"path"
	"strconv"
)

type OTrace struct {
	ClientConfig
}

func (otrace *OTrace) GetOfferInfo(shootContext abstractions.ShootContext, stock Stock, options configs.Options) (*OTraceOfferRoot, error) {
	q := &url.Values{}
	q.Add("offer", stock.Sku)
	q.Add("supplier", strconv.Itoa(stock.SupplierID))
	q.Add("warehouse", strconv.Itoa(stock.WarehouseID))
	q.Add("region", strconv.Itoa(options.RegionID))
	q.Add("format", "json")

	u := otrace.url
	u.Path = path.Join(u.Path, "/v1/otrace")
	u.RawQuery = q.Encode()
	result := &OTraceOfferRoot{}
	err := otrace.Get("index otrace", shootContext, &u, result, false)

	if err != nil {
		return nil, err
	}

	return result, nil
}

type OTraceOfferRoot struct {
	Offer *OTraceOffer `json:"offer"`
	Urls  *OTraceUrls  `json:"urls"`
}

type OTraceOffer struct {
	FeedID  string `json:"feed_id"`
	OfferID string `json:"offer_id"`
}

type OTraceUrls struct {
	Report *OTraceReportUrls `json:"report"`
}

type OTraceReportUrls struct {
	OfferInfoURL string `json:"offer_info"`
}
type OTraceClient interface {
	GetOfferInfo(shootContext abstractions.ShootContext, stock Stock, options configs.Options) (*OTraceOfferRoot, error)
}
