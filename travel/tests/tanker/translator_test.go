package tanker

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/library/go/core/log"
	aviaLogging "a.yandex-team.ru/travel/avia/library/go/logging"
	wizardtanker "a.yandex-team.ru/travel/avia/wizard/pkg/tanker"
	"a.yandex-team.ru/travel/avia/wizard/pkg/wizard/domain/models"
	"a.yandex-team.ru/travel/avia/wizard/pkg/wizard/lib/consts"
	"a.yandex-team.ru/travel/library/go/tanker"
)

var logger, _ = aviaLogging.NewLogger(&aviaLogging.Config{Level: log.InfoLevel})

func TestGetTranslation_TranslationExists_ReturnsIt(t *testing.T) {
	key := "key"
	translator := wizardtanker.NewTranslator(
		[]string{"ru", "en"},
		tanker.Keyset{
			key: &tanker.KeysetEntry{
				Info: &tanker.Info{IsPlural: false},
				Translations: map[string]*tanker.Translation{
					"ru": {Form: "Я люблю Go"},
					"en": {Form: "I love Go"},
				},
			},
		},
		logger,
	)

	test := func(language, expectedTranslation string) func(*testing.T) {
		return func(t *testing.T) {
			translation, err := translator.GetTranslation(key, models.Lang(language))

			assert.NoError(t, err)
			assert.Equal(t, expectedTranslation, translation)
		}
	}

	t.Run("ru", test("ru", "Я люблю Go"))
	t.Run("en", test("en", "I love Go"))
}

func TestGetTranslation_MissingKey_ReturnsError(t *testing.T) {
	key := "key"
	translator := wizardtanker.NewTranslator(
		[]string{"ru"},
		tanker.Keyset{},
		logger,
	)

	_, err := translator.GetTranslation(key, "ru")

	assert.Error(t, err)
	assert.Contains(t, err.Error(), "there is no translation for key")
}

func TestGetTranslation_KeyIsPlural_ReturnsError(t *testing.T) {
	key := "key"
	translator := wizardtanker.NewTranslator(
		[]string{"ru"},
		tanker.Keyset{
			key: &tanker.KeysetEntry{
				Info: &tanker.Info{IsPlural: true},
				Translations: map[string]*tanker.Translation{
					"ru": {Form: "Я люблю Go"},
				},
			},
		},
		logger,
	)

	_, err := translator.GetTranslation(key, "ru")

	assert.Error(t, err)
	assert.Contains(t, err.Error(), "couldn't apply non-pluralized translation for plural key")
}

func TestGetTranslation_MissingLanguage_ReturnsError(t *testing.T) {
	key := "key"
	translator := wizardtanker.NewTranslator(
		[]string{"ru"},
		tanker.Keyset{
			key: &tanker.KeysetEntry{
				Info: &tanker.Info{IsPlural: false},
				Translations: map[string]*tanker.Translation{
					"ru": {Form: "Я люблю Go"},
				},
			},
		},
		logger,
	)

	_, err := translator.GetTranslation(key, "en")

	assert.Error(t, err)
	assert.Contains(t, err.Error(), "there is no translation for language")
}

func TestGetTemplatedTranslation_TranslationExists_ReturnsIt(t *testing.T) {
	key := "key"
	translator := wizardtanker.NewTranslator(
		[]string{"ru", "en"},
		tanker.Keyset{
			key: &tanker.KeysetEntry{
				Info: &tanker.Info{IsPlural: false},
				Translations: map[string]*tanker.Translation{
					"ru": {Form: "Информация о рейсе {{.flightNumber}}"},
					"en": {Form: "Flight information for {{.flightNumber}}"},
				},
			},
		},
		logger,
	)
	flightNumber := "SU 1404"

	test := func(language, expectedTranslation string) func(*testing.T) {
		return func(t *testing.T) {
			translation, err := translator.GetTemplatedTranslation(
				key,
				models.Lang(language),
				map[string]interface{}{"flightNumber": flightNumber},
			)

			assert.NoError(t, err)
			assert.Equal(t, expectedTranslation, translation)
		}
	}

	t.Run("ru", test("ru", "Информация о рейсе SU 1404"))
	t.Run("en", test("en", "Flight information for SU 1404"))
}

type TestStruct struct {
	Field string
}

func (obj *TestStruct) MethodWithoutParams() string {
	return "method value"
}
func (obj *TestStruct) MethodWithParams(a, b string) string {
	return a + b
}

func TestGetTemplatedTranslation_TranslationWithStructFieldOrMethod_ReturnsIt(t *testing.T) {
	key := "key"
	translator := wizardtanker.NewTranslator(
		[]string{"en"},
		tanker.Keyset{
			key: &tanker.KeysetEntry{
				Info: &tanker.Info{IsPlural: false},
				Translations: map[string]*tanker.Translation{
					"en": {
						Form: "Field value = {{.struct.Field}}, " +
							"Non-parameterized method result = {{.struct.MethodWithoutParams}}, " +
							"Parameterized method result = {{.struct.MethodWithParams \"SU\" \"1404\"}}",
					},
				},
			},
		},
		logger,
	)
	expectedTranslation := "Field value = field value, " +
		"Non-parameterized method result = method value, " +
		"Parameterized method result = SU1404"

	translation, err := translator.GetTemplatedTranslation(key, "en", map[string]interface{}{"struct": &TestStruct{Field: "field value"}})

	assert.NoError(t, err)
	assert.Equal(t, expectedTranslation, translation)
}

