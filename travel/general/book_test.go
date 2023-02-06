package connector

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"testing"
	"time"

	"a.yandex-team.ru/library/go/test/assertpb"
	tpb "a.yandex-team.ru/travel/proto"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"google.golang.org/protobuf/types/known/timestamppb"

	"a.yandex-team.ru/travel/buses/backend/internal/common/dict"

	"a.yandex-team.ru/travel/buses/backend/internal/common/connector/mock"
	"a.yandex-team.ru/travel/buses/backend/internal/common/logging"
	pb "a.yandex-team.ru/travel/buses/backend/proto"
	wpb "a.yandex-team.ru/travel/buses/backend/proto/worker"
)

func TestHTTPClient_PostBook(t *testing.T) {
	testSupplierID := dict.GetSuppliersList()[0]
	testSupplier, _ := dict.GetSupplier(testSupplierID)

	testContacts := &wpb.TBookContacts{
		Phone: "+7 123 456-78-90",
		Email: "test@example.org",
	}
	testPassengers := []*wpb.TBookPassenger{
		{
			FirstName:      "Тест",
			MiddleName:     "Тестович",
			LastName:       "Тестов",
			BirthDate:      &tpb.TDate{Year: 1980, Month: 1, Day: 10},
			Gender:         &pb.TGender{Id: pb.EGenderType_GENDER_TYPE_MALE, PartnerId: "GenderPartnerId"},
			Citizenship:    &pb.TCitizenship{Id: "RU", PartnerId: "CitizenshipPartnerId"},
			DocumentType:   &pb.TDocumentType{Id: pb.EDocumentType_DOCUMENT_TYPE_RU_PASSPORT, PartnerId: "DocumentType.ID"},
			DocumentNumber: "1234 123456",
			TicketType:     &wpb.TBookPassengerTicketType{Id: pb.ETicketType_TICKET_TYPE_FULL, PartnerId: "TicketType.ID"},
			Seat:           &pb.TSeat{Id: "42", PartnerId: "Seat.ID"},
		},
	}
	expectedRequest := JSONBookingMapper{
		Passengers: []JSONBookingPassengerMapper{
			{
				JSONPassengerMixin: JSONPassengerMixin{
					FirstName:  "Тест",
					MiddleName: "Тестович",
					LastName:   "Тестов",
					BirthDate: JSONTime(
						time.Date(1980, 1, 10, 0, 0, 0, 0, time.UTC),
					),
				},
				Phone:           "+7 123 456-78-90",
				Email:           "test@example.org",
				SeatCode:        "Seat.ID",
				CitizenshipCode: "CitizenshipPartnerId",
				GenderCode:      "GenderPartnerId",
				DocTypeCode:     "DocumentType.ID",
				DocTypeID:       1,
				DocSeries:       "1234",
				DocNumber:       "123456",
				TicketTypeCode:  "TicketType.ID",
			},
		},
	}

	postBook := func(responseStatus int, responseBody string) (*wpb.TOrder, *wpb.TExplanation, error) {
		logger, _ := logging.New(&logging.DefaultConfig)
		client, err := NewClientWithTransport(
			&Config{APIURL: "http://mock"},
			testSupplierID,
			logger,
			mock.TransportMock(func(req *http.Request) *http.Response {
				assert.Equal(t, fmt.Sprintf("/%s/rides/rideID/book", testSupplier.Name), req.URL.Path)
				assert.Equal(t, "POST", req.Method)

				body, err := ioutil.ReadAll(req.Body)
				assert.NoError(t, err)

				var bookRequest JSONBookingMapper
				assert.NoError(t, json.Unmarshal(body, &bookRequest))

				assert.Equal(t, expectedRequest, bookRequest)

				return &http.Response{
					StatusCode: responseStatus,
					Body:       ioutil.NopCloser(bytes.NewBufferString(responseBody)),
					Request:    req,
					// Must be set to non-nil value or it panics
					Header: make(http.Header),
				}
			}),
		)
		if !assert.NoError(t, err) {
			return nil, nil, nil
		}
		return client.PostBook("busfor:rideID", testContacts, testPassengers)
	}

	t.Run("order mapping", func(t *testing.T) {
		order, _, err := postBook(http.StatusOK, `
{
	"result": {
		"@id": "eyJvcmRlcl9zaWQiOiA5Njl9",
		"status": {
			"id": 1,
			"name": "booked"
		},
		"expirationDateTime": "2020-09-23T00:00:00.000000+00:00",
		"tickets": [
			{
				"@id": "eyJ0aWNrZXRfc2lkIjogODkyLCAib3JkZXJfc2lkIjogOTY5fQ==",
				"status": {
					"id": 1,
					"name": "booked"
				},
				"data": {
					"code": "data.code",
					"number": "data.number",
					"series": "data.series",
					"barcode": "data.barcode",
					"platform": "data.platform"
				},
				"url": "ticket.url",
				"price": 105,
				"priceVat": "nds_none",
				"feeVat": "nds_none",
				"passenger": {
					"birthDate": "1980-01-10T00:00:00",
					"citizenship": "RU",
					"docNumber": "1234123456",
					"firstName": "Тест",
					"lastName": "Тестов",
					"middleName": "Тестович",
					"seat": "1",
					"docType": {
						"id": 1,
						"name": "id"
					},
					"genderType": {
						"id": 1,
						"name": "male"
					},
					"ticketType": {
						"id": 1,
						"name": "full"
					}
				}
			}
		]
	}
}`,
		)

		require.NoError(t, err)
		assertpb.Equal(t, &wpb.TOrder{
			Id:     "eyJvcmRlcl9zaWQiOiA5Njl9",
			Status: wpb.EOrderStatus_OS_BOOKED,
			ExpiresAt: &timestamppb.Timestamp{
				Seconds: time.Date(2020, 9, 23, 0, 0, 0, 0, time.UTC).Unix(),
			},
			Tickets: []*wpb.TTicket{
				{
					Id:       "eyJ0aWNrZXRfc2lkIjogODkyLCAib3JkZXJfc2lkIjogOTY5fQ==",
					Status:   wpb.ETicketStatus_TS_BOOKED,
					BlankUrl: "ticket.url",
					Code:     "data.code",
					Series:   "data.series",
					Number:   "data.number",
					Barcode:  "data.barcode",
					Platform: "data.platform",
					Price:    &tpb.TPrice{Amount: 10500, Currency: tpb.ECurrency_C_RUB, Precision: 2},
					PriceVat: wpb.ETicketVAT_TV_NDS_NONE,
					FeeVat:   wpb.ETicketVAT_TV_NDS_NONE,
					Passenger: &wpb.TTicketPassenger{
						FirstName:              "Тест",
						OptionalMiddleName:     &wpb.TTicketPassenger_MiddleName{MiddleName: "Тестович"},
						LastName:               "Тестов",
						BirthDate:              &tpb.TDate{Year: 1980, Month: 1, Day: 10},
						OptionalGender:         &wpb.TTicketPassenger_Gender{Gender: pb.EGenderType_GENDER_TYPE_MALE},
						OptionalCitizenship:    &wpb.TTicketPassenger_Citizenship{Citizenship: "RU"},
						OptionalDocumentType:   &wpb.TTicketPassenger_DocumentType{DocumentType: pb.EDocumentType_DOCUMENT_TYPE_RU_PASSPORT},
						OptionalDocumentNumber: &wpb.TTicketPassenger_DocumentNumber{DocumentNumber: "1234123456"},
						TicketType:             pb.ETicketType_TICKET_TYPE_FULL,
						Seat:                   "1",
					},
				},
			},
		}, order)
	})

	for _, testCase := range errorTestCases {
		t.Run(testCase.Name, func(t *testing.T) {
			order, _, err := postBook(testCase.ResponseStatus, testCase.ResponseBody)

			assert.Nil(t, order)
			assert.True(t, testCase.Check(t, err))
		})
	}
}

func Test_splitDocumentNumber(t *testing.T) {
	t.Run("foreign_passport", func(t *testing.T) {
		series, number := splitDocumentNumber(pb.EDocumentType_DOCUMENT_TYPE_FOREIGN_PASSPORT, "123 45678")
		assert.Equal(t, "", series)
		assert.Equal(t, "123 45678", number)
	})

	t.Run("number_without_series", func(t *testing.T) {
		series, number := splitDocumentNumber(pb.EDocumentType_DOCUMENT_TYPE_RU_PASSPORT, "№12345678")
		assert.Equal(t, "", series)
		assert.Equal(t, "№12345678", number)
	})

	t.Run("number_with_series", func(t *testing.T) {
		series, number := splitDocumentNumber(pb.EDocumentType_DOCUMENT_TYPE_RU_PASSPORT, "IV-НГ № 456329")
		assert.Equal(t, "IV-НГ", series)
		assert.Equal(t, "456329", number)
	})

	t.Run("ru_passport_with_joined_series_and_num", func(t *testing.T) {
		series, number := splitDocumentNumber(pb.EDocumentType_DOCUMENT_TYPE_RU_PASSPORT, "6513473829")
		assert.Equal(t, "6513", series)
		assert.Equal(t, "473829", number)
	})
}
