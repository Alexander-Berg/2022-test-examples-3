package seo

import (
	"testing"

	proto "a.yandex-team.ru/travel/proto"
	"a.yandex-team.ru/travel/proto/dicts/rasp"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/geo"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/i18n"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/lang"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/points"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/templater"
	"a.yandex-team.ru/travel/trains/search_api/internal/seo/models"
	"github.com/stretchr/testify/assert"
)

const (
	directionTemplateStr = "Отправление {{FromPoint|ablative}}" +
		"{% if FromStationsTitles %}" +
		" — {{FromStationsTitles|join_last:\" и \"|join:\", \"}}\n" +
		"{% endif %}, \n" +
		"прибытие {{ToPoint|locative}}\n" +
		"{% if ToStationsTitles %}\n" +
		" — {{ToStationsTitles|join_last:\" и \"|join:\", \"}}\n" +
		"{% endif %}. \n" +
		"Первый поезд отправляется в {{FirstTrainDeparture}}, последний — в {{LastTrainDeparture}} по местному времени. " +
		"По маршруту проходит около {{TrainsCount}} {{AboutTrainsStr}} в день\n" +
		"{% if FirmTrains or ExpressTrains %}\n, в том числе \n{% endif %}\n" +
		"{% if FirmTrains %}\n{{FirmTrainsStr}} {{FirmTrains|join:\", \"}}\n{% endif %}\n" +
		"{% if FirmTrains and ExpressTrains %} и {% endif %}\n" +
		"{% if ExpressTrains %}\n{{ExpressTrainsStr}} {{ExpressTrains|join:\", \"}}\n{% endif %}."
	pricesTemplateStr = "Минимальная стоимость железнодорожного билета " +
		"{{FromPoint|ablative}} {{ToPoint|directional}} — " +
		"{% for carPrice in PricesByCarType %}" +
		"{% if forloop.First %}" +
		"{{carPrice.MinPricePretty}} {{carPrice.CarTypeLocative}}" +
		"{% elif forloop.Last and PricesByCarType|length > 3 %}" +
		". Самый дорогой билет {{carPrice.CarTypeAccusative}}: его цена начинается от {{carPrice.MinPricePretty}}" +
		"{% else  %}" +
		", {{carPrice.CarTypeLocative}} — от {{carPrice.MinPricePretty}}" +
		"{% endif %}" +
		"{% endfor %}" +
		"."
)

var (
	moscow = points.NewSettlement(&rasp.TSettlement{
		TitleRuNominativeCase:    "Москва",
		TitleRuAccusativeCase:    "Москву",
		TitleRuGenitiveCase:      "Москвы",
		TitleRuPrepositionalCase: "Москве",
		TitleRuPreposition:       "в",
		Title: &proto.TTranslationCase{
			Ru: &proto.TTranslationCaseRu{
				Nominative:          "Москва",
				Accusative:          "Москву",
				Genitive:            "Москвы",
				Prepositional:       "Москве",
				LocativePreposition: "в",
			},
		},
	})
	spb = points.NewSettlement(&rasp.TSettlement{
		TitleRuNominativeCase:    "Санкт-Петербург",
		TitleRuAccusativeCase:    "Санкт-Петербург",
		TitleRuGenitiveCase:      "Санкт-Петербурга",
		TitleRuPrepositionalCase: "Санкт-Петербурге",
		TitleRuPreposition:       "в",
		Title: &proto.TTranslationCase{
			Ru: &proto.TTranslationCaseRu{
				Nominative:          "Санкт-Петербург",
				Accusative:          "Санкт-Петербург",
				Genitive:            "Санкт-Петербурга",
				Prepositional:       "Санкт-Петербурге",
				LocativePreposition: "в",
			},
		},
	})
)

func newSimpleKeySet() *simpleKeySet {
	return &simpleKeySet{
		keySet: map[string]string{
			"preposition_to":   "в",
			"preposition_in":   "в",
			"preposition_from": "из",
		},
	}
}

type simpleKeySet struct {
	keySet map[string]string
}

func (k simpleKeySet) ExecuteSingular(key string, language lang.Lang, extraSlice ...map[string]interface{}) (string, error) {
	if res, ok := k.keySet[key]; ok {
		return res, nil
	}
	return key, nil
}

func init() {
	geobaseClient := geo.NewFakeGeobaseClient()
	linguisticsTranslator := i18n.NewLinguisticsTranslator(
		geobaseClient,
		newSimpleKeySet(),
		make(map[lang.Lang]lang.Lang),
		make(map[lang.LinguisticForm]lang.LinguisticForm),
	)
	translatableFactory := i18n.NewTranslatableFactory(linguisticsTranslator, i18n.FakeKeyset)
	templater.SetTranslator(translatableFactory)
}

