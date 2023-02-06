package handler

import (
	"a.yandex-team.ru/travel/rasp/train_offer_storage/internal/api/models"
	"encoding/json"
	"github.com/stretchr/testify/assert"
	"strings"
	"testing"
)

const SaveRequest string = `{
  "train_number": "882М",
  "train_ticket_number": "7082М",
  "train_title": "Москва — Владимир",
  "station_from_id": 2000001,
  "station_to_id": 9601539,
  "departure": "2020-03-26T07:31:00+03:00",
  "car_number": "10",
  "car_type": "sitting",
  "service_class": "2С",
  "electronic_registration": true,
  "bedding": true,
  "passengers": [
    {
      "first_name": "Джон",
      "last_name": "Маклейн",
      "patronymic": "-",
      "sex": "M",
      "birth_date": "1930-12-13",
      "doc_type": "ПН",
      "doc_id": "3709321354",
      "citizenship_geo_id": 225,
      "loyalty_cards": [],
      "tariff": "full",
      "age_group": "adults"
    }
  ],
  "places": [],
  "customer_email": "ganintsev@yandex-team.ru",
  "customer_phone": "+79122865336",
  "partner": "im",
  "deduplication_key": "cdb62eb3-4a42-4fb3-a862-2dced3309002",
  "label_params": {
    "utm_source": "email",
    "utm_medium": "transaction",
    "utm_campaign": "buy",
    "device": "desktop",
    "icookie": "Cm6Gn6vWHT4fienl6a/YVC45Qh1LkaSmBRUEmwCdNapoy/wYQ+fnOKtUr4IysHkkogLGmuhsIFrsyx5nOVdJrBEdjDY=",
    "terminal": "travel",
    "is_transfer": false,
    "ytp_referer": "test_referer"
  },
  "subscription_params": {
    "subscribe": false,
    "timezone": "Asia/Yekaterinburg",
    "language": "ru",
    "national_version": "ru"
  },
  "user_info": {
    "ip": "2a02:6b8:b081:501::1:3",
    "yandex_uid": "666909541573815708",
    "login": null,
    "geo_id": 54,
    "is_mobile": false
  },
  "mock_payment": false,
  "im_initial_station_name": "МОСКВА КУР",
  "im_final_station_name": "ВЛАДИМИР П"
}`

func TestStoreOfferRequest(t *testing.T) {
	decoder := json.NewDecoder(strings.NewReader(SaveRequest))
	var request models.StoreOfferRequest
	err := decoder.Decode(&request)
	assert.NoError(t, err)
	assert.Equal(t, true, request.Bedding)
	assert.Equal(t, "7082М", request.TrainTicketNumber)
	assert.Equal(t, "ПН", request.Passengers[0].DocType)
	assert.Equal(t, "desktop", request.LabelParams.Device)
	assert.Equal(t, "test_referer", request.LabelParams.YtpReferer)
}
