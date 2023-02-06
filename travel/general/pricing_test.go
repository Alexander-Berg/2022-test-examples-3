package pricing

import (
	"fmt"
	"testing"
	"time"

	"a.yandex-team.ru/library/go/test/assertpb"
	"github.com/golang/protobuf/ptypes/timestamp"
	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/rasp/train_bandit_api/internal/model"
	"a.yandex-team.ru/travel/rasp/train_bandit_api/pkg/utils"
	pb "a.yandex-team.ru/travel/rasp/train_bandit_api/proto"
)

var (
	maxTime = time.Date(3000, 1, 1, 0, 0, 0, 0, MskLocation)
)

func createCharge() *pb.TCharge {
	charge := &pb.TCharge{
		Permille:                  110,
		OptionalMinTariffPermille: &pb.TCharge_MinTariffPermille{MinTariffPermille: 60},
		BanditType:                "fix11",
		Context: &pb.TContext{
			CarType:   model.CarTypePlatzkarte,
			ICookie:   "p5OmuCgY+FfEaXhUTO6/okv6lVk/AHrxu0XGsXFLGxC6dgc/W8uswMG81HgB+GEmjgqizfXvt6jSJvFRiCR9Luh8kNw=",
			PointFrom: "s2006004",
			PointTo:   "s9602499",
			Departure: &timestamp.Timestamp{Seconds: 1592516460},
			Arrival:   &timestamp.Timestamp{Seconds: 1592547180},
			TrainType: "фирменный «Арктика»",
		},
		TicketFees: map[uint32]*pb.TTicketFee{
			0: &pb.TTicketFee{
				TicketPrice: &pb.TTicketPrice{
					Amount:        utils.ToProtoPriceRUB(1100),
					ServiceAmount: utils.ToProtoPriceRUB(100),
				},
			},
			1: &pb.TTicketFee{
				TicketPrice: &pb.TTicketPrice{
					Amount:        utils.ToProtoPriceRUB(2100),
					ServiceAmount: utils.ToProtoPriceRUB(100),
				},
			},
		},
	}
	return charge
}

func TestPlatzkarte(t *testing.T) {
	p, _ := New(&DefaultPricingConfig)
	charge := createCharge()
	charge, err := p.CalculateFees(charge, true, maxTime)
	assert.NoError(t, err)
	assert.True(t, charge.TicketFees[0].IsBanditFeeApplied)
	assertpb.Equal(t, utils.ToProtoPriceRUB(110), charge.TicketFees[0].Fee)
	assertpb.Equal(t, utils.ToProtoPriceRUB(11), charge.TicketFees[0].ServiceFee)
	assert.True(t, charge.TicketFees[1].IsBanditFeeApplied)
	assertpb.Equal(t, utils.ToProtoPriceRUB(220), charge.TicketFees[1].Fee)
	assertpb.Equal(t, utils.ToProtoPriceRUB(11), charge.TicketFees[1].ServiceFee)
}

func TestComparment(t *testing.T) {
	p, _ := New(&DefaultPricingConfig)
	charge := createCharge()
	charge.Context.CarType = "comparment"
	charge, err := p.CalculateFees(charge, true, maxTime)
	assert.NoError(t, err)
	assert.True(t, charge.TicketFees[0].IsBanditFeeApplied)
	assertpb.Equal(t, utils.ToProtoPriceRUB(121), charge.TicketFees[0].Fee)
	assertpb.Equal(t, utils.ToProtoPriceRUB(0), charge.TicketFees[0].ServiceFee)
}

func TestFreeChild(t *testing.T) {
	p, _ := New(&DefaultPricingConfig)
	charge := createCharge()
	charge.TicketFees[0].TicketPrice = &pb.TTicketPrice{
		Amount:        utils.ToProtoPriceRUB(0),
		ServiceAmount: utils.ToProtoPriceRUB(0),
	}
	charge, err := p.CalculateFees(charge, true, maxTime)
	assert.NoError(t, err)
	assert.True(t, charge.TicketFees[0].IsBanditFeeApplied)
	assertpb.Equal(t, utils.ToProtoPriceRUB(0), charge.TicketFees[0].Fee)
	assertpb.Equal(t, utils.ToProtoPriceRUB(0), charge.TicketFees[0].ServiceFee)
}

func TestMinFee(t *testing.T) {
	conf := DefaultPricingConfig
	p, _ := New(&conf)
	charge := createCharge()
	charge.TicketFees[0].TicketPrice = &pb.TTicketPrice{
		Amount:        utils.ToProtoPriceRUB(100),
		ServiceAmount: utils.ToProtoPriceRUB(0),
	}
	charge, err := p.CalculateFees(charge, true, maxTime)
	assert.NoError(t, err)
	assert.False(t, charge.TicketFees[0].IsBanditFeeApplied)
	assertpb.Equal(t, utils.ToProtoPriceRUB(42.2), charge.TicketFees[0].Fee)
	assertpb.Equal(t, utils.ToProtoPriceRUB(0), charge.TicketFees[0].ServiceFee)
}

func TestGetPartnerFees(t *testing.T) {
	conf := DefaultPricingConfig
	conf.PartnerFeeHistory = []PartnerFeeChange{
		{Amount: 10, From: time.Date(2010, 1, 1, 0, 0, 0, 0, MskLocation)},
		{Amount: 20, From: time.Date(2020, 1, 1, 0, 0, 0, 0, MskLocation)},
		{Amount: 30, From: time.Date(2030, 1, 1, 0, 0, 0, 0, MskLocation)},
	}
	p, _ := New(&conf)
	type testCase struct {
		now            time.Time
		expectedAmount float64
	}
	testCases := []testCase{
		{expectedAmount: 10, now: time.Date(2011, 1, 1, 0, 0, 0, 0, MskLocation)},
		{expectedAmount: 10, now: time.Date(2020, 1, 1, 0, 0, 0, 0, MskLocation)},
		{expectedAmount: 20, now: time.Date(2020, 1, 1, 0, 0, 0, 0, time.UTC)},
		{expectedAmount: 20, now: time.Date(2020, 1, 2, 0, 0, 0, 0, MskLocation)},
		{expectedAmount: 30, now: maxTime},
	}
	for i, tc := range testCases {
		t.Run(fmt.Sprintf("testCase-%v-now-%v", i, tc.now), func(t *testing.T) {
			partnerFee, partnerRefundFee, err := p.getPartnerFees(tc.now)
			assert.NoError(t, err)
			assert.Equal(t, tc.expectedAmount, partnerFee)
			assert.Equal(t, tc.expectedAmount, partnerRefundFee)
		})
	}
	t.Run("errorNotFoundFee", func(t *testing.T) {
		_, _, err := p.getPartnerFees(time.Date(1999, 1, 1, 0, 0, 0, 0, MskLocation))
		assert.Error(t, err)
	})
}
