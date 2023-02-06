package blocktests

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestRegex(t *testing.T) {
	t.Run(
		"test regexp", func(t *testing.T) {
			expected := []string{
				"https://yandex.ru/legal/yandex_plus_conditions",
				"https://yandex.ru/legal/kinopoisk_vod",
			}
			text := "Условия: https://yandex.ru/legal/yandex_plus_conditions/) КиноПоиск - условия " +
				"платного просмотра контента https://yandex.ru/legal/kinopoisk_vod/ 18+ " +
				"Аудиогиды предоставлены платформой izi.TRAVEL"
			result := urlRegex.FindAllString(text, -1)
			assert.Equal(t, expected, result)
		},
	)
}
