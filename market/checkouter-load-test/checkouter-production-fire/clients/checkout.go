package clients

import (
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/abstractions"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/configs"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/model"
	"fmt"
	"net/url"
	"strconv"
	"strings"
	"time"
)

const MISSING = "MISSING"
const PRICE = "PRICE"

type Checkouter struct {
	ClientConfig
}

type CartRootRequest struct {
	BuyerRegionID int           `json:"buyerRegionId"`
	BuyerCurrency string        `json:"buyerCurrency"`
	Cart          []CartRequest `json:"carts"`
	Promocode     *string       `json:"promocode"`
	CoinIdsToUse  *[]int        `json:"coinIdsToUse,omitempty"`
}

type CartRequest struct {
	ShopID        int        `json:"shopId"`
	Items         []CartItem `json:"items"`
	CartDelivery  `json:"delivery"`
	PaymentMethod *string `json:"paymentMethod"`
	PaymentType   *string `json:"paymentType"`
	Buyer         Buyer   `json:"buyer"`
}

type CartItem struct {
	FeedID            int      `json:"feedId"`
	OfferID           string   `json:"offerId"`
	ShowInfo          string   `json:"showInfo"`
	Count             int      `json:"count"`
	BuyerPrice        uint64   `json:"buyerPrice"`
	BuyerPriceNominal *uint64  `json:"buyerPriceNominal,omitempty"`
	Changes           []string `json:"changes"`
	SupplierType      string   `json:"supplierType"`
}

type CartDelivery struct {
	RegionID int               `json:"regionId"`
	Address  map[string]string `json:"address"`
}

type CartResponse struct {
	DeliveryOptions []DeliveryOption `json:"deliveryOptions"`
	CartItems       []CartItem       `json:"items"`
}

type DeliveryOption struct {
	DeliveryServiceID int                `json:"deliveryServiceId"`
	ID                string             `json:"id"`
	Type              string             `json:"type"`
	Outlets           []CartOutlet       `json:"outlets"`
	DeliveryIntervals []DeliveryInterval `json:"deliveryIntervals"`
	PaymentOptions    []PaymentOptions   `json:"paymentOptions"`
}

type PaymentOptions struct {
	PaymentMethod string `json:"paymentMethod"`
	PaymentType   string `json:"paymentType"`
}

type DeliveryInterval struct {
	Date          string         `json:"date"`
	TimeIntervals []TimeInterval `json:"intervals"`
}

type TimeInterval struct {
	FromTime string `json:"fromTime"`
	ToTime   string `json:"toTime"`
}

type CartRootResponse struct {
	Cart         []CartResponse `json:"carts"`
	CoinIdsToUse *[]int         `json:"coinIdsToUse"`
	Cashback     *Cashback      `json:"cashback,omitempty"`
}

type OrdersByUIDResponse struct {
	Orders []OrderRequest `json:"orders"`
}

type CheckoutRequest struct {
	BuyerRegionID int            `json:"buyerRegionId"`
	BuyerCurrency string         `json:"buyerCurrency"`
	PaymentType   string         `json:"paymentType"`
	PaymentMethod string         `json:"paymentMethod"`
	Orders        []OrderRequest `json:"orders"`
	Buyer         Buyer          `json:"buyer"`
	Promocode     *string        `json:"promocode"`
}

type GetOrdersRequest struct {
	FromDate string   `json:"fromDate"`
	PageInfo PageInfo `json:"pageInfo"`
	Partials []string `json:"partials"`
	RGBs     []string `json:"rgbs"`
}

type PageInfo struct {
	From        int `json:"from"`
	To          int `json:"to"`
	PageSize    int `json:"pageSize"`
	CurrentPage int `json:"currentPage"`
}

type OrderResponse struct {
	ID int `json:"id"`
}

type OrderRequest struct {
	ID           int        `json:"id"`
	ShopID       int        `json:"shopId"`
	Items        []CartItem `json:"items"`
	Delivery     Delivery   `json:"delivery"`
	Notes        string     `json:"notes"`
	CoinIdsToUse *[]int     `json:"coinIdsToUse"`
}

type Delivery struct {
	DeliveryServiceID int               `json:"deliveryServiceId"`
	ID                string            `json:"id"`
	OutletID          *int              `json:"outletId,omitempty"`
	RegionID          int               `json:"regionId"`
	Outlet            *CheckoutOutlet   `json:"outlet,omitempty"`
	Address           map[string]string `json:"address,omitempty"`
	Dates             Dates             `json:"dates,omitempty"`
}