func TestGetTemplatedTranslationWithCase_TranslationExists_ReturnsIt(t *testing.T) {
	key := "key"
	translator := wizardtanker.NewTranslator(
		[]string{"en"},
		tanker.Keyset{
			key: &tanker.KeysetEntry{
				Info: &tanker.Info{IsPlural: false},
				Translations: map[string]*tanker.Translation{
					"en": {
						Form: "Tickets from {{case .point}}",
					},
					"ru": {
						Form: "Авиабилеты из {{case .point \"genitive\"}}",
					},
				},
			},
		},
		logger,
	)
	declinator := func(point models.Point, grammaticalCase ...models.GrammaticalCase) (string, error) {
		if len(grammaticalCase) > 0 {
			return "Москвы", nil
		}
		return "Moscow", nil
	}

	translation, err := translator.GetTemplatedTranslationWithCase(
		key,
		consts.LangEN,
		map[string]interface{}{"point": &models.Settlement{}},
		declinator,
	)

	assert.NoError(t, err)
	assert.Equal(t, "Tickets from Moscow", translation)

	translation, err = translator.GetTemplatedTranslationWithCase(
		key,
		consts.LangRU,
		map[string]interface{}{"point": &models.Settlement{}},
		declinator,
	)

	assert.NoError(t, err)
	assert.Equal(t, "Авиабилеты из Москвы", translation)
}

func TestGetTemplatedTranslation_MissingKey_ReturnsError(t *testing.T) {
	key := "key"
	translator := wizardtanker.NewTranslator(
		[]string{"ru"},
		tanker.Keyset{},
		logger,
	)

	_, err := translator.GetTemplatedTranslation(key, "ru", map[string]interface{}{})

	assert.Error(t, err)
	assert.Contains(t, err.Error(), "there is no translation for key")
}

func TestGetTemplatedTranslation_KeyIsPlural_ReturnsError(t *testing.T) {
	key := "key"
	translator := wizardtanker.NewTranslator(
		[]string{"ru"},
		tanker.Keyset{
			key: &tanker.KeysetEntry{
				Info: &tanker.Info{IsPlural: true},
				Translations: map[string]*tanker.Translation{
					"ru": {Form: "Информация о рейсе {{.flightNumber}}"},
				},
			},
		},
		logger,
	)
	flightNumber := "SU 1404"

	_, err := translator.GetTemplatedTranslation(key, "ru", map[string]interface{}{"flightNumber": flightNumber})

	assert.Error(t, err)
	assert.Contains(t, err.Error(), "couldn't apply non-pluralized translation for plural key")
}

func TestGetTemplatedTranslation_MissingLanguage_ReturnsError(t *testing.T) {
	key := "key"
	translator := wizardtanker.NewTranslator(
		[]string{"ru", "en"},
		tanker.Keyset{
			key: &tanker.KeysetEntry{
				Info: &tanker.Info{IsPlural: false},
				Translations: map[string]*tanker.Translation{
					"ru": {Form: "Информация о рейсе {{.flightNumber}}"},
				},
			},
		},
		logger,
	)
	flightNumber := "SU 1404"

	_, err := translator.GetTemplatedTranslation(key, "en", map[string]interface{}{"flightNumber": flightNumber})

	assert.Error(t, err)
	assert.Contains(t, err.Error(), "there is no translation for language")
}

