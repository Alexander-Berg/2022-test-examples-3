package filters

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/trains/search_api/internal/direction/segments"
)

type testVariantOption int

func (o testVariantOption) isVariantOption() {}

func TestSelectedVariantsThroughSelectedView(t *testing.T) {
	storage := NewVariantStorage()
	v1, v2, v3 := newVariant(), newVariant(), newVariant()

	hiddenOption := testVariantOption(1)
	selectedOption := testVariantOption(2)

	storage.SelectOption(selectedOption)
	storage.AddVariant(v1, hiddenOption)
	storage.AddVariant(v2, hiddenOption)
	storage.AddVariant(v3, hiddenOption)

	assert.False(t, storage.HasSelectedVariants())
	assert.False(t, storage.IsSelectedVariant(v2))

	storage.AddVariant(v2, selectedOption)
	assert.True(t, storage.HasSelectedVariants())
	assert.False(t, storage.IsSelectedVariant(v1))
	assert.True(t, storage.IsSelectedVariant(v2))
	assert.False(t, storage.IsSelectedVariant(v3))
}

func TestCheckAvailabilityInStorage(t *testing.T) {
	storage := NewVariantStorage()
	v1, v2, v3 := newVariant(), newVariant(), newVariant()

	hiddenOption := testVariantOption(1)
	selectedOption := testVariantOption(2)

	storage.AddVariant(v1, hiddenOption)
	storage.AddVariant(v2, selectedOption)
	storage.AddVariant(v3, hiddenOption)

	assert.False(t, storage.IsAvailableVariant(v3))

	storage.MakeAvailableVariant(v3)
	assert.False(t, storage.IsAvailableVariant(v1))
	assert.False(t, storage.IsAvailableVariant(v2))
	assert.True(t, storage.IsAvailableVariant(v3))
}

func TestGetAvailableVariants(t *testing.T) {
	storage := NewVariantStorage()
	v1, v2, v3 := newVariant(), newVariant(), newVariant()

	option1 := testVariantOption(1)
	option2 := testVariantOption(2)

	storage.AddVariant(v1, option1)
	storage.AddVariant(v2, option1, option2)
	storage.AddVariant(v3, option2)

	assert.Len(t, storage.GetAvailableVariantsByOption(option1), 0)
	assert.Len(t, storage.GetAvailableVariantsByOption(option2), 0)

	storage.MakeAvailableVariant(v1)
	assert.Len(t, storage.GetAvailableVariantsByOption(option1), 1)
	assert.Len(t, storage.GetAvailableVariantsByOption(option2), 0)

	storage.MakeAvailableVariant(v2)
	assert.Len(t, storage.GetAvailableVariantsByOption(option1), 2)
	assert.Len(t, storage.GetAvailableVariantsByOption(option2), 1)

	v4 := newVariant()
	option3 := testVariantOption(3)
	storage.AddVariant(v4, option3)
	storage.MakeAvailableVariant(v4)
	assert.Len(t, storage.GetAvailableVariantsByOption(option1), 2)
	assert.Len(t, storage.GetAvailableVariantsByOption(option2), 1)
}

func newVariant() segments.TrainVariant {
	return segments.TrainVariant{
		Segment: new(segments.TrainSegment),
	}
}
