package app

import (
	"io/ioutil"
	"os"
	"testing"

	"a.yandex-team.ru/library/go/test/assertpb"
	tpb "a.yandex-team.ru/travel/proto"
	"github.com/stretchr/testify/assert"
	uzap "go.uber.org/zap"

	"a.yandex-team.ru/travel/buses/backend/internal/common/dict"
	pb "a.yandex-team.ru/travel/buses/backend/proto"
	wpb "a.yandex-team.ru/travel/buses/backend/proto/worker"
)

const dummyConfigData = `
common:
 service-id: 151

partners:
 ecolines:
   rates:
     revenue: 2.0
     yandex-fee: 4.0
   product-ids:
     ticket: "ecolines-ticket-v2"
     partner-fee: "ecolines-partner-fee-v2"
     yandex-fee: "ecolines-yandex-fee-v2"

 etraffic:
   rates:
     revenue: 0.01
     yandex-fee: 5.8
   product-ids:
     ticket: "etraffic-ticket"
     partner-fee: "etraffic-fee"
     yandex-fee: "etraffic-yandex-fee"

 ok:
   rates:
     revenue: 0.8
     yandex-fee: 5.0
   product-ids:
     ticket: "ok-ticket"
     partner-fee: "ok-fee"
     yandex-fee: "ok-yandex-fee"
   history:
     revenue:
       2019-12-01: 2.0
`

func makeTempFile(contents string) (string, func(), error) {
	tempFile, err := ioutil.TempFile("", "")
	if err != nil {
		return "", nil, err
	}
	if _, err = tempFile.Write([]byte(contents)); err != nil {
		return "", nil, err
	}
	if err = tempFile.Close(); err != nil {
		return "", nil, err
	}
	return tempFile.Name(), func() {
		_ = os.Remove(tempFile.Name())
	}, nil
}

