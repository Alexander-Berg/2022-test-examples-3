package filters

import (
	"testing"

	"github.com/stretchr/testify/assert"

	tpb "a.yandex-team.ru/travel/proto"
	"a.yandex-team.ru/travel/trains/search_api/api/tariffs"
	"a.yandex-team.ru/travel/trains/search_api/internal/direction/models"
	"a.yandex-team.ru/travel/trains/search_api/internal/direction/segments"
)

func TestCheckCoachTypeFilter(t *testing.T) {
	filter := NewCoachTypeFilter()
	v1 := newVariantWithCoachType("soft", 101)
	v2 := newVariantWithCoachType("soft", 102)
	v3 := newVariantWithCoachType("platzkarte", 103)

	_ = filter.LoadSelected([]string{"platzkarte"})
	filter.BindVariants(v1, v2, v3)
	filter.MakeAvailableVariant(v2)

	t.Run("Selecting", func(t *testing.T) {
		assert.True(t, filter.HasSelectedVariants())
		assert.False(t, filter.IsSelectedVariant(v1))
		assert.False(t, filter.IsSelectedVariant(v2))
		assert.True(t, filter.IsSelectedVariant(v3))
	})

	t.Run("Dumping", func(t *testing.T) {
		assert.Equal(t, []models.CoachTypeFilterResponse{
			{
				Value:        "common",
				Available:    false,
				Selected:     false,
				MinimumPrice: nil,
			},
			{
				Value:        "compartment",
				Available:    false,
				Selected:     false,
				MinimumPrice: nil,
			},
			{
				Value:        "platzkarte",
				Available:    false,
				Selected:     true,
				MinimumPrice: nil,
			},
			{
				Value:        "sitting",
				Available:    false,
				Selected:     false,
				MinimumPrice: nil,
			},
			{
				Value:     "soft",
				Available: true,
				Selected:  false,
				MinimumPrice: &models.PriceResponse{
					Value:    102,
					Currency: "RUB",
				},
			},
			{
				Value:        "suite",
				Available:    false,
				Selected:     false,
				MinimumPrice: nil,
			},
			{
				Value:        "unknown",
				Available:    false,
				Selected:     false,
				MinimumPrice: nil,
			},
		}, filter.Dump())
	})
}

func newVariantWithCoachType(coachType string, price int) segments.TrainVariant {
	return segments.TrainVariant{
		Place: &tariffs.TrainPlace{
			CoachType: coachType,
			Price: &tpb.TPrice{
				Currency:  1,
				Amount:    int64(price * 100),
				Precision: 2,
			},
		},
	}
}
