package main

import (
	"a.yandex-team.ru/library/go/core/log/zap"
	"a.yandex-team.ru/market/idx/golibrary/pbsn"
	delivery "a.yandex-team.ru/market/proto/delivery/delivery_calc"
	"a.yandex-team.ru/yt/go/ypath"
	"a.yandex-team.ru/yt/go/yttest"
	"bytes"
	"fmt"
	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
	"github.com/golang/protobuf/proto"
	"github.com/stretchr/testify/assert"
	uberzap "go.uber.org/zap"
	"net/http"
	"net/http/httptest"
	"runtime"
	"testing"
)

func prepareDeliveryUploader(env *yttest.Env) (config DeliveryUploaderConfig) {
	numRetry = 5

	config.numWorkers = 5
	config.errorsTablePath = string(env.TmpPath())
	config.deliveryOptionsBucketTablePath = string(env.TmpPath())
	config.deliveryOptionsGroupTablePath = string(env.TmpPath())
	config.pickupBucketTablePath = string(env.TmpPath())
	config.postBucketTablePath = string(env.TmpPath())
	config.pickupOptionalsBucketTablePath = string(env.TmpPath())
	config.modifiersTablePath = string(env.TmpPath())
	config.color = "WHITE"

	return
}

func RunDeliveryUploader(env *yttest.Env, bucketUrls, modifierUrls []string, client *http.Client) (config DeliveryUploaderConfig) {
	config = prepareDeliveryUploader(env)
	logger = &zap.Logger{L: uberzap.NewNop()}

	uploader := NewDeliveryUploader(env.YT, config, logger)
	doUpload(client, bucketUrls, modifierUrls, uploader)

	return
}

type MockHTTPResponse struct {
	Code int
	Data []byte
}

type MockHTTPFileHandler struct {
	Pos  int
	Data []MockHTTPResponse
}

func NewMockHTTPFileHandler(responses ...MockHTTPResponse) *MockHTTPFileHandler {
	return &MockHTTPFileHandler{
		Data: responses,
	}
}

func (handler *MockHTTPFileHandler) ServeHTTP(writer http.ResponseWriter, _ *http.Request) {
	if handler.Pos >= len(handler.Data) {
		writer.WriteHeader(http.StatusGone)
		return
	}

	writer.WriteHeader(handler.Data[handler.Pos].Code)
	if handler.Data[handler.Pos].Data != nil {
		if _, err := writer.Write(handler.Data[handler.Pos].Data); err != nil {
			panic(err)
		}
	}
	handler.Pos++
}

func GenerateFirstModifier() *delivery.DeliveryModifier {
	return &delivery.DeliveryModifier{
		Id: proto.Int64(1),
		Action: &delivery.ModifierAction{
			Action: &delivery.ModifierAction_CostModificationRule{
				CostModificationRule: &delivery.CostModificationRule{
					Currency:    proto.String("RUR"),
					Operation:   delivery.ValueModificationOperation_MULTIPLY.Enum(),
					Parameter:   proto.Float64(1.1),
					ResultLimit: nil,
				},
			},
		},
		Condition: &delivery.ModifierCondition{
			Regions:      nil,
			DeliveryCost: nil,
		},
	}
}

func GenerateSecondModifier() *delivery.DeliveryModifier {
	return &delivery.DeliveryModifier{
		Id: proto.Int64(2),
		Action: &delivery.ModifierAction{
			Action: &delivery.ModifierAction_TimeModificationRule{
				TimeModificationRule: &delivery.TimeModificationRule{
					Operation: delivery.ValueModificationOperation_ADD.Enum(),
					Parameter: proto.Float64(2),
					ResultLimit: &delivery.IntegerValueLimiter{
						MinValue: nil,
						MaxValue: proto.Int32(14),
					},
				},
			},
		},
		Condition: &delivery.ModifierCondition{
			Regions:      []int64{213, 172},
			DeliveryCost: nil,
		},
	}
}

func GenerateThirdModifier() *delivery.DeliveryModifier {
	return &delivery.DeliveryModifier{
		Id: proto.Int64(3),
		Action: &delivery.ModifierAction{
			Action: &delivery.ModifierAction_ServicesModificationRule{
				ServicesModificationRule: &delivery.DeliveryServicesModificationRule{
					PayedByCustomerServices: []string{"beer_for_courier", "air"},
				},
			},
		},
		Condition: &delivery.ModifierCondition{
			Regions: nil,
			DeliveryCost: &delivery.DeliveryCostCondition{
				PercentFromOfferPrice: proto.Float64(10),
				ComparisonOperation:   delivery.ComparisonOperation_EQUAL.Enum(),
			},
		},
	}
}