func Test(t *testing.T) {
	t.Run("LoadBillingData", func(t *testing.T) {
		tempFileName, removeTempFile, err := makeTempFile(dummyConfigData)
		if err != nil {
			t.Fatalf("cannot create temp file (%s)", err)
		}
		defer removeTempFile()

		billingData, err := LoadBillingData(tempFileName)
		if assert.NoError(t, err) {
			assert.Equal(t, &BillingData{
				Common: CommonData{ServiceID: 151},
				Partners: map[string]PartnerData{
					"ecolines": {
						Rates: RatesData{
							YandexFee: 4.0,
							Revenue:   2.0,
						},
						ProductIds: ProductIdsData{
							Ticket:     "ecolines-ticket-v2",
							PartnerFee: "ecolines-partner-fee-v2",
							YandexFee:  "ecolines-yandex-fee-v2",
						},
					},
					"etraffic": {
						Rates: RatesData{
							YandexFee: 5.8,
							Revenue:   0.01,
						},
						ProductIds: ProductIdsData{
							Ticket:     "etraffic-ticket",
							PartnerFee: "etraffic-fee",
							YandexFee:  "etraffic-yandex-fee",
						},
					},
					"ok": {
						Rates: RatesData{
							YandexFee: 5.0,
							Revenue:   0.8,
						},
						ProductIds: ProductIdsData{
							Ticket:     "ok-ticket",
							PartnerFee: "ok-fee",
							YandexFee:  "ok-yandex-fee",
						},
					},
				},
			}, billingData)
		}
	})

	t.Run("WithYandexFee", func(t *testing.T) {
		testSupplier, _ := dict.GetSupplier(dict.GetSuppliersList()[0])
		billingData := BillingData{
			Partners: map[string]PartnerData{
				testSupplier.Name: {Rates: RatesData{YandexFee: 4.0}},
			},
		}
		rideWithFee, bookParamsWithFee, err := billingData.WithYandexFee(
			&pb.TRide{
				Status:   pb.ERideStatus_RIDE_STATUS_SALE,
				Supplier: &pb.TSupplier{ID: testSupplier.ID},
				Price:    &tpb.TPrice{Amount: 100, Currency: tpb.ECurrency_C_RUB, Precision: 100},
			},
			&pb.TBookParams{
				TicketTypes: []*pb.TTicketType{
					{
						Id:        1,
						PartnerId: "1",
						Price:     &tpb.TPrice{Amount: 100, Currency: tpb.ECurrency_C_RUB, Precision: 100},
						YandexFee: nil,
					},
					{
						Id:        2,
						PartnerId: "2",
						Price:     &tpb.TPrice{Amount: 50, Currency: tpb.ECurrency_C_RUB, Precision: 100},
						YandexFee: nil,
					},
				},
			},
		)

		if assert.NoError(t, err) {
			assertpb.Equal(t, &pb.TRide{
				Status:    pb.ERideStatus_RIDE_STATUS_SALE,
				Supplier:  &pb.TSupplier{ID: testSupplier.ID},
				Price:     &tpb.TPrice{Amount: 104, Currency: tpb.ECurrency_C_RUB, Precision: 100},
				YandexFee: &tpb.TPrice{Amount: 4, Currency: tpb.ECurrency_C_RUB, Precision: 100},
			}, rideWithFee)

			assertpb.Equal(t, &pb.TBookParams{
				TicketTypes: []*pb.TTicketType{
					{
						Id:        1,
						PartnerId: "1",
						Price:     &tpb.TPrice{Amount: 104, Currency: tpb.ECurrency_C_RUB, Precision: 100},
						YandexFee: &tpb.TPrice{Amount: 4, Currency: tpb.ECurrency_C_RUB, Precision: 100},
					},
					{
						Id:        2,
						PartnerId: "2",
						Price:     &tpb.TPrice{Amount: 52, Currency: tpb.ECurrency_C_RUB, Precision: 100},
						YandexFee: &tpb.TPrice{Amount: 2, Currency: tpb.ECurrency_C_RUB, Precision: 100},
					},
				},
			}, bookParamsWithFee)
		}
	})

	t.Run("RidesWithYandexFee", func(t *testing.T) {
		testSupplier, _ := dict.GetSupplier(dict.GetSuppliersList()[0])
		billingData := BillingData{
			Partners: map[string]PartnerData{
				testSupplier.Name: {Rates: RatesData{YandexFee: 4.0}},
			},
		}
		logger, logs := setupLogsCapture()
		ridesWithFee := billingData.RidesWithYandexFee([]*pb.TRide{
			{
				Status:   pb.ERideStatus_RIDE_STATUS_SALE,
				Supplier: &pb.TSupplier{ID: testSupplier.ID},
				Price:    &tpb.TPrice{Amount: 100, Currency: tpb.ECurrency_C_RUB, Precision: 100},
			},
		}, logger)

		assertpb.Equal(t, []*pb.TRide{
			{
				Status:    pb.ERideStatus_RIDE_STATUS_SALE,
				Supplier:  &pb.TSupplier{ID: testSupplier.ID},
				Price:     &tpb.TPrice{Amount: 104, Currency: tpb.ECurrency_C_RUB, Precision: 100},
				YandexFee: &tpb.TPrice{Amount: 4, Currency: tpb.ECurrency_C_RUB, Precision: 100},
			},
		}, ridesWithFee)
		assert.Equal(t, 0, logs.Len())
	})

	t.Run("RidesWithYandexFee_error_logging", func(t *testing.T) {
		ecolines, _ := dict.GetSupplierByName("ecolines")
		unitiki, _ := dict.GetSupplierByName("unitiki-new")
		billingData := BillingData{
			Partners: map[string]PartnerData{
				ecolines.Name: {Rates: RatesData{YandexFee: 4.0}},
			},
		}
		logger, logs := setupLogsCapture()

		rides := []*pb.TRide{
			{
				Status:   pb.ERideStatus_RIDE_STATUS_SALE,
				Supplier: &pb.TSupplier{ID: 99},
				Price:    &tpb.TPrice{Amount: 100, Currency: tpb.ECurrency_C_RUB},
			},
			{
				Status:   pb.ERideStatus_RIDE_STATUS_SALE,
				Supplier: &pb.TSupplier{ID: unitiki.ID},
				Price:    &tpb.TPrice{Amount: 100, Currency: tpb.ECurrency_C_RUB},
			},
		}
		assertpb.Equal(t, rides, billingData.RidesWithYandexFee(rides, logger))

		logEntries := logs.TakeAll()
		if assert.Equal(t, 2, len(logEntries)) {
			assert.Equal(t, uzap.ErrorLevel, logEntries[0].Level)
			assert.Equal(t, "cannot add yandex fee: RideWithYandexFee: getRideRates: no such supplier with id = 99", logEntries[0].Message)
			assert.Equal(t, uzap.ErrorLevel, logEntries[1].Level)
			assert.Equal(t, "cannot add yandex fee: RideWithYandexFee: getRideRates: no billing data for supplier unitiki-new", logEntries[1].Message)
		}
	})

	t.Run("WithRevenue", func(t *testing.T) {
		ecolines, _ := dict.GetSupplierByName("ecolines")
		billingData := BillingData{
			Partners: map[string]PartnerData{
				ecolines.Name: {Rates: RatesData{Revenue: 2.0}},
			},
		}

		order := &wpb.TOrder{
			Tickets: []*wpb.TTicket{
				{
					Price: &tpb.TPrice{Amount: 1000, Precision: 1, Currency: tpb.ECurrency_C_EUR},
				},
				{
					Price:   &tpb.TPrice{Amount: 10000, Precision: 2, Currency: tpb.ECurrency_C_RUB},
					Revenue: &tpb.TPrice{Amount: 100, Precision: 2, Currency: tpb.ECurrency_C_RUB},
				},
			},
		}

		order, err := billingData.WithRevenue(order, ecolines.ID)
		if assert.NoError(t, err) {
			assertpb.Equal(t, &wpb.TOrder{
				Tickets: []*wpb.TTicket{
					{
						Price:   &tpb.TPrice{Amount: 1000, Precision: 1, Currency: tpb.ECurrency_C_EUR},
						Revenue: &tpb.TPrice{Amount: 20, Precision: 1, Currency: tpb.ECurrency_C_EUR},
					},
					{
						Price:   &tpb.TPrice{Amount: 10000, Precision: 2, Currency: tpb.ECurrency_C_RUB},
						Revenue: &tpb.TPrice{Amount: 100, Precision: 2, Currency: tpb.ECurrency_C_RUB},
					},
				},
			}, order)
		}
	})
}
