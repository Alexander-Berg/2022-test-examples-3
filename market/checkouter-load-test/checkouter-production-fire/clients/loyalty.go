package clients

import (
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/abstractions"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/configs"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/model"
	"fmt"
)

type LoyaltyClient interface {
	CreateCoin(ctx abstractions.ShootContext, promoID int, uid int) (int, error)
	GetCoins(ctx abstractions.ShootContext, uid int) (coinIDs []int, err error)
	FetchBonusesForCart(ctx abstractions.ShootContext, uid int, regionID int, body OrderItemsRequest) (response *СoinsForCart, err error)
}

func NewLoyaltyClient(environment string, tvmSecret string, limiter RpsLimiter, deps *configs.ClientDependencies) LoyaltyClient {
	requestProcessor := deps.RequestProcessor
	ticketFunc := deps.TicketFunc
	switch environment {
	case configs.Production:
		return &Loyalty{getAuthenticatedClient(getLoyaltyProdURL(), limiter, requestProcessor, ticketFunc(tvmProdID, 2014006, tvmSecret))}
	case configs.Testing:
		return &Loyalty{getAuthenticatedClient(getLoyaltyTestURL(), limiter, requestProcessor, ticketFunc(tvmTestID, 2013484, tvmSecret))}
	}
	panic(fmt.Errorf("unknown environment %s", environment))
}

type Loyalty struct {
	ClientConfig
}

// GetCoins invokes /coins/person
func (l *Loyalty) GetCoins(ctx abstractions.ShootContext, uid int) (coinIDs []int, err error) {
	coinsResponse := &coinsForUser{}
	err = l.Get(
		"loyalty get-coins",
		ctx,
		l.WithPathAndQuery("/coins/person", fmt.Sprintf("uid=%d", uid)),
		coinsResponse,
		false,
	)
	if err != nil {
		return
	}
	for _, coin := range coinsResponse.Coins {
		coinIDs = append(coinIDs, coin.ID)
	}
	return
}

// CreateCoin invokes /coins/createCoin/v2
func (l *Loyalty) CreateCoin(ctx abstractions.ShootContext, promoID int, uid int) (coin int, err error) {
	response := &UserCoinResponse{}
	err = l.Post(
		"loyalty create-coins",
		ctx,
		l.WithPathAndQuery("/coins/createCoin/v2", fmt.Sprintf("promoId=%d&reason=ORDER", promoID)),
		map[string]string{},
		singleCoinCreationRequest{
			IdempotencyKey: fmt.Sprintf("%d", uid),
			UID:            uid,
		},
		response,
		false,
	)
	if err != nil {
		return
	}
	return response.ID, nil
}

// FetchBonusesForCart invokes /coins/cart/WHITE/v2
func (l *Loyalty) FetchBonusesForCart(ctx abstractions.ShootContext, uid int, regionID int, body OrderItemsRequest) (response *СoinsForCart, err error) {
	response = &СoinsForCart{}
	err = l.Post(
		model.LabelLoyaltyFetchBonusesForCart,
		ctx,
		l.WithPathAndQuery("/coins/cart/WHITE/v2", fmt.Sprintf("uid=%d&regionId=%d", uid, regionID)),
		map[string]string{},
		body,
		response,
		false,
	)
	if err != nil {
		return
	}
	return response, err
}

type coinsForUser struct {
	Coins []UserCoinResponse `json:"coins"`
}

type UserCoinResponse struct {
	ID int `json:"id"`
}

type singleCoinCreationRequest struct {
	IdempotencyKey string `json:"idempotencyKey"`
	UID            int    `json:"uid"`
}

type СoinsForCart struct {
	ApplicableCoins []UserCoinResponse            `json:"applicableCoins"`
	DisabledCoins   map[string][]UserCoinResponse `json:"disabledCoins"`
}

type OrderItemsRequest struct {
	Items []CartItem `json:"items"`
}
