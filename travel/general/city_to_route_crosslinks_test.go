package repositories

import (
	"context"
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/core/log/nop"
	"a.yandex-team.ru/library/go/ptr"
	"a.yandex-team.ru/travel/avia/avia_statistics/api/internal/landingcityto"
)

func TestCityToRouteCrosslinkRepository(t *testing.T) {
	logger := &nop.Logger{}
	data := []landingcityto.CityToRouteCrosslink{
		{
			ToID:            1,
			CrosslinkFromID: 3,
			CrosslinkToID:   4,
			NationalVersion: "ru",
			Price:           ptr.Uint32(1000),
			Currency:        ptr.String("RUR"),
			Date:            ptr.String("2020-10-01"),
		},
		{
			ToID:            1,
			CrosslinkFromID: 5,
			CrosslinkToID:   6,
			NationalVersion: "ru",
			Price:           ptr.Uint32(2000),
			Currency:        ptr.String("RUR"),
			Date:            ptr.String("2020-10-02"),
		},
	}
	dataGetter := func() ([]landingcityto.CityToRouteCrosslink, error) { return data, nil }
	s := NewCityToRouteCrosslinksRepository(logger, dataGetter)
	_ = s.Update()
	t.Run(
		"Test entry found", func(t *testing.T) {
			result, err := s.Get(context.TODO(), 1, "ru")
			require.NoError(t, err, "There are should be no errors")
			require.Equal(t, data, result)
		},
	)

	t.Run(
		"Test not found", func(t *testing.T) {
			result, err := s.Get(context.TODO(), 70, "ru")
			require.Nil(t, result, "Result should be nil")
			require.Error(t, err, "Error should be returned")
			require.Equal(t, landingcityto.ErrorNotFound, err, "Error should ErrorNotFound")
		},
	)
}
