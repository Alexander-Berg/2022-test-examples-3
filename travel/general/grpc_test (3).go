package handler

import (
	"context"
	"net/http"
	"testing"
	"time"

	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"google.golang.org/genproto/googleapis/type/date"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"

	"a.yandex-team.ru/library/go/core/log/nop"
	"a.yandex-team.ru/library/go/core/xerrors"
	priceAPI "a.yandex-team.ru/travel/app/backend/api/aviaprice/v1"
	common "a.yandex-team.ru/travel/app/backend/api/common/v1"
	"a.yandex-team.ru/travel/app/backend/internal/avia"
	"a.yandex-team.ru/travel/app/backend/internal/lib/clientscommon"
	"a.yandex-team.ru/travel/app/backend/internal/lib/priceindexclient"
	"a.yandex-team.ru/travel/app/backend/internal/points"
	"a.yandex-team.ru/travel/proto/dicts/rasp"
)

type priceIndexClientMock struct {
	mock.Mock
}

func (c *priceIndexClientMock) GetPrice(ctx context.Context, req *priceindexclient.PriceReq) (*priceindexclient.PriceRsp, error) {
	args := c.Called(ctx, req)
	return args.Get(0).(*priceindexclient.PriceRsp), args.Error(1)
}

func (c *priceIndexClientMock) GetPriceBatch(ctx context.Context, req *priceindexclient.PriceBatchReq) (*priceindexclient.PriceBatchRsp, error) {
	args := c.Called(ctx, req)
	return args.Get(0).(*priceindexclient.PriceBatchRsp), args.Error(1)
}

type pointParserMock struct {
	mock.Mock
}

func (c *pointParserMock) ParseByPointKey(pointKey string) (point points.Point, err error) {
	args := c.Called(pointKey)
	return args.Get(0).(points.Point), args.Error(1)
}

func TestGetPrice_Simple(t *testing.T) {
	fromInt := 213
	toInt := 53
	request := &priceindexclient.PriceBatchReq{
		Requests: []*priceindexclient.BatchReq{
			{
				FromID: fromInt,
				ToID:   toInt,
				DateForward: &date.Date{
					Year:  2022,
					Month: 5,
					Day:   1,
				},
				DateBackward: &date.Date{
					Year:  2022,
					Month: 5,
					Day:   1,
				},
			},
			{
				FromID: fromInt,
				ToID:   toInt,
				DateForward: &date.Date{
					Year:  2022,
					Month: 5,
					Day:   1,
				},
				DateBackward: &date.Date{
					Year:  2022,
					Month: 5,
					Day:   2,
				},
			},
		},
	}
	piMock := new(priceIndexClientMock)
	ctx := context.Background()
	bd := "2022-05-01"
	bd2 := "2022-05-02"
	piMock.On("GetPriceBatch", ctx, request).Return(&priceindexclient.PriceBatchRsp{
		Status: "ok",
		Data: []priceindexclient.PriceBatchData{
			{
				FromID:       213,
				ToID:         53,
				ForwardDate:  "2022-05-01",
				BackwardDate: &bd,
				MinPrice: priceindexclient.MinPrice{
					Currency:  "RUR",
					Value:     3109,
					BaseValue: 3109,
				},
			},
			{
				FromID:       54,
				ToID:         2,
				ForwardDate:  "2022-05-01",
				BackwardDate: &bd2,
				MinPrice: priceindexclient.MinPrice{
					Currency:  "RUR",
					Value:     3110,
					BaseValue: 3110,
				},
			},
		},
	}, nil)
	pointKeyMock := new(pointParserMock)
	from := "c213"
	to := "c53"
	pointKeyMock.On("ParseByPointKey", from).Return(createSettlementWithID(fromInt), nil)
	pointKeyMock.On("ParseByPointKey", to).Return(createSettlementWithID(toInt), nil)
	handler := NewGRPCHandler(&nop.Logger{}, &avia.PriceConfig{CountDays: 2}, piMock, pointKeyMock)

	req := priceAPI.PriceReq{
		PointKeyFrom: from,
		PointKeyTo:   to,
		DateForward: &date.Date{
			Year:  2022,
			Month: 5,
			Day:   1,
		},
	}
	rsp, err := handler.GetPrice(ctx, &req)
	require.NoError(t, err)

	require.Equal(t, 2, len(rsp.Data))
	require.Equal(t, &priceAPI.PriceItem{
		Price: &common.Price{
			Value:    3109,
			Currency: clientscommon.CurrencyRUB,
		},
		IsLow:     false,
		IsRoughly: true,
	}, rsp.Data["2022-05-01"])
	require.Equal(t, &priceAPI.PriceItem{
		Price: &common.Price{
			Value:    3110,
			Currency: clientscommon.CurrencyRUB,
		},
		IsLow:     false,
		IsRoughly: true,
	}, rsp.Data["2022-05-02"])
}

