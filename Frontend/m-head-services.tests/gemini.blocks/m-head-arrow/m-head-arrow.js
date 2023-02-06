BEM.DOM.decl('m-head-arrow', {
    _supportLS: function() {
        return false;
    },

    // Имитируем ответ ручки.
    _getServices: function() {
        var deferred = $.Deferred();

        if(this.findBlockOutside('m-head_error_yes')) {
            setTimeout(function() {
                deferred.reject(null, 'error');
            });
        } else {
            setTimeout(function() {
                /* eslint-disable */
                deferred.resolve({
                    "main": [{
                        "application_id": "wiki",
                        "name": "Вики",
                        "title": "Вики",
                        "url": "https://wiki.yandex-team.ru/",
                        "title_en": "Wiki",
                        "url_en": "",
                        "name_en": "Wiki",
                        "icon": "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4NCjwhLS0gR2VuZXJhdG9yOiBBZG9iZSBJbGx1c3RyYXRvciAxNi4wLjQsIFNWRyBFeHBvcnQgUGx1Zy1JbiAuIFNWRyBWZXJzaW9uOiA2LjAwIEJ1aWxkIDApICAtLT4NCjwhRE9DVFlQRSBzdmcgUFVCTElDICItLy9XM0MvL0RURCBTVkcgMS4xLy9FTiIgImh0dHA6Ly93d3cudzMub3JnL0dyYXBoaWNzL1NWRy8xLjEvRFREL3N2ZzExLmR0ZCI+DQo8c3ZnIHZlcnNpb249IjEuMSIgaWQ9IkxheWVyXzEiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIgeG1sbnM6eGxpbms9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkveGxpbmsiIHg9IjBweCIgeT0iMHB4Ig0KCSB3aWR0aD0iMTZweCIgaGVpZ2h0PSIxNnB4IiB2aWV3Qm94PSIwIDAgMTYgMTYiIGVuYWJsZS1iYWNrZ3JvdW5kPSJuZXcgMCAwIDE2IDE2IiB4bWw6c3BhY2U9InByZXNlcnZlIj4NCjxwYXRoIGQ9Ik01LjQ3NSwxNGwyLjU4LTkuMTYyTDEwLjY4NSwxNGgxLjY0OWwyLjU5OC0xMC44NzRMMTYsMi44NzNWMmgtMy42OTl2MC44NzNsMS4xMiwwLjIxOGwwLjIwNywwLjI0NGwtMS43ODMsNy41MjFMOS4zMjMsMg0KCUg3LjYxNWwtMi40NDYsOC42NTZMMy4yNiwzLjIzMWwwLjE0Mi0wLjI1M2wxLjA1NC0wLjEwNFYySDB2MC44NzNsMS4wNywwLjI1M0wzLjgyNCwxNEg1LjQ3NXoiLz4NCjwvc3ZnPg=="
                    }, {
                        "application_id": "startrek",
                        "name": "Трекер",
                        "title": "Трекер",
                        "url": "https://st.yandex-team.ru/",
                        "title_en": "Tracker",
                        "url_en": "",
                        "name_en": "Tracker",
                        "icon": "PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxNiAxNiIgZW5hYmxlLWJhY2tncm91bmQ9Im5ldyAwIDAgMTYgMTYiPjxwYXRoIGZpbGwtcnVsZT0iZXZlbm9kZCIgZD0ibTguMDk3LjI3NWMzLjQzIDQuNjM4IDQuNzYzIDkuMjMxIDQuMzIxIDE0LjM3OS0uODctMS42ODYtMS41MzktMy4xNjctMi41MzUtNC41OTktLjMyMy0uNDk1LS40ODktLjQyNi0xLjE5LS4wMDYtMi4yOTQgMS41MjItMy42MTIgMy40ODgtNS4xOTMgNS42NzcuMzY4LTUuMzMyIDEuMjIyLTExLjI5OCA0LjU5Ny0xNS40NTFtLS4wMjQgMS41N2wtLjQ4MyA0LjcwOS0xLjUzMy4zOCAxLjI5OC40ODUtLjEyOSAxLjEzMy45NDktMS4wMzIuOTk5IDEuMDIxLS4xNTktMS4xMjEgMS4zMjYtLjQ4Ni0xLjU5Mi0uMzgtLjY3Ni00LjcwOSIvPjwvc3ZnPg=="
                    }, {
                        "application_id": "mail",
                        "name": "Почта",
                        "title": "Почта",
                        "url": "https://mail.yandex-team.ru/",
                        "title_en": "Mail",
                        "url_en": "",
                        "name_en": "Mail",
                        "icon": "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4NCjwhLS0gR2VuZXJhdG9yOiBBZG9iZSBJbGx1c3RyYXRvciAxNi4wLjQsIFNWRyBFeHBvcnQgUGx1Zy1JbiAuIFNWRyBWZXJzaW9uOiA2LjAwIEJ1aWxkIDApICAtLT4NCjwhRE9DVFlQRSBzdmcgUFVCTElDICItLy9XM0MvL0RURCBTVkcgMS4xLy9FTiIgImh0dHA6Ly93d3cudzMub3JnL0dyYXBoaWNzL1NWRy8xLjEvRFREL3N2ZzExLmR0ZCI+DQo8c3ZnIHZlcnNpb249IjEuMSIgaWQ9IkxheWVyXzEiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIgeG1sbnM6eGxpbms9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkveGxpbmsiIHg9IjBweCIgeT0iMHB4Ig0KCSB3aWR0aD0iMTZweCIgaGVpZ2h0PSIxNnB4IiB2aWV3Qm94PSIwIDAgMTYgMTYiIGVuYWJsZS1iYWNrZ3JvdW5kPSJuZXcgMCAwIDE2IDE2IiB4bWw6c3BhY2U9InByZXNlcnZlIj4NCjxwYXRoIGZpbGwtcnVsZT0iZXZlbm9kZCIgY2xpcC1ydWxlPSJldmVub2RkIiBkPSJNMCwxNFYyaDE2djEySDB6IE0xNCwxMlY0LjQ2N0w4LjQsOC4ySDcuNkwyLDQuNDY3VjEySDE0eiIvPg0KPC9zdmc+DQo="
                    }, {
                        "application_id": "staff",
                        "name": "Стафф",
                        "title": "Стафф",
                        "url": "https://staff.yandex-team.ru/",
                        "title_en": "Staff",
                        "url_en": "",
                        "name_en": "Staff",
                        "icon": "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4NCjwhLS0gR2VuZXJhdG9yOiBBZG9iZSBJbGx1c3RyYXRvciAxNi4wLjQsIFNWRyBFeHBvcnQgUGx1Zy1JbiAuIFNWRyBWZXJzaW9uOiA2LjAwIEJ1aWxkIDApICAtLT4NCjwhRE9DVFlQRSBzdmcgUFVCTElDICItLy9XM0MvL0RURCBTVkcgMS4xLy9FTiIgImh0dHA6Ly93d3cudzMub3JnL0dyYXBoaWNzL1NWRy8xLjEvRFREL3N2ZzExLmR0ZCI+DQo8c3ZnIHZlcnNpb249IjEuMSIgaWQ9IkxheWVyXzEiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIgeG1sbnM6eGxpbms9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkveGxpbmsiIHg9IjBweCIgeT0iMHB4Ig0KCSB3aWR0aD0iMTZweCIgaGVpZ2h0PSIxNnB4IiB2aWV3Qm94PSIwIDAgMTYgMTYiIGVuYWJsZS1iYWNrZ3JvdW5kPSJuZXcgMCAwIDE2IDE2IiB4bWw6c3BhY2U9InByZXNlcnZlIj4NCjxwYXRoIGZpbGwtcnVsZT0iZXZlbm9kZCIgY2xpcC1ydWxlPSJldmVub2RkIiBkPSJNMTUsMTQuNDAxQzE1LDE1LjI4MywxMy44NTYsMTYsMTMsMTZIMmMtMC44NiwwLTItMC43MTctMi0xLjU5OUMwLDEyLDUsOSw2LDloMA0KCUM1LDgsNCw2LjM2OCw0LDVWNGMwLTIuMDYzLDEuNDA0LTQsMy40MDktNEM5LjQxMiwwLDExLDEuOTM3LDExLDR2MWMwLDEuNDQ5LTEsMy0yLDRDMTAsOSwxNSwxMiwxNSwxNC40MDF6Ii8+DQo8L3N2Zz4NCg=="
                    }, {
                        "application_id": "at",
                        "name": "Этушка",
                        "title": "Этушка",
                        "url": "https://my.at.yandex-team.ru/",
                        "title_en": "Atushka",
                        "url_en": "",
                        "name_en": "Atushka",
                        "icon": "PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxNiAxNiIgZW5hYmxlLWJhY2tncm91bmQ9Im5ldyAwIDAgMTYgMTYiPjxwYXRoIGQ9Im02IDguODg3aDUuODkzdi4xMTNjMCAuODAxLS4xMDUgMS4xMjQtLjMxOCAxLjg0My0uMjExLjcxOS0uNTMxIDEuMzUyLS45NjUgMS44OThzLS45OC45ODEtMS42NCAxLjMwNC0xLjQzNy40ODMtMi4zMzQuNDgzYy0uNTA2IDAtLjkzNC0uMDQtMS4yODItLjExOC0uMzUtLjA3OS0uNjQyLS4xNjMtLjg4LS4yNTNsLS4zMTEtLjM0OS0uMzc5LTIuNzY0aC0xLjc4NHY0LjE2OWMuNDkuMTcyLjk1My4zMDkgMS4zODYuNDEuNDM0LjEwMS44NDIuMTggMS4yMjUuMjM1LjM4My4wNTcuNzQ0LjA5NCAxLjA4MS4xMTIuMzM4LjAyLjY2LjAzLjk2Ny4wMy41NTkgMCAxLjEyMy0uMDI2IDEuNjktLjA3OS41NjctLjA1MiAxLjEyNy0uMTYgMS42NzgtLjMyNS41NTUtLjE2NSAxLjA5NC0uMzk0IDEuNjIzLS42ODZzMS4wMzUtLjY4MiAxLjUxOC0xLjE2OWMuMTg2LS4xODcuNDA0LS40NDMuNjYyLS43Ny4yNTYtLjMyNS41MDQtLjcyNS43NDItMS4xOTZzLjQzOS0xLjAyMS42MDQtMS42NDZjLjE2NC0uNjI1LjI0OC0xLjMzOS4yNDgtMi4xNCAwLTEuMTY4LS4xODItMi4yNDQtLjU0MS0zLjIyNS0uMzYxLS45ODEtLjkwNi0xLjgyNC0xLjYzOS0yLjUyOHMtMS42NS0xLjI1My0yLjc1LTEuNjQ2LTIuMzg3LS41OS0zLjg1OS0uNTljLS41NDQgMC0xLjA0MS4wMjEtMS40ODkuMDYyLS40NDkuMDQxLS44NTkuMDkzLTEuMjMxLjE1Ny0uMzcyLjA2NC0uNzA0LjEzNS0uOTk1LjIxNHMtLjU1Mi4xNTEtLjc4Mi4yMTl2My43MTloMS43MDJsLjM5MS0yLjI0Ny4zMzQtLjMyNmMuMjk5LS4xMi42MDItLjIwNC45MDktLjI1My4zMDYtLjA0OS42NDgtLjA3MyAxLjAyMy0uMDczLjI5MiAwIC42MDguMDIxLjk0OS4wNjIuMzQxLjA0MS42ODcuMTIgMS4wMzYuMjM2LjM0OS4xMTYuNjk0LjI3NyAxLjAzNS40ODNzLjY2MS40NzguOTYuODE0Yy40MTQuNDcyLjc0IDEuMDY3Ljk3OSAxLjc4Ny4yNDQuNzQyLjM5MSAxLjQ0OS40MzggMi4xMjRoLTUuODk0djEuOTA5Ii8+PC9zdmc+"
                    }, {
                        "application_id": "cal",
                        "name": "Календарь",
                        "title": "Календарь",
                        "url": "https://calendar.yandex-team.ru/",
                        "title_en": "Calendar",
                        "url_en": "https://calendar.yandex-team.ru/",
                        "name_en": "Calendar",
                        "icon": "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4NCjwhLS0gR2VuZXJhdG9yOiBBZG9iZSBJbGx1c3RyYXRvciAxNi4wLjQsIFNWRyBFeHBvcnQgUGx1Zy1JbiAuIFNWRyBWZXJzaW9uOiA2LjAwIEJ1aWxkIDApICAtLT4NCjwhRE9DVFlQRSBzdmcgUFVCTElDICItLy9XM0MvL0RURCBTVkcgMS4xLy9FTiIgImh0dHA6Ly93d3cudzMub3JnL0dyYXBoaWNzL1NWRy8xLjEvRFREL3N2ZzExLmR0ZCI+DQo8c3ZnIHZlcnNpb249IjEuMSIgaWQ9IkxheWVyXzEiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIgeG1sbnM6eGxpbms9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkveGxpbmsiIHg9IjBweCIgeT0iMHB4Ig0KCSB3aWR0aD0iMTZweCIgaGVpZ2h0PSIxNnB4IiB2aWV3Qm94PSIwIDAgMTYgMTYiIGVuYWJsZS1iYWNrZ3JvdW5kPSJuZXcgMCAwIDE2IDE2IiB4bWw6c3BhY2U9InByZXNlcnZlIj4NCjxwYXRoIGZpbGwtcnVsZT0iZXZlbm9kZCIgY2xpcC1ydWxlPSJldmVub2RkIiBkPSJNMCwxNVYzaDJWMWg0djJoNFYxaDR2MmgydjEySDB6IE0xNCwxM1Y2SDJ2N0gxNHogTTUsM0gzdjJoMlYzeiBNMTMsM2gtMnYyaDJWM3oiDQoJLz4NCjwvc3ZnPg0K"
                    }],
                    "secondary": [{
                        "application_id": "planner",
                        "name": "ABC",
                        "title": "ABC",
                        "url": "https://abc.yandex-team.ru/",
                        "title_en": "ABC",
                        "url_en": "",
                        "name_en": "ABC",
                        "icon": ""
                    }, {
                        "application_id": "idm",
                        "name": "IDM",
                        "title": "IDM",
                        "url": "https://idm.yandex-team.ru/",
                        "title_en": "IDM",
                        "url_en": "",
                        "name_en": "IDM",
                        "icon": ""
                    }, {
                        "application_id": "invite",
                        "name": "Переговорки",
                        "title": "Переговорки",
                        "url": "https://calendar.yandex-team.ru/invite/",
                        "title_en": "Invite",
                        "url_en": "",
                        "name_en": "Invite",
                        "icon": ""
                    }, {
                        "application_id": "libra",
                        "name": "Библиотека",
                        "title": "Библиотека",
                        "url": "https://lib.yandex-team.ru/",
                        "title_en": "Library",
                        "url_en": "",
                        "name_en": "Library",
                        "icon": ""
                    }, {
                        "application_id": "doc",
                        "name": "Документация",
                        "title": "Документация",
                        "url": "https://doc.yandex-team.ru/",
                        "title_en": "Documentation",
                        "url_en": "",
                        "name_en": "Documentation",
                        "icon": ""
                    }, {
                        "application_id": "lego",
                        "name": "Лего",
                        "title": "Лего",
                        "url": "https://lego.yandex-team.ru/",
                        "title_en": "Lego",
                        "url_en": "",
                        "name_en": "Lego",
                        "icon": ""
                    }, {
                        "application_id": "mag",
                        "name": "Журнал",
                        "title": "Журнал",
                        "url": "https://clubs.at.yandex-team.ru/mag/?from=header",
                        "title_en": "Magazine",
                        "url_en": "",
                        "name_en": "Magazine",
                        "icon": ""
                    }, {
                        "application_id": "guide",
                        "name": "Гайд",
                        "title": "Гайд",
                        "url": "http://guide.yandex-team.ru/",
                        "title_en": "Guide",
                        "url_en": "",
                        "name_en": "Guide",
                        "icon": ""
                    }, {
                        "application_id": "ml",
                        "name": "Рассылки",
                        "title": "Рассылки",
                        "url": "https://ml.yandex-team.ru/",
                        "title_en": "Maillists",
                        "url_en": "",
                        "name_en": "Maillists",
                        "icon": ""
                    }, {
                        "application_id": "ОТРС",
                        "name": "ОТРС",
                        "title": "ОТРС",
                        "url": "https://otrs.yandex-team.ru/",
                        "title_en": "OTRS",
                        "url_en": "",
                        "name_en": "OTRS",
                        "icon": ""
                    }, {
                        "application_id": "stat",
                        "name": "Статистика",
                        "title": "Статистика",
                        "url": "https://stat.yandex-team.ru/",
                        "title_en": "Statistics",
                        "url_en": "",
                        "name_en": "Statistics",
                        "icon": ""
                    }, {
                        "application_id": "study",
                        "name": "Обучатор",
                        "title": "Обучатор",
                        "url": "https://study.yandex-team.ru/",
                        "title_en": "Study",
                        "url_en": "https://study.yandex-team.ru/",
                        "name_en": "Study",
                        "icon": ""
                    }]
                });
            });
        }
        return deferred.promise();
    }
});