type Buyer struct {
	LastName   string `json:"lastName"`
	FirstName  string `json:"firstName"`
	MiddleName string `json:"middleName"`
	Phone      string `json:"phone"`
	Email      string `json:"email"`
	IP         string `json:"ip"`
	DontCall   bool   `json:"dontCall"`
}

type CartOutlet struct {
	ID int `json:"id"`
}

type CheckoutOutlet struct {
	OutletID int `json:"outletId"`
}

type Dates struct {
	FromDate string `json:"fromDate"`
	ToDate   string `json:"toDate"`
	FromTime string `json:"fromTime"`
	ToTime   string `json:"toTime"`
}

type CheckoutResponse struct {
	CheckedOut bool            `json:"checkedOut"`
	Orders     []OrderResponse `json:"orders"`
}

type CheckoutParams struct {
	Address       map[string]string
	Delivery      Delivery
	PaymentMethod string
}

type Cashback struct {
	Emit  CashbackOptions `json:"emit"`
	Spend CashbackOptions `json:"spend"`
}

type CashbackOptions struct {
	Amount *float64 `json:"amount"`
}

func (checkouter *Checkouter) Cart(shootContext abstractions.ShootContext, req *CartRootRequest, options configs.Options, uid int, useFlash bool) (*CartRootResponse, error) {
	q := url.Values{}

	q.Add("uid", strconv.Itoa(uid))
	q.Add("rgb", "BLUE")
	q.Add("minifyOutlets", "true")
	q.Add("debugAllCourierOptions", "1")
	if options.ShipmentDay >= 0 {
		q.Add("forceShipmentDay", strconv.Itoa(options.ShipmentDay))
	}

	u := checkouter.url
	u.Path = "/cart"
	u.RawQuery = q.Encode()

	requestHeaders := map[string]string{"X-Hit-Rate-Group": "UNLIMIT"}

	var rearrFactors []string

	if options.DoNotShootGo {
		rearrFactors = append(rearrFactors, "disable_external_requests=1")
	}

	if options.CartToReportDegradation {
		rearrFactors = append(rearrFactors, "graceful_degradation_force_level=0")
	}

	if useFlash {
		rearrFactors = append(rearrFactors, "promo_enable_by_shop_promo_id="+options.FlashShopPromoID)
	}

	if len(rearrFactors) > 0 {
		requestHeaders["X-Market-Rearrfactors"] = strings.Join(rearrFactors, ";")
	}

	result := &CartRootResponse{}
	err := checkouter.Post(model.LabelCheckouterCart, shootContext, &u, requestHeaders, req, result, false)

	if err != nil {
		return nil, err
	}

	return result, nil
}

func (checkouter *Checkouter) Checkout(shootContext abstractions.ShootContext, req *CheckoutRequest, options configs.Options, uid int, useFlash bool) (*CheckoutResponse, error) {
	q := url.Values{}

	q.Add("uid", strconv.Itoa(uid))
	q.Add("rgb", "BLUE")
	q.Add("minifyOutlets", "true")
	q.Add("debugAllCourierOptions", "1")
	if options.ShipmentDay >= 0 {
		q.Add("forceShipmentDay", strconv.Itoa(options.ShipmentDay))
	}

	u := checkouter.url
	u.Path = "/checkout"
	u.RawQuery = q.Encode()

	headers := map[string]string{"X-Hit-Rate-Group": "UNLIMIT"}

	var rearrFactors []string

	if options.DoNotShootGo {
		rearrFactors = append(rearrFactors, "disable_external_requests=1")
	}

	if useFlash {
		rearrFactors = append(rearrFactors, "promo_enable_by_shop_promo_id="+options.FlashShopPromoID)
	}

	if len(rearrFactors) > 0 {
		headers["X-Market-Rearrfactors"] = strings.Join(rearrFactors, ";")
	}

	result := &CheckoutResponse{}
	err := checkouter.Post(model.LabelCheckouterCheckout, shootContext, &u, headers, req, result, false)
	if err != nil {
		return nil, err
	}

	for _, order := range result.Orders {
		fmt.Printf("create order: '%v'\n", order.ID)
	}

	return result, nil
}

func (checkouter *Checkouter) Unfreeze(shootContext abstractions.ShootContext, orderID int) error {
	u := checkouter.url
	u.Path = fmt.Sprintf("/orders/%v/unfreeze-stocks", orderID)
	return checkouter.Post(model.LabelCheckouterUnfreeze, shootContext, &u, map[string]string{}, nil, nil, false)
}

