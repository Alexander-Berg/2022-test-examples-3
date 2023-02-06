package next

import (
	"fmt"
	"testing"

	tankerpb "a.yandex-team.ru/travel/proto/tanker"
	"github.com/stretchr/testify/assert"
	"golang.org/x/text/language"
	"golang.org/x/text/message"
)

var keySetPb = &tankerpb.KeySet{
	Keys: map[string]*tankerpb.Key{
		"trains": &tankerpb.Key{
			Name: "trains",
			Translations: map[string]*tankerpb.Translation{
				"ru": &tankerpb.Translation{
					Language: "ru",
					Payload: &tankerpb.TranslationPayload{
						SingularForm: "поезд",
						DualForm:     "поезда",
						FewForm:      "поезда",
						ManyForm:     "поездов",
						ZeroForm:     "поездов",
						OtherForm:    "поездов",
					},
				},
				"en": &tankerpb.Translation{
					Language: "en",
					Payload: &tankerpb.TranslationPayload{
						SingularForm: "train",
						DualForm:     "trains",
						FewForm:      "trains",
						ManyForm:     "trains",
						ZeroForm:     "trains",
						OtherForm:    "trains",
					},
				},
			},
		},
		"trains.about": &tankerpb.Key{
			Name: "trains.about",
			Translations: map[string]*tankerpb.Translation{
				"ru": &tankerpb.Translation{
					Language: "ru",
					Payload: &tankerpb.TranslationPayload{
						SingularForm: "поезда",
						DualForm:     "поездов",
						FewForm:      "поездов",
						ManyForm:     "поездов",
						ZeroForm:     "поездов",
						OtherForm:    "поездов",
					},
				},
				"en": &tankerpb.Translation{
					Language: "en",
					Payload: &tankerpb.TranslationPayload{
						SingularForm: "train",
						DualForm:     "trains",
						FewForm:      "trains",
						ManyForm:     "trains",
						ZeroForm:     "trains",
						OtherForm:    "trains",
					},
				},
			},
		},
	},
}

func TestGetPlural(t *testing.T) {
	keySet, err := NewKeySet(keySetPb)
	assert.NoError(t, err)

	type TestCase struct {
		lang     string
		counts   []int
		expected string
	}
	testCases := []TestCase{
		{"ru", []int{0, 5, 9, 10, 11, 12, 13, 14, 25, 50, 112, 187}, "поездов"},
		{"ru", []int{1, 21, 561}, "поезд"},
		{"ru", []int{2, 3, 4, 22, 674}, "поезда"},
		{"en", []int{1}, "train"},
		{"en", []int{0, 2, 5, 11, 21, 187, 671}, "trains"},
	}
	for _, testCase := range testCases {
		for _, count := range testCase.counts {
			t.Run(fmt.Sprintf("lang-%s-count-%d", testCase.lang, count), func(t *testing.T) {
				res, err := keySet.GetPlural("trains", testCase.lang, count)
				assert.NoError(t, err)
				assert.Equal(t, testCase.expected, res)
			})
		}
	}
	t.Run("trains.about-key", func(t *testing.T) {
		res, err := keySet.GetPlural("trains.about", "ru", 1)
		assert.NoError(t, err)
		assert.Equal(t, "поезда", res)

		res, err = keySet.GetPlural("trains.about", "ru", 2)
		assert.NoError(t, err)
		assert.Equal(t, "поездов", res)
	})
	t.Run("no-key-error", func(t *testing.T) {
		_, err := keySet.GetPlural("unknown.key", "ru", 1)
		assert.Error(t, err)
	})
	t.Run("no-side-effect-on-default-printer", func(t *testing.T) {
		p := message.NewPrinter(language.Russian)
		res := p.Sprintf("trains", 1)
		assert.Equal(t, "trains", res)

		res = fmt.Sprintf("%d trains", 1)
		assert.Equal(t, "1 trains", res)
	})
}