func TestGetPluralizedTemplatedTranslation_TranslationExists_ReturnsIt(t *testing.T) {
	key := "key"
	translator := wizardtanker.NewTranslator(
		[]string{"ru", "en", "be", "kk", "uz", "tr", "uk"},
		tanker.Keyset{
			key: &tanker.KeysetEntry{
				Info: &tanker.Info{IsPlural: true},
				Translations: map[string]*tanker.Translation{
					"ru": {
						Form1: "{{.count}} отзыв",
						Form2: "{{.count}} отзыва",
						Form3: "{{.count}} отзывов",
					},
					"en": {
						Form1: "{{.count}} review",
						Form2: "{{.count}} reviews",
					},
					"be": {
						Form1: "{{.count}} водгук",
						Form2: "{{.count}} водгукі",
						Form3: "{{.count}} водгукаў",
					},
					"kk": {
						Form1: "{{.count}} пікір",
						Form2: "{{.count}} пікір",
					},
					"uz": {
						Form1: "{{.count}} ta fikr-mulohaza",
						Form2: "{{.count}} ta fikr-mulohaza",
					},
					"tr": {
						Form1: "{{.count}} yorum",
						Form2: "{{.count}} yorum",
					},
					"uk": {
						Form1: "{{.count}} відгук",
						Form2: "{{.count}} відгуки",
						Form3: "{{.count}} відгуків",
					},
				},
			},
		},
		logger,
	)

	test := func(reviews int, language, expectedTranslation string) func(*testing.T) {
		return func(t *testing.T) {
			translation, err := translator.GetTemplatedPluralizedTranslation(
				key,
				models.Lang(language),
				reviews,
				map[string]interface{}{"count": reviews},
			)

			assert.NoError(t, err)
			assert.Equal(t, expectedTranslation, translation)
		}
	}

	t.Run("ru_0", test(0, "ru", "0 отзывов"))
	t.Run("ru_1", test(1, "ru", "1 отзыв"))
	t.Run("ru_2", test(2, "ru", "2 отзыва"))
	t.Run("ru_10", test(10, "ru", "10 отзывов"))
	t.Run("ru_21", test(21, "ru", "21 отзыв"))
	t.Run("ru_100", test(100, "ru", "100 отзывов"))

	t.Run("en_0", test(0, "en", "0 reviews"))
	t.Run("en_1", test(1, "en", "1 review"))
	t.Run("en_2", test(2, "en", "2 reviews"))
	t.Run("en_21", test(21, "en", "21 reviews"))

	t.Run("be_0", test(0, "be", "0 водгукаў"))
	t.Run("be_1", test(1, "be", "1 водгук"))
	t.Run("be_2", test(2, "be", "2 водгукі"))
	t.Run("be_10", test(10, "be", "10 водгукаў"))
	t.Run("be_21", test(21, "be", "21 водгук"))
	t.Run("be_100", test(100, "be", "100 водгукаў"))

	t.Run("kk_0", test(0, "kk", "0 пікір"))
	t.Run("kk_1", test(1, "kk", "1 пікір"))
	t.Run("kk_2", test(2, "kk", "2 пікір"))
	t.Run("kk_10", test(10, "kk", "10 пікір"))
	t.Run("kk_21", test(21, "kk", "21 пікір"))
	t.Run("kk_100", test(100, "kk", "100 пікір"))

	t.Run("uz_0", test(0, "uz", "0 ta fikr-mulohaza"))
	t.Run("uz_1", test(1, "uz", "1 ta fikr-mulohaza"))
	t.Run("uz_2", test(2, "uz", "2 ta fikr-mulohaza"))
	t.Run("uz_10", test(10, "uz", "10 ta fikr-mulohaza"))
	t.Run("uz_21", test(21, "uz", "21 ta fikr-mulohaza"))
	t.Run("uz_100", test(100, "uz", "100 ta fikr-mulohaza"))

	t.Run("tr_0", test(0, "tr", "0 yorum"))
	t.Run("tr_1", test(1, "tr", "1 yorum"))
	t.Run("tr_2", test(2, "tr", "2 yorum"))
	t.Run("tr_10", test(10, "tr", "10 yorum"))
	t.Run("tr_21", test(21, "tr", "21 yorum"))
	t.Run("tr_100", test(100, "tr", "100 yorum"))

	t.Run("uk_0", test(0, "uk", "0 відгуків"))
	t.Run("uk_1", test(1, "uk", "1 відгук"))
	t.Run("uk_2", test(2, "uk", "2 відгуки"))
	t.Run("uk_10", test(10, "uk", "10 відгуків"))
	t.Run("uk_21", test(21, "uk", "21 відгук"))
	t.Run("uk_100", test(100, "uk", "100 відгуків"))
}

func TestGetPluralizedTemplatedTranslation_MissingKey_ReturnsError(t *testing.T) {
	key := "key"
	translator := wizardtanker.NewTranslator(
		[]string{"ru"},
		tanker.Keyset{},
		logger,
	)

	_, err := translator.GetTemplatedPluralizedTranslation(key, "ru", 1, map[string]interface{}{})

	assert.Error(t, err)
	assert.Contains(t, err.Error(), "there is no translation for key")
}

func TestGetPluralizedTemplatedTranslation_KeyIsNotPlural_ReturnsError(t *testing.T) {
	key := "key"
	translator := wizardtanker.NewTranslator(
		[]string{"ru"},
		tanker.Keyset{
			key: &tanker.KeysetEntry{
				Info:         &tanker.Info{IsPlural: false},
				Translations: map[string]*tanker.Translation{},
			},
		},
		logger,
	)

	_, err := translator.GetTemplatedPluralizedTranslation(key, "ru", 1, map[string]interface{}{})

	assert.Error(t, err)
	assert.Contains(t, err.Error(), "couldn't apply pluralized translation for non-plural key key")
}

func TestGetPluralizedTemplatedTranslation_MissingLanguage_ReturnsError(t *testing.T) {
	key := "key"
	translator := wizardtanker.NewTranslator(
		[]string{"ru"},
		tanker.Keyset{
			key: &tanker.KeysetEntry{
				Info:         &tanker.Info{IsPlural: true},
				Translations: map[string]*tanker.Translation{},
			},
		},
		logger,
	)

	_, err := translator.GetTemplatedPluralizedTranslation(key, "ru", 1, map[string]interface{}{})

	assert.Error(t, err)
	assert.Contains(t, err.Error(), "there is no translation for language ru for key key")
}
