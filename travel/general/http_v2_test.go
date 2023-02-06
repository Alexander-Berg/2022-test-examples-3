package main

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"regexp"
	"testing"
	"unicode/utf8"

	arclog "a.yandex-team.ru/library/go/core/log"
	aviaLogging "a.yandex-team.ru/travel/avia/library/go/logging"
	"a.yandex-team.ru/travel/avia/suggests/api/yt"
	"github.com/labstack/echo/v4"
	"github.com/stretchr/testify/assert"
)

var (
	vnukovoAirport      = Point{pointKey: "s9600215", title: "Внуково", haveAirport: true, haveNotHiddenAirport: true}
	sheremetyevoAirport = Point{pointKey: "s9600213", title: "Шереметьево", haveAirport: true, haveNotHiddenAirport: true}
	roshchinoAirport    = Point{pointKey: "s9600384", title: "Рощино", haveAirport: true, haveNotHiddenAirport: true}
	rotterdamAirport    = Point{pointKey: "s9630336", title: "Роттердам", haveAirport: true, haveNotHiddenAirport: true}
	airports            = []Point{vnukovoAirport, sheremetyevoAirport, roshchinoAirport, rotterdamAirport}

	vnukovoCity   = Point{pointKey: "c10720", title: "Внуково"}
	moscowCity    = Point{pointKey: "c213", title: "Москва", haveAirport: true, haveNotHiddenAirport: true}
	tyumenCity    = Point{pointKey: "c55", title: "Тюмень", haveAirport: true, haveNotHiddenAirport: true}
	rotterdamCity = Point{pointKey: "c20755", title: "Роттердам", haveAirport: true, haveNotHiddenAirport: true}
	rodnikiCity   = Point{pointKey: "c20659", title: "Родники"}
	cities        = []Point{vnukovoCity, moscowCity, tyumenCity, rotterdamCity, rodnikiCity}

	russiaCountry      = Point{pointKey: "l225", title: "Россия", haveAirport: true, haveNotHiddenAirport: true}
	netherlandsCountry = Point{pointKey: "l118", title: "Нидерланды", haveAirport: true, haveNotHiddenAirport: true}
	countries          = []Point{russiaCountry, netherlandsCountry}

	stationMap = map[string]string{
		vnukovoAirport.pointKey:      moscowCity.pointKey,
		sheremetyevoAirport.pointKey: moscowCity.pointKey,
		rotterdamAirport.pointKey:    rotterdamCity.pointKey,
		roshchinoAirport.pointKey:    tyumenCity.pointKey,
	}
)

func generateSuggest(id int64, point Point) SuggestMetaInfo {
	return SuggestMetaInfo{
		id:                   id,
		pointKey:             point.pointKey,
		langID:               1,
		majority:             1,
		popularity:           0,
		directionPopularity:  0,
		pointTypeOrder:       getPointType(point.pointKey),
		secondPointTypeOrder: getPointType(point.pointKey),
		sameLang:             false,
		isCommonSynonym:      false,
		likeOther:            false,
		suggestLen:           utf8.RuneCountInString(point.title),
		likeCode:             false,
		misprint:             false,
		fullSuggest:          false,
		level:                0,
		added:                false,
		punted:               false,
		haveAirport:          point.haveAirport,
		title:                point.title,
	}
}

func generateSuggests(startID int64, points ...Point) []SuggestMetaInfo {
	var (
		currID   = startID
		suggests []SuggestMetaInfo
	)

	for _, point := range points {
		suggests = append(suggests, generateSuggest(currID, point))
		currID++
	}

	return suggests
}

func setConfig() {
	cleanQueryStringParam = regexp.MustCompile("[^\\p{L}\\d\\_\\-\\ \\{\\}\\;\\'\\,\\.\\[\\]\\<\\>\\:\\\"\\(\\)]+")
	cleanJSONPCallbackNameRegexp = regexp.MustCompile("^(callback|jQuery|suggest2_provider_jsonp_)([0-9])*$")
	originRegexp = regexp.MustCompile(`\.yandex\.(com|ru|ua|uz|com.tr|kz)$`)

	config = Config{
		Engine: Engine{
			SuggestLanguages:      map[string][]string{"ru": {"ru", "synonym", "code", "en"}},
			SuggestLanguagesTypos: map[string][]string{"ru": {"ru", "synonym", "en"}},
			PuntoLanguagesRaw:     map[string][]string{"ru": {"en"}},
			DefaultSuggests:       5,
			MaxSuggest:            10,
		},
	}

	appLogger, _ = aviaLogging.NewLogger(
		&aviaLogging.Config{
			Level:               arclog.InfoLevel,
			StdoutLoggingConfig: nil,
			FileLoggingConfig:   nil,
			SentryLoggingConfig: nil,
		},
	)
	ytLogger, _ = yt.NewLogger("console")

	SuggestStat = EngineStat{}
}

