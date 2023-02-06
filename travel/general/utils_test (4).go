package cache

import (
	"time"

	"github.com/golang/protobuf/ptypes"
	pts "github.com/golang/protobuf/ptypes/timestamp"

	travel "a.yandex-team.ru/travel/proto"
	"a.yandex-team.ru/travel/trains/search_api/api/tariffs"
)

func newDirectionTariffInfo(fromID, toID int, departureDate *travel.TDate) *tariffs.DirectionTariffInfo {
	return &tariffs.DirectionTariffInfo{
		DeparturePointExpressId: int32(fromID),
		ArrivalPointExpressId:   int32(toID),
		DepartureDate:           departureDate,
		CreatedAt:               createDatetimeProto(2020, 1, 2, 3, 4, 5),
		UpdatedAt:               createDatetimeProto(2020, 11, 22, 23, 24, 25),
	}
}

func createDateProto(date time.Time) *travel.TDate {
	return &travel.TDate{
		Year:  int32(date.Year()),
		Month: int32(date.Month()),
		Day:   int32(date.Day()),
	}
}

func shortDate(year int, month int, day int) time.Time {
	return time.Date(year, time.Month(month), day, 0, 0, 0, 0, time.UTC)
}

func createTimeProto(ts time.Time) *pts.Timestamp {
	protoTS, _ := ptypes.TimestampProto(ts)
	return protoTS
}

func createDatetime(year int, month int, day int, hour int, min int, sec int) time.Time {
	return time.Date(year, time.Month(month), day, hour, min, sec, 0, time.UTC)
}

func createDatetimeProto(year int, month int, day int, hour int, min int, sec int) *pts.Timestamp {
	return createTimeProto(createDatetime(year, month, day, hour, min, sec))
}
