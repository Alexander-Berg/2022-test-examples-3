package filters

import (
	"context"
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/trains/search_api/internal/direction/segments"
)

func TestMergeAvailabilities(t *testing.T) {
	availabilities := [][]bool{
		{false, false, true, false, true},
		{false, true, true, false, true},
	}
	assert.Equal(t, []bool{false, false, true, false, true},
		intersectSelectors(availabilities[0], availabilities[1]))
}

func TestFilterAvailabilityDependsOnOtherFiltersOnly(t *testing.T) {
	selectors := [][]bool{
		{false, true, true, false, true},
		{false, true, true, false, false},
		{false, false, true, false, true},
	}

	group := NewGroup()
	for i, selector := range selectors {
		f := &fakeFilter{
			BaseFilter: NewBaseFilter(FilterName(fmt.Sprintf("fake%d", i))),
			selector:   selector,
		}
		group.AddFilter(f)
	}

	variants := newVariants(5)
	assert.Len(t, group.Apply(context.Background(), variants), 1)
	assert.Equal(t, map[string]interface{}{
		"fake0": map[segments.TrainVariant]struct{}{variants[2]: {}},
		"fake1": map[segments.TrainVariant]struct{}{variants[2]: {}},
		"fake2": map[segments.TrainVariant]struct{}{variants[2]: {}},
	}, group.Dump())
}

type fakeVariantOption bool

func (o fakeVariantOption) isVariantOption() {}

type fakeFilter struct {
	BaseFilter
	selector []bool
}

func (f *fakeFilter) BindVariants(variants ...segments.TrainVariant) {
	f.variantStorage.SelectOption(fakeVariantOption(true))
	for i, v := range variants {
		f.variantStorage.AddVariant(v, fakeVariantOption(f.selector[i]))
	}
}

func (f *fakeFilter) GetSearchParams() map[string][]string {
	return nil
}

func (f *fakeFilter) Dump() interface{} {
	response := make(map[segments.TrainVariant]struct{})
	for _, v := range f.variantStorage.GetAvailableVariantsByOption(fakeVariantOption(true)) {
		response[v] = struct{}{}
	}
	return response
}

func newVariants(number int) segments.TrainVariants {
	variants := make(segments.TrainVariants, 0, number)
	for i := 0; i < number; i++ {
		variants = append(variants, segments.TrainVariant{
			Segment: new(segments.TrainSegment),
		})
	}
	return variants
}
