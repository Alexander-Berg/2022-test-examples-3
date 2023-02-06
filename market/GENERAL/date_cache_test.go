package routes

import (
	"testing"
	"time"

	"a.yandex-team.ru/library/go/test/requirepb"
	cr "a.yandex-team.ru/market/combinator/proto/grpc"
)

func TestDateCache(t *testing.T) {
	yearNow := cachedYear
	for i := 0; i < 3; i++ {
		dt0 := time.Date(yearNow+i, time.Month(1), int(1), 10, 0, 0, 0, time.UTC)
		pdt0 := CreateDate(dt0)
		requirepb.Equal(t, pdt0, &cr.Date{
			Year:  uint32(yearNow + i),
			Month: uint32(1),
			Day:   uint32(1),
		})
		dt := time.Date(yearNow+i, time.Month(12), int(31), 10, 0, 0, 0, time.UTC)
		pdt := CreateDate(dt)
		requirepb.Equal(t, pdt, &cr.Date{
			Year:  uint32(yearNow + i),
			Month: uint32(12),
			Day:   uint32(31),
		})
	}
}
