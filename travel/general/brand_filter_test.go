package filters

import (
	"testing"

	"github.com/stretchr/testify/assert"

	tpb "a.yandex-team.ru/travel/proto"
	"a.yandex-team.ru/travel/proto/dicts/rasp"
	"a.yandex-team.ru/travel/trains/search_api/api/tariffs"
	"a.yandex-team.ru/travel/trains/search_api/internal/direction/models"
	"a.yandex-team.ru/travel/trains/search_api/internal/direction/segments"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/geo"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/i18n"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/lang"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/testutils"
)

const (
	fakeBrandName = "fakeName"
	fakeBrandID   = 1001
)

func TestCheckBrandFilter(t *testing.T) {
	logger := testutils.NewLogger(t)
	filter := NewBrandFilter(
		logger, lang.Ru,
		newFakeTranslatableFactory(),
		map[string]int{fakeBrandName: fakeBrandID},
		map[int]string{fakeBrandID: fakeBrandName},
	)
	v1 := newBrandedVariant(0, "", 101)
	v2 := newBrandedVariant(1002, "simple title", 102)
	v3 := newBrandedVariant(fakeBrandID, fakeBrandName, 103)

	_ = filter.LoadSelected([]string{fakeBrandName})
	filter.BindVariants(v1, v2, v3)
	filter.MakeAvailableVariant(v2)

	t.Run("Selecting", func(t *testing.T) {
		assert.True(t, filter.HasSelectedVariants())
		assert.False(t, filter.IsSelectedVariant(v1))
		assert.False(t, filter.IsSelectedVariant(v2))
		assert.True(t, filter.IsSelectedVariant(v3))
	})

	t.Run("Dumping", func(t *testing.T) {
		assert.Equal(t, []models.BrandFilterResponse{
			{
				Value:        fakeBrandID,
				Available:    false,
				Selected:     true,
				MinimumPrice: nil,
				Title:        fakeBrandName,
				IsHighSpeed:  false,
			},
			{
				Value:     1002,
				Available: true,
				Selected:  false,
				MinimumPrice: &models.PriceResponse{
					Value:    102,
					Currency: "RUB",
				},
				Title:       "simple title",
				IsHighSpeed: false,
			},
		}, filter.Dump())
	})
}

func newBrandedVariant(id int, brandTitle string, price int) segments.TrainVariant {
	var brand *rasp.TNamedTrain
	if id != 0 {
		brand = &rasp.TNamedTrain{
			Id:           int32(id),
			TitleDefault: brandTitle,
		}
	}

	return segments.TrainVariant{
		Segment: &segments.TrainSegment{
			TrainBrand: brand,
		},
		Place: &tariffs.TrainPlace{
			Price: &tpb.TPrice{
				Currency:  1,
				Amount:    int64(price * 100),
				Precision: 2,
			},
		},
	}
}

func newFakeTranslatableFactory() *i18n.TranslatableFactory {
	return i18n.NewTranslatableFactory(
		i18n.NewLinguisticsTranslator(
			geo.NewFakeGeobaseClient(),
			i18n.FakeKeyset,
			make(map[lang.Lang]lang.Lang),
			make(map[lang.LinguisticForm]lang.LinguisticForm),
		),
		i18n.FakeKeyset,
	)
}
