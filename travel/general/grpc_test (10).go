package handler

import (
	"a.yandex-team.ru/travel/proto/trains"
	"a.yandex-team.ru/travel/rasp/train_offer_storage/internal/api/models"
	"a.yandex-team.ru/travel/rasp/train_offer_storage/internal/logging"
	pb "a.yandex-team.ru/travel/rasp/train_offer_storage/proto"
	"context"
	"github.com/gofrs/uuid"
	"github.com/golang/protobuf/proto"
	"github.com/golang/protobuf/ptypes/timestamp"
	"github.com/stretchr/testify/assert"
	"testing"
)

const offerID = "8fbefbb1-bd28-4aaa-bcdf-464565f42726"

type mockStorage struct {
}

func CreateOffer() *trains.TTrainServiceOffer {
	return &trains.TTrainServiceOffer{
		OfferId:   offerID,
		TrainInfo: &trains.TTrainInfo{TrainTitle: "Super Train"},
		Departure: &timestamp.Timestamp{Seconds: 1587558807},
		Passengers: []*trains.TPassenger{
			{
				PassengerId: 1,
				TariffCode:  "full",
			},
		},
		Places: []uint32{1, 2, 3, 4},
	}
}

func (m *mockStorage) Close(ctx context.Context) error {
	panic("implement me")
}

func (m *mockStorage) CreateTable(ctx context.Context) error {
	panic("implement me")
}

func (m *mockStorage) Save(ctx context.Context, data []byte, token uuid.UUID) error {
	panic("implement me")
}

func (m *mockStorage) Get(ctx context.Context, token uuid.UUID) (data []byte, err error) {
	offer := CreateOffer()
	return proto.Marshal(offer)
}

func (m *mockStorage) GetMany(ctx context.Context, tokens []uuid.UUID) (datas [][]byte, err error) {
	offer := CreateOffer()
	data, _ := proto.Marshal(offer)
	datas = append(datas, data)
	return datas, nil
}

func CreateHandler() GRPCHandler {
	var storager models.Storager = &mockStorage{}
	logger, _ := logging.New(&logging.DefaultConfig)

	return GRPCHandler{
		Logger:   logger,
		Storager: storager,
	}
}

func TestGetOffer(t *testing.T) {
	handler := CreateHandler()
	request := pb.TGetOfferRequest{OfferId: offerID}

	response, err := handler.GetOffer(context.TODO(), &request)

	assert.NoError(t, err)
	assert.Equal(t, response.Offer.OfferId, offerID)
	assert.Equal(t, response.Offer.TrainInfo.TrainTitle, "Super Train")
	assert.Equal(t, response.Offer.Passengers[0].PassengerId, uint32(1))
	assert.Equal(t, len(response.Offer.Places), 4)
}

func TestGetOffers(t *testing.T) {
	handler := CreateHandler()
	request := pb.TGetOffersRequest{OfferIds: []string{offerID}}

	response, err := handler.GetOffers(context.TODO(), &request)

	assert.NoError(t, err)
	assert.Equal(t, response.Offers[0].OfferId, offerID)
	assert.Equal(t, response.Offers[0].TrainInfo.TrainTitle, "Super Train")
	assert.Equal(t, response.Offers[0].Passengers[0].PassengerId, uint32(1))
	assert.Equal(t, len(response.Offers[0].Places), 4)
}