func GetGoodPathModifiers() *delivery.ShopDeliveryModifiers {
	modifiers := make([]*delivery.DeliveryModifier, 2)

	modifiers[0] = GenerateFirstModifier()
	modifiers[1] = GenerateSecondModifier()

	return &delivery.ShopDeliveryModifiers{
		GenerationId: proto.Int64(5),
		Modifiers:    modifiers,
	}
}

func GetBadPathModifiers() *delivery.ShopDeliveryModifiers {
	return &delivery.ShopDeliveryModifiers{
		GenerationId: proto.Int64(6),
		Modifiers:    []*delivery.DeliveryModifier{GenerateThirdModifier()},
	}
}

func GetDeliveryBucketsFileWithVersion() *delivery.FeedDeliveryOptionsResp {
	return &delivery.FeedDeliveryOptionsResp{
		RequestId:    nil,
		ResponseCode: proto.Int32(http.StatusOK),
		FeedId:       nil,
		GenerationId: proto.Int64(964),
		UpdateTimeTs: nil,
		DeliveryOptionsByFeed: &delivery.DeliveryOptions{
			DeliveryOptionBuckets: nil,
			DeliveryOptionGroups: []*delivery.DeliveryOptionsGroup{
				{
					DeliveryOptionGroupId: proto.Int64(15366),
					DeliveryOptions: []*delivery.DeliveryOption{
						{
							DeliveryCost:     proto.Int64(200),
							MinDaysCount:     proto.Int32(1),
							MaxDaysCount:     proto.Int32(3),
							OrderBefore:      proto.Int32(11),
							ShopDeliveryCost: nil,
						},
					},
					PaymentTypes: []delivery.PaymentType{
						delivery.PaymentType_CARD_ON_DELIVERY,
						delivery.PaymentType_CASH_ON_DELIVERY,
					},
				},
				{
					DeliveryOptionGroupId: proto.Int64(16138),
					DeliveryOptions: []*delivery.DeliveryOption{
						{
							DeliveryCost:     proto.Int64(-100),
							MinDaysCount:     proto.Int32(15),
							MaxDaysCount:     proto.Int32(25),
							OrderBefore:      proto.Int32(23),
							ShopDeliveryCost: proto.Int64(300),
						},
					},
					PaymentTypes: []delivery.PaymentType{
						delivery.PaymentType_PREPAYMENT_CARD,
						delivery.PaymentType_PREPAYMENT_OTHER,
					},
				},
			},
			PickupBuckets: nil,
			PostBuckets:   nil,
			PickupBucketsV2: []*delivery.PickupOptionsBucket{
				{
					BucketId:   proto.Int64(326),
					Program:    delivery.ProgramType_DAAS.Enum(),
					Currency:   proto.String("EUR"),
					CarrierIds: []int32{100, 98},
					PickupDeliveryRegions: []*delivery.PickupDeliveryRegion{
						{
							RegionId:      proto.Int64(1984),
							OptionGroupId: proto.Int64(15366),
							OutletGroups: []*delivery.OutletGroup{
								{
									Dimensions: &delivery.OutletDimensions{
										Width:  proto.Float64(36.6),
										Height: proto.Float64(37.2),
										Length: proto.Float64(38.9),
										DimSum: proto.Float64(22.3),
									},
									OutletId: []int64{6416, 444643},
								},
							},
						},
					},
					TariffId: proto.Int64(66),
				},
			},
			PostBucketsV2: []*delivery.PickupOptionsBucket{
				{
					BucketId:   proto.Int64(164),
					Program:    delivery.ProgramType_MARKET_DELIVERY_WHITE_PROGRAM.Enum(),
					Currency:   proto.String("USD"),
					CarrierIds: []int32{99, 97},
					PickupDeliveryRegions: []*delivery.PickupDeliveryRegion{
						{
							RegionId:      proto.Int64(1966),
							OptionGroupId: proto.Int64(16138),
							OutletGroups:  nil,
						},
					},
					TariffId: proto.Int64(68),
				},
			},
		},
		Currency:       []string{"RUR"},
		UseYmlDelivery: nil,
		Version:        proto.Int64(366),
	}
}

