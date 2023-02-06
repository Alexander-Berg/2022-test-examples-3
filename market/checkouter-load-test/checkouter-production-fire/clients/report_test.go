package clients

import (
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/configs"
	"encoding/json"
	"github.com/stretchr/testify/require"
	"testing"
)

const ReportPrimeResponse = "{ \"search\": { \"total\": 43, \"results\": [ { \"categories\": [ { \"id\": 13314855 } ], \"offers\": { \"count\": 3, \"items\": [ { \"categories\": [ { \"id\": 13314855 } ], \"fee\": \"0.0000\", \"feeSum\": \"0\", \"feeShow\": \"feeShow\", \"shop\": { \"id\": 431782, \"feed\": { \"id\": \"feedId\", \"offerId\": \"offerId.offerId\", \"categoryId\": \"\" } }, \"supplier\": { \"id\": 465852, \"type\": \"1\", \"warehouseId\": 147 }, \"weight\": \"0.01\", \"shopSku\": \"shopSku.shopSku\" } ] }, \"skuStats\": { \"totalCount\": 3, \"beforeFiltersCount\": 3, \"afterFiltersCount\": 3 } } ] }}"
const ReportOfferInfoResponseWithPriceRawValue = "{\"search\":{\"total\":1,\"totalOffers\":1,\"totalFreeOffers\":0,\"results\":[{\"entity\":\"offer\",\"categories\":[{\"entity\":\"category\",\"id\":91491}],\"prices\":{\"currency\":\"RUR\",\"value\":\"31999\",\"isDeliveryIncluded\":false,\"isPickupIncluded\":false,\"rawValue\":\"31999\"},\"model\":{\"id\":865640004},\"feeShow\":\"test\",\"shop\":{\"entity\":\"shop\",\"id\":431782},\"supplier\":{\"entity\":\"shop\",\"id\":1094898,\"type\":\"3\"},\"weight\":\"0.4\"}]}}"
const ReportOfferInfoResponseWithoutPriceRawValue = "{\"search\":{\"total\":1,\"totalOffers\":1,\"totalFreeOffers\":0,\"results\":[{\"entity\":\"offer\",\"categories\":[{\"entity\":\"category\",\"id\":91491}],\"prices\":{\"currency\":\"RUR\",\"value\":\"31999\",\"isDeliveryIncluded\":false,\"isPickupIncluded\":false},\"model\":{\"id\":865640004},\"feeShow\":\"test\",\"shop\":{\"entity\":\"shop\",\"id\":431782},\"supplier\":{\"entity\":\"shop\",\"id\":1094898,\"type\":\"3\"},\"weight\":\"0.4\"}]}}"

func reportPrimeResponseDeserialized() ReportPrimeSearchRoot {
	return ReportPrimeSearchRoot{
		Search: ReportPrimeSearch{
			Total: 43,
			Results: []ReportProduct{
				{ReportOffers{
					Count: 3,
					Items: []ReportOfferItem{
						{
							Weight:   "0.01",
							Supplier: Supplier{ID: 465852, Type: "1", WarehouseID: 147},
							Categories: []Category{
								{
									ID: 13314855,
								},
							},
							Shop: ReportShopFeed{Feed{
								FeedID:  "feedId",
								OfferID: "offerId.offerId",
							}},
							FeeShow: "feeShow",
							ShopSku: "shopSku.shopSku",
						},
					}},
				},
			},
		},
	}
}

func TestReportPrimeDeserializer(t *testing.T) {
	result := &ReportPrimeSearchRoot{}
	err := json.Unmarshal([]byte(ReportPrimeResponse), result)
	if err != nil {
		panic(err)
	}
	expected := reportPrimeResponseDeserialized()
	require.Equal(t, &expected, result)
}

func TestReportPrimeToStocksConverter(t *testing.T) {
	reportData := reportPrimeResponseDeserialized()
	stocks, _ := convertToStocks(reportData)
	require.Equal(t, []Stock{
		{
			Sku:      "shopSku.shopSku",
			Quantity: 0,
			SSItem: SSItem{
				ShopSku:     "shopSku.shopSku",
				SupplierID:  465852,
				WarehouseID: 147,
			},
		},
	}, stocks)
}

func TestConvertReportOfferItemToOfferInfo(t *testing.T) {
	reportSearchRootWithPriceRawValue := &ReportSearchRoot{}
	reportSearchRootWithoutPriceRawValue := &ReportSearchRoot{}
	err := json.Unmarshal([]byte(ReportOfferInfoResponseWithPriceRawValue), reportSearchRootWithPriceRawValue)
	if err != nil {
		panic(err)
	}
	err = json.Unmarshal([]byte(ReportOfferInfoResponseWithoutPriceRawValue), reportSearchRootWithoutPriceRawValue)
	if err != nil {
		panic(err)
	}
	gunConfig := configs.GunConfig{MaxWeight: 9999999999}
	testOptions := configs.Options{GunConfig: gunConfig}

	offerInfoWithPriceRawValue, _ := ConvertReportOfferItemToOfferInfo(testOptions, reportSearchRootWithPriceRawValue.ReportSearch.ReportResults[0])
	offerInfoWithoutPriceRawValue, _ := ConvertReportOfferItemToOfferInfo(testOptions, reportSearchRootWithoutPriceRawValue.ReportSearch.ReportResults[0])

	require.Equal(t, uint64(31999), offerInfoWithPriceRawValue.Price)
	require.Equal(t, uint64(31999), offerInfoWithoutPriceRawValue.Price)
}