func TestDirectionTemplate(t *testing.T) {
	data := models.SeoDirectionData{
		FromPoint: moscow,
		ToPoint:   spb,
		FromStationsTitles: []string{
			"Ленинградский вокзал",
			"Киевский вокзал",
			"Восточный вокзал",
			"Курский вокзал",
		},
		ToStationsTitles: []string{
			"Московский вокзал",
			"Ладожский вокзал",
		},
		FirstTrainDeparture: "00:20",
		LastTrainDeparture:  "23:55",
		TrainsCount:         31,
		AboutTrainsStr:      "поезда",
		FirmTrainsStr:       "фирменные поезда",
		FirmTrains: []string{
			"«Мегаполис»",
			"«Смена/А.Бетанкур - двухэтажный состав»",
			"«Двухэтажный состав»",
			"«Экспресс»",
			"«Гранд-Экспресс»",
			"«Красная стрела»",
			"«Арктика»",
		},
		ExpressTrainsStr: "скоростные поезда",
		ExpressTrains: []string{
			"«Сапсан»",
			"«Ласточка»",
			"«Стриж»",
		},
	}
	t.Run("simple", func(t *testing.T) {
		simpleTemplate, err := templater.InitTemplate("{{FromPoint|ablative|capfirst}}")
		assert.NoError(t, err)
		result, err := templater.ExecuteTemplate(simpleTemplate, data)
		assert.NoError(t, err)
		assert.Equal(t, "Из Москвы", result)
	})
	t.Run("fullData", func(t *testing.T) {
		directionTemplate, err := templater.InitTemplate(directionTemplateStr)
		assert.NoError(t, err)
		result, err := templater.ExecuteTemplate(directionTemplate, data)
		assert.NoError(t, err)
		assert.Equal(t, "Отправление из Москвы — Ленинградский вокзал, Киевский вокзал, Восточный вокзал и Курский вокзал, прибытие в Санкт-Петербурге — Московский вокзал и Ладожский вокзал. Первый поезд отправляется в 00:20, последний — в 23:55 по местному времени. По маршруту проходит около 31 поезда в день, в том числе фирменные поезда «Мегаполис», «Смена/А.Бетанкур - двухэтажный состав», «Двухэтажный состав», «Экспресс», «Гранд-Экспресс», «Красная стрела», «Арктика» и скоростные поезда «Сапсан», «Ласточка», «Стриж».", result)
	})
	t.Run("noFromStationsTitles", func(t *testing.T) {
		directionTemplate, err := templater.InitTemplate(directionTemplateStr)
		assert.NoError(t, err)
		data1 := data
		data1.FromStationsTitles = nil
		result, err := templater.ExecuteTemplate(directionTemplate, data1)
		assert.NoError(t, err)
		assert.Equal(t, "Отправление из Москвы, прибытие в Санкт-Петербурге — Московский вокзал и Ладожский вокзал. Первый поезд отправляется в 00:20, последний — в 23:55 по местному времени. По маршруту проходит около 31 поезда в день, в том числе фирменные поезда «Мегаполис», «Смена/А.Бетанкур - двухэтажный состав», «Двухэтажный состав», «Экспресс», «Гранд-Экспресс», «Красная стрела», «Арктика» и скоростные поезда «Сапсан», «Ласточка», «Стриж».", result)
	})
}

func TestPricesTemplate(t *testing.T) {
	data := models.SeoDirectionData{
		FromPoint: moscow,
		ToPoint:   spb,
		PricesByCarType: []*models.CarTypePrices{
			&models.CarTypePrices{
				CarTypeLocative:   "в сидячем вагоне",
				CarTypeAccusative: "в сидячий вагон",
				MinPricePretty:    "756 ₽",
			},
			&models.CarTypePrices{
				CarTypeLocative:   "в купе",
				CarTypeAccusative: "в купе",
				MinPricePretty:    "883 ₽",
			},
			&models.CarTypePrices{
				CarTypeLocative:   "в плацкарте",
				CarTypeAccusative: "в плацкарт",
				MinPricePretty:    "1 003 ₽",
			},
			&models.CarTypePrices{
				CarTypeLocative:   "в СВ",
				CarTypeAccusative: "в СВ",
				MinPricePretty:    "2 964 ₽",
			},
			&models.CarTypePrices{
				CarTypeLocative:   "в вагоне люкс",
				CarTypeAccusative: "в вагон люкс",
				MinPricePretty:    "11 490 ₽",
			},
		},
	}
	t.Run("fullData", func(t *testing.T) {
		directionTemplate, err := templater.InitTemplate(pricesTemplateStr)
		assert.NoError(t, err)
		result, err := templater.ExecuteTemplate(directionTemplate, data)
		assert.NoError(t, err)
		assert.Equal(t, "Минимальная стоимость железнодорожного билета из Москвы в Санкт-Петербург — 756 ₽ в сидячем вагоне, в купе — от 883 ₽, в плацкарте — от 1 003 ₽, в СВ — от 2 964 ₽. Самый дорогой билет в вагон люкс: его цена начинается от 11 490 ₽.", result)
	})
}
