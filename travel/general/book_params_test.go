package connector

import (
	"bytes"
	"io/ioutil"
	"net/http"
	"testing"
	"time"

	"a.yandex-team.ru/library/go/test/assertpb"
	tpb "a.yandex-team.ru/travel/proto"
	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/buses/backend/internal/common/connector/mock"
	"a.yandex-team.ru/travel/buses/backend/internal/common/logging"
	pb "a.yandex-team.ru/travel/buses/backend/proto"
)

func TestHTTPClient_GetBookParams_RideRefinement(t *testing.T) {
	getBookParams := func(jsonResponse string) (*pb.TBookParams, *pb.TRideRefinement) {
		logger, _ := logging.New(&logging.DefaultConfig)
		client, err := NewClientWithTransport(
			&Config{APIURL: "http://mock"},
			10,
			logger,
			mock.TransportMock(func(req *http.Request) *http.Response {
				assert.Equal(t, "/etraffic/rides/rideID/book-params", req.URL.Path)
				return &http.Response{
					StatusCode: 200,
					Body:       ioutil.NopCloser(bytes.NewBufferString(jsonResponse)),
					Request:    req,
					// Must be set to non-nil value or it panics
					Header: make(http.Header),
				}
			}),
		)
		if !assert.NoError(t, err) {
			return nil, nil
		}

		bookParams, rideRefinement, _, err := client.GetBookParams("etraffic:rideID")
		if !assert.NoError(t, err) {
			return nil, nil
		}

		return bookParams, rideRefinement
	}

	t.Run("empty", func(t *testing.T) {
		bookParams, rideRefinement := getBookParams(`{}`)

		if assert.NotNil(t, bookParams) {
			assertpb.Equal(t, &pb.TRideRefinement{}, rideRefinement)
		}
	})

	t.Run("full", func(t *testing.T) {
		bookParams, rideRefinement := getBookParams(`{
			"result": {
				"from": {
					"id": "s1",
					"desc": "Station 1"
				},
				"to": {
					"id": "s2",
					"desc": "Station 2"
				},
				"departure": "2000-01-01T12:00:00",
				"arrival": "2000-01-01T23:00:00",
				"price": 123,
				"fee": 1
			}
		}`)

		if assert.NotNil(t, bookParams) {
			assertpb.Equal(t, &pb.TRideRefinement{
				From:          &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION, Id: 1},
				FromDesc:      "Station 1",
				To:            &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION, Id: 2},
				ToDesc:        "Station 2",
				DepartureTime: time.Date(2000, 1, 1, 12, 0, 0, 0, time.UTC).Unix(),
				ArrivalTime:   time.Date(2000, 1, 1, 23, 0, 0, 0, time.UTC).Unix(),
				Price:         &tpb.TPrice{Amount: 12300, Currency: tpb.ECurrency_C_RUB, Precision: 2},
				Fee:           &tpb.TPrice{Amount: 100, Currency: tpb.ECurrency_C_RUB, Precision: 2},
			}, rideRefinement)
		}
	})
}
