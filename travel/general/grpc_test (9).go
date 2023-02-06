package handler

import (
	"context"
	"testing"
	"time"

	"a.yandex-team.ru/library/go/core/metrics/nop"
	tm "a.yandex-team.ru/travel/library/go/metrics"
	common "a.yandex-team.ru/travel/proto"
	"github.com/golang/protobuf/ptypes"
	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/rasp/train_bandit_api/internal/bandit"
	"a.yandex-team.ru/travel/rasp/train_bandit_api/internal/bandit/manager"
	"a.yandex-team.ru/travel/rasp/train_bandit_api/internal/logging"
	"a.yandex-team.ru/travel/rasp/train_bandit_api/internal/model"
	"a.yandex-team.ru/travel/rasp/train_bandit_api/internal/pricing"
	pb "a.yandex-team.ru/travel/rasp/train_bandit_api/proto"
)

type mockChargerGetter struct {
	t          *testing.T
	charger    bandit.Charger
	banditType string
	version    uint64
}

func (m mockChargerGetter) GetCharger(requestedBanditType string) (charger bandit.Charger, banditType string, version uint64, err error) {
	return m.charger, m.banditType, m.version, nil
}

func createTicketPrices() []*pb.TTicketPrices {
	departureTimestamp, _ := ptypes.TimestampProto(time.Date(2020, 9, 2, 18, 0, 0, 0, time.UTC))
	arrivalTimestamp, _ := ptypes.TimestampProto(time.Date(2020, 9, 3, 23, 0, 0, 0, time.UTC))
	prices := []*pb.TTicketPrices{
		{
			Context: &pb.TContext{
				ICookie:   "some cookie",
				PointFrom: "other from",
				CarType:   model.CarTypePlatzkarte,
				Departure: departureTimestamp,
				Arrival:   arrivalTimestamp,
			},
			InternalId: 2,
			TicketPrices: map[uint32]*pb.TTicketPrice{
				3: {
					Amount:        &common.TPrice{Precision: 2, Currency: common.ECurrency_C_RUB, Amount: 300000},
					ServiceAmount: &common.TPrice{Precision: 2, Currency: common.ECurrency_C_RUB, Amount: 3000},
				},
			},
		},
	}

	return prices
}

func TestGetCharge(t *testing.T) {
	logger, _ := logging.New(&logging.DefaultConfig)
	pricer, _ := pricing.New(&pricing.DefaultPricingConfig)

	bm := &mockChargerGetter{
		charger:    bandit.NewFixed(110),
		banditType: manager.BanditTypeFixed11,
		version:    0,
	}

	handler := GRPCHandler{
		ChargerGetter: bm,
		Logger:        logger,
		Pricer:        pricer,
		Metrics:       tm.NewAppMetrics(nop.Registry{}),
	}
	request := pb.TGetChargeRequest{ContextsWithPrices: createTicketPrices()}

	ctx, cancel := context.WithCancel(context.Background())

	response, err := handler.GetCharge(ctx, &request)
	cancel()

	assert.NoError(t, err)
	assert.Equal(t, 1, len(response.ChargesByContexts))

	assert.Equal(t, uint32(2), response.ChargesByContexts[0].InternalId)
	assert.Equal(t, uint32(110), response.ChargesByContexts[0].Permille)
	assert.True(t, response.ChargesByContexts[0].TicketFees[3].IsBanditFeeApplied)
	assert.Equal(t, int64(32670), response.ChargesByContexts[0].TicketFees[3].Fee.Amount)
	assert.Equal(t, int64(330), response.ChargesByContexts[0].TicketFees[3].ServiceFee.Amount)
}
