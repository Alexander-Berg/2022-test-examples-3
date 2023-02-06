package main

import (
	"a.yandex-team.ru/library/go/core/log/zap"
	"a.yandex-team.ru/market/idx/golibrary/pbsn"
	delivery "a.yandex-team.ru/market/proto/delivery/delivery_calc"
	delivery_yt "a.yandex-team.ru/market/proto/delivery/delivery_yt"
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

func prepareLegacyDeliveryUploader(env *yttest.Env) (config DeliveryUploaderConfig) {
	numRetry = 5

	config.numWorkers = 5
	config.legacyBucketsPath = string(env.TmpPath())
	config.color = "BLUE"

	return
}

func RunLegacyDeliveryUploader(env *yttest.Env, bucketUrls []string, client *http.Client) (config DeliveryUploaderConfig) {
	config = prepareLegacyDeliveryUploader(env)
	logger = &zap.Logger{L: uberzap.NewNop()}

	uploader := NewDeliveryUploader(env.YT, config, logger)
	doUpload(client, bucketUrls, []string{}, uploader)

	return
}

func GetLegacyDeliveryBucketsFileWithVersion() *delivery.FeedDeliveryOptionsResp {
	return &delivery.FeedDeliveryOptionsResp{
		RequestId:    nil,
		ResponseCode: proto.Int32(http.StatusOK),
		FeedId:       nil,
		GenerationId: proto.Int64(964),
		UpdateTimeTs: nil,
		DeliveryOptionsByFeed: &delivery.DeliveryOptions{
			DeliveryOptionBuckets: []*delivery.DeliveryOptionsBucket{
				{
					DeliveryOptBucketId: proto.Int64(1),
					DeliveryOptionGroupRegs: []*delivery.DeliveryOptionsGroupRegion{
						{
							Region:             proto.Int32(172),
							DeliveryOptGroupId: proto.Int64(643264),
							OptionType:         delivery.OptionType_NORMAL_OPTION.Enum(),
						},
						{
							Region:             proto.Int32(213),
							DeliveryOptGroupId: proto.Int64(643264),
							OptionType:         delivery.OptionType_NORMAL_OPTION.Enum(),
						},
					},
					Currency:   proto.String("RUR"),
					CarrierIds: []int32{122},
					Program:    delivery.ProgramType_MARKET_DELIVERY_PROGRAM.Enum(),
					TariffId:   proto.Int64(102345),
				},
				{
					DeliveryOptBucketId: proto.Int64(2),
					DeliveryOptionGroupRegs: []*delivery.DeliveryOptionsGroupRegion{
						{
							Region:             proto.Int32(173),
							DeliveryOptGroupId: proto.Int64(643264),
							OptionType:         delivery.OptionType_NORMAL_OPTION.Enum(),
						},
					},
					Currency:   proto.String("RUR"),
					CarrierIds: []int32{122},
					Program:    delivery.ProgramType_MARKET_DELIVERY_PROGRAM.Enum(),
					TariffId:   proto.Int64(102345),
				},
			},
			PickupBuckets: []*delivery.PickupBucket{
				{
					BucketId: proto.Int64(3),
					DeliveryOptionGroupOutlets: []*delivery.DeliveryOptionsGroupOutlet{
						{
							OptionGroupId: proto.Int64(33243),
							OutletId:      proto.Int64(10001841669),
							Region:        proto.Int32(120842),
						},
						{
							OptionGroupId: proto.Int64(33243),
							OutletId:      proto.Int64(10001841669),
							Region:        proto.Int32(120842),
						},
						{
							OptionGroupId: proto.Int64(33243),
							OutletId:      proto.Int64(10001841669),
							Region:        proto.Int32(120842),
						},
					},
					Program:    delivery.ProgramType_MARKET_DELIVERY_PROGRAM.Enum(),
					Currency:   proto.String("RUR"),
					CarrierIds: []int32{1231231},
					TariffId:   proto.Int64(1023456),
				},
			},
			PostBuckets: []*delivery.PostBucket{
				{
					BucketId: proto.Int64(4),
					DeliveryOptionGroupPostOutlets: []*delivery.DeliveryOptionsGroupPostOutlet{
						{
							Region:        proto.Int32(63),
							MbiOutletId:   proto.Int64(101010101),
							OptionGroupId: proto.Int64(4444),
							PostCode:      proto.Int32(443001),
						},
						{
							Region:        proto.Int32(21),
							MbiOutletId:   proto.Int64(202020202),
							OptionGroupId: proto.Int64(4444),
							PostCode:      proto.Int32(428000),
						},
					},
					Program:    delivery.ProgramType_MARKET_DELIVERY_PROGRAM.Enum(),
					Currency:   proto.String("RUR"),
					CarrierIds: []int32{232323},
					TariffId:   proto.Int64(234567),
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
				{
					DeliveryOptionGroupId: proto.Int64(4444),
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
		},
		Currency:       []string{"RUR"},
		UseYmlDelivery: nil,
		Version:        proto.Int64(366),
	}
}

func EncodeLegacyFile(magic string, message proto.Message) ([]byte, error) {
	buffer := bytes.NewBuffer([]byte{})
	writer := pbsn.NewUnbufferedWriter(buffer)
	err := writer.WriteProto(magic, message)
	return buffer.Bytes(), err
}

func GenerateLegacyFiles() (bucketsUrls []string, server *httptest.Server) {
	router := chi.NewRouter()
	router.Use(middleware.Recoverer)
	server = httptest.NewServer(router)

	router.Route("/buckets", func(r chi.Router) {
		bucketsWithVersion, err := EncodeLegacyFile("DCFA", GetLegacyDeliveryBucketsFileWithVersion())
		if err != nil {
			panic(err)
		}

		r.Handle("/versioned_buckets", NewMockHTTPFileHandler(
			MockHTTPResponse{
				Code: http.StatusOK,
				Data: bucketsWithVersion,
			},
		))
		bucketsUrls = append(bucketsUrls, fmt.Sprintf("%s/buckets/versioned_buckets", server.URL))
	})
	return
}

func ParseRow(t *testing.T, dataRow *LegacyDataRow) (*delivery_yt.CommonDeliveryOptionsBucket, *delivery_yt.DeliveryOptionsGroupsForBucket, *delivery.DeliveryOptionsGroup) {
	bucket := &delivery_yt.CommonDeliveryOptionsBucket{}
	optionGroups := &delivery_yt.DeliveryOptionsGroupsForBucket{}
	deliveryOptionGroup := &delivery.DeliveryOptionsGroup{}
	err := proto.Unmarshal(dataRow.Bucket, bucket)
	assert.NoError(t, err)
	err = proto.Unmarshal(dataRow.OptionGroups, optionGroups)
	assert.NoError(t, err)
	err = proto.Unmarshal(dataRow.DeliveryOptionsGroup, deliveryOptionGroup)
	assert.NoError(t, err)
	return bucket, optionGroups, deliveryOptionGroup
}

func CheckCourierBuckets1(t *testing.T, bucket *delivery_yt.CommonDeliveryOptionsBucket, optionGroups *delivery_yt.DeliveryOptionsGroupsForBucket) {
	currentBucket := &delivery_yt.CommonDeliveryOptionsBucket{
		DeliveryOptBucketId: proto.Int64(1),
		GenerationId:        proto.Int64(964),
		Program:             delivery.ProgramType_MARKET_DELIVERY_PROGRAM.Enum(),
		Currency:            proto.String("RUR"),
		TariffId:            proto.Int64(102345),
		CarrierIds:          []int32{122},
		DeliveryOptionGroupRegs: []*delivery.DeliveryOptionsGroupRegion{
			{
				Region:             proto.Int32(172),
				DeliveryOptGroupId: proto.Int64(643264),
				OptionType:         delivery.OptionType_NORMAL_OPTION.Enum(),
			},
			{
				Region:             proto.Int32(213),
				DeliveryOptGroupId: proto.Int64(643264),
				OptionType:         delivery.OptionType_NORMAL_OPTION.Enum(),
			},
		},
	}
	currentOptions := &delivery_yt.DeliveryOptionsGroupsForBucket{
		DeliveryOptionGroups: []*delivery.DeliveryOptionsGroup{
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
		},
	}
	assert.Equal(t, proto.MarshalTextString(bucket), proto.MarshalTextString(currentBucket))
	assert.Equal(t, proto.MarshalTextString(optionGroups), proto.MarshalTextString(currentOptions))
}

func CheckCourierBuckets2(t *testing.T, bucket *delivery_yt.CommonDeliveryOptionsBucket, optionGroups *delivery_yt.DeliveryOptionsGroupsForBucket) {
	currentBucket := &delivery_yt.CommonDeliveryOptionsBucket{
		DeliveryOptBucketId: proto.Int64(2),
		GenerationId:        proto.Int64(964),
		DeliveryOptionGroupRegs: []*delivery.DeliveryOptionsGroupRegion{
			{
				Region:             proto.Int32(173),
				DeliveryOptGroupId: proto.Int64(643264),
				OptionType:         delivery.OptionType_NORMAL_OPTION.Enum(),
			},
		},
		Currency:   proto.String("RUR"),
		CarrierIds: []int32{122},
		Program:    delivery.ProgramType_MARKET_DELIVERY_PROGRAM.Enum(),
		TariffId:   proto.Int64(102345),
	}
	currentOptions := &delivery_yt.DeliveryOptionsGroupsForBucket{
		DeliveryOptionGroups: []*delivery.DeliveryOptionsGroup{
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
		},
	}
	assert.Equal(t, proto.MarshalTextString(bucket), proto.MarshalTextString(currentBucket))
	assert.Equal(t, proto.MarshalTextString(optionGroups), proto.MarshalTextString(currentOptions))
}

func CheckPickupBuckets(t *testing.T, bucket *delivery_yt.CommonDeliveryOptionsBucket, optionGroups *delivery_yt.DeliveryOptionsGroupsForBucket) {
	currentBucket := &delivery_yt.CommonDeliveryOptionsBucket{
		DeliveryOptBucketId: proto.Int64(3),
		GenerationId:        proto.Int64(964),
		Program:             delivery.ProgramType_MARKET_DELIVERY_PROGRAM.Enum(),
		Currency:            proto.String("RUR"),
		CarrierIds:          []int32{1231231},
		TariffId:            proto.Int64(1023456),
		DeliveryOptionGroupOutlets: []*delivery.DeliveryOptionsGroupOutlet{
			{
				OptionGroupId: proto.Int64(33243),
				OutletId:      proto.Int64(10001841669),
				Region:        proto.Int32(120842),
			},
			{
				OptionGroupId: proto.Int64(33243),
				OutletId:      proto.Int64(10001841669),
				Region:        proto.Int32(120842),
			},
			{
				OptionGroupId: proto.Int64(33243),
				OutletId:      proto.Int64(10001841669),
				Region:        proto.Int32(120842),
			},
		},
	}
	currentOptions := &delivery_yt.DeliveryOptionsGroupsForBucket{
		DeliveryOptionGroups: []*delivery.DeliveryOptionsGroup{
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
	}
	assert.Equal(t, proto.MarshalTextString(bucket), proto.MarshalTextString(currentBucket))
	assert.Equal(t, proto.MarshalTextString(optionGroups), proto.MarshalTextString(currentOptions))
}

func CheckPostBuckets(t *testing.T, bucket *delivery_yt.CommonDeliveryOptionsBucket, optionGroups *delivery_yt.DeliveryOptionsGroupsForBucket) {
	currentBucket := &delivery_yt.CommonDeliveryOptionsBucket{
		DeliveryOptBucketId: proto.Int64(4),
		GenerationId:        proto.Int64(964),
		Program:             delivery.ProgramType_MARKET_DELIVERY_PROGRAM.Enum(),
		Currency:            proto.String("RUR"),
		CarrierIds:          []int32{232323},
		TariffId:            proto.Int64(234567),
		DeliveryOptionGroupPostOutlets: []*delivery.DeliveryOptionsGroupPostOutlet{
			{
				Region:        proto.Int32(63),
				MbiOutletId:   proto.Int64(101010101),
				OptionGroupId: proto.Int64(4444),
				PostCode:      proto.Int32(443001),
			},
			{
				Region:        proto.Int32(21),
				MbiOutletId:   proto.Int64(202020202),
				OptionGroupId: proto.Int64(4444),
				PostCode:      proto.Int32(428000),
			},
		},
	}
	currentOptions := &delivery_yt.DeliveryOptionsGroupsForBucket{
		DeliveryOptionGroups: []*delivery.DeliveryOptionsGroup{
			{
				DeliveryOptionGroupId: proto.Int64(4444),
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
	}
	assert.Equal(t, proto.MarshalTextString(bucket), proto.MarshalTextString(currentBucket))
	assert.Equal(t, proto.MarshalTextString(optionGroups), proto.MarshalTextString(currentOptions))
}

func CheckLegacyBucketsTable(t *testing.T, env *yttest.Env, config DeliveryUploaderConfig) {
	deliveryBuckets := make([]LegacyDataRow, 0, 4)
	err := env.DownloadSlice(ypath.Path(config.legacyBucketsPath), &deliveryBuckets)
	assert.NoError(t, err)
	assert.Equal(t, 4, len(deliveryBuckets))

	for _, v := range deliveryBuckets {
		assert.True(t, v.BucketID == 1 || v.BucketID == 2 || v.BucketID == 3 || v.BucketID == 4)
		bucket, optionGroups, deliveryOptionGroup := ParseRow(t, &v)
		switch v.BucketID {
		case 1:
			CheckCourierBuckets1(t, bucket, optionGroups)
		case 2:
			CheckCourierBuckets2(t, bucket, optionGroups)
		case 3:
			CheckPickupBuckets(t, bucket, optionGroups)
		case 4:
			CheckPostBuckets(t, bucket, optionGroups)
		}
		assert.Equal(t, proto.MarshalTextString(optionGroups.DeliveryOptionGroups[0]), proto.MarshalTextString(deliveryOptionGroup))
	}
}

func TestLegacyUploader(t *testing.T) {
	if runtime.GOOS != "linux" {
		return
	}

	bucketUrls, server := GenerateLegacyFiles()
	env, cancel := yttest.NewEnv(t)
	defer cancel()

	config := RunLegacyDeliveryUploader(env, bucketUrls, server.Client())
	server.Close()
	CheckLegacyBucketsTable(t, env, config)

}
