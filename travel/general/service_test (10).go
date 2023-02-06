package feeservice

import (
	"context"
	"testing"

	"a.yandex-team.ru/travel/proto/dicts/rasp"
	"github.com/stretchr/testify/assert"
	"google.golang.org/grpc"

	pb "a.yandex-team.ru/travel/rasp/train_bandit_api/proto"

	"a.yandex-team.ru/travel/trains/search_api/api/tariffs"
	"a.yandex-team.ru/travel/trains/search_api/internal/direction/segments"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/testutils"
)

type MockBanditClient struct {
	GetChargeRequest  *pb.TGetChargeRequest
	GetChargeResponse *pb.TGetChargeResponse
}

func (m *MockBanditClient) GetCharge(ctx context.Context, in *pb.TGetChargeRequest, opts ...grpc.CallOption) (*pb.TGetChargeResponse, error) {
	m.GetChargeRequest = in
	return m.GetChargeResponse, nil
}

func (m *MockBanditClient) GetChargeStringContext(ctx context.Context, in *pb.TGetChargeStringCtxRequest, opts ...grpc.CallOption) (*pb.TGetChargeStringCtxResponse, error) {
	panic("GetChargeStringContext not implemented")
}

func (m *MockBanditClient) GetChargeByToken(ctx context.Context, in *pb.TGetChargeByTokenRequest, opts ...grpc.CallOption) (*pb.TGetChargeByTokenResponse, error) {
	panic("GetChargeByToken not implemented")
}