func setSuggestFabric(charsBeginMap map[string][]SuggestMetaInfo, popularCities map[string][]SuggestMetaInfo) {
	sf = SuggestFabric{
		LangMap: map[string]int{
			"ru":      1,
			"en":      4,
			"synonym": 98,
			"code":    99,
		},
		NGramIndexes: map[int]NGramIndex{
			1: {CharsBeginsMap: charsBeginMap},
		},
		PopularCities: popularCities,
		StationMap:    stationMap,
	}

	var points []Point
	points = append(points, airports...)
	points = append(points, cities...)
	points = append(points, countries...)

	sf.PointMap = make(map[string]Point)
	for _, point := range points {
		sf.PointMap[point.pointKey] = point
	}

	sf.InitSettlement2AllStations()

	sf.ReverseLangMap = make(map[int]string)
	for lang, langID := range sf.LangMap {
		sf.ReverseLangMap[langID] = lang
	}
}

func assertResponseForRequest(t *testing.T, request string, response string, compareOnlyLength bool) {
	e := echo.New()
	req := httptest.NewRequest(http.MethodGet, request, nil)
	rec := httptest.NewRecorder()
	c := e.NewContext(req, rec)

	if assert.NoError(t, IndexV2(c)) {
		assert.Equal(t, http.StatusOK, rec.Code)
		var v1, v2 interface{}
		_ = json.Unmarshal([]byte(response), &v1)
		_ = json.Unmarshal(rec.Body.Bytes(), &v2)
		if compareOnlyLength {
			bytes1, _ := json.Marshal(v1)
			bytes2, _ := json.Marshal(v2)
			assert.Equal(t, len(bytes1), len(bytes2))
		} else {
			assert.Equal(t, v1, v2)
		}
	}
}

func TestWithoutFlags(t *testing.T) {
	request := "/v2/avia?lang=ru&national_version=ru&field=to&query=ро"
	response := `[
		"ро",
		[
			[
				1,
				"Рощино",
				{
					"point_key": "s9600384",
					"point_code": "",
					"city_title": "Тюмень",
					"region_title": "",
					"country_title": "",
					"hidden": 0,
					"have_airport": 1,
					"have_not_hidden_airport": 1,
					"added": 0,
					"missprint": 0
				},
				[]
			],
			[
				0,
				"Роттердам",
				{
					"point_key": "c20755",
					"point_code": "",
					"city_title": "Роттердам",
					"region_title": "",
					"country_title": "",
					"hidden": 0,
					"have_airport": 1,
					"have_not_hidden_airport": 1,
					"added": 0,
					"missprint": 0
				},
				[]
			]
		]
	]`
	setConfig()
	setSuggestFabric(
		map[string][]SuggestMetaInfo{
			"ро": generateSuggests(0, roshchinoAirport, rotterdamAirport, rotterdamCity, rodnikiCity, russiaCountry),
		},
		nil,
	)

	assertResponseForRequest(t, request, response, false)
}

func TestShowEqualAirportsFlag(t *testing.T) {
	request := "/v2/avia?lang=ru&national_version=ru&field=to&query=ро&showEqualAirports=true"
	response := `[
		"ро",
		[
			[
				1,
				"Рощино",
				{
					"point_key": "s9600384",
					"point_code": "",
					"city_title": "Тюмень",
					"region_title": "",
					"country_title": "",
					"hidden": 0,
					"have_airport": 1,
					"have_not_hidden_airport": 1,
					"added": 0,
					"missprint": 0
				},
				[]
			],
			[
				1,
				"Роттердам",
				{
					"point_key": "s9630336",
					"point_code": "",
					"city_title": "Роттердам",
					"region_title": "",
					"country_title": "",
					"hidden": 0,
					"have_airport": 1,
					"have_not_hidden_airport": 1,
					"added": 0,
					"missprint": 0
				},
				[]
			],
			[
				0,
				"Роттердам",
				{
					"point_key": "c20755",
					"point_code": "",
					"city_title": "Роттердам",
					"region_title": "",
					"country_title": "",
					"hidden": 0,
					"have_airport": 1,
					"have_not_hidden_airport": 1,
					"added": 0,
					"missprint": 0
				},
				[]
			]
		]
	]`
	setConfig()
	setSuggestFabric(
		map[string][]SuggestMetaInfo{
			"ро": generateSuggests(0, roshchinoAirport, rotterdamAirport, rotterdamCity, rodnikiCity, russiaCountry),
		},
		nil,
	)

	assertResponseForRequest(t, request, response, false)
}