func GetDeliveryBucketsFileWithoutVersion() *delivery.FeedDeliveryOptionsResp {
	return &delivery.FeedDeliveryOptionsResp{
		RequestId:    nil,
		ResponseCode: proto.Int32(http.StatusOK),
		FeedId:       nil,
		GenerationId: proto.Int64(9864),
		UpdateTimeTs: nil,
		DeliveryOptionsByFeed: &delivery.DeliveryOptions{
			DeliveryOptionBuckets: []*delivery.DeliveryOptionsBucket{
				{
					DeliveryOptBucketId: proto.Int64(24923),
					DeliveryOptionGroupRegs: []*delivery.DeliveryOptionsGroupRegion{
						{
							Region:             proto.Int32(172),
							DeliveryOptGroupId: nil,
							OptionType:         delivery.OptionType_FORBIDDEN_OPTION.Enum(),
						},
						{
							Region:             proto.Int32(213),
							DeliveryOptGroupId: proto.Int64(146244),
							OptionType:         nil,
						},
					},
					Currency:   proto.String("KZT"),
					CarrierIds: []int32{122},
					Program:    delivery.ProgramType_MARKET_DELIVERY_PROGRAM.Enum(),
					TariffId:   nil,
				},
			},
			DeliveryOptionGroups: []*delivery.DeliveryOptionsGroup{
				{
					DeliveryOptionGroupId: proto.Int64(146244),
					DeliveryOptions: []*delivery.DeliveryOption{
						{
							DeliveryCost:     proto.Int64(29900),
							MinDaysCount:     proto.Int32(10),
							MaxDaysCount:     proto.Int32(11),
							OrderBefore:      proto.Int32(13),
							ShopDeliveryCost: nil,
						},
					},
					PaymentTypes: []delivery.PaymentType{
						delivery.PaymentType_YANDEX,
					},
				},
				{
					DeliveryOptionGroupId: proto.Int64(643264),
					DeliveryOptions: []*delivery.DeliveryOption{
						{
							DeliveryCost:     proto.Int64(9900),
							MinDaysCount:     proto.Int32(11),
							MaxDaysCount:     proto.Int32(13),
							OrderBefore:      proto.Int32(1),
							ShopDeliveryCost: proto.Int64(99900),
						},
					},
					PaymentTypes: []delivery.PaymentType{
						delivery.PaymentType_CARD_ON_DELIVERY,
						delivery.PaymentType_CASH_ON_DELIVERY,
					},
				},
				{
					DeliveryOptionGroupId: proto.Int64(33243),
					DeliveryOptions: []*delivery.DeliveryOption{
						{
							DeliveryCost:     proto.Int64(19900),
							MinDaysCount:     proto.Int32(5),
							MaxDaysCount:     proto.Int32(13),
							OrderBefore:      proto.Int32(22),
							ShopDeliveryCost: nil,
						},
					},
					PaymentTypes: nil,
				},
			},
			PickupBuckets: []*delivery.PickupBucket{
				{
					BucketId:   proto.Int64(363),
					Program:    delivery.ProgramType_FF_LIGHT_PROGRAM.Enum(),
					Currency:   proto.String("BYN"),
					CarrierIds: nil,
					DeliveryOptionGroupOutlets: []*delivery.DeliveryOptionsGroupOutlet{
						{
							OutletId:      proto.Int64(225),
							OptionGroupId: proto.Int64(643264),
						},
					},
					TariffId: proto.Int64(112),
				},
			},
			PostBuckets: []*delivery.PostBucket{
				{
					BucketId:   proto.Int64(364),
					Program:    delivery.ProgramType_REGULAR_PROGRAM.Enum(),
					Currency:   proto.String("BYR"),
					CarrierIds: []int32{123, 321},
					DeliveryOptionGroupPostOutlets: []*delivery.DeliveryOptionsGroupPostOutlet{
						{
							PostCode:      proto.Int32(111241),
							OptionGroupId: proto.Int64(33243),
						},
					},
					TariffId: nil,
				},
			},
			PickupBucketsV2: nil,
			PostBucketsV2:   nil,
		},
		Currency:       []string{"RUB"},
		UseYmlDelivery: nil,
		Version:        nil,
	}
}

func EncodeFile(magic string, message proto.Message) ([]byte, error) {
	buffer := bytes.NewBuffer([]byte{})
	writer := pbsn.NewUnbufferedWriter(buffer)
	err := writer.WriteProto(magic, message)
	return buffer.Bytes(), err
}

