package calendar

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"google.golang.org/genproto/googleapis/type/date"

	"a.yandex-team.ru/library/go/core/log/nop"
	"a.yandex-team.ru/travel/app/backend/internal/common"
	"a.yandex-team.ru/travel/app/backend/internal/lib/calendarclient"
)

func TestGetDays(t *testing.T) {
	service := NewService(&nop.Logger{}, Config{}, nil)
	cache := createMap(map[string]string{
		"2020-12-27": "weekend",
		"2020-12-28": "weekday",
		"2020-12-29": "weekday",
		"2020-12-30": "weekday",
		"2020-12-31": "holiday",
		"2021-01-01": "holiday",
	})
	service.cacheValue.Store(cache)

	from := date.Date{
		Year:  2020,
		Month: 12,
		Day:   28,
	}
	to := date.Date{
		Year:  2020,
		Month: 12,
		Day:   30,
	}
	res, err := service.GetDays(&from, &to)
	require.NoError(t, err)

	expected := createMap(map[string]string{
		"2020-12-28": "weekday",
		"2020-12-29": "weekday",
		"2020-12-30": "weekday",
	})
	assert.Equal(t, expected, res)
}

func TestGetDays_WithoutDate(t *testing.T) {
	service := NewService(&nop.Logger{}, Config{}, nil)
	cache := createMap(map[string]string{
		"2020-12-27": "weekend",
		"2020-12-28": "weekday",
		"2020-12-29": "weekday",
		"2020-12-30": "weekday",
		"2020-12-31": "holiday",
		"2021-01-01": "holiday",
	})
	service.cacheValue.Store(cache)

	res, err := service.GetDays(nil, nil)
	require.NoError(t, err)

	assert.Equal(t, cache, res)
}

func createMap(days map[string]string) map[calendarclient.Date]string {
	cache := make(map[calendarclient.Date]string, len(days))
	for k, v := range days {
		t, _ := common.ParseDate(k)
		cache[calendarclient.Date{Time: t}] = v
	}
	return cache
}
