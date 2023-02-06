describe('Daria.mThemeWeather', function() {

    function weather(overrides) {
        var template = {
            "season": "summer",
            "daytime": "day",
            "wind-speed": "2.0",
            "now": {
                "condition": "partly-cloudy-and-showers",
                "temperature": "23"
            }
        };

        return _.defaultsDeep(overrides, template);
    }

    beforeEach(function() {
        /** @type Daria.mThemeWeather */
        this.mThemeWeather = ns.Model.get('theme-weather');
    });

    xdescribe('#getSkinParams', function() {
        function wit(message, params, hasWeatherNewTheme) {
            it(message, function() {
                var _weather = params.weather || {};
                var _skin = params.skin;

                this._date = new Date(0, 1, 2, 3);

                if (params.before) {
                    params.before.call(this);
                }

                this.sinon.stub(Date, 'now').returns(this._date);
                this.sinon.stub(Daria, 'hasWeatherNewTheme').returns(Boolean(hasWeatherNewTheme));

                this.mThemeWeather.setData({
                    "weather": weather(_weather)
                });

                var skin = this.mThemeWeather.getSkinParams();
                Object.keys(_skin).forEach(function(key) {
                    expect(skin[key], key).to.be.eql(_skin[key]);
                });
            });
        }

        describe('Старая погодная тема', function() {

            describe('Весна', function() {
                wit('конец лета, температура < 6', {
                    before: function() {
                        this._date = new Date(0, 8, 7, 1);
                    },
                    weather: {
                        "season": "summer",
                        "now": {
                            "condition": "clear",
                            "temperature": 5
                        }
                    },
                    skin: {
                        season: 'spring',
                        condition: 'clear',
                        dayTime: 'day'
                    }
                });

                wit('ясно', {
                    weather: {
                        "season": "spring",
                        "now": {
                            "condition": "clear",
                            "temperature": 7
                        }
                    },
                    skin: {
                        season: 'spring',
                        condition: 'clear',
                        dayTime: 'day'
                    }
                });

                wit('облачно', {
                    weather: {
                        "season": "spring",
                        "now": {
                            "condition": "cloudy",
                            "temperature": 12
                        }
                    },
                    skin: {
                        season: 'spring',
                        condition: 'clouds',
                        dayTime: 'day'
                    }
                });

                wit('ожидается облачность', {
                    weather: {
                        "season": "spring",
                        "now": {
                            "condition": "partly-cloudy",
                            "temperature": 13
                        }
                    },
                    skin: {
                        season: 'spring',
                        condition: 'partly_cloudy',
                        dayTime: 'day'
                    }
                });

                wit('дождь', {
                    weather: {
                        "season": "spring",
                        "now": {
                            "condition": "cloudy-and-rain",
                            "temperature": 5
                        }
                    },
                    skin: {
                        season: 'spring',
                        condition: 'rain',
                        dayTime: 'day'
                    }
                });

                wit('шторм', {
                    weather: {
                        "season": "spring",
                        "now": {
                            "condition": "cloudy-thunderstorms-with-rain",
                            "temperature": 10
                        }
                    },
                    skin: {
                        season: 'spring',
                        condition: 'storm',
                        dayTime: 'day'
                    }
                });

                wit('ветер', {
                    weather: {
                        "now": {
                            "condition": "wind",
                            "temperature": 13
                        }
                    },
                    skin: {
                        season: 'spring',
                        condition: 'wind',
                        dayTime: 'day'
                    }
                });

                wit('неизвестный скин', {
                    weather: {
                        "now": {
                            "condition": "partly-cloudy-and-snow",
                            "temperature": 13
                        }
                    },
                    skin: {
                        season: 'spring',
                        condition: 'clear',
                        dayTime: 'day'
                    }
                });

                wit('неизвестный скин ночь', {
                    weather: {
                        "daytime": "night",
                        "now": {
                            "condition": "partly-cloudy-and-snow",
                            "temperature": 13
                        }
                    },
                    skin: {
                        season: 'spring',
                        condition: 'clear',
                        dayTime: 'night'
                    }
                });
            });

            describe('Лето', function() {
                wit('ясно', {
                    weather: {
                        "now": {
                            "condition": "clear",
                            "temperature": 22
                        }
                    },
                    skin: {
                        season: 'summer',
                        condition: 'clear',
                        dayTime: 'day'
                    }
                });

                wit('облачно', {
                    weather: {
                        "now": {
                            "condition": "cloudy",
                            "temperature": 26
                        }
                    },
                    skin: {
                        season: 'summer',
                        condition: 'clouds',
                        dayTime: 'day'
                    }
                });

                wit('туман', {
                    weather: {
                        "now": {
                            "condition": "fog",
                            "temperature": 26
                        }
                    },
                    skin: {
                        season: 'summer',
                        condition: 'fog',
                        dayTime: 'day'
                    }
                });

                wit('град', {
                    weather: {
                        "now": {
                            "condition": "hail",
                            "temperature": 26
                        }
                    },
                    skin: {
                        season: 'summer',
                        condition: 'hail',
                        dayTime: 'day'
                    }
                });

                wit('ожидается облачность', {
                    weather: {
                        "now": {
                            "condition": "partly-cloudy",
                            "temperature": 28
                        }
                    },
                    skin: {
                        season: 'summer',
                        condition: 'partly_cloudy',
                        dayTime: 'day'
                    }
                });

                wit('дождь', {
                    weather: {
                        "season": "autumn",
                        "now": {
                            "condition": "cloudy-and-rain",
                            "temperature": 25
                        }
                    },
                    skin: {
                        season: 'summer',
                        condition: 'rain',
                        dayTime: 'day'
                    }
                });

                wit('дождливо', {
                    weather: {
                        "season": "autumn",
                        "now": {
                            "condition": "partly-cloudy-and-showers",
                            "temperature": 25
                        }
                    },
                    skin: {
                        season: 'summer',
                        condition: 'rainy',
                        dayTime: 'day'
                    }
                });

                wit('шторм', {
                    weather: {
                        "now": {
                            "condition": "cloudy-thunderstorms-with-rain",
                            "temperature": 20
                        }
                    },
                    skin: {
                        season: 'summer',
                        condition: 'storm',
                        dayTime: 'day'
                    }
                });

                wit('ветер', {
                    weather: {
                        "now": {
                            "condition": "wind",
                            "temperature": 20
                        }
                    },
                    skin: {
                        season: 'summer',
                        condition: 'wind',
                        dayTime: 'day'
                    }
                });

                wit('неизвестный скин', {
                    weather: {
                        "now": {
                            "condition": "partly-cloudy-and-snow",
                            "temperature": 20
                        }
                    },
                    skin: {
                        season: 'summer',
                        condition: 'clear',
                        dayTime: 'day'
                    }
                });
            });

            describe('Осень', function() {
                wit('5 < температура < 20, туман', {
                    weather: {
                        "season": "autumn",
                        "now": {
                            "condition": "fog",
                            "temperature": 15
                        }
                    },
                    skin: {
                        season: 'autumn',
                        condition: 'fog',
                        dayTime: 'day'
                    }
                });

                wit('ясно', {
                    weather: {
                        "season": "autumn",
                        "now": {
                            "condition": "clear",
                            "temperature": 15
                        }
                    },
                    skin: {
                        season: 'autumn',
                        condition: 'clear',
                        dayTime: 'day'
                    }
                });

                wit('облачно', {
                    weather: {
                        "season": "autumn",
                        "now": {
                            "condition": "cloudy",
                            "temperature": 15
                        }
                    },
                    skin: {
                        season: 'autumn',
                        condition: 'clouds',
                        dayTime: 'day'
                    }
                });

                wit('ожидается облачность', {
                    weather: {
                        "season": "autumn",
                        "now": {
                            "condition": "partly-cloudy",
                            "temperature": 15
                        }
                    },
                    skin: {
                        season: 'autumn',
                        condition: 'partly_cloudy',
                        dayTime: 'day'
                    }
                });

                wit('дождь', {
                    weather: {
                        "season": "autumn",
                        "now": {
                            "condition": "cloudy-and-rain",
                            "temperature": 15
                        }
                    },
                    skin: {
                        season: 'autumn',
                        condition: 'rain',
                        dayTime: 'day'
                    }
                });

                wit('дождливо', {
                    weather: {
                        "season": "autumn",
                        "now": {
                            "condition": "partly-cloudy-and-light-rain",
                            "temperature": 15
                        }
                    },
                    skin: {
                        season: 'autumn',
                        condition: 'rainy',
                        dayTime: 'day'
                    }
                });

                wit('шторм', {
                    weather: {
                        "season": "autumn",
                        "now": {
                            "condition": "cloudy-thunderstorms-with-rain",
                            "temperature": 15
                        }
                    },
                    skin: {
                        season: 'autumn',
                        condition: 'storm',
                        dayTime: 'day'
                    }
                });

                wit('ветер', {
                    weather: {
                        "season": "autumn",
                        "now": {
                            "condition": "wind",
                            "temperature": 15
                        }
                    },
                    skin: {
                        season: 'autumn',
                        condition: 'wind',
                        dayTime: 'day'
                    }
                });

                wit('неизвестный скин', {
                    weather: {
                        "season": "autumn",
                        "now": {
                            "condition": "mostly-clear-rain",
                            "temperature": 15
                        }
                    },
                    skin: {
                        season: 'autumn',
                        condition: 'clear',
                        dayTime: 'day'
                    }
                });
            });

            describe('Зима', function() {
                wit('при температуре < 0', {
                    weather: {
                        "now": {
                            "condition": "cloudy",
                            "temperature": -1
                        }
                    },
                    skin: {
                        season: 'winter',
                        condition: 'clouds',
                        dayTime: 'day'
                    }
                });

                wit('без метели 1', {
                    weather: {
                        "wind-speed": 6.7,
                        "now": {
                            "condition": "cloudy",
                            "temperature": -1
                        }
                    },
                    skin: {
                        season: 'winter',
                        condition: 'clouds',
                        dayTime: 'day'
                    }
                });

                wit('без метели 2', {
                    weather: {
                        "wind-speed": "5.7",
                        "now": {
                            "condition": "cloudy-and-snow",
                            "temperature": -1
                        }
                    },
                    skin: {
                        season: 'winter',
                        condition: 'heavy_snow',
                        dayTime: 'day'
                    }
                });

                wit('метель 1', {
                    weather: {
                        "wind-speed": "6.7",
                        "now": {
                            "condition": "cloudy-and-snow",
                            "temperature": -1
                        }
                    },
                    skin: {
                        season: 'winter',
                        condition: 'blizzard',
                        dayTime: 'day'
                    }
                });

                wit('метель 2', {
                    weather: {
                        "wind-speed": "6.7",
                        "now": {
                            "condition": "partly-cloudy-and-snow",
                            "temperature": 5
                        }
                    },
                    skin: {
                        season: 'winter',
                        condition: 'blizzard',
                        dayTime: 'day'
                    }
                });

                wit('оттепель', {
                    weather: {
                        "season": "winter",
                        "now": {
                            "condition": "clear",
                            "temperature": 0
                        }
                    },
                    skin: {
                        season: 'winter',
                        condition: 'thaw',
                        dayTime: 'day'
                    }
                });

                wit('ясно', {
                    weather: {
                        "season": "winter",
                        "now": {
                            "condition": "clear",
                            "temperature": -1
                        }
                    },
                    skin: {
                        season: 'winter',
                        condition: 'clear',
                        dayTime: 'day'
                    }
                });

                wit('облака', {
                    weather: {
                        "season": "winter",
                        "now": {
                            "condition": "clouds",
                            "temperature": -1
                        }
                    },
                    skin: {
                        season: 'winter',
                        condition: 'clouds',
                        dayTime: 'day'
                    }
                });

                wit('облачно, дождь', {
                    weather: {
                        "season": "winter",
                        "now": {
                            "condition": "cloudy_rain",
                            "temperature": -1
                        }
                    },
                    skin: {
                        season: 'winter',
                        condition: 'cloudy_rain',
                        dayTime: 'day'
                    }
                });

                wit('туман', {
                    weather: {
                        "season": "winter",
                        "now": {
                            "condition": "fog",
                            "temperature": -1
                        }
                    },
                    skin: {
                        season: 'winter',
                        condition: 'fog',
                        dayTime: 'day'
                    }
                });

                wit('сильный снег', {
                    weather: {
                        "season": "winter",
                        "now": {
                            "condition": "overcast-and-light-snow",
                            "temperature": -1
                        }
                    },
                    skin: {
                        season: 'winter',
                        condition: 'heavy_snow',
                        dayTime: 'day'
                    }
                });

                wit('возможны осадки', {
                    weather: {
                        "season": "winter",
                        "now": {
                            "condition": "mostly-clear-slight-possibility-of-wet-snow",
                            "temperature": -1
                        }
                    },
                    skin: {
                        season: 'winter',
                        condition: 'partly_cloudy_rain',
                        dayTime: 'day'
                    }
                });

                wit('неизвестный скин 1', {
                    weather: {
                        "now": {
                            "condition": "mostly-clear-rain",
                            "temperature": 5
                        }
                    },
                    skin: {
                        season: 'winter',
                        condition: 'clear',
                        dayTime: 'day'
                    }
                });

                wit('неизвестный скин 2', {
                    weather: {
                        "now": {
                            "condition": "cloudy-thunderstorms-with-snow",
                            "temperature": -1
                        }
                    },
                    skin: {
                        season: 'winter',
                        condition: 'clear',
                        dayTime: 'day'
                    }
                });
            });

            describe('Май', function() {
                wit('14 < температуры < 20', {
                    weather: {
                        "season": "summer",
                        "now": {
                            "condition": "wind",
                            "temperature": 15
                        }
                    },
                    skin: {
                        season: 'may',
                        condition: 'wind',
                        dayTime: 'day'
                    }
                });

                wit('19 < температуры < 25, зима', {
                    weather: {
                        "season": "winter",
                        "now": {
                            "condition": "wind",
                            "temperature": 20
                        }
                    },
                    skin: {
                        season: 'may',
                        condition: 'wind',
                        dayTime: 'day'
                    }
                });

                wit('19 < температуры < 25, весна', {
                    weather: {
                        "season": "spring",
                        "now": {
                            "condition": "wind",
                            "temperature": 20
                        }
                    },
                    skin: {
                        season: 'may',
                        condition: 'wind',
                        dayTime: 'day'
                    }
                });

                wit('ясно', {
                    weather: {
                        "season": "spring",
                        "now": {
                            "condition": "clear",
                            "temperature": 20
                        }
                    },
                    skin: {
                        season: 'may',
                        condition: 'clear',
                        dayTime: 'day'
                    }
                });

                wit('облачно', {
                    weather: {
                        "season": "spring",
                        "now": {
                            "condition": "cloudy",
                            "temperature": 20
                        }
                    },
                    skin: {
                        season: 'may',
                        condition: 'clouds',
                        dayTime: 'day'
                    }
                });

                wit('туман', {
                    weather: {
                        "season": "spring",
                        "now": {
                            "condition": "fog",
                            "temperature": 20
                        }
                    },
                    skin: {
                        season: 'may',
                        condition: 'fog',
                        dayTime: 'day'
                    }
                });

                wit('град', {
                    weather: {
                        "season": "spring",
                        "now": {
                            "condition": "hail",
                            "temperature": 20
                        }
                    },
                    skin: {
                        season: 'may',
                        condition: 'hail',
                        dayTime: 'day'
                    }
                });

                wit('ожидается облачность', {
                    weather: {
                        "season": "spring",
                        "now": {
                            "condition": "partly-cloudy",
                            "temperature": 20
                        }
                    },
                    skin: {
                        season: 'may',
                        condition: 'partly_cloudy',
                        dayTime: 'day'
                    }
                });

                wit('дождь', {
                    weather: {
                        "season": "spring",
                        "now": {
                            "condition": "cloudy-and-rain",
                            "temperature": 20
                        }
                    },
                    skin: {
                        season: 'may',
                        condition: 'rain',
                        dayTime: 'day'
                    }
                });

                wit('дождливо', {
                    weather: {
                        "season": "spring",
                        "now": {
                            "condition": "partly-cloudy-and-showers",
                            "temperature": 20
                        }
                    },
                    skin: {
                        season: 'may',
                        condition: 'rainy',
                        dayTime: 'day'
                    }
                });

                wit('шторм', {
                    weather: {
                        "season": "spring",
                        "now": {
                            "condition": "cloudy-thunderstorms-with-rain",
                            "temperature": 20
                        }
                    },
                    skin: {
                        season: 'may',
                        condition: 'storm',
                        dayTime: 'day'
                    }
                });

                wit('ветер', {
                    weather: {
                        "season": "spring",
                        "wind-speed": "4.1",
                        "now": {
                            "condition": "clouds",
                            "temperature": 23
                        }
                    },
                    skin: {
                        season: 'may',
                        condition: 'wind',
                        dayTime: 'day'
                    }
                });

                wit('неизвестный скин', {
                    weather: {
                        "season": "spring",
                        "now": {
                            "condition": "mostly-clear-rain",
                            "temperature": 20
                        }
                    },
                    skin: {
                        season: 'may',
                        condition: 'clear',
                        dayTime: 'day'
                    }
                });
            });

            describe('Специальные', function() {
                wit('Ивана Купала', {
                    before: function() {
                        this._date = new Date(0, 6, 7, 1);
                    },
                    skin: {
                        season: 'special',
                        condition: 'kupala',
                        page_is_dark: true
                    }
                });

                wit('Пустыня', {
                    weather: {
                        "now": {
                            "condition": "cloudy-thunderstorms-with-snow",
                            "temperature": 41
                        }
                    },
                    skin: {
                        season: 'special',
                        condition: 'desert'
                    }
                });

                wit('Ветер', {
                    weather: {
                        "wind-speed": "4.1",
                        "now": {
                            "condition": "cloudy-and-light-rain",
                            "temperature": 30
                        }
                    },
                    skin: {
                        season: 'special',
                        condition: 'wind'
                    }
                });
            });

            describe('Разное', function() {

                it('Если мы сгенерировали несуществующий скин, то логируем это в monitoring.txt', function() {
                    this.mThemeWeather.setData({
                        'weather': {'w': 1}
                    });

                    this.sinon.stub(this.mThemeWeather, '_getSkinParams').returns({
                        'season': 'season',
                        'condition': 'condition',
                        'dayTime': 'day'
                    });
                    this.sinon.stub(Jane.ErrorLog, 'send');
                    this.mThemeWeather.getSkinParams();

                    expect(Jane.ErrorLog.send).to.be.calledWith({
                        'name': 'wrong-weather-theme-skin',
                        'data': JSON.stringify({'w': 1})
                    });
                });

                it('По умолчанию "Лето, ясно, день"', function() {
                    this.mThemeWeather.setData({
                        "weather": {}
                    });

                    expect(this.mThemeWeather.getSkinParams()).to.be.eql({
                        season: 'summer',
                        condition: 'clear',
                        dayTime: 'day'
                    });
                });


                wit('Лето, ночь, "переменная облачность, временами дождь"', {
                    weather: {
                        "daytime": "night",
                        "now": {
                            "condition": "partly-cloudy-and-showers",
                            "temperature": "23"
                        }
                    },
                    skin: {
                        season: 'summer',
                        condition: 'rainy',
                        dayTime: 'night',
                        page_is_dark: true
                    }
                });
            });
        });

        xdescribe('Новая погодная тема', function() {

            describe('Весна', function() {
                wit('конец лета, температура < 6', {
                    before: function() {
                        this._date = new Date(0, 8, 7, 1);
                    },
                    weather: {
                        "season": "summer",
                        "now": {
                            "condition": "clear",
                            "temperature": 5
                        }
                    },
                    skin: {
                        season: 'spring',
                        condition: 'clear',
                        dayTime: 'day'
                    }
                }, true);

                wit('ясно', {
                    weather: {
                        "season": "spring",
                        "now": {
                            "condition": "clear",
                            "temperature": 7
                        }
                    },
                    skin: {
                        season: 'spring',
                        condition: 'clear',
                        dayTime: 'day'
                    }
                }, true);

                wit('облачно', {
                    weather: {
                        "season": "spring",
                        "now": {
                            "condition": "cloudy",
                            "temperature": 12
                        }
                    },
                    skin: {
                        season: 'spring',
                        condition: 'clouds',
                        dayTime: 'day'
                    }
                }, true);

                wit('ожидается облачность', {
                    weather: {
                        "season": "spring",
                        "now": {
                            "condition": "partly-cloudy",
                            "temperature": 13
                        }
                    },
                    skin: {
                        season: 'spring',
                        condition: 'partly_cloudy',
                        dayTime: 'day'
                    }
                }, true);

                wit('дождь', {
                    weather: {
                        "season": "spring",
                        "now": {
                            "condition": "cloudy-and-rain",
                            "temperature": 5
                        }
                    },
                    skin: {
                        season: 'spring',
                        condition: 'rain',
                        dayTime: 'day'
                    }
                }, true);

                wit('шторм', {
                    weather: {
                        "season": "spring",
                        "now": {
                            "condition": "cloudy-thunderstorms-with-rain",
                            "temperature": 10
                        }
                    },
                    skin: {
                        season: 'spring',
                        condition: 'storm',
                        dayTime: 'day'
                    }
                }, true);

                wit('ветер', {
                    weather: {
                        "now": {
                            "condition": "wind",
                            "temperature": 13
                        }
                    },
                    skin: {
                        season: 'spring',
                        condition: 'wind',
                        dayTime: 'day'
                    }
                }, true);

                wit('неизвестный скин', {
                    weather: {
                        "now": {
                            "condition": "partly-cloudy-and-snow",
                            "temperature": 13
                        }
                    },
                    skin: {
                        season: 'spring',
                        condition: 'clear',
                        dayTime: 'day'
                    }
                }, true);

                wit('неизвестный скин ночь', {
                    weather: {
                        "daytime": "night",
                        "now": {
                            "condition": "partly-cloudy-and-snow",
                            "temperature": 13
                        }
                    },
                    skin: {
                        season: 'spring',
                        condition: 'clear',
                        dayTime: 'night'
                    }
                }, true);
            });

            describe('Лето', function() {
                wit('ясно', {
                    weather: {
                        "now": {
                            "condition": "clear",
                            "temperature": 22
                        }
                    },
                    skin: {
                        season: 'summer',
                        condition: 'clear',
                        dayTime: 'day'
                    }
                }, true);

                wit('ясно 2', {
                    weather: {
                        "now": {
                            "condition": "clear",
                            "temperature": 50
                        }
                    },
                    skin: {
                        season: 'summer',
                        condition: 'clear',
                        dayTime: 'day'
                    }
                }, true);

                wit('облачно', {
                    weather: {
                        "now": {
                            "condition": "cloudy",
                            "temperature": 26
                        }
                    },
                    skin: {
                        season: 'summer',
                        condition: 'clouds',
                        dayTime: 'day'
                    }
                }, true);

                wit('туман', {
                    weather: {
                        "now": {
                            "condition": "fog",
                            "temperature": 26
                        }
                    },
                    skin: {
                        season: 'summer',
                        condition: 'fog',
                        dayTime: 'day'
                    }
                }, true);

                wit('град', {
                    weather: {
                        "now": {
                            "condition": "hail",
                            "temperature": 26
                        }
                    },
                    skin: {
                        season: 'summer',
                        condition: 'hail',
                        dayTime: 'day'
                    }
                }, true);

                wit('ожидается облачность', {
                    weather: {
                        "now": {
                            "condition": "partly-cloudy",
                            "temperature": 28
                        }
                    },
                    skin: {
                        season: 'summer',
                        condition: 'partly_cloudy',
                        dayTime: 'day'
                    }
                }, true);

                wit('дождь', {
                    weather: {
                        "season": "autumn",
                        "now": {
                            "condition": "cloudy-and-rain",
                            "temperature": 25
                        }
                    },
                    skin: {
                        season: 'summer',
                        condition: 'rain',
                        dayTime: 'day'
                    }
                }, true);

                wit('дождливо', {
                    weather: {
                        "season": "autumn",
                        "now": {
                            "condition": "partly-cloudy-and-showers",
                            "temperature": 25
                        }
                    },
                    skin: {
                        season: 'summer',
                        condition: 'rainy',
                        dayTime: 'day'
                    }
                }, true);

                wit('шторм', {
                    weather: {
                        "now": {
                            "condition": "cloudy-thunderstorms-with-rain",
                            "temperature": 20
                        }
                    },
                    skin: {
                        season: 'summer',
                        condition: 'storm',
                        dayTime: 'day'
                    }
                }, true);

                wit('ветер', {
                    weather: {
                        "now": {
                            "condition": "wind",
                            "temperature": 20
                        }
                    },
                    skin: {
                        season: 'summer',
                        condition: 'wind',
                        dayTime: 'day'
                    }
                }, true);

                wit('неизвестный скин', {
                    weather: {
                        "now": {
                            "condition": "partly-cloudy-and-snow",
                            "temperature": 20
                        }
                    },
                    skin: {
                        season: 'summer',
                        condition: 'clear',
                        dayTime: 'day'
                    }
                }, true);
            });

            describe('Осень', function() {
                wit('5 < температура < 20, туман', {
                    weather: {
                        "season": "autumn",
                        "now": {
                            "condition": "fog",
                            "temperature": 15
                        }
                    },
                    skin: {
                        season: 'autumn',
                        condition: 'fog',
                        dayTime: 'day'
                    }
                }, true);

                wit('ясно', {
                    weather: {
                        "season": "autumn",
                        "now": {
                            "condition": "clear",
                            "temperature": 15
                        }
                    },
                    skin: {
                        season: 'autumn',
                        condition: 'clear',
                        dayTime: 'day'
                    }
                }, true);

                wit('облачно', {
                    weather: {
                        "season": "autumn",
                        "now": {
                            "condition": "cloudy",
                            "temperature": 15
                        }
                    },
                    skin: {
                        season: 'autumn',
                        condition: 'clouds',
                        dayTime: 'day'
                    }
                }, true);

                wit('ожидается облачность', {
                    weather: {
                        "season": "autumn",
                        "now": {
                            "condition": "partly-cloudy",
                            "temperature": 15
                        }
                    },
                    skin: {
                        season: 'autumn',
                        condition: 'partly_cloudy',
                        dayTime: 'day'
                    }
                }, true);

                wit('дождь', {
                    weather: {
                        "season": "autumn",
                        "now": {
                            "condition": "cloudy-and-rain",
                            "temperature": 15
                        }
                    },
                    skin: {
                        season: 'autumn',
                        condition: 'rain',
                        dayTime: 'day'
                    }
                }, true);

                wit('дождливо', {
                    weather: {
                        "season": "autumn",
                        "now": {
                            "condition": "partly-cloudy-and-light-rain",
                            "temperature": 15
                        }
                    },
                    skin: {
                        season: 'autumn',
                        condition: 'rainy',
                        dayTime: 'day'
                    }
                }, true);

                wit('шторм', {
                    weather: {
                        "season": "autumn",
                        "now": {
                            "condition": "cloudy-thunderstorms-with-rain",
                            "temperature": 15
                        }
                    },
                    skin: {
                        season: 'autumn',
                        condition: 'storm',
                        dayTime: 'day'
                    }
                }, true);

                wit('ветер', {
                    weather: {
                        "season": "autumn",
                        "now": {
                            "condition": "wind",
                            "temperature": 15
                        }
                    },
                    skin: {
                        season: 'autumn',
                        condition: 'wind',
                        dayTime: 'day'
                    }
                }, true);

                wit('неизвестный скин', {
                    weather: {
                        "season": "autumn",
                        "now": {
                            "condition": "mostly-clear-rain",
                            "temperature": 15
                        }
                    },
                    skin: {
                        season: 'autumn',
                        condition: 'clear',
                        dayTime: 'day'
                    }
                }, true);
            });

            describe('Зима', function() {
                wit('при температуре < 0', {
                    weather: {
                        "now": {
                            "condition": "cloudy",
                            "temperature": -1
                        }
                    },
                    skin: {
                        season: 'winter',
                        condition: 'clouds',
                        dayTime: 'day'
                    }
                }, true);

                wit('без метели 1', {
                    weather: {
                        "wind-speed": 6.7,
                        "now": {
                            "condition": "cloudy",
                            "temperature": -1
                        }
                    },
                    skin: {
                        season: 'winter',
                        condition: 'clouds',
                        dayTime: 'day'
                    }
                }, true);

                wit('без метели 2', {
                    weather: {
                        "wind-speed": "5.7",
                        "now": {
                            "condition": "cloudy-and-snow",
                            "temperature": -1
                        }
                    },
                    skin: {
                        season: 'winter',
                        condition: 'heavy_snow',
                        dayTime: 'day'
                    }
                }, true);

                wit('метель 1', {
                    weather: {
                        "wind-speed": "6.7",
                        "now": {
                            "condition": "cloudy-and-snow",
                            "temperature": -1
                        }
                    },
                    skin: {
                        season: 'winter',
                        condition: 'blizzard',
                        dayTime: 'day'
                    }
                }, true);

                wit('метель 2', {
                    weather: {
                        "wind-speed": "6.7",
                        "now": {
                            "condition": "partly-cloudy-and-snow",
                            "temperature": 5
                        }
                    },
                    skin: {
                        season: 'winter',
                        condition: 'blizzard',
                        dayTime: 'day'
                    }
                }, true);

                wit('оттепель', {
                    weather: {
                        "season": "winter",
                        "now": {
                            "condition": "clear",
                            "temperature": 0
                        }
                    },
                    skin: {
                        season: 'winter',
                        condition: 'thaw',
                        dayTime: 'day'
                    }
                }, true);

                wit('ясно', {
                    weather: {
                        "season": "winter",
                        "now": {
                            "condition": "clear",
                            "temperature": -1
                        }
                    },
                    skin: {
                        season: 'winter',
                        condition: 'clear',
                        dayTime: 'day'
                    }
                }, true);

                wit('облака', {
                    weather: {
                        "season": "winter",
                        "now": {
                            "condition": "clouds",
                            "temperature": -1
                        }
                    },
                    skin: {
                        season: 'winter',
                        condition: 'clouds',
                        dayTime: 'day'
                    }
                }, true);

                wit('облачно, дождь', {
                    weather: {
                        "season": "winter",
                        "now": {
                            "condition": "cloudy_rain",
                            "temperature": -1
                        }
                    },
                    skin: {
                        season: 'winter',
                        condition: 'cloudy_rain',
                        dayTime: 'day'
                    }
                }, true);

                wit('туман', {
                    weather: {
                        "season": "winter",
                        "now": {
                            "condition": "fog",
                            "temperature": -1
                        }
                    },
                    skin: {
                        season: 'winter',
                        condition: 'fog',
                        dayTime: 'day'
                    }
                }, true);

                wit('сильный снег', {
                    weather: {
                        "season": "winter",
                        "now": {
                            "condition": "overcast-and-light-snow",
                            "temperature": -1
                        }
                    },
                    skin: {
                        season: 'winter',
                        condition: 'heavy_snow',
                        dayTime: 'day'
                    }
                }, true);

                wit('возможны осадки', {
                    weather: {
                        "season": "winter",
                        "now": {
                            "condition": "mostly-clear-slight-possibility-of-wet-snow",
                            "temperature": -1
                        }
                    },
                    skin: {
                        season: 'winter',
                        condition: 'partly_cloudy_rain',
                        dayTime: 'day'
                    }
                }, true);

                wit('неизвестный скин 1', {
                    weather: {
                        "now": {
                            "condition": "mostly-clear-rain",
                            "temperature": 5
                        }
                    },
                    skin: {
                        season: 'winter',
                        condition: 'clear',
                        dayTime: 'day'
                    }
                }, true);

                wit('неизвестный скин 2', {
                    weather: {
                        "now": {
                            "condition": "cloudy-thunderstorms-with-snow",
                            "temperature": -1
                        }
                    },
                    skin: {
                        season: 'winter',
                        condition: 'clear',
                        dayTime: 'day'
                    }
                }, true);
            });

            describe('Разное', function() {

                it('Если мы сгенерировали несуществующий скин, то логируем это в monitoring.txt', function() {
                    this.mThemeWeather.setData({
                        'weather': {'w': 1}
                    });

                    this.sinon.stub(this.mThemeWeather, '_getSkinParams').returns({
                        'season': 'season',
                        'condition': 'condition',
                        'dayTime': 'day'
                    });
                    this.sinon.stub(Jane.ErrorLog, 'send');
                    this.mThemeWeather.getSkinParams();

                    expect(Jane.ErrorLog.send).to.be.calledWith({
                        'name': 'wrong-weather-theme-skin',
                        'data': JSON.stringify({'w': 1})
                    });
                });

                it('По умолчанию "Лето, ясно, день"', function() {
                    this.mThemeWeather.setData({
                        "weather": {}
                    });

                    expect(this.mThemeWeather.getSkinParams()).to.be.eql({
                        season: 'summer',
                        condition: 'clear',
                        dayTime: 'day'
                    });
                });


                wit('Лето, ночь, "переменная облачность, временами дождь"', {
                    weather: {
                        "daytime": "night",
                        "now": {
                            "condition": "partly-cloudy-and-showers",
                            "temperature": "23"
                        }
                    },
                    skin: {
                        season: 'summer',
                        condition: 'rainy',
                        dayTime: 'night',
                        page_is_dark: true
                    }
                });
            });
        });
    });
});