func GenerateFiles() (bucketsUrls, modifiersUrls []string, server *httptest.Server) {
	router := chi.NewRouter()
	router.Use(middleware.Recoverer)
	server = httptest.NewServer(router)

	router.Route("/modifiers", func(r chi.Router) {
		goodModifiers, err := EncodeFile("DCMD", GetGoodPathModifiers())
		if err != nil {
			panic(err)
		}

		r.Handle("/good_modifiers", NewMockHTTPFileHandler(
			MockHTTPResponse{
				Code: http.StatusOK,
				Data: goodModifiers,
			},
		))
		modifiersUrls = append(modifiersUrls, fmt.Sprintf("%s/modifiers/good_modifiers", server.URL))

		badModifiers, err := EncodeFile("DCMD", GetBadPathModifiers())
		if err != nil {
			panic(err)
		}

		// We don't retry after 404 response
		r.Handle("/bad_modifiers", NewMockHTTPFileHandler(
			MockHTTPResponse{
				Code: http.StatusNotFound,
			},
			MockHTTPResponse{
				Code: http.StatusOK,
				Data: badModifiers,
			},
		))
		modifiersUrls = append(modifiersUrls, fmt.Sprintf("%s/modifiers/bad_modifiers", server.URL))
	})

	router.Route("/buckets", func(r chi.Router) {
		bucketsWithVersion, err := EncodeFile("DCFA", GetDeliveryBucketsFileWithVersion())
		if err != nil {
			panic(err)
		}

		// Retry on 503
		r.Handle("/versioned_buckets", NewMockHTTPFileHandler(
			MockHTTPResponse{
				Code: http.StatusServiceUnavailable,
				Data: nil,
			},
			MockHTTPResponse{
				Code: http.StatusOK,
				Data: bucketsWithVersion,
			},
		))
		bucketsUrls = append(bucketsUrls, fmt.Sprintf("%s/buckets/versioned_buckets", server.URL))

		bucketsWithoutVersion, err := EncodeFile("DCFA", GetDeliveryBucketsFileWithoutVersion())
		if err != nil {
			panic(err)
		}

		// Retry on 429
		r.Handle("/unversioned_buckets", NewMockHTTPFileHandler(
			MockHTTPResponse{
				Code: http.StatusTooManyRequests,
				Data: nil,
			},
			MockHTTPResponse{
				Code: http.StatusOK,
				Data: bucketsWithoutVersion,
			},
		))
		bucketsUrls = append(bucketsUrls, fmt.Sprintf("%s/buckets/unversioned_buckets", server.URL))
	})

	return
}

func CheckErrorsTable(t *testing.T, env *yttest.Env, config DeliveryUploaderConfig) {
	errors := make([]ErrorRow, 0, 1)
	err := env.DownloadSlice(ypath.Path(config.errorsTablePath), &errors)
	assert.NoError(t, err)
	assert.Equal(t, 1, len(errors))
	assert.Equal(t, "response code is 404", errors[0].Error)
}

func CheckModifiersTable(t *testing.T, env *yttest.Env, config DeliveryUploaderConfig) {
	modifiers := make([]DataRow, 0, 2)
	err := env.DownloadSlice(ypath.Path(config.modifiersTablePath), &modifiers)
	assert.NoError(t, err)
	assert.Equal(t, 2, len(modifiers))

	for _, modifierRow := range modifiers {
		var modifier delivery.DeliveryModifier
		err = proto.Unmarshal(modifierRow.Data, &modifier)
		assert.NoError(t, err)
		assert.NotNil(t, modifier.Id)
		assert.Equal(t, modifierRow.ID, *modifier.Id)
		assert.True(t, *modifier.Id == 1 || *modifier.Id == 2)
	}
}

func CheckOptionsGroupsTable(t *testing.T, env *yttest.Env, config DeliveryUploaderConfig) {
	optionsGroups := make([]DataRow, 0, 5)
	err := env.DownloadSlice(ypath.Path(config.deliveryOptionsGroupTablePath), &optionsGroups)
	assert.NoError(t, err)
	assert.Equal(t, 5, len(optionsGroups))

	optionsGroupsIdsSet := map[int64]bool{
		15366:  true,
		16138:  true,
		146244: true,
		643264: true,
		33243:  true,
	}

	optionsGroupsIdsToVersion := map[int64]int64{
		15366:  366,
		16138:  366,
		146244: 0,
		643264: 0,
		33243:  0,
	}

	for _, optionGroupRow := range optionsGroups {
		assert.True(t, optionsGroupsIdsSet[optionGroupRow.ID])
		optionsGroupsIdsSet[optionGroupRow.ID] = false
		if optionGroupRow.Version != nil {
			assert.Equal(t, optionsGroupsIdsToVersion[optionGroupRow.ID], *optionGroupRow.Version)
		}
		var optionGroup delivery.DeliveryOptionsGroup
		err = proto.Unmarshal(optionGroupRow.Data, &optionGroup)
		assert.NoError(t, err)
		assert.NotNil(t, optionGroup.DeliveryOptionGroupId)
		assert.Equal(t, optionGroupRow.ID, *optionGroup.DeliveryOptionGroupId)
	}
}

