package pricecalendar

import (
	"testing"

	commonModels "a.yandex-team.ru/travel/trains/library/go/httputil/clients/common/models"
	"github.com/stretchr/testify/assert"

	pcpb "a.yandex-team.ru/travel/trains/search_api/api/price_calendar"
	"a.yandex-team.ru/travel/trains/search_api/api/tariffs"
	"a.yandex-team.ru/travel/trains/search_api/internal/direction/segments"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/testutils"
)

func TestGetEmptyPriceReason(t *testing.T) {
	t.Run("AnySoldOut", func(t *testing.T) {
		segment := &segments.TrainSegment{
			BrokenClasses: &tariffs.TariffBrokenClasses{
				Platzkarte:  []uint32{commonModels.TariffErrorSoldOut},
				Compartment: []uint32{commonModels.TariffErrorServiceNotAllowed},
			},
		}
		res := getEmptyPriceReason(segment)
		assert.Equal(t, pcpb.EmptyPriceReason_EMPTY_PRICE_REASON_SOLD_OUT, res)
	})
	t.Run("NoOneSoldOut", func(t *testing.T) {
		segment := &segments.TrainSegment{
			BrokenClasses: &tariffs.TariffBrokenClasses{
				Platzkarte:  []uint32{commonModels.TariffErrorChildTariff},
				Compartment: []uint32{commonModels.TariffErrorServiceNotAllowed},
			},
		}
		res := getEmptyPriceReason(segment)
		assert.Equal(t, pcpb.EmptyPriceReason_EMPTY_PRICE_REASON_OTHER, res)
	})
	t.Run("EmptyBrokenClasses", func(t *testing.T) {
		segment := &segments.TrainSegment{}
		res := getEmptyPriceReason(segment)
		assert.Equal(t, pcpb.EmptyPriceReason_EMPTY_PRICE_REASON_OTHER, res)
	})
}

func TestUpdateDayPrice(t *testing.T) {
	t.Run("MinPriceUpdate", func(t *testing.T) {
		targetDayPrice := &pcpb.DayPrice{
			Price: testutils.PriceOf(1000),
		}
		segment := &segments.TrainSegment{
			BrokenClasses: &tariffs.TariffBrokenClasses{
				Platzkarte: []uint32{commonModels.TariffErrorSoldOut},
			},
			Places: []*tariffs.TrainPlace{
				&tariffs.TrainPlace{
					Price: testutils.PriceOf(900),
				},
			},
		}
		updateDayPrice(targetDayPrice, segment)
		assert.EqualValues(t, 900, targetDayPrice.Price.Amount)
	})
	t.Run("MinPriceNoUpdate", func(t *testing.T) {
		targetDayPrice := &pcpb.DayPrice{
			Price: testutils.PriceOf(800),
		}
		segment := &segments.TrainSegment{
			Places: []*tariffs.TrainPlace{
				&tariffs.TrainPlace{
					Price: testutils.PriceOf(1000),
				},
			},
		}
		updateDayPrice(targetDayPrice, segment)
		assert.EqualValues(t, 800, targetDayPrice.Price.Amount)
	})
	t.Run("EmptyPriceUpdate", func(t *testing.T) {
		targetDayPrice := &pcpb.DayPrice{
			EmptyPriceReason: pcpb.EmptyPriceReason_EMPTY_PRICE_REASON_SOLD_OUT,
		}
		segment := &segments.TrainSegment{
			Places: []*tariffs.TrainPlace{
				&tariffs.TrainPlace{
					Price: testutils.PriceOf(1000),
				},
			},
		}
		updateDayPrice(targetDayPrice, segment)
		assert.Equal(t, pcpb.EmptyPriceReason_EMPTY_PRICE_REASON_INVALID, targetDayPrice.EmptyPriceReason)
		assert.EqualValues(t, 1000, targetDayPrice.Price.Amount)
	})
	t.Run("EmptyPriceNoUpdate", func(t *testing.T) {
		targetDayPrice := &pcpb.DayPrice{
			Price: testutils.PriceOf(1000),
		}
		segment := &segments.TrainSegment{
			BrokenClasses: &tariffs.TariffBrokenClasses{
				Platzkarte: []uint32{commonModels.TariffErrorSoldOut},
			},
		}
		updateDayPrice(targetDayPrice, segment)
		assert.Equal(t, pcpb.EmptyPriceReason_EMPTY_PRICE_REASON_INVALID, targetDayPrice.EmptyPriceReason)
		assert.EqualValues(t, 1000, targetDayPrice.Price.Amount)
	})
	t.Run("EmptyPriceReasonUpdate", func(t *testing.T) {
		targetDayPrice := &pcpb.DayPrice{
			EmptyPriceReason: pcpb.EmptyPriceReason_EMPTY_PRICE_REASON_OTHER,
		}
		segment := &segments.TrainSegment{
			BrokenClasses: &tariffs.TariffBrokenClasses{
				Platzkarte: []uint32{commonModels.TariffErrorSoldOut},
			},
		}
		updateDayPrice(targetDayPrice, segment)
		assert.Equal(t, pcpb.EmptyPriceReason_EMPTY_PRICE_REASON_SOLD_OUT, targetDayPrice.EmptyPriceReason)
	})
	t.Run("EmptyPriceReasonNoUpdate", func(t *testing.T) {
		targetDayPrice := &pcpb.DayPrice{
			EmptyPriceReason: pcpb.EmptyPriceReason_EMPTY_PRICE_REASON_SOLD_OUT,
		}
		segment := &segments.TrainSegment{
			BrokenClasses: &tariffs.TariffBrokenClasses{
				Platzkarte: []uint32{commonModels.TariffErrorServiceNotAllowed},
			},
		}
		updateDayPrice(targetDayPrice, segment)
		assert.Equal(t, pcpb.EmptyPriceReason_EMPTY_PRICE_REASON_SOLD_OUT, targetDayPrice.EmptyPriceReason)
	})
}