func TestOnlyCitiesFlag(t *testing.T) {
	request := "/v2/avia?lang=ru&national_version=ru&field=to&query=ро&only_cities=true"
	response := `[
		"ро",
		[
			[
				0,
				"Роттердам",
				{
					"point_key": "c20755",
					"point_code": "",
					"city_title": "Роттердам",
					"region_title": "",
					"country_title": "",
					"hidden": 0,
					"have_airport": 1,
					"have_not_hidden_airport": 1,
					"added": 0,
					"missprint": 0
				},
				[]
			]
		]
	]`
	setConfig()
	setSuggestFabric(
		map[string][]SuggestMetaInfo{
			"ро": generateSuggests(0, rotterdamAirport, roshchinoAirport, rotterdamCity, rodnikiCity, russiaCountry),
		},
		nil,
	)

	assertResponseForRequest(t, request, response, false)
}

func TestNeedCountryFlag(t *testing.T) {
	request := "/v2/avia?lang=ru&national_version=ru&field=to&query=ро&need_country=true"
	response := `[
		"ро",
		[
			[
				1,
				"Рощино",
				{
					"point_key": "s9600384",
					"point_code": "",
					"city_title": "Тюмень",
					"region_title": "",
					"country_title": "",
					"hidden": 0,
					"have_airport": 1,
					"have_not_hidden_airport": 1,
					"added": 0,
					"missprint": 0
				},
				[]
			],
			[
				0,
				"Роттердам",
				{
					"point_key": "c20755",
					"point_code": "",
					"city_title": "Роттердам",
					"region_title": "",
					"country_title": "",
					"hidden": 0,
					"have_airport": 1,
					"have_not_hidden_airport": 1,
					"added": 0,
					"missprint": 0
				},
				[]
			],
			[
				2,
				"Россия",
				{
					"point_key": "l225",
					"point_code": "",
					"city_title": "",
					"region_title": "",
					"country_title": "",
					"hidden": 0,
					"have_airport": 1,
					"have_not_hidden_airport": 1,
					"added": 0,
					"missprint": 0
				},
				[]
			]
		]
	]`
	setConfig()
	setSuggestFabric(
		map[string][]SuggestMetaInfo{
			"ро": generateSuggests(0, rotterdamAirport, roshchinoAirport, rotterdamCity, rodnikiCity, russiaCountry),
		},
		nil,
	)

	assertResponseForRequest(t, request, response, false)
}

func TestNestedSuggests(t *testing.T) {
	request := "/v2/avia?lang=ru&national_version=ru&field=to&query=моск"
	response := `[
		"моск",
		[
			[
				0,
				"Москва",
				{
					"point_key": "c213",
					"point_code": "",
					"city_title": "Москва",
					"region_title": "",
					"country_title": "",
					"hidden": 0,
					"have_airport": 1,
					"have_not_hidden_airport": 1,
					"added": 0,
					"missprint": 0
				},
				[
					[
						1,
						"Внуково",
						{
							"point_key": "s9600215",
							"point_code": "",
							"city_title": "Москва",
							"region_title": "",
							"country_title": "",
							"hidden": 0,
							"have_airport": 1,
							"have_not_hidden_airport": 1,
							"added": 1,
							"missprint": 0
						},
						[]
					],
					[
						1,
						"Шереметьево",
						{
							"point_key": "s9600213",
							"point_code": "",
							"city_title": "Москва",
							"region_title": "",
							"country_title": "",
							"hidden": 0,
							"have_airport": 1,
							"have_not_hidden_airport": 1,
							"added": 1,
							"missprint": 0
						},
						[]
					]
				]
			]
		]
	]`
	setConfig()
	setSuggestFabric(
		map[string][]SuggestMetaInfo{
			"моск": generateSuggests(0, moscowCity),
		},
		nil,
	)

	assertResponseForRequest(t, request, response, true)
}

func TestFlattenSuggestsFlag(t *testing.T) {
	request := "/v2/avia?lang=ru&national_version=ru&field=to&query=моск&flatten_suggests=true"
	response := `[
		"моск",
		[
			[
				0,
				"Москва",
				{
					"point_key": "c213",
					"point_code": "",
					"city_title": "Москва",
					"region_title": "",
					"country_title": "",
					"hidden": 0,
					"have_airport": 1,
					"have_not_hidden_airport": 1,
					"added": 0,
					"missprint": 0
				},
				[]
			],
			[
				1,
				"Внуково",
				{
					"point_key": "s9600215",
					"point_code": "",
					"city_title": "Москва",
					"region_title": "",
					"country_title": "",
					"hidden": 0,
					"have_airport": 1,
					"have_not_hidden_airport": 1,
					"added": 1,
					"missprint": 0
				},
				[]
			],
			[
				1,
				"Шереметьево",
				{
					"point_key": "s9600213",
					"point_code": "",
					"city_title": "Москва",
					"region_title": "",
					"country_title": "",
					"hidden": 0,
					"have_airport": 1,
					"have_not_hidden_airport": 1,
					"added": 1,
					"missprint": 0
				},
				[]
			]
		]
	]`
	setConfig()
	setSuggestFabric(
		map[string][]SuggestMetaInfo{
			"моск": generateSuggests(0, moscowCity),
		},
		nil,
	)

	assertResponseForRequest(t, request, response, true)
}