func CheckCourierBucketsTable(t *testing.T, env *yttest.Env, config DeliveryUploaderConfig) {
	courierBuckets := make([]DataRow, 0, 1)
	err := env.DownloadSlice(ypath.Path(config.deliveryOptionsBucketTablePath), &courierBuckets)
	assert.NoError(t, err)
	assert.Equal(t, 1, len(courierBuckets))
	assert.EqualValues(t, 24923, courierBuckets[0].ID)
	var courierBucket delivery.DeliveryOptionsBucket
	err = proto.Unmarshal(courierBuckets[0].Data, &courierBucket)
	assert.NoError(t, err)
	assert.NotNil(t, courierBucket.DeliveryOptBucketId)
	assert.Equal(t, courierBuckets[0].ID, *courierBucket.DeliveryOptBucketId)
}

func CheckPickupBucketsTable(t *testing.T, env *yttest.Env, config DeliveryUploaderConfig) {
	pickupBuckets := make([]DataRow, 0, 1)
	err := env.DownloadSlice(ypath.Path(config.pickupBucketTablePath), &pickupBuckets)
	assert.NoError(t, err)
	assert.Equal(t, 1, len(pickupBuckets))
	assert.EqualValues(t, 363, pickupBuckets[0].ID)
	var pickupBucket delivery.PickupBucket
	err = proto.Unmarshal(pickupBuckets[0].Data, &pickupBucket)
	assert.NoError(t, err)
	assert.NotNil(t, pickupBucket.BucketId)
	assert.Equal(t, pickupBuckets[0].ID, *pickupBucket.BucketId)
}

func CheckPickupOptionsBucketsTable(t *testing.T, env *yttest.Env, config DeliveryUploaderConfig) {
	pickupOptionsBuckets := make([]DataRow, 0, 2)
	err := env.DownloadSlice(ypath.Path(config.pickupOptionalsBucketTablePath), &pickupOptionsBuckets)
	assert.NoError(t, err)
	assert.Equal(t, 2, len(pickupOptionsBuckets))

	for _, pickupOptionsBucketRow := range pickupOptionsBuckets {
		assert.True(t, pickupOptionsBucketRow.ID == 326 || pickupOptionsBucketRow.ID == 164)

		assert.NotNil(t, pickupOptionsBucketRow.Version)
		assert.EqualValues(t, 366, *pickupOptionsBucketRow.Version)

		var pickupOptionsBucket delivery.PickupOptionsBucket
		err = proto.Unmarshal(pickupOptionsBucketRow.Data, &pickupOptionsBucket)
		assert.NoError(t, err)
		assert.NotNil(t, pickupOptionsBucket.BucketId)
		assert.Equal(t, pickupOptionsBucketRow.ID, *pickupOptionsBucket.BucketId)
	}
}

func CheckPostBucketsTable(t *testing.T, env *yttest.Env, config DeliveryUploaderConfig) {
	postBuckets := make([]DataRow, 0, 1)
	err := env.DownloadSlice(ypath.Path(config.postBucketTablePath), &postBuckets)
	assert.NoError(t, err)
	assert.Equal(t, 1, len(postBuckets))
	assert.EqualValues(t, 364, postBuckets[0].ID)
	var postBucket delivery.PostBucket
	err = proto.Unmarshal(postBuckets[0].Data, &postBucket)
	assert.NoError(t, err)
	assert.NotNil(t, postBucket.BucketId)
	assert.Equal(t, postBuckets[0].ID, *postBucket.BucketId)
}

func CheckTables(t *testing.T, env *yttest.Env, config DeliveryUploaderConfig) {
	CheckErrorsTable(t, env, config)
	CheckModifiersTable(t, env, config)
	CheckOptionsGroupsTable(t, env, config)
	CheckCourierBucketsTable(t, env, config)
	CheckPickupBucketsTable(t, env, config)
	CheckPickupOptionsBucketsTable(t, env, config)
	CheckPostBucketsTable(t, env, config)
}

func TestUploader(t *testing.T) {
	if runtime.GOOS != "linux" {
		return
	}

	bucketUrls, modifierUrls, server := GenerateFiles()
	env, cancel := yttest.NewEnv(t)
	defer cancel()

	config := RunDeliveryUploader(env, bucketUrls, modifierUrls, server.Client())
	server.Close()
	CheckTables(t, env, config)
}