func TestGetPrice_Internal503Error(t *testing.T) {
	fromInt := 213
	toInt := 53
	request := &priceindexclient.PriceBatchReq{
		Requests: []*priceindexclient.BatchReq{
			{
				FromID: fromInt,
				ToID:   toInt,
				DateForward: &date.Date{
					Year:  2022,
					Month: 5,
					Day:   1,
				},
				DateBackward: &date.Date{
					Year:  2022,
					Month: 5,
					Day:   1,
				},
			},
			{
				FromID: fromInt,
				ToID:   toInt,
				DateForward: &date.Date{
					Year:  2022,
					Month: 5,
					Day:   1,
				},
				DateBackward: &date.Date{
					Year:  2022,
					Month: 5,
					Day:   2,
				},
			},
		},
	}
	piMock := new(priceIndexClientMock)
	ctx := context.Background()
	piMock.On("GetPriceBatch", ctx, request).Return(
		&priceindexclient.PriceBatchRsp{
			Status: "error",
			Data:   []priceindexclient.PriceBatchData{},
		},
		xerrors.Errorf("bad response from price-index service: %w", clientscommon.StatusError{
			Status:        http.StatusServiceUnavailable,
			Response:      nil,
			ResponseRaw:   "",
			InternalError: nil,
		}),
	)
	pointKeyMock := new(pointParserMock)
	from := "c213"
	to := "c53"
	pointKeyMock.On("ParseByPointKey", from).Return(createSettlementWithID(fromInt), nil)
	pointKeyMock.On("ParseByPointKey", to).Return(createSettlementWithID(toInt), nil)
	handler := NewGRPCHandler(&nop.Logger{}, &avia.PriceConfig{CountDays: 2}, piMock, pointKeyMock)

	req := priceAPI.PriceReq{
		PointKeyFrom: from,
		PointKeyTo:   to,
		DateForward: &date.Date{
			Year:  2022,
			Month: 5,
			Day:   1,
		},
	}
	rsp, err := handler.GetPrice(ctx, &req)
	require.ErrorIs(t, err, status.Error(codes.Unavailable, "price index unavailable"))
	require.Nil(t, nil, rsp)
}

func createSettlementWithID(id int) points.Point {
	return points.NewSettlement(&rasp.TSettlement{Id: int32(id)})
}

func TestGetMedian_Simple(t *testing.T) {
	handler := NewGRPCHandler(&nop.Logger{}, &avia.PriceConfig{}, nil, nil)

	m := handler.getMedian([]priceindexclient.PriceBatchData{
		{
			MinPrice: priceindexclient.MinPrice{
				Value: 2,
			},
		},
		{
			MinPrice: priceindexclient.MinPrice{
				Value: 1,
			},
		},
		{
			MinPrice: priceindexclient.MinPrice{
				Value: 4,
			},
		},
	})

	require.Equal(t, float64(2), m)
}

func TestIsRecentPrice_False(t *testing.T) {
	handler := NewGRPCHandler(&nop.Logger{}, &avia.PriceConfig{}, nil, nil)
	updatedAt := "2022-05-22T10:55:07.617628"

	isRecentPrice := handler.isRecentPrice(&updatedAt, time.Now())

	require.Equal(t, false, isRecentPrice)
}

func TestIsRecentPrice_True(t *testing.T) {
	handler := NewGRPCHandler(&nop.Logger{}, &avia.PriceConfig{}, nil, nil)
	updatedAt := "2022-05-22T10:55:07.617628"
	now := time.Date(2022, time.May, 22, 10, 55, 0, 0, time.UTC)

	isRecentPrice := handler.isRecentPrice(&updatedAt, now)

	require.Equal(t, true, isRecentPrice)
}
