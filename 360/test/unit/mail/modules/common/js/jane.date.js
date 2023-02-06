describe('Jane.Date', function() {

    describe('.dateToDay', function() {
        it('должен привести дату к 00 часам указанного в ней дня', function() {
            var date = new Date(2013, 11, 3, 10, 54, 2);
            var dateResult = Jane.Date.dateToDay(date);
            expect(Jane.Date.format('%Date_iso', dateResult.getTime())).to.be.equal('2013-12-03T00:00:00');
        });
    });

    describe('#format', function() {
        it('по умолчанию текущая дата', function() {
            var d = new Date();
            expect(Number(Jane.Date.format('%-d'))).to.be.equal(d.getDate());
        });

        it('дату можно указать в виде объекта Date', function() {
            expect(Jane.Date.format('%d', new Date(2013, 6, 3))).to.be.equal('03');
        });

        it('дату можно указать в виде строки цифр или числа timestamp с миллисек.', function() {
            var d = new Date(2013, 6, 3);
            expect(Jane.Date.format('%d', d.getTime())).to.be.equal('03');
        });

        it('дату можно указать в виде псевдо-объекта из шаблона yate', function() {
            var d = [
                {data: new Date(2013, 6, 3)}
            ];
            expect(Jane.Date.format('%d', jpath(d, '.data')[0])).to.be.equal('03');
        });

        it('если дата указана неправильно, должно вернуть null', function() {
            expect(Jane.Date.format('%d', new Date('hfgjshfghjdg'))).to.be.equal(null);
        });

        it('так как последовательность флагов зависит от локали, нельзя указывать более одного флага в формате, для составных форматов должен быть определен отдельный флаг', function() {
            try {
                expect(Jane.Date.format('%Y-%m-%d', new Date(2013, 6, 3))).to.be.equal('2013-06-03');
            } catch(e) {
                expect(1).to.be.equal(1);
            }
        });


        it('%a - сокращенное название дня недели, в соответствии с настройками локали с заглавной буквы', function() {
            expect(Jane.Date.format('%a', new Date(2013, 6, 3))).to.be.equal('Ср');
        });

        it('%A - полное название дня недели, в соответствии с настройками локали с заглавной буквы', function() {
            expect(Jane.Date.format('%A', new Date(2013, 6, 3))).to.be.equal('Среда');
        });

        it('%b - аббревиатура названия месяца, в соответствии с настройками локали с заглавной буквы', function() {
            expect(Jane.Date.format('%b', new Date(2013, 6, 3))).to.be.equal('Июл');
        });

        it('%h - аббревиатура названия месяца, в соответствии с настройками локали (псевдоним %b)', function() {
            expect(Jane.Date.format('%h', new Date(2013, 6, 3))).to.be.equal('Июл');
        });

        it('%B - полное название месяца, в соответствии с настройками локали с заглавной буквы', function() {
            expect(Jane.Date.format('%B', new Date(2013, 6, 3))).to.be.equal('Июль');
        });

        it('%f - аббревиатура названия месяца с точкой, в соответствии с настройками локали с заглавной буквы', function() {
            expect(Jane.Date.format('%f', new Date(2013, 6, 3))).to.be.equal('Июл.');
        });

        it('%v - [позавтчера|вчера|сегодня|завтра|послезавтра] или число в формате "%d %B", если человеческое определение не подходит', function() {
            expect(Jane.Date.format('%v')).to.be.equal('Сегодня');
        });


        it('%c - предпочитаемое отображение даты и времени, в зависимости от текущей локали (на данный момент формат общий "%Y-%m-%d %H:%M:%S")', function() {
            expect(Jane.Date.format('%c', new Date(2013, 6, 3, 12, 12, 12))).to.be.equal('2013-07-03 12:12:12');
        });

        it('%C - двухзначный порядковый номер столетия с ведущим нулем (год, деленный на 100, усеченный до целого)', function() {
            expect(Jane.Date.format('%C', new Date(2013, 6, 3))).to.be.equal('20');
        });

        it('%d - двухзначное представление дня месяца с ведущим нулем', function() {
            expect(Jane.Date.format('%d', new Date(2013, 6, 3))).to.be.equal('03');
        });

        it('%D - дата в формате MM/DD/YY', function() {
            expect(Jane.Date.format('%D', new Date(2013, 6, 3))).to.be.equal('07/03/13');
        });

        it('%e - день месяца, с ведущим пробелом, если он состоит из одной цифры', function() {
            expect(Jane.Date.format('%e', new Date(2013, 6, 3))).to.be.equal(' 3');
        });

        it('%F - дата в формате YYYY-MM-DD', function() {
            expect(Jane.Date.format('%F', new Date(2013, 6, 3))).to.be.equal('2013-07-03');
        });

        it('%g - двухзначный номер года в соответствии со стандартом ISO-8601:1988', function() {
            expect(Jane.Date.format('%g', new Date(2013, 6, 3))).to.be.equal('13');
        });

        it('%G - полная четырехзначная версия %g', function() {
            expect(Jane.Date.format('%G', new Date(2013, 6, 3))).to.be.equal('2013');
        });

        it('%H - двухзначный номер часа в 24-часовом формате с ведущим нулем', function() {
            expect(Jane.Date.format('%H', new Date(2013, 6, 3, 20))).to.be.equal('20');
        });

        it('%I - двухзначный номер часа в 12-часовом формате с ведущим нулем', function() {
            expect(Jane.Date.format('%I', new Date(2013, 6, 3, 6))).to.be.equal('06');
        });

        it('%j - трехзначный номер дня в году с ведущими нулями', function() {
            expect(Jane.Date.format('%j', new Date(2013, 2, 3))).to.be.equal('062');
        });

        it('%m - двухзначный порядковый номер месяца с ведущим нулем', function() {
            expect(Jane.Date.format('%m', new Date(2013, 6, 3))).to.be.equal('07');
        });

        it('%M - двухзначный номер минуты с ведущим нулем', function() {
            expect(Jane.Date.format('%M', new Date(2013, 6, 3, 12, 6))).to.be.equal('06');
        });

        it('%n - перенос строки', function() {
            expect(Jane.Date.format('%n')).to.be.equal("\n");
        });

        it('%p - "AM" или "PM" в верхнем регистре, в зависимости от указанного времени и локали', function() {
            expect(Jane.Date.format('%p', new Date(2013, 6, 3, 12, 6))).to.be.equal('ПП');
        });

        it('%P - "am" или "pm" в нижнем регистре, в зависимости от указанного времени и локали', function() {
            expect(Jane.Date.format('%P', new Date(2013, 6, 3, 12, 6))).to.be.equal('пп');
        });

        it('%r - время в 12 часовом формате - 02:55:02 PM', function() {
            expect(Jane.Date.format('%r', new Date(2013, 6, 3, 12, 6, 6))).to.be.equal('12:06:06 ПП');
        });

        it('%R - время в 24 часовом формате HH:MM', function() {
            expect(Jane.Date.format('%R', new Date(2013, 6, 3, 20, 6, 6))).to.be.equal('20:06');
        });

        it('%S - двухзначный номер секунды с ведущим нулем', function() {
            expect(Jane.Date.format('%S', new Date(2013, 6, 3, 20, 6, 6))).to.be.equal('06');
        });

        it('%t - табуляция', function() {
            expect(Jane.Date.format('%t')).to.be.equal("\t");
        });

        it('%T - ISO 8601 формат времени HH:MM:SS', function() {
            expect(Jane.Date.format('%T', new Date(2013, 6, 3, 20, 6, 6))).to.be.equal('20:06:06');
        });

        it('%w - день недели, с вс - 0', function() {
            expect(Jane.Date.format('%w', new Date(2013, 6, 3))).to.be.equal('3');
        });

        it('%x - предпочитаемое отображение даты, без времени, в зависимости от локали, на данный момент формат фиксированный "%m/%d/%y"', function() {
            expect(Jane.Date.format('%x', new Date(2013, 6, 3))).to.be.equal('07/03/13');
        });

        it('%X - предпочитаемое отображение времени в зависимости от локали, без даты, на данный момент формат фиксированный "%H:%M:%S"', function() {
            expect(Jane.Date.format('%X', new Date(2013, 6, 3, 12, 12, 12))).to.be.equal('12:12:12');
        });

        it('%y - последние 2 цифры года с ведущим нулем', function() {
            expect(Jane.Date.format('%y', new Date(2009, 6, 3))).to.be.equal('09');
        });

        it('%Y - год', function() {
            expect(Jane.Date.format('%Y', new Date(2013, 6, 3))).to.be.equal('2013');
        });

        it('%u - порядковый номер дня недели согласно стандарту ISO-8601 (с 1 - пн. по 7 - вс.)', function() {
            expect(Jane.Date.format('%u', new Date(2013, 6, 7))).to.be.equal('7');
        });

        it('%l - час в 12-часовом формате, с пробелом перед одиночной цифрой', function() {
            expect(Jane.Date.format('%l', new Date(2013, 6, 7, 6, 6, 6))).to.be.equal(' 6');
        });

        it('%z - смещение временной зоны относительно UTC (пример -0500)', function() {
            // Допустим у меня зона UTC

            // Date всегда локален, поэтому
            // new Date('Wed Jul 03 2013 13:15:43 GMT+0400 (MSK)') -> Wed Jul 03 2013 09:15:43 GMT+0000 (UTC)
            // и смещение зоны, соответственно, всегда тоже локальное

            var dt = new Date();
            var timezoneOffset = dt.getTimezoneOffset();
            var expectResult = 'not-defined';
            switch (timezoneOffset) {
                case 0:
                    expectResult = '+0000';
                    break;

                case -60:
                    expectResult = '+0100';
                    break;

                case -120:
                    expectResult = '+0200';
                    break;

                case -180:
                    expectResult = '+0300';
                    break;

                case -240:
                    expectResult = '+0400';
                    break;
            }
            expect(Jane.Date.format('%z', new Date())).to.be.equal(expectResult);
        });

        it('%s - метка времени Эпохи Unix (аналог getTime() без миллисек.)', function() {
            var d = new Date();
            expect(Number(Jane.Date.format('%s', d))).to.be.equal(d.getTime() / 1000 | 0);
        });


        it('%Date_iso - ISO 8601 формат даты и времени: %Y-%m-%dT%H:%M:%S', function() {
            expect(Jane.Date.format('%Date_iso', new Date(2013, 6, 3, 12, 12, 12))).to.be.equal('2013-07-03T12:12:12');
        });

        it('%Date_dBY_year_in_HM - зависимый отт локали вывод времени в формате: 4 ноября 2013 года в 7:04', function() {
            expect(Jane.Date.format('%Date_dBY_year_in_HM', new Date(2013, 6, 3, 12, 12, 12))).to.be.equal('3 июля 2013 года в 12:12');
        });

        it('%Date_dBY_year - зависимый отт локали вывод времени в формате: 4 ноября 2013 года ', function() {
            expect(Jane.Date.format('%Date_dBY_year', new Date(2013, 6, 3))).to.be.equal('3 июля 2013 года');
        });

        it('%Date_dBY - зависимый отт локали вывод времени в формате: 4 ноября 2013', function() {
            expect(Jane.Date.format('%Date_dBY', new Date(2013, 6, 3))).to.be.equal('3 июля 2013');
        });

        it('%Date_dBA - зависимый отт локали вывод времени в формате: 4 ноября, Среда', function() {
            expect(Jane.Date.format('%Date_dBA', new Date(2013, 6, 3))).to.be.equal('3 июля, Среда');
        });

        it('%Date_AdBY - зависимый отт локали вывод времени в формате: Среда, 4 ноября 2013', function() {
            expect(Jane.Date.format('%Date_AdBY', new Date(2013, 6, 3))).to.be.equal('Среда, 3 июля 2013');
        });

        it('%Date_df_in_HM - зависимый отт локали вывод времени в формате: 4 ноя. в 12:36', function() {
            expect(Jane.Date.format('%Date_df_in_HM', new Date(2013, 6, 3, 12, 12))).to.be.equal('3 июл. в 12:12');
        });

        it('%Date_dfY - зависимый отт локали вывод времени в формате: 4 ноя. 2013', function() {
            expect(Jane.Date.format('%Date_dfY', new Date(2013, 6, 3))).to.be.equal('3 июл. 2013');
        });

        it('%Date_dB_in_HM - зависимый отт локали вывод времени в формате: 11 ноября в 12:36', function() {
            expect(Jane.Date.format('%Date_dB_in_HM', new Date(2013, 6, 3, 12, 12))).to.be.equal('3 июля в 12:12');
        });

        it('%Date_dmY__dot - вывод времени в формате: 04.05.2013', function() {
            expect(Jane.Date.format('%Date_dmY__dot', new Date(2013, 6, 3))).to.be.equal('03.07.2013');
        });

        it('%Date_dB - зависимый отт локали вывод времени в формате: 3 июля', function() {
            expect(Jane.Date.format('%Date_dB', new Date(2013, 6, 3))).to.be.equal('3 июля');
        });

        it('%Date_df - зависимый отт локали вывод времени в формате: 3 июл.', function() {
            expect(Jane.Date.format('%Date_df', new Date(2013, 6, 3))).to.be.equal('3 июл.');
        });

        it('%Date_FT - вывод времени в формате: 2013-07-01 12:43:01', function() {
            expect(Jane.Date.format('%Date_FT', new Date(2013, 6, 3, 12, 12, 12))).to.be.equal('2013-07-03 12:12:12');
        });

        it('%Date_dmY__minus - вывод времени в формате: 01-07-2013', function() {
            expect(Jane.Date.format('%Date_dmY__minus', new Date(2013, 6, 3))).to.be.equal('03-07-2013');
        });

        it('"0" модификатор должен подставлять ведущий ноль для флагов %C, %d, %e, %g, %H, %I, %j, %m, %M, %S, %V, %W, %y, %l', function() {
            var d = new Date(1709, 1, 1, 6, 6, 6);
            var data = [
                Jane.Date.format('%0C', d),
                Jane.Date.format('%0d', d),
                Jane.Date.format('%0e', d),
                Jane.Date.format('%0g', d),
                Jane.Date.format('%0H', d),
                Jane.Date.format('%0I', d),
                Jane.Date.format('%0j', d),
                Jane.Date.format('%0m', d),
                Jane.Date.format('%0M', d),
                Jane.Date.format('%0S', d),
                Jane.Date.format('%0V', d),
                Jane.Date.format('%0W', d),
                Jane.Date.format('%0y', d),
                Jane.Date.format('%0l', d)
            ];

            expect(data).to.be.eql(['17', '01', '01', '09', '06', '06', '032', '02', '06', '06', '05', '04', '09', '06']);
        });

        it('"_" модификатор должен подставлять ведущий пробел для флагов %C, %d, %e, %g, %H, %I, %j, %m, %M, %S, %V, %W, %y, %l', function() {
            var d = new Date(1709, 1, 1, 6, 6, 6);
            var data = [
                Jane.Date.format('%_C', d),
                Jane.Date.format('%_d', d),
                Jane.Date.format('%_e', d),
                Jane.Date.format('%_g', d),
                Jane.Date.format('%_H', d),
                Jane.Date.format('%_I', d),
                Jane.Date.format('%_j', d),
                Jane.Date.format('%_m', d),
                Jane.Date.format('%_M', d),
                Jane.Date.format('%_S', d),
                Jane.Date.format('%_V', d),
                Jane.Date.format('%_W', d),
                Jane.Date.format('%_y', d),
                Jane.Date.format('%_l', d)
            ];

            expect(data).to.be.eql(['17', ' 1', ' 1', ' 9', ' 6', ' 6', ' 32', ' 2', ' 6', ' 6', ' 5', ' 4', ' 9', ' 6']);
        });

        it('"-" модификатор должен удалять ведущий пробел или ноль для флагов %C, %d, %e, %g, %H, %I, %j, %m, %M, %S, %V, %W, %y, %l', function() {
            var d = new Date(1709, 1, 1, 6, 6, 6);
            var data = [
                Jane.Date.format('%-C', d),
                Jane.Date.format('%-d', d),
                Jane.Date.format('%-e', d),
                Jane.Date.format('%-g', d),
                Jane.Date.format('%-H', d),
                Jane.Date.format('%-I', d),
                Jane.Date.format('%-j', d),
                Jane.Date.format('%-m', d),
                Jane.Date.format('%-M', d),
                Jane.Date.format('%-S', d),
                Jane.Date.format('%-V', d),
                Jane.Date.format('%-W', d),
                Jane.Date.format('%-y', d),
                Jane.Date.format('%-l', d)
            ];

            expect(data).to.be.eql(['17', '1', '1', '9', '6', '6', '32', '2', '6', '6', '5', '4', '9', '6']);
        });

        it('"^" модификатор должен переводить значения флагов %a, %A, %b, %h, %B, %f, %v в верхний регистр', function() {
            var d = new Date(2013, 6, 3);
            var data = [
                Jane.Date.format('%^a', d),
                Jane.Date.format('%^A', d),
                Jane.Date.format('%^b', d),
                Jane.Date.format('%^h', d),
                Jane.Date.format('%^B', d),
                Jane.Date.format('%^f', d),
                Jane.Date.format('%^v')
            ];

            expect(data).to.be.eql(['СР', 'СРЕДА', 'ИЮЛ', 'ИЮЛ', 'ИЮЛЬ', 'ИЮЛ.', 'СЕГОДНЯ']);
        });

        it('"#" модификатор должен переводить значения флагов %a, %A, %b, %h, %B, %f, %v в нижний регистр', function() {
            var d = new Date(2013, 6, 3);
            var data = [
                Jane.Date.format('%#a', d),
                Jane.Date.format('%#A', d),
                Jane.Date.format('%#b', d),
                Jane.Date.format('%#h', d),
                Jane.Date.format('%#B', d),
                Jane.Date.format('%#f', d),
                Jane.Date.format('%#v')
            ];

            expect(data).to.be.eql(['ср', 'среда', 'июл', 'июл', 'июль', 'июл.', 'сегодня']);
        });

        it('"~" модификатор должен переводить значения флагов %b, %h, %B, %f в родительный падеж', function() {
            var d = new Date(2013, 4, 8);
            var data = [
                Jane.Date.format('%~b', d),
                Jane.Date.format('%~h', d),
                Jane.Date.format('%~B', d),
                Jane.Date.format('%~f', d)
            ];

            expect(data).to.be.eql(['Мая', 'Мая', 'Мая', 'Мая']);
        });

        it('"!" модификатор должен переводить значения флагов %b, %h, %B, %f в именительный падеж', function() {
            var d = new Date(2013, 4, 8);
            var data = [
                Jane.Date.format('%!b', d),
                Jane.Date.format('%!h', d),
                Jane.Date.format('%!B', d),
                Jane.Date.format('%!f', d)
            ];

            expect(data).to.be.eql(['Май', 'Май', 'Май', 'Май']);
        });

        it('[!,~] и [^,#] модификаторы допустимо использовать одновременно', function() {
            var d = new Date(2013, 4, 8);
            var data = [
                Jane.Date.format('%#~b', d),
                Jane.Date.format('%#~h', d),
                Jane.Date.format('%#~B', d),
                Jane.Date.format('%#~f', d)
            ];

            expect(data).to.be.eql(['мая', 'мая', 'мая', 'мая']);
        });
    });

    describe('.isDateTheDayBeforeNow', function() {
        beforeEach(function() {
            this.currentDate = Date.now();
        });

        it('должен вернуть true, если от проверяемой даты прошло больше дня', function() {
            var testedDate = this.currentDate - 48 * 3600 * 1000; // два дня
            expect(Jane.Date.isDateTheDayBeforeNow(testedDate)).to.be.ok;
        });
        it('должен вернуть true, если от проверяемой даты прошел 1 день', function() {
            var testedDate = this.currentDate - 24 * 3600 * 1000; // один день
            expect(Jane.Date.isDateTheDayBeforeNow(testedDate)).to.be.ok;
        });
        it('должен вернуть false, если проверямая дата в будущем или разница с текущей меньше дня', function() {
            var testedDate = this.currentDate + 24 * 3600 * 1000; // один день
            expect(Jane.Date.isDateTheDayBeforeNow(testedDate)).to.not.be.ok;
        });
    });

    describe('#monthName', function() {
        it('должен вернуть полное название месяца в ИП с заглавной буквы', function() {
            expect(Jane.Date.monthName(0)).to.be.equal('Январь');
        });
    });

    describe('#shortMonthName', function() {
        it('должен вернуть сокращенное название месяца в ИП с заглавной буквы без точки', function() {
            expect(Jane.Date.shortMonthName(0)).to.be.equal('Янв');
        });
    });

    describe('#weekDay', function() {
        it('должен вернуть 2х буквенное название дня недели с заглавной буквы, с учетом того, что отсчет начинается с Вс', function() {
            expect(Jane.Date.weekDay(0)).to.be.equal('Вс');
        });
    });

    describe('#humanDate', function() {
        it('дата в формате "сегодня в 9:23" или "01.07.2013", если не текущее число', function() {
            var d = new Date();
            d.setHours(12);
            d.setMinutes(12);
            expect(Jane.Date.humanDate(d)).to.be.equal('сегодня в 12:12');
            expect(Jane.Date.humanDate(new Date(2013, 6, 2))).to.be.equal('02.07.2013');
        });
    });

    describe('#getDaysInterval', function() {
        beforeEach(function() {
            this.sinon.stub(Jane.Date, 'daysIntervalLocalization');
        });

        it('Должен сформировать объект с днями, часами, минутами по разнице между переданными датами', function() {
            var startDate = new Date(2014, 3, 23, 11, 20, 0);
            var endDate = new Date(2014, 3, 24, 13, 35, 0);

            Jane.Date.getDaysInterval(startDate, endDate);

            expect(Jane.Date.daysIntervalLocalization).to.be.calledWithExactly({
                days: 1,
                hours: 2,
                minutes: 15
            });
        });

        it('Должен возвращать дни равными 0, если их нет в интервале', function() {
            var startDate = new Date(2014, 3, 23, 11, 20, 0);
            var endDate = new Date(2014, 3, 23, 13, 35, 0);

            Jane.Date.getDaysInterval(startDate, endDate);

            expect(Jane.Date.daysIntervalLocalization).to.be.calledWithExactly({
                days: 0,
                hours: 2,
                minutes: 15
            });
        });

        it('Должен возвращать часы равными 0, если их нет в интервале', function() {
            var startDate = new Date(2014, 3, 23, 11, 20, 0);
            var endDate = new Date(2014, 3, 23, 11, 35, 0);

            Jane.Date.getDaysInterval(startDate, endDate);

            expect(Jane.Date.daysIntervalLocalization).to.be.calledWithExactly({
                days: 0,
                hours: 0,
                minutes: 15
            });
        });

        it('Должен возвращать минуты равными 0, если их нет в интервале', function() {
            var startDate = new Date(2014, 3, 23, 11, 20, 0);
            var endDate = new Date(2014, 3, 23, 11, 20, 0);

            Jane.Date.getDaysInterval(startDate, endDate);

            expect(Jane.Date.daysIntervalLocalization).to.be.calledWithExactly({
                days: 0,
                hours: 0,
                minutes: 0
            });
        });

        it('Должен округлять минуты по правилам округления', function() {
            var startDate = new Date(2014, 3, 23, 11, 20, 0);
            var endDate = new Date(2014, 3, 23, 11, 20, 56);

            Jane.Date.getDaysInterval(startDate, endDate);

            expect(Jane.Date.daysIntervalLocalization).to.be.calledWithExactly({
                days: 0,
                hours: 0,
                minutes: 1
            });
        });
    });

    describe('#dateWithCustomTimeZone', function() {
        beforeEach(function() {
            this.date = new Date("Tue Jun 17 2014 10:00:00 GMT+0000");
            this.timestamp = this.date.getTime();
            this.houreOffset = 3;
            this.hour = this.date.getUTCHours();
        });

        it('Должен вернуть экземпляр Date', function() {
            this.customDate = Jane.Date.dateWithCustomTimeZone(this.timestamp, this.houreOffset);

            expect(this.customDate).to.be.instanceof(Date);
        });

        xit('Должен вернуть +3 часа если таймзона +3', function() {
            this.customDate = Jane.Date.dateWithCustomTimeZone(this.timestamp, this.houreOffset);
            var customHour = this.customDate.getHours();

            expect(customHour - this.hour).to.be.equal(this.houreOffset);
        });

        xit('Должен вернуть -5 часа если таймзона -5', function() {
            this.houreOffset = -5;
            this.customDate = Jane.Date.dateWithCustomTimeZone(this.timestamp, this.houreOffset);
            var customHour = this.customDate.getHours();

            expect(customHour - this.hour).to.be.equal(this.houreOffset);
        });

        it('Должен вернуть +1 день если если время перешло за 23 часа', function() {
            this.houreOffset = 15;
            this.customDate = Jane.Date.dateWithCustomTimeZone(this.timestamp, this.houreOffset);
            var dayDate = this.date.getUTCDate();
            var customDayDate = this.customDate.getDate();

            expect(customDayDate - dayDate).to.be.equal(1);
        });

        it('Должен вернуть timestamp для созданной даты при изменении параметров даты', function() {
            this.customDate = Jane.Date.dateWithCustomTimeZone(this.timestamp, this.houreOffset);
            this.customDate.setHours(this.customDate.getHours() + 1);
            var currentTimestamp = this.customDate.getTime();
            expect(currentTimestamp - this.timestamp).to.be.equal(Jane.Date.HOUR);
        });

        it('Должен вернуть таймзону, в которой был создан объект даты', function() {
            this.customDate = Jane.Date.dateWithCustomTimeZone(this.timestamp, this.houreOffset);

            expect(this.customDate.getTimezoneOffset()).to.be.equal(-180);
        });
    });

    describe('#toCalendarISODate', function() {

        beforeEach(function() {
            Daria.tz_offset = 0;
        });

        it('Должен преобразовывать дату в ISO8106 формат', function() {
            expect(Jane.Date.toCalendarISODate(new Date(2014, 0, 1))).to.be.equal('2014-01-01T00:00:00Z');
        });
    });

    describe('#parseCalendarDate', function() {
        beforeEach(function() {
            const date = new Date(2014, 10, 23, 12, 10, 39);
            const dateString = '2014-11-23 12:10:39';

            this.dates = {
                date,
                dateString,
                timestamp: date.getTime(),
                timestampString: date.getTime() + ''
            };
        });

        it('Должен корректно преобразовать число с timestamp-ом к объекту Date', function() {
            expect(Jane.Date.parseCalendarDate(this.dates.timestamp)).to.eql(this.dates.date);
        });

        it('Должен корректно преобразовать строку с timestamp-ом к объекту Date', function() {
            expect(Jane.Date.parseCalendarDate(this.dates.timestampString)).to.eql(this.dates.date);
        });

        it('Должен разобрать дату формата "YYYY-MM-DD hh:mm:ss" в объект даты с указанными в строке значениям', function() {
            expect(Jane.Date.parseCalendarDate(this.dates.dateString)).to.eql(this.dates.date);
        });

        it('Должен вернуть null, если передан неверный формат строки', function() {
            expect(Jane.Date.parseCalendarDate('2014-11-23')).to.be.equal(null);
        });

        it('Должен вернуть null, если ничего не передано', function() {
            expect(Jane.Date.parseCalendarDate()).to.be.equal(null);
        });
    });

    describe('#toHumanFormat', function() {
        beforeEach(function() {
            this.sinon.stub(Daria, 'passportNow').returns((new Date(2018, 6, 13)).getTime());
        });

        it('Должен вернуть "Сегодня"', function() {
            expect(Jane.Date.toHumanFormat(new Date(2018, 6, 13, 12, 00))).to.be.equal('сегодня');
        });

        it('Должен вернуть отформатированную дату', function() {
            expect(Jane.Date.toHumanFormat(new Date(2018, 6, 12, 12, 00))).to.be.equal('12 июля 2018 года');
        });
    });

});