func TestApplyTrainSegmentsFee(t *testing.T) {
	logger := testutils.NewLogger(t)
	mockBanditClient := &MockBanditClient{}
	service := NewService(mockBanditClient, logger)

	t.Run("onePlace", func(t *testing.T) {
		mockBanditClient.GetChargeResponse = &pb.TGetChargeResponse{
			ChargesByContexts: []*pb.TCharge{
				&pb.TCharge{
					TicketFees: map[uint32]*pb.TTicketFee{
						0: &pb.TTicketFee{
							TicketPrice: &pb.TTicketPrice{
								Amount:        testutils.PriceOf(100000),
								ServiceAmount: testutils.PriceOf(10000),
							},
							Fee:        testutils.PriceOf(5000),
							ServiceFee: testutils.PriceOf(500),
						},
					},
				},
			},
		}
		trainSegments := segments.TrainSegments{
			&segments.TrainSegment{
				DepartureStation: &rasp.TThreadStation{Id: 2},
				ArrivalStation:   &rasp.TThreadStation{Id: 213},
				Places: []*tariffs.TrainPlace{
					&tariffs.TrainPlace{
						CoachType: "platzkarte",
						Price:     testutils.PriceOf(100000),
						PriceDetails: &tariffs.TrainPlacePriceDetails{
							TicketPrice:  testutils.PriceOf(100000),
							ServicePrice: testutils.PriceOf(10000),
							Fee:          testutils.PriceOf(0),
						},
					},
				},
			},
		}
		err := service.ApplyTrainSegmentsFee(context.Background(), trainSegments, "", "", SegmentShortContextProvider)
		assert.NoError(t, err)
		assert.EqualValues(t, 5500, trainSegments[0].Places[0].PriceDetails.Fee.Amount)
		assert.EqualValues(t, 105500, trainSegments[0].Places[0].Price.Amount)
	})

	t.Run("samePlaces", func(t *testing.T) {
		mockBanditClient.GetChargeResponse = &pb.TGetChargeResponse{
			ChargesByContexts: []*pb.TCharge{
				&pb.TCharge{
					TicketFees: map[uint32]*pb.TTicketFee{
						0: &pb.TTicketFee{
							TicketPrice: &pb.TTicketPrice{
								Amount:        testutils.PriceOf(100000),
								ServiceAmount: testutils.PriceOf(10000),
							},
							Fee:        testutils.PriceOf(5000),
							ServiceFee: testutils.PriceOf(500),
						},
					},
				},
			},
		}
		trainSegments := segments.TrainSegments{
			&segments.TrainSegment{
				DepartureStation: &rasp.TThreadStation{Id: 2},
				ArrivalStation:   &rasp.TThreadStation{Id: 213},
				Places: []*tariffs.TrainPlace{
					&tariffs.TrainPlace{
						CoachType: "platzkarte",
						Price:     testutils.PriceOf(100000),
						PriceDetails: &tariffs.TrainPlacePriceDetails{
							TicketPrice:  testutils.PriceOf(100000),
							ServicePrice: testutils.PriceOf(10000),
							Fee:          testutils.PriceOf(0),
						},
					},
					&tariffs.TrainPlace{
						CoachType: "platzkarte",
						Price:     testutils.PriceOf(100000),
						PriceDetails: &tariffs.TrainPlacePriceDetails{
							TicketPrice:  testutils.PriceOf(100000),
							ServicePrice: testutils.PriceOf(10000),
							Fee:          testutils.PriceOf(0),
						},
					},
				},
			},
		}
		err := service.ApplyTrainSegmentsFee(context.Background(), trainSegments, "", "", SegmentShortContextProvider)
		assert.NoError(t, err)
		assert.Equal(t, 1, len(mockBanditClient.GetChargeRequest.ContextsWithPrices))
		assert.Equal(t, 1, len(mockBanditClient.GetChargeRequest.ContextsWithPrices[0].TicketPrices))
		assert.EqualValues(t, 5500, trainSegments[0].Places[0].PriceDetails.Fee.Amount)
		assert.EqualValues(t, 105500, trainSegments[0].Places[0].Price.Amount)
		assert.EqualValues(t, 5500, trainSegments[0].Places[1].PriceDetails.Fee.Amount)
		assert.EqualValues(t, 105500, trainSegments[0].Places[1].Price.Amount)
	})

	t.Run("differentPlaces", func(t *testing.T) {
		mockBanditClient.GetChargeResponse = &pb.TGetChargeResponse{
			ChargesByContexts: []*pb.TCharge{
				&pb.TCharge{
					TicketFees: map[uint32]*pb.TTicketFee{
						0: &pb.TTicketFee{
							TicketPrice: &pb.TTicketPrice{
								Amount:        testutils.PriceOf(100000),
								ServiceAmount: testutils.PriceOf(10000),
							},
							Fee:        testutils.PriceOf(5000),
							ServiceFee: testutils.PriceOf(500),
						},
						1: &pb.TTicketFee{
							TicketPrice: &pb.TTicketPrice{
								Amount:        testutils.PriceOf(200000),
								ServiceAmount: testutils.PriceOf(20000),
							},
							Fee:        testutils.PriceOf(7000),
							ServiceFee: testutils.PriceOf(700),
						},
					},
				},
			},
		}
		trainSegments := segments.TrainSegments{
			&segments.TrainSegment{
				DepartureStation: &rasp.TThreadStation{Id: 2},
				ArrivalStation:   &rasp.TThreadStation{Id: 213},
				Places: []*tariffs.TrainPlace{
					&tariffs.TrainPlace{
						CoachType: "platzkarte",
						Price:     testutils.PriceOf(100000),
						PriceDetails: &tariffs.TrainPlacePriceDetails{
							TicketPrice:  testutils.PriceOf(100000),
							ServicePrice: testutils.PriceOf(10000),
							Fee:          testutils.PriceOf(0),
						},
					},
					&tariffs.TrainPlace{
						CoachType: "platzkarte",
						Price:     testutils.PriceOf(200000),
						PriceDetails: &tariffs.TrainPlacePriceDetails{
							TicketPrice:  testutils.PriceOf(200000),
							ServicePrice: testutils.PriceOf(20000),
							Fee:          testutils.PriceOf(0),
						},
					},
				},
			},
		}
		err := service.ApplyTrainSegmentsFee(context.Background(), trainSegments, "", "", SegmentShortContextProvider)
		assert.NoError(t, err)
		assert.Equal(t, 1, len(mockBanditClient.GetChargeRequest.ContextsWithPrices))
		assert.Equal(t, 2, len(mockBanditClient.GetChargeRequest.ContextsWithPrices[0].TicketPrices))
		assert.EqualValues(t, 5500, trainSegments[0].Places[0].PriceDetails.Fee.Amount)
		assert.EqualValues(t, 105500, trainSegments[0].Places[0].Price.Amount)
		assert.EqualValues(t, 7700, trainSegments[0].Places[1].PriceDetails.Fee.Amount)
		assert.EqualValues(t, 207700, trainSegments[0].Places[1].Price.Amount)
	})

	t.Run("samePlacesWithDifferentSegments", func(t *testing.T) {
		mockBanditClient.GetChargeResponse = &pb.TGetChargeResponse{
			ChargesByContexts: []*pb.TCharge{
				&pb.TCharge{
					TicketFees: map[uint32]*pb.TTicketFee{
						0: &pb.TTicketFee{
							TicketPrice: &pb.TTicketPrice{
								Amount:        testutils.PriceOf(100000),
								ServiceAmount: testutils.PriceOf(10000),
							},
							Fee:        testutils.PriceOf(5000),
							ServiceFee: testutils.PriceOf(500),
						},
					},
				},
				&pb.TCharge{
					TicketFees: map[uint32]*pb.TTicketFee{
						1: &pb.TTicketFee{
							TicketPrice: &pb.TTicketPrice{
								Amount:        testutils.PriceOf(100000),
								ServiceAmount: testutils.PriceOf(10000),
							},
							Fee:        testutils.PriceOf(7000),
							ServiceFee: testutils.PriceOf(700),
						},
					},
				},
			},
		}
		trainSegments := segments.TrainSegments{
			&segments.TrainSegment{
				DepartureStation: &rasp.TThreadStation{Id: 2},
				ArrivalStation:   &rasp.TThreadStation{Id: 213},
				Places: []*tariffs.TrainPlace{
					&tariffs.TrainPlace{
						CoachType: "platzkarte",
						Price:     testutils.PriceOf(100000),
						PriceDetails: &tariffs.TrainPlacePriceDetails{
							TicketPrice:  testutils.PriceOf(100000),
							ServicePrice: testutils.PriceOf(10000),
							Fee:          testutils.PriceOf(0),
						},
					},
				},
			},
			&segments.TrainSegment{
				DepartureStation: &rasp.TThreadStation{Id: 2},
				ArrivalStation:   &rasp.TThreadStation{Id: 213},
				TrainBrand:       &rasp.TNamedTrain{TitleDefault: "сапсан"},
				Places: []*tariffs.TrainPlace{
					&tariffs.TrainPlace{
						CoachType: "platzkarte",
						Price:     testutils.PriceOf(100000),
						PriceDetails: &tariffs.TrainPlacePriceDetails{
							TicketPrice:  testutils.PriceOf(100000),
							ServicePrice: testutils.PriceOf(10000),
							Fee:          testutils.PriceOf(0),
						},
					},
				},
			},
		}
		err := service.ApplyTrainSegmentsFee(context.Background(), trainSegments, "", "", SegmentShortContextProvider)
		assert.NoError(t, err)
		assert.Equal(t, 2, len(mockBanditClient.GetChargeRequest.ContextsWithPrices))
		assert.EqualValues(t, 5500, trainSegments[0].Places[0].PriceDetails.Fee.Amount)
		assert.EqualValues(t, 105500, trainSegments[0].Places[0].Price.Amount)
		assert.EqualValues(t, 7700, trainSegments[1].Places[0].PriceDetails.Fee.Amount)
		assert.EqualValues(t, 107700, trainSegments[1].Places[0].Price.Amount)
	})
}
