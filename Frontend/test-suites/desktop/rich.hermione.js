const runOnProject = require('../../commands/util/runOnProject');
const testWithThemes = require('../../commands/util/testWithThemes');

const mockImage = "\"data:image/svg+xml;charset=utf8,%3Csvg xmlns='http://www.w3.org/2000/svg' height='10' " +
    "width='10'%3E%3Cpath fill='%23f6ff01' d='M0 0h10v10H0z'/%3E%3Cpath d='M2 6l2 3 5-7' fill='none' " +
    "stroke='%23ff2fe4'/%3E%3C/svg%3E\"";

function copy(obj) {
    return JSON.parse(JSON.stringify(obj));
}

function mockRich(data) {
    let obj = copy(require('./mocks-rich/base'));

    data.img.url = mockImage;
    obj[1][0][5] = Object.assign(obj[1][0][5], data);

    return obj;
}

runOnProject(['home', 'serp'], 'desktop', function() {
    describe('rich', function() {
        let text = 'игра престолов';

        for (const aspect of ['square', 'portrait', 'landscape']) {
            for (const size of ['xs', 's', 'm', 'l', 'xl']) {
                testWithThemes(`aspect ${aspect} size ${size}`, function() {
                    return this.browser
                        .click('.mini-suggest__input')
                        .yaMockSuggest(text, mockRich({
                            img: {
                                aspect: aspect,
                                badgeColor: '#32ba43',
                                size: size,
                                color: '#101624',
                                badge: '9,0',
                            },
                        }))
                        .keys(text)
                        .waitForVisible('.mini-suggest__popup-content')
                        .assertView('popup', '.mini-suggest__popup-content');
                });
            }
        }

        testWithThemes('bg & badge color', function() {
            return this.browser
                .click('.mini-suggest__input')
                .yaMockSuggest(text, mockRich({
                    img: {
                        aspect: 'portrait',
                        badgeColor: 'deepPink',
                        size: 'm',
                        color: '#4eb0da',
                        badge: '9,0',
                    },
                }))
                .keys(text)
                .waitForVisible('.mini-suggest__popup-content')
                .assertView('popup', '.mini-suggest__popup-content');
        });

        for (const flag of ['shade', 'contain', 'cover', 'round', 'blurred']) {
            testWithThemes(flag, function() {
                return this.browser
                    .click('.mini-suggest__input')
                    .yaMockSuggest(text, mockRich({
                        img: {
                            aspect: 'portrait',
                            badgeColor: '#32ba43',
                            size: 'm',
                            color: '#101624',
                            badge: '9,0',
                            [flag]: true,
                        },
                    }))
                    .keys(text)
                    .waitForVisible('.mini-suggest__popup-content')
                    .assertView('popup', '.mini-suggest__popup-content');
            });
        }

        for (const flag of ['monochrome']) {
            testWithThemes(flag, function() {
                return this.browser
                    .click('.mini-suggest__input')
                    .yaMockSuggest(text, mockRich({
                        img: {
                            aspect: 'portrait',
                            badgeColor: '#32ba43',
                            size: 'm',
                            color: '#101624',
                            badge: '9,0',
                        },
                        [flag]: true,
                    }))
                    .keys(text)
                    .waitForVisible('.mini-suggest__popup-content')
                    .assertView('popup', '.mini-suggest__popup-content');
            });
        }
    });
});