func (checkouter *Checkouter) RecentOrders(shootContext abstractions.ShootContext, path string, uid int) error {

	q := url.Values{}
	q.Add("pageSize", "20")
	q.Add("active", "true")
	q.Add("rgb", "BLUE,WHITE")
	q.Add("digitalEnabled", "false")

	addQueryValues(&q, "status", []string{"UNPAID", "PROCESSING", "DELIVERY", "PICKUP", "PENDING", "DELIVERED"})
	addQueryValues(&q, "partials", []string{"DELIVERY", "ITEMS"})
	addQueryValues(&q, "context", []string{"MARKET", "SELF_CHECK", "CHECK_ORDER", "PINGER", "PRODUCTION_TESTING"})

	u := checkouter.url
	u.Path = fmt.Sprintf(path, uid)
	u.RawPath = q.Encode()

	return checkouter.Get(model.LabelCheckouterRecentOrders, shootContext, &u, nil, false)
}

func (checkouter *Checkouter) OptionsAvailabilitiesOfOrders(shootContext abstractions.ShootContext, orderID int, path string, uid int) error {

	q := url.Values{}

	q.Add("clientId", strconv.Itoa(uid))
	q.Add("clientRole", "USER")
	q.Add("orderId", strconv.Itoa(orderID))
	q.Add("rgb", "BLUE,WHITE")
	q.Add("digitalEnabled", "false")

	u := checkouter.url
	u.Path = path
	return checkouter.Get(model.LabelCheckouterOptionsAvailabilities, shootContext, &u, nil, false)
}

func (checkouter *Checkouter) OrdersByUID(shootContext abstractions.ShootContext, path string, uid int) error {

	q := url.Values{}

	q.Add("archived", "false")
	q.Add("toDate", "")
	q.Add("page", "1")
	q.Add("pageSize", "20")

	threeYearsAgo := time.Now().AddDate(-3, 0, 0)
	formatted := fmt.Sprintf(
		"%02d-%02d-%d", threeYearsAgo.Day(), threeYearsAgo.Month(), threeYearsAgo.Year())

	q.Add("fromDate", formatted)
	q.Add("showReturnStatuses", "0")
	q.Add("digitalEnabled", "false")
	q.Add("rgb", "BLUE,WHITE")

	addQueryValues(&q, "partials", []string{"CHANGE_REQUEST", "CASHBACK_EMIT_INFO"})
	addQueryValues(&q, "context", []string{"MARKET", "SELF_CHECK", "CHECK_ORDER", "PINGER", "PRODUCTION_TESTING"})

	u := checkouter.url
	u.Path = fmt.Sprintf(path, uid)
	u.RawPath = q.Encode()
	return checkouter.Get(model.LabelCheckouterOrdersByUID, shootContext, &u, nil, false)
}

func (checkouter *Checkouter) GetOrdersByID(label string, shootContext abstractions.ShootContext, path string, q *url.Values, orderID int) error {

	u := checkouter.url
	u.Path = fmt.Sprintf(path, orderID)
	u.RawPath = q.Encode()

	return checkouter.Get(label, shootContext, &u, nil, false)
}

func (checkouter *Checkouter) GetOrders(shootContext abstractions.ShootContext, req *GetOrdersRequest, path string, uid int) error {
	q := url.Values{}

	q.Add("clientRole", "USER")
	q.Add("clientId", strconv.Itoa(uid))
	q.Add("shopId", "")
	q.Add("archived", "false")

	u := checkouter.url
	u.Path = path
	u.RawPath = q.Encode()

	return checkouter.Post(model.LabelCheckouterGetOrders, shootContext, &u, map[string]string{"charset": "utf-8"}, req, nil, false)
}

func addQueryValues(q *url.Values, key string, values []string) {
	for _, value := range values {
		q.Add(key, value)
	}
}

type CheckouterClient interface {
	Cart(shootContext abstractions.ShootContext, req *CartRootRequest, options configs.Options, uid int, useFlash bool) (*CartRootResponse, error)
	Checkout(shootContext abstractions.ShootContext, req *CheckoutRequest, options configs.Options, uid int, useFlash bool) (*CheckoutResponse, error)
	Unfreeze(shootContext abstractions.ShootContext, orderID int) error
	RecentOrders(shootContext abstractions.ShootContext, path string, uid int) error
	OptionsAvailabilitiesOfOrders(shootContext abstractions.ShootContext, orderID int, path string, uid int) error
	OrdersByUID(shootContext abstractions.ShootContext, path string, uid int) error
	GetOrdersByID(label string, shootContext abstractions.ShootContext, path string, q *url.Values, orderID int) error
	GetOrders(shootContext abstractions.ShootContext, req *GetOrdersRequest, path string, uid int) error
}