func TestPopularCities(t *testing.T) {
	request := "/v2/avia?lang=ru&national_version=ru&field=to&query=росс"
	response := `[
		"росс",
		[
			[
				0,
				"Москва",
				{
					"point_key": "c213",
					"point_code": "",
					"city_title": "Москва",
					"region_title": "",
					"country_title": "",
					"hidden": 0,
					"have_airport": 1,
					"have_not_hidden_airport": 1,
					"added": 1,
					"missprint": 0
				},
				[]
			]
		]
	]`
	setConfig()
	setSuggestFabric(
		map[string][]SuggestMetaInfo{
			"росс": generateSuggests(0, russiaCountry),
		},
		map[string][]SuggestMetaInfo{
			russiaCountry.pointKey: generateSuggests(1, moscowCity),
		},
	)

	assertResponseForRequest(t, request, response, false)
}

func TestPopularCitiesWithNeedCountryFlag(t *testing.T) {
	request := "/v2/avia?lang=ru&national_version=true&field=to&query=росс&need_country=true"
	response := `[
		"росс",
		[
			[
				2,
				"Россия",
				{
					"point_key": "l225",
					"point_code": "",
					"region_title": "",
					"city_title": "",
					"country_title": "",
					"hidden": 0,
					"have_airport": 1,
					"have_not_hidden_airport": 1,
					"added": 0,
					"missprint": 0
				},
				[
					[
						0,
						"Москва",
						{
							"point_key": "c213",
							"point_code": "",
							"city_title": "Москва",
							"region_title": "",
							"country_title": "",
							"hidden": 0,
							"have_airport": 1,
							"have_not_hidden_airport": 1,
							"added": 1,
							"missprint": 0
						},
						[]
					]
				]
			]
		]
	]`
	setConfig()
	setSuggestFabric(
		map[string][]SuggestMetaInfo{
			"росс": generateSuggests(0, russiaCountry),
		},
		map[string][]SuggestMetaInfo{
			russiaCountry.pointKey: generateSuggests(1, moscowCity),
		},
	)

	assertResponseForRequest(t, request, response, false)
}

func TestCountParam(t *testing.T) {
	request := "/v2/avia?lang=ru&national_version=ru&field=from&query=ро&showEqualAirports=true&need_country=true&count=1"
	response := `[
		"ро",
		[
			[
				1,
				"Рощино",
				{
					"point_key": "s9600384",
					"point_code": "",
					"city_title": "Тюмень",
					"region_title": "",
					"country_title": "",
					"hidden": 0,
					"have_airport": 1,
					"have_not_hidden_airport": 1,
					"added": 0,
					"missprint": 0
				},
				[]
			]
		]
	]`

	setConfig()
	setSuggestFabric(
		map[string][]SuggestMetaInfo{
			"ро": generateSuggests(0, roshchinoAirport, rotterdamAirport, rotterdamCity, rodnikiCity, russiaCountry),
		},
		map[string][]SuggestMetaInfo{
			russiaCountry.pointKey: generateSuggests(5, moscowCity),
		},
	)

	assertResponseForRequest(t, request, response, false)
}

func TestFieldFromWithNeedCountryFlag(t *testing.T) {
	request := "/v2/avia?lang=ru&national_version=ru&field=from&query=ро&need_country=true"
	response := `[
		"ро",
		[
			[
				1,
				"Рощино",
				{
					"point_key": "s9600384",
					"point_code": "",
					"city_title": "Тюмень",
					"region_title": "",
					"country_title": "",
					"hidden": 0,
					"have_airport": 1,
					"have_not_hidden_airport": 1,
					"added": 0,
					"missprint": 0
				},
				[]
			],
			[
				0,
				"Роттердам",
				{
					"point_key": "c20755",
					"point_code": "",
					"city_title": "Роттердам",
					"region_title": "",
					"country_title": "",
					"hidden": 0,
					"have_airport": 1,
					"have_not_hidden_airport": 1,
					"added": 0,
					"missprint": 0
				},
				[]
			]
		]
	]`
	setConfig()
	setSuggestFabric(
		map[string][]SuggestMetaInfo{
			"ро": generateSuggests(0, roshchinoAirport, rotterdamAirport, rotterdamCity, rodnikiCity, russiaCountry),
		},
		nil,
	)

	assertResponseForRequest(t, request, response, false)
}
