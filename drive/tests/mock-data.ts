/* eslint-disable */
const mockData = {
    'sessions': [{
        'offer_proto': {
            'OfferId': '73d18d24-c5bdbad8-75bd10a3-5fa74cd5',
            'PaymentDiscretization': 31415,
            'TransferredFrom': '',
            'InstanceType': 'pack_offer_builder',
            'Switchable': false,
            'GroupName': 'Победитель',
            'TargetHolderTag': '',
            'OriginalRidingStart': {'Y': 55.75862885, 'X': 37.54284286, 'XHP': 37.54284286, 'YHP': 55.75862885},
            'Corrector': ['flows_corrector', 'duration_controller_special'],
            'UserId': 'fb958e93-d0da-44b7-82e3-8d171043588e',
            'ConstructorId': 'game_winner_1h',
            'DeviceIdAccept': '3FD40F32-4566-418B-BD78-82038156B7F6',
            'PackOffer': {
                'ShortName': '1ч',
                'IsParkingIncludeInPackFlag': true,
                'RerunPriceKM': 0,
                'OvertimeParkingIsRiding': false,
                'Duration': 3600,
                'RemainingDurationPushThreshold': 2940,
                'ReturningDuration': 0,
                'MileageLimit': 0,
                'PackPrice': 0
            },
            'SelectedCreditCard': '',
            'CashbackPercent': 0,
            'SubName': 'Победитель',
            'AreaInfos': [{'AreaId': 'kazan_Zhilploshadka', 'Fee': 20000}],
            'Deadline': 1594032783,
            'FromScanner': false,
            'Timestamp': 1594032483,
            'PriceConstructorId': 'game_winner_1h',
            'ChargableAccounts': ['card', 'bonus'],
            'Transferable': true,
            'Name': 'Победитель',
            'ObjectId': '78f47f58-09cc-45ac-a1c8-8796d3356887',
            'ObjectModel': '',
            'ShortName': '1ч',
            'SelectedCharge': 'card',
            'OnlyOriginalOfferCashback': true,
            'ParentId': '',
            'NextOfferId': '',
            'StandartOffer': {
                'RidingPriceModeling': false,
                'PriceParking': 300,
                'FullPricesContext': {
                    'Market': {
                        'OverrunKm': 0,
                        'Parking': {'PriceModelName': '', 'Price': 300},
                        'Riding': {'PriceModelName': '', 'Price': 700},
                        'Km': {'PriceModelName': '', 'Price': 0}
                    }, 'Equilibrium': {'Parking': 10000, 'EquilibriumUtilization': 28800, 'Riding': 10000, 'Km': 10000}
                },
                'ParkingPriceModeling': false,
                'UseRounding': false,
                'UseDeposit': true,
                'UseDefaultShortDescriptions': true,
                'PriceRiding': 700,
                'DepositAmount': 27182,
                'InsuranceType': 'standart',
                'KmPriceModeling': false,
                'Agreement': 'act_default',
                'PriceKm': 0,
                'DebtThreshold': 1024,
            },
            'BehaviourConstructorId': 'game_winner_1h',
            'DiscountsInfo': {
                'Discounts': [{
                    'IsPromoCode': false,
                    'Identifier': '',
                    'Visible': false,
                    'Details': [{'AdditionalTime': 30, 'TagName': 'old_state_reservation', 'Discount': 0}],
                    'Discount': 0
                }]
            }
        },
        'geo_tags': ['msc_area'],
        'start_geo_tags': ['msc_area'],
        'car': {
            'number': 'а201аа97',
            'sf': [78],
            'vin': '',
            'model_id': 'porsche_carrera',
            'registration_id': '',
            'view': 0,
            'id': '78f47f58-09cc-45ac-a1c8-8796d3356887',
            'imei': '867962042035496',
            'osago_mds_key': 'OSAGO/XW8ZZZ61ZJG063131/majorAPI/1563557390.pdf',
            'responsible_picker': '',
            'fuel_card_number': '',
            'documents': [{
                'link': 'https://carsharing-car-documents.s3.yandex.net/OSAGO/XW8ZZZ61ZJG063131/majorAPI/1563557390.pdf',
                'title': 'ОСАГО'
            }, {
                'link': 'https://carsharing-car-documents.s3.yandex.net/STS/XW8ZZZ61ZJG063131/1550050343.pdf',
                'title': 'СТС'
            }],
            'registration_mds_key': 'STS/XW8ZZZ61ZJG063131/1550050343.pdf'
        },
        'device_diff': {
            'start': {
                'timestamp': 1594032480,
                'longitude': 37.54284286,
                'latitude': 55.75862885,
                'course': 5,
                'type': 'GPSCurrent',
                'since': 1594023770
            },
            'finish': {
                'timestamp': 1594032560,
                'longitude': 37.54284286,
                'latitude': 55.75862885,
                'course': 5,
                'type': 'GPSCurrent',
                'since': 1594023770
            },
            'mileage': 0,
            'hr_mileage': '0 м'
        },
        'segment': {
            'delegation_type': 'p2p_pass_offer',
            'start': 1594032485,
            'bill': [{'type': 'pack', 'title': 'Тариф «Победитель»', 'cost': 0}, {
                'type': 'total',
                'title': 'Итого',
                'cost': 0
            }],
            'total_price': 0,
            'offer_name': 'Победитель',
            'diff': {
                'start': {
                    'timestamp': 1594032480,
                    'longitude': 37.54284286,
                    'latitude': 55.75862885,
                    'course': 5,
                    'type': 'GPSCurrent',
                    'since': 1594023770
                },
                'finish': {
                    'timestamp': 1594032560,
                    'longitude': 37.54284286,
                    'latitude': 55.75862885,
                    'course': 5,
                    'type': 'GPSCurrent',
                    'since': 1594023770
                },
                'mileage': 0,
                'hr_mileage': '0 м'
            },
            '__type': 'riding_compilation',
            'events': [{
                'timestamp': 1594032485,
                'action': 'set_performer',
                'tag_name': 'old_state_reservation',
                'event_id': 1856574
            }, {
                'timestamp': 1594032494,
                'action': 'evolve',
                'tag_name': 'old_state_acceptance',
                'event_id': 1856577
            }, {
                'timestamp': 1594032496,
                'action': 'evolve',
                'tag_name': 'old_state_riding',
                'event_id': 1856580
            }, {
                'timestamp': 1594032504,
                'action': 'evolve',
                'tag_name': 'old_state_parking',
                'event_id': 1856583
            }, {
                'timestamp': 1594032521,
                'action': 'update_data',
                'tag_name': 'old_state_parking',
                'event_id': 1856584
            }, {
                'timestamp': 1594032521,
                'action': 'evolve',
                'tag_name': 'old_state_parking',
                'event_id': 1856587
            }, {
                'timestamp': 1594032563,
                'action': 'evolve',
                'tag_name': 'old_state_reservation',
                'event_id': 1856593
            }, {
                'timestamp': 1594032563,
                'action': 'drop_performer',
                'tag_name': 'old_state_reservation',
                'event_id': 1856594
            }],
            'session_id': '73d18d24-c5bdbad8-75bd10a3-5fa74cd5',
            'total_duration': 78,
            'offer': {
                'switchable': false,
                'duration': 3600,
                'overtime_price': 700,
                'device_id': '3FD4****B7F6',
                'price_constructor_id': 'game_winner_1h',
                'wallets': [{'selected': true, 'id': 'card'}, {'id': 'bonus'}],
                'type': 'pack_offer_builder',
                'finish_instant': 1595926122,
                'localizations': {'custom_act': 'act_default'},
                'extension': 0,
                'detailed_description': '\n<!doctypehtml>\n<html> \n<head> \n\t<meta content="telephone=no"name=format-detection> \n\t<meta charset=utf-8> \n<meta charset="utf-8" />\n    <meta name="viewport" content="width=device-width, initial-scale=1" />\n\n\t<title>Победитель\n\t</title> \n\t<style>html{font-family:sans-serif;-ms-text-size-adjust:100%;-webkit-text-size-adjust:100%}body{margin:0; padding-bottom: 100px}article,aside,details,figcaption,figure,footer,header,hgroup,main,menu,nav,section,summary{display:block}audio,canvas,progress,video{display:inline-block;vertical-align:baseline}audio:not([controls]){display:none;height:0}[hidden],template{display:none}a{background-color:transparent}a:active,a:hover{outline:0}abbr[title]{border-bottom:1px dotted}b,strong{font-weight:700}dfn{font-style:italic}h1{font-size:2em;margin:.67em 0}mark{background:#ff0;color:#000}small{font-size:80%}sub,sup{font-size:75%;line-height:0;position:relative;vertical-align:baseline}sup{top:-.5em}sub{bottom:-.25em}img{border:0}svg:not(:root){overflow:hidden}figure{margin:1em 40px}hr{box-sizing:content-box;height:0}pre{overflow:auto}code,kbd,pre,samp{font-family:monospace,monospace;font-size:1em}button,input,optgroup,select,textarea{color:inherit;font:inherit;margin:0}button{overflow:visible}button,select{text-transform:none}button,html input[type=button],input[type=reset],input[type=submit]{-webkit-appearance:button;cursor:pointer}button[disabled],html input[disabled]{cursor:default}button::-moz-focus-inner,input::-moz-focus-inner{border:0;padding:0}input{line-height:normal}input[type=checkbox],input[type=radio]{box-sizing:border-box;padding:0}input[type=number]::-webkit-inner-spin-button,input[type=number]::-webkit-outer-spin-button{height:auto}input[type=search]{-webkit-appearance:textfield;box-sizing:content-box}input[type=search]::-webkit-search-cancel-button,input[type=search]::-webkit-search-decoration{-webkit-appearance:none}fieldset{border:1px solid silver;margin:0 2px;padding:.35em .625em .75em}legend{border:0;padding:0}textarea{overflow:auto}optgroup{font-weight:700}table{border-collapse:collapse;border-spacing:0}td,th{padding:0}@font-face{font-family:ysans-bold;src:url(https://carsharing.s3.yandex.net/drive/webassets/ysans-bold.ttf)}@font-face{font-family:ysans-regular;src:url(https://carsharing.s3.yandex.net/drive/webassets/ysans-regular.ttf)}@font-face{font-family:ysans-light;src:url(https://carsharing.s3.yandex.net/drive/webassets/ysans-light.ttf)}#container{padding-top:40px; padding-left:20px; padding-right:20px}.header{margin-bottom:20px; font-size:32px; font-family:ysans-bold,sans-serif; line-height:35px}.par{font-size:16px; line-height:20px; font-family:ysans-regular,sans-serif}table{margin-bottom: 30px;}thead{padding-bottom: 4px;}.margin-m{margin-bottom:20px;}.margin-s{margin-bottom:8px;}.sub-header{font-size:16px; font-family:ysans-bold,sans-serif;}a, a:visited, a:active{color: #0021FF !important; text-decoration: none !important;}.large-value{font-family: ysans-light, sans-serif; font-size: 26px; padding-right: 40px; vertical-align: top ;}tr, td{vertical-align: middle;}td > .par{line-height: 36px; padding-top: 6px}\n</style> \n</head> \n<body> \n\t<div id=container> \n\t\t<div class=header>Тариф «Победитель»\n\t\t</div>\n\t\t<table> \n\t\t\t<thead> \n\t\t\t\t<tr>\n\t\t\t\t\t<td>\n\t\t\t\t\t\t<div class="sub-header">Включает\n\t\t\t\t\t\t</div>\n\t\t\t\t\t</td>\n\t\t\t\t\t<td>\n\t\t\t\t\t\t<div class="sub-header">Перерасход\n\t\t\t\t\t\t</div>\n\t\t\t\t\t</td>\n\t\t\t\t</tr>\n\t\t\t</thead> \n\t\t\t<tbody> \n\t\t\t\t<tr>\n\t\t\t\t\t<td>\n\t\t\t\t\t\t<div class="large-value">60 минут\n\t\t\t\t\t\t</div>\n\t\t\t\t\t</td>\n\t\t\t\t\t<td>\n\t\t\t\t\t\t<div class="par">7₽ за минуту в пути\n\t\t\t\t\t\t</div>\n\t\t\t\t\t\t<pre>\n\t\t\t\t\t\t<div class="par">3₽ за минуту в ожидании\n\t\t\t\t\t\t</div>\n\t\t\t\t\t\t</pre>\n\t\t\t\t\t</td>\n\t\t\t\t</tr>\n\t\t\t</tbody> \n\t\t</table> \n\t\t<div class="sub-header margin-s">Поздравляем!\n\t\t</div>\n\t\t<div class="par margin-m">Удача на вашей стороне! Этот тариф доступен только победителям предновогодней Акции. Вы можете воспользоваться им  лишь один раз, после этого он исчезнет. Вы сможете кататься бесплатно на машине целый час, если вам нужно больше времени на поездку, не переживайте. Как только час пройдет, машина не превратится в тыкву, вы будете оплачивать минуты как обычно. 7₽ за минуту в поездке и 3₽ за минуту в ожидании. И мы не забудем про ваши скидки. Так что берите машину и наслаждайтесь победой!\n\t\t</div>\n\t\t<div class="sub-header margin-s">Как работает перерасход\n\t\t</div>\n\t\t<div class="par margin-m">В тариф включены только минуты, километры вообще не учитываются. Если закончились оплаченные минуты, пока машина ещё в аренде — вы просто платите за перерасход минут.\n\t\t</div>\n\t\t<div class="sub-header margin-s">Будьте внимательны\n\t\t</div>\n\t\t<div class="par margin-m">Час начинает тикать сразу после окончания первых бесплатных минут. А при раннем завершении поездки оставшиеся минуты сгорают.\n\t\t</div>\n\t</div>\n</body>\n</html>',
                'agreement': 'act_default',
                'prices': {
                    'parking_discounted': 300,
                    'parking': 300,
                    'discount': {'discount': 0, 'details': [], 'id': ''},
                    'riding_discounted': 700,
                    'km': 0,
                    'use_deposit': false,
                    'riding': 700,
                    'insurance_type': 'standart',
                    'deposit': 0,
                    'km_discounted': 0
                },
                'visual': {'offer_visual_type': '', 'bottom_color': '#FF69B4', 'top_color': '#FF69B4'},
                'parent_id': '',
                'behaviour_constructor_id': 'game_winner_1h',
                'pack_price_undiscounted': 0,
                'subname': 'Победитель',
                'rerun_price_km': 0,
                'deadline': 1594032783,
                'mileage_limit': 0,
                'constructor_id': 'game_winner_1h',
                'start_instant': 1595922522,
                'offer_id': '73d18d24-c5bdbad8-75bd10a3-5fa74cd5',
                'is_corp_session': false,
                'pack_price': 0,
                'overrun_price': 0,
                'from_scanner': false,
                'short_name': '1ч',
                'short_description': ['0 км', 'Нет бесплатного ожидания'],
                'name': 'Победитель',
                'group_name': 'Победитель',
                'debt': {'threshold': 1024}
            },
            'finish': 1594032563
        },
        'trace_tags': [],
        'user_details': {
            'last_name': 'Семченко',
            'setup': {
                'phone': {'verified': true, 'number': '+375295316552'},
                'email': {'verified': true, 'address': 'a.a.semchenko@yandex.ru'}
            },
            'first_name': 'Александр',
            'id': 'fb958e93-d0da-44b7-82e3-8d171043588e',
            'preliminary_payments': {'enabled': false, 'amount': 0},
            'status': 'active',
            'pn': '(app)',
            'username': 'a.a.semchenko'
        }
    },
        {
            "car": {
                "documents": [
                    {
                        "link": "https://carsharing-car-documents.s3.yandex.net/OSAGO/XW8ZZZ61ZJG063131/majorAPI/1563557390.pdf",
                        "title": "ОСАГО"
                    },
                    {
                        "link": "https://carsharing-car-documents.s3.yandex.net/STS/XW8ZZZ61ZJG063131/1550050343.pdf",
                        "title": "СТС"
                    }
                ],
                "fuel_card_number": "",
                "id": "78f47f58-09cc-45ac-a1c8-8796d3356887",
                "imei": "867962042035496",
                "insurance_agreement_number": null,
                "model_id": "porsche_carrera",
                "number": "а201аа97",
                "osago_mds_key": "OSAGO/XW8ZZZ61ZJG063131/majorAPI/1563557390.pdf",
                "registration_id": "",
                "registration_mds_key": "STS/XW8ZZZ61ZJG063131/1550050343.pdf",
                "responsible_picker": "",
                "sf": [
                    78
                ],
                "view": 0,
                "vin": ""
            },
            "device_diff": {
                "finish": {
                    "course": 5,
                    "latitude": 55.75862885,
                    "longitude": 37.54284286,
                    "since": 1594023770,
                    "timestamp": 1594032560,
                    "type": "GPSCurrent"
                },
                "hr_mileage": "0 м",
                "mileage": 0,
                "start": {
                    "course": 5,
                    "latitude": 55.75862885,
                    "longitude": 37.54284286,
                    "since": 1594023770,
                    "timestamp": 1594032480,
                    "type": "GPSCurrent"
                }
            },
            "geo_tags": [
                "msc_area"
            ],
            "offer_proto": {
                "AreaInfos": [
                    {
                        "AreaId": "kazan_Zhilploshadka",
                        "Fee": 20000
                    }
                ],
                "BehaviourConstructorId": "game_winner_1h",
                "CashbackPercent": 0,
                "ChargableAccounts": [
                    "card",
                    "bonus"
                ],
                "ConstructorId": "game_winner_1h",
                "Corrector": [
                    "flows_corrector",
                    "duration_controller_special"
                ],
                "Deadline": 1594032783,
                "DeviceIdAccept": "3FD40F32-4566-418B-BD78-82038156B7F6",
                "DiscountsInfo": {
                    "Discounts": [
                        {
                            "Details": [
                                {
                                    "AdditionalTime": 30,
                                    "Discount": 0,
                                    "TagName": "old_state_reservation"
                                }
                            ],
                            "Discount": 0,
                            "Identifier": "",
                            "IsPromoCode": false,
                            "Visible": false
                        }
                    ]
                },
                "FromScanner": false,
                "GroupName": "Победитель",
                "InstanceType": "pack_offer_builder",
                "Name": "Победитель",
                "NextOfferId": "",
                "ObjectId": "78f47f58-09cc-45ac-a1c8-8796d3356887",
                "ObjectModel": "",
                "OfferId": "73d18d24-c5bdbad8-75bd10a3-5fa74cd5",
                "OnlyOriginalOfferCashback": true,
                "OriginalRidingStart": {
                    "X": 37.54284286,
                    "XHP": 37.54284286,
                    "Y": 55.75862885,
                    "YHP": 55.75862885
                },
                "PackOffer": {
                    "Duration": 3600,
                    "IsParkingIncludeInPackFlag": true,
                    "MileageLimit": 0,
                    "OvertimeParkingIsRiding": false,
                    "PackPrice": 0,
                    "RemainingDurationPushThreshold": 2940,
                    "RerunPriceKM": 0,
                    "ReturningDuration": 0,
                    "ShortName": "1ч"
                },
                "ParentId": "",
                "PaymentDiscretization": 31415,
                "PriceConstructorId": "game_winner_1h",
                "SelectedCharge": "card",
                "SelectedCreditCard": "",
                "ShortName": "1ч",
                "StandartOffer": {
                    "Agreement": "act_default",
                    "DebtThreshold": 1024,
                    "DepositAmount": 27182,
                    "InsuranceType": "standart",
                    "KmPriceModeling": false,
                    "ParkingPriceModeling": false,
                    "PriceKm": 0,
                    "PriceParking": 300,
                    "PriceRiding": 700,
                    "RidingPriceModeling": false,
                    "UseDefaultShortDescriptions": true,
                    "UseDeposit": true,
                    "UseRounding": false
                },
                "SubName": "Победитель",
                "Switchable": false,
                "TargetHolderTag": "",
                "Timestamp": 1594032483,
                "Transferable": true,
                "TransferredFrom": "",
                "UserId": "fb958e93-d0da-44b7-82e3-8d171043588e"
            },
            "segment": {
                "__type": "riding_compilation",
                "bill": [
                    {
                        "cost": 0,
                        "title": "Тариф «Победитель»",
                        "type": "pack"
                    },
                    {
                        "cost": 0,
                        "title": "Итого",
                        "type": "total"
                    }
                ],
                "delegation_type": "p2p_pass_offer",
                "diff": {
                    "finish": {
                        "course": 5,
                        "latitude": 55.75862885,
                        "longitude": 37.54284286,
                        "since": 1594023770,
                        "timestamp": 1594032560,
                        "type": "GPSCurrent"
                    },
                    "hr_mileage": "0 м",
                    "mileage": 0,
                    "start": {
                        "course": 5,
                        "latitude": 55.75862885,
                        "longitude": 37.54284286,
                        "since": 1594023770,
                        "timestamp": 1594032480,
                        "type": "GPSCurrent"
                    }
                },
                "events": [
                    {
                        "action": "set_performer",
                        "event_id": 1856574,
                        "tag_name": "old_state_reservation",
                        "timestamp": 1594032485
                    },
                    {
                        "action": "evolve",
                        "event_id": 1856577,
                        "tag_name": "old_state_acceptance",
                        "timestamp": 1594032494
                    },
                    {
                        "action": "evolve",
                        "event_id": 1856580,
                        "tag_name": "old_state_riding",
                        "timestamp": 1594032496
                    },
                    {
                        "action": "evolve",
                        "event_id": 1856583,
                        "tag_name": "old_state_parking",
                        "timestamp": 1594032504
                    },
                    {
                        "action": "update_data",
                        "event_id": 1856584,
                        "tag_name": "old_state_parking",
                        "timestamp": 1594032521
                    },
                    {
                        "action": "evolve",
                        "event_id": 1856587,
                        "tag_name": "old_state_parking",
                        "timestamp": 1594032521
                    },
                    {
                        "action": "evolve",
                        "event_id": 1856593,
                        "tag_name": "old_state_reservation",
                        "timestamp": 1594032563
                    },
                    {
                        "action": "drop_performer",
                        "event_id": 1856594,
                        "tag_name": "old_state_reservation",
                        "timestamp": 1594032563
                    }
                ],
                "finish": null,
                "offer": {
                    "agreement": "act_default",
                    "behaviour_constructor_id": "game_winner_1h",
                    "constructor_id": "game_winner_1h",
                    "deadline": 1594032783,
                    "debt": {
                        "threshold": 1024
                    },
                    "device_id": "3FD4****B7F6",
                    "duration": 3600,
                    "extension": 0,
                    "finish_instant": 1596737277,
                    "from_scanner": false,
                    "group_name": "Победитель",
                    "is_corp_session": false,
                    "localizations": {
                        "custom_act": "act_default"
                    },
                    "mileage_limit": 0,
                    "name": "Победитель",
                    "offer_id": "73d18d24-c5bdbad8-75bd10a3-5fa74cd5",
                    "overrun_price": 0,
                    "overtime_price": 700,
                    "pack_price": 0,
                    "pack_price_undiscounted": 0,
                    "parent_id": "",
                    "price_constructor_id": "game_winner_1h",
                    "prices": {
                        "deposit": 0,
                        "discount": {
                            "details": [],
                            "discount": 0,
                            "id": ""
                        },
                        "insurance_type": "standart",
                        "km": 0,
                        "km_discounted": 0,
                        "parking": 300,
                        "parking_discounted": 300,
                        "riding": 700,
                        "riding_discounted": 700,
                        "use_deposit": false
                    },
                    "rerun_price_km": 0,
                    "short_description": [
                        "0 км",
                        "Нет бесплатного ожидания"
                    ],
                    "short_name": "1ч",
                    "start_instant": 1596733677,
                    "subname": "Победитель",
                    "switchable": false,
                    "type": "pack_offer_builder",
                    "visual": {
                        "bottom_color": "#FF69B4",
                        "offer_visual_type": "",
                        "top_color": "#FF69B4"
                    },
                    "wallets": [
                        {
                            "id": "card",
                            "selected": true
                        },
                        {
                            "id": "bonus"
                        }
                    ]
                },
                "offer_name": "Победитель",
                "session_id": "73d18d24-c5bdbad8-75bd10a3-5fa74cd5",
                "start": 1594032485,
                "total_duration": 78,
                "total_price": 0
            },
            "start_geo_tags": [
                "msc_area"
            ],
            "trace_tags": [],
            "user_details": {
                "first_name": "Александр",
                "id": "fb958e93-d0da-44b7-82e3-8d171043588e",
                "last_name": "Семченко",
                "pn": "(app)",
                "preliminary_payments": {
                    "amount": 0,
                    "enabled": false
                },
                "setup": {
                    "email": {
                        "address": "a.a.semchenko@yandex.ru",
                        "verified": true
                    },
                    "phone": {
                        "number": "+375295316552",
                        "verified": true
                    }
                },
                "status": "active",
                "username": "a.a.semchenko"
            }
        },
        {
            "car": {
                "documents": [
                    {
                        "link": "https://carsharing-car-documents.s3.yandex.net/OSAGO/XW8ZZZ61ZJG063131/majorAPI/1563557390.pdf",
                        "title": "ОСАГО"
                    },
                    {
                        "link": "https://carsharing-car-documents.s3.yandex.net/STS/XW8ZZZ61ZJG063131/1550050343.pdf",
                        "title": "СТС"
                    }
                ],
                "fuel_card_number": "",
                "id": "79314805-ca87-436a-b48f-711d5d0fd81d",
                "imei": "7767812",
                "insurance_agreement_number": null,
                "model_id": "vw_polo",
                "number": "б903вг176",
                "osago_mds_key": "OSAGO/XW8ZZZ61ZJG063131/majorAPI/1563557390.pdf",
                "patches": [
                    630
                ],
                "registration_id": "",
                "registration_mds_key": "STS/XW8ZZZ61ZJG063131/1550050343.pdf",
                "responsible_picker": "",
                "sf": [
                    15,
                    78,
                    128,
                    158
                ],
                "view": 1,
                "vin": ""
            },
            "device_diff": {
                "finish": {
                    "course": 5,
                    "latitude": 55.7338562,
                    "longitude": 37.58692169,
                    "since": 1592488761,
                    "timestamp": 1592554867,
                    "type": "GPSCurrent"
                },
                "hr_mileage": "0 м",
                "mileage": 0,
                "start": {
                    "course": 5,
                    "latitude": 55.7338562,
                    "longitude": 37.58692169,
                    "since": 1592488761,
                    "timestamp": 1592554772,
                    "type": "GPSCurrent"
                }
            },
            "geo_tags": [
                "msc_area"
            ],
            "offer_proto": {
                "AreaInfos": [
                    {
                        "AreaId": "kazan_Zhilploshadka",
                        "Fee": 20000
                    }
                ],
                "BehaviourConstructorId": "game_winner_1h",
                "CashbackPercent": 0,
                "ChargableAccounts": [
                    "card",
                    "bonus"
                ],
                "ConstructorId": "game_winner_1h",
                "Corrector": [
                    "flows_corrector",
                    "duration_controller_special"
                ],
                "Deadline": 1592555075,
                "DeviceIdAccept": "3FD40F32-4566-418B-BD78-82038156B7F6",
                "DiscountsInfo": {
                    "Discounts": [
                        {
                            "Details": [
                                {
                                    "AdditionalTime": 60,
                                    "Discount": 0,
                                    "TagName": "old_state_reservation"
                                }
                            ],
                            "Discount": 0,
                            "Identifier": "",
                            "IsPromoCode": false,
                            "Visible": false
                        }
                    ]
                },
                "FromScanner": false,
                "GroupName": "Победитель",
                "InstanceType": "pack_offer_builder",
                "Name": "Победитель",
                "NextOfferId": "",
                "ObjectId": "79314805-ca87-436a-b48f-711d5d0fd81d",
                "ObjectModel": "",
                "OfferId": "37900b02-d5bfffb6-cc2aeea4-52af4f2",
                "OnlyOriginalOfferCashback": true,
                "OriginalRidingStart": {
                    "X": 37.58692169,
                    "XHP": 37.58692169,
                    "Y": 55.7338562,
                    "YHP": 55.7338562
                },
                "PackOffer": {
                    "Duration": 3600,
                    "IsParkingIncludeInPackFlag": true,
                    "MileageLimit": 0,
                    "OvertimeParkingIsRiding": false,
                    "PackPrice": 0,
                    "RemainingDurationPushThreshold": 2940,
                    "RerunPriceKM": 0,
                    "ReturningDuration": 0,
                    "ShortName": "1ч"
                },
                "ParentId": "",
                "PaymentDiscretization": 31415,
                "PriceConstructorId": "game_winner_1h",
                "SelectedCharge": "card",
                "SelectedCreditCard": "",
                "ShortName": "1ч",
                "StandartOffer": {
                    "Agreement": "",
                    "DebtThreshold": 1024,
                    "DepositAmount": 27182,
                    "InsuranceType": "standart",
                    "KmPriceModeling": false,
                    "ParkingPriceModeling": false,
                    "PriceKm": 0,
                    "PriceParking": 300,
                    "PriceRiding": 700,
                    "RidingPriceModeling": false,
                    "UseDefaultShortDescriptions": true,
                    "UseDeposit": true,
                    "UseRounding": false
                },
                "SubName": "Победитель",
                "Switchable": false,
                "TargetHolderTag": "",
                "Timestamp": 1592554775,
                "Transferable": true,
                "TransferredFrom": "",
                "UserId": "fb958e93-d0da-44b7-82e3-8d171043588e"
            },
            "segment": {
                "__type": "riding_compilation",
                "bill": [
                    {
                        "cost": 0,
                        "title": "Тариф «Победитель»",
                        "type": "pack"
                    },
                    {
                        "cost": 0,
                        "title": "Итого",
                        "type": "total"
                    }
                ],
                "diff": {
                    "finish": {
                        "course": 5,
                        "latitude": 55.7338562,
                        "longitude": 37.58692169,
                        "since": 1592488761,
                        "timestamp": 1592554867,
                        "type": "GPSCurrent"
                    },
                    "hr_mileage": "0 м",
                    "mileage": 0,
                    "start": {
                        "course": 5,
                        "latitude": 55.7338562,
                        "longitude": 37.58692169,
                        "since": 1592488761,
                        "timestamp": 1592554772,
                        "type": "GPSCurrent"
                    }
                },
                "events": [
                    {
                        "action": "set_performer",
                        "event_id": 1731140,
                        "tag_name": "old_state_reservation",
                        "timestamp": 1592554777
                    },
                    {
                        "action": "evolve",
                        "event_id": 1731143,
                        "tag_name": "old_state_acceptance",
                        "timestamp": 1592554813
                    },
                    {
                        "action": "evolve",
                        "event_id": 1731146,
                        "tag_name": "old_state_riding",
                        "timestamp": 1592554846
                    },
                    {
                        "action": "evolve",
                        "event_id": 1731149,
                        "tag_name": "old_state_reservation",
                        "timestamp": 1592554873
                    },
                    {
                        "action": "drop_performer",
                        "event_id": 1731150,
                        "tag_name": "old_state_reservation",
                        "timestamp": 1592554873
                    }
                ],
                "finish": 1592654873,
                "offer": {
                    "behaviour_constructor_id": "game_winner_1h",
                    "constructor_id": "game_winner_1h",
                    "deadline": 1592555075,
                    "debt": {
                        "threshold": 1024
                    },
                    "device_id": "3FD4****B7F6",
                    "duration": 3600,
                    "extension": 0,
                    "finish_instant": 1596737277,
                    "from_scanner": false,
                    "group_name": "Победитель",
                    "is_corp_session": false,
                    "localizations": {},
                    "mileage_limit": 0,
                    "name": "Победитель",
                    "offer_id": "37900b02-d5bfffb6-cc2aeea4-52af4f2",
                    "overrun_price": 0,
                    "overtime_price": 700,
                    "pack_price": 0,
                    "pack_price_undiscounted": 0,
                    "parent_id": "",
                    "price_constructor_id": "game_winner_1h",
                    "prices": {
                        "deposit": 0,
                        "discount": {
                            "details": [],
                            "discount": 0,
                            "id": ""
                        },
                        "insurance_type": "standart",
                        "km": 0,
                        "km_discounted": 0,
                        "parking": 300,
                        "parking_discounted": 300,
                        "riding": 700,
                        "riding_discounted": 700,
                        "use_deposit": false
                    },
                    "rerun_price_km": 0,
                    "short_description": [
                        "0 км",
                        "1 мин бесплатного ожидания"
                    ],
                    "short_name": "1ч",
                    "start_instant": 1596733677,
                    "subname": "Победитель",
                    "switchable": false,
                    "type": "pack_offer_builder",
                    "visual": {
                        "bottom_color": "#FF69B4",
                        "offer_visual_type": "",
                        "top_color": "#FF69B4"
                    },
                    "wallets": [
                        {
                            "id": "card",
                            "selected": true
                        },
                        {
                            "id": "bonus"
                        }
                    ]
                },
                "offer_name": "Победитель",
                "session_id": "37900b02-d5bfffb6-cc2aeea4-52af4f2",
                "start": 1592554777,
                "total_duration": 96,
                "total_price": 0
            },
            "start_geo_tags": [
                "msc_area"
            ],
            "trace_tags": [
                "ugc_dirty_interior_garbarage",
                "ugc_dirty_interior"
            ],
            "user_details": {
                "first_name": "Александр",
                "id": "fb958e93-d0da-44b7-82e3-8d171043588e",
                "last_name": "Семченко",
                "pn": "(app)",
                "preliminary_payments": {
                    "amount": 0,
                    "enabled": false
                },
                "setup": {
                    "email": {
                        "address": "a.a.semchenko@yandex.ru",
                        "verified": true
                    },
                    "phone": {
                        "number": "+375295316552",
                        "verified": true
                    }
                },
                "status": "active",
                "username": "a.a.semchenko"
            }
        },
        {
            "car": {
                "fuel_card_number": "",
                "id": "78f47f58-09cc-45ac-a1c8-8796d3356887",
                "imei": "867962042035496",
                "insurance_agreement_number": null,
                "model_id": "porsche_carrera",
                "number": "а201аа97",
                "osago_mds_key": "OSAGO/XW8ZZZ61ZJG063131/majorAPI/1563557390.pdf",
                "registration_id": "",
                "registration_mds_key": "STS/XW8ZZZ61ZJG063131/1550050343.pdf",
                "responsible_picker": "",
                "sf": [
                    78
                ],
                "view": 0,
                "vin": ""
            },
            "device_diff": {
                "finish": {
                    "course": 5,
                    "latitude": 55.75862885,
                    "longitude": 37.54284286,
                    "since": 1594023770,
                    "timestamp": 1594032820,
                    "type": "GPSCurrent"
                },
                "hr_mileage": "0 м",
                "mileage": 0,
                "start": {
                    "course": 5,
                    "latitude": 55.75862885,
                    "longitude": 37.54284286,
                    "since": 1594023770,
                    "timestamp": 1594032560,
                    "type": "GPSCurrent"
                }
            },
            "geo_tags": [
                "msc_area"
            ],
            "offer_proto": {
                "AreaInfos": [
                    {
                        "AreaId": "kazan_Zhilploshadka",
                        "Fee": 20000
                    }
                ],
                "BehaviourConstructorId": "game_winner_1h",
                "CashbackPercent": 0,
                "ChargableAccounts": [
                    "card",
                    "bonus"
                ],
                "ConstructorId": "game_winner_1h",
                "Corrector": [
                    "flows_corrector",
                    "duration_controller_special"
                ],
                "Deadline": 1594032701,
                "DeviceIdAccept": "296AFE2C-2203-490E-868A-3D9068F819F3",
                "DiscountsInfo": {
                    "Discounts": [
                        {
                            "Details": [
                                {
                                    "AdditionalTime": 30,
                                    "Discount": 0,
                                    "TagName": "old_state_reservation"
                                }
                            ],
                            "Discount": 0,
                            "Identifier": "",
                            "IsPromoCode": false,
                            "Visible": false
                        }
                    ]
                },
                "FromScanner": false,
                "GroupName": "Победитель",
                "InstanceType": "pack_offer_builder",
                "Name": "Победитель",
                "NextOfferId": "",
                "ObjectId": "78f47f58-09cc-45ac-a1c8-8796d3356887",
                "ObjectModel": "",
                "OfferId": "edbd0ccc-b6bed6e1-4883294f-71a76020",
                "OnlyOriginalOfferCashback": true,
                "OriginalRidingStart": {
                    "X": 37.54284286,
                    "XHP": 37.54284286,
                    "Y": 55.75862885,
                    "YHP": 55.75862885
                },
                "PackOffer": {
                    "Duration": 3535,
                    "IsParkingIncludeInPackFlag": true,
                    "MileageLimit": 0,
                    "OvertimeParkingIsRiding": false,
                    "PackPrice": 0,
                    "RemainingDurationPushThreshold": 2940,
                    "RerunPriceKM": 0,
                    "ReturningDuration": 0,
                    "ShortName": "1ч"
                },
                "ParentId": "",
                "PaymentDiscretization": 31415,
                "PriceConstructorId": "game_winner_1h",
                "SelectedCharge": "card",
                "SelectedCreditCard": "",
                "ShortName": "1ч",
                "StandartOffer": {
                    "Agreement": "act_default",
                    "DebtThreshold": 1024,
                    "DepositAmount": 27182,
                    "InsuranceType": "standart",
                    "KmPriceModeling": false,
                    "ParkingPriceModeling": false,
                    "PriceKm": 0,
                    "PriceParking": 300,
                    "PriceRiding": 700,
                    "RidingPriceModeling": false,
                    "UseDefaultShortDescriptions": true,
                    "UseDeposit": true,
                    "UseRounding": false
                },
                "SubName": "Победитель",
                "Switchable": false,
                "TargetHolderTag": "",
                "Timestamp": 1594032559,
                "TransferType": 2,
                "Transferable": true,
                "TransferredFrom": "73d18d24-c5bdbad8-75bd10a3-5fa74cd5",
                "UserId": "1087a921-5bbb-49c7-aaae-0e1c6170073d"
            },
            "segment": {
                "__type": "riding_compilation",
                "bill": [
                    {
                        "cost": 0,
                        "title": "Тариф «Победитель»",
                        "type": "pack"
                    },
                    {
                        "cost": 0,
                        "title": "Итого",
                        "type": "total"
                    }
                ],
                "diff": {
                    "finish": {
                        "course": 5,
                        "latitude": 55.75862885,
                        "longitude": 37.54284286,
                        "since": 1594023770,
                        "timestamp": 1594032820,
                        "type": "GPSCurrent"
                    },
                    "hr_mileage": "0 м",
                    "mileage": 0,
                    "start": {
                        "course": 5,
                        "latitude": 55.75862885,
                        "longitude": 37.54284286,
                        "since": 1594023770,
                        "timestamp": 1594032560,
                        "type": "GPSCurrent"
                    }
                },
                "events": [
                    {
                        "action": "set_performer",
                        "event_id": 1856598,
                        "tag_name": "old_state_reservation",
                        "timestamp": 1594032563
                    },
                    {
                        "action": "evolve",
                        "event_id": 1856658,
                        "tag_name": "old_state_acceptance",
                        "timestamp": 1594032823
                    },
                    {
                        "action": "evolve",
                        "event_id": 1856661,
                        "tag_name": "old_state_reservation",
                        "timestamp": 1594032825
                    },
                    {
                        "action": "drop_performer",
                        "event_id": 1856662,
                        "tag_name": "old_state_reservation",
                        "timestamp": 1594032825
                    }
                ],
                "finish": 1594032825,
                "offer": {
                    "agreement": "act_default",
                    "behaviour_constructor_id": "game_winner_1h",
                    "constructor_id": "game_winner_1h",
                    "deadline": 1594032701,
                    "debt": {
                        "threshold": 1024
                    },
                    "duration": 3535,
                    "extension": 0,
                    "finish_instant": 1596737599,
                    "from_scanner": false,
                    "group_name": "Победитель",
                    "is_corp_session": false,
                    "localizations": {
                        "custom_act": "act_default"
                    },
                    "mileage_limit": 0,
                    "name": "Победитель",
                    "offer_id": "edbd0ccc-b6bed6e1-4883294f-71a76020",
                    "overrun_price": 0,
                    "overtime_price": 700,
                    "pack_price": 0,
                    "pack_price_undiscounted": 0,
                    "parent_id": "",
                    "price_constructor_id": "game_winner_1h",
                    "prices": {
                        "deposit": 0,
                        "discount": {
                            "details": [],
                            "discount": 0,
                            "id": ""
                        },
                        "insurance_type": "standart",
                        "km": 0,
                        "km_discounted": 0,
                        "parking": 300,
                        "parking_discounted": 300,
                        "riding": 700,
                        "riding_discounted": 700,
                        "use_deposit": false
                    },
                    "rerun_price_km": 0,
                    "short_description": [
                        "0 км",
                        "Нет бесплатного ожидания"
                    ],
                    "short_name": "1ч",
                    "start_instant": 1596734064,
                    "subname": "Победитель",
                    "switchable": false,
                    "type": "pack_offer_builder",
                    "visual": {
                        "bottom_color": "#FF69B4",
                        "offer_visual_type": "",
                        "top_color": "#FF69B4"
                    },
                    "wallets": [
                        {
                            "id": "card",
                            "selected": true
                        },
                        {
                            "id": "bonus"
                        }
                    ]
                },
                "offer_name": "Победитель",
                "session_id": "edbd0ccc-b6bed6e1-4883294f-71a76020",
                "start": 1594032563,
                "total_duration": 262,
                "total_price": 0
            },
            "start_geo_tags": [
                "msc_area"
            ],
            "trace_tags": [],
            "user_details": {
                "first_name": "Ангелина",
                "id": "1087a921-5bbb-49c7-aaae-0e1c6170073d",
                "last_name": "Ковалёва",
                "pn": "",
                "preliminary_payments": {
                    "amount": 0,
                    "enabled": false
                },
                "setup": {
                    "email": {
                        "address": "anhelinak@yandex.by",
                        "verified": true
                    },
                    "phone": {
                        "number": "+37533684187",
                        "verified": true
                    }
                },
                "status": "active",
                "username": "anhelinak"
            }
        },
        {
            "car": {
                "documents": [
                    {
                        "link": "http://carsharing-car-documents.s3.yandex.net/OSAGO/XW8ZZZ61ZKG065213/majorAPI/1569345107.pdf",
                        "title": "ОСАГО"
                    },
                    {
                        "link": "http://carsharing-car-documents.s3.yandex.net/STS/XW8ZZZ61ZKG065213/1569673687.pdf",
                        "title": "СТС"
                    }
                ],
                "former_numbers": [],
                "fuel_card_number": "",
                "id": "632fce5e-b3a1-b9c0-afcc-3dec31e4c855",
                "imei": "867962042755234",
                "insurance_agreement_number": "001AT-20/0260356",
                "insurance_provider": "renins",
                "model_id": "vw_polo",
                "number": "в389рн799",
                "osago_mds_key": "OSAGO/XW8ZZZ61ZKG065213/majorAPI/1569345107.pdf",
                "registration_id": "9915085511",
                "registration_mds_key": "STS/XW8ZZZ61ZKG065213/1569673687.pdf",
                "responsible_picker": "",
                "sf": [
                    68,
                    83,
                    2544
                ],
                "view": 0,
                "vin": "XW8ZZZ61ZKG065213"
            },
            "device_diff": {
                "finish": {
                    "course": 186,
                    "latitude": 55.25674438,
                    "longitude": 36.66936493,
                    "since": 1594412256,
                    "timestamp": 1594416387,
                    "type": "GPSCurrent"
                },
                "hr_mileage": "0 м",
                "mileage": 0,
                "start": {
                    "course": 186,
                    "latitude": 55.25674438,
                    "longitude": 36.66936493,
                    "since": 1594412256,
                    "timestamp": 1594414477,
                    "type": "GPSCurrent"
                }
            },
            "geo_tags": [
                "msc_area"
            ],
            "offer_proto": {
                "BehaviourConstructorId": "general_minutes_msc",
                "CashbackPercent": 0,
                "ChargableAccounts": [
                    "card",
                    "bonus"
                ],
                "ConstructorId": "general_minutes_msc",
                "Corrector": [
                    "split-2-checker",
                    "plus_discount_5",
                    "free_times_acceptance",
                    "free_time_night",
                    "duration_control_spec",
                    "birthday_discount",
                    "DRIVEANALYTICS-679.destination_predictor",
                    "surges.v2",
                    "destination_surges.v2",
                    "offer_corrector_surges1",
                    "flows_corrector_2"
                ],
                "Deadline": 1594414798,
                "DeviceIdAccept": "6486D1ED-AE72-4E80-AAB0-AC7C2A16360E",
                "DiscountsInfo": {
                    "Discounts": [
                        {
                            "Details": [
                                {
                                    "AdditionalTime": -300,
                                    "Discount": 0,
                                    "TagName": "old_state_reservation"
                                }
                            ],
                            "Discount": 0,
                            "Identifier": "fee",
                            "IsPromoCode": false,
                            "Visible": false
                        },
                        {
                            "Details": [
                                {
                                    "AdditionalTime": -300,
                                    "Discount": 0,
                                    "TagName": "old_state_reservation"
                                }
                            ],
                            "Discount": 0,
                            "Identifier": "fee",
                            "IsPromoCode": false,
                            "Visible": false
                        },
                        {
                            "Details": [
                                {
                                    "AdditionalTime": -300,
                                    "Discount": 0,
                                    "TagName": "old_state_reservation"
                                }
                            ],
                            "Discount": 0,
                            "Identifier": "fee",
                            "IsPromoCode": false,
                            "Visible": false
                        },
                        {
                            "Details": [
                                {
                                    "AdditionalTime": -300,
                                    "Discount": 0,
                                    "TagName": "old_state_reservation"
                                }
                            ],
                            "Discount": 0,
                            "Identifier": "fee",
                            "IsPromoCode": false,
                            "Visible": false
                        },
                        {
                            "Details": [
                                {
                                    "AdditionalTime": -300,
                                    "Discount": 0,
                                    "TagName": "old_state_reservation"
                                }
                            ],
                            "Discount": 0,
                            "Identifier": "fee",
                            "IsPromoCode": false,
                            "Visible": false
                        },
                        {
                            "Details": [
                                {
                                    "AdditionalTime": -300,
                                    "Discount": 0,
                                    "TagName": "old_state_reservation"
                                }
                            ],
                            "Discount": 0,
                            "Identifier": "fee",
                            "IsPromoCode": false,
                            "Visible": false
                        },
                        {
                            "Details": [
                                {
                                    "AdditionalTime": -300,
                                    "Discount": 0,
                                    "TagName": "old_state_reservation"
                                }
                            ],
                            "Discount": 0,
                            "Identifier": "fee",
                            "IsPromoCode": false,
                            "Visible": false
                        },
                        {
                            "Details": [
                                {
                                    "AdditionalTime": -300,
                                    "Discount": 0,
                                    "TagName": "old_state_reservation"
                                }
                            ],
                            "Discount": 0,
                            "Identifier": "fee",
                            "IsPromoCode": false,
                            "Visible": false
                        },
                        {
                            "Details": [
                                {
                                    "AdditionalTime": -300,
                                    "Discount": 0,
                                    "TagName": "old_state_reservation"
                                }
                            ],
                            "Discount": 0,
                            "Identifier": "fee",
                            "IsPromoCode": false,
                            "Visible": false
                        },
                        {
                            "Details": [
                                {
                                    "AdditionalTime": 1200,
                                    "Discount": 0,
                                    "TagName": "old_state_reservation"
                                }
                            ],
                            "Discount": 0,
                            "Identifier": "walking_time",
                            "IsPromoCode": false,
                            "Visible": false
                        },
                        {
                            "Discount": 0.05,
                            "Identifier": "plus_discount_5",
                            "IsPromoCode": false,
                            "Visible": true
                        },
                        {
                            "Details": [
                                {
                                    "AdditionalTime": 300,
                                    "Discount": 0,
                                    "TagName": "old_state_acceptance"
                                }
                            ],
                            "Discount": 0,
                            "Identifier": "free_times_acceptance",
                            "IsPromoCode": false,
                            "Visible": false
                        },
                        {
                            "Details": [
                                {
                                    "AdditionalTime": 0,
                                    "Discount": 0,
                                    "FreeTimetable": {
                                        "Restrictions": [
                                            {
                                                "TimeFrom": 2330,
                                                "TimeTo": 500,
                                                "TimezoneShift": 3
                                            }
                                        ]
                                    },
                                    "TagName": "old_state_parking",
                                    "TagsInPoint": [
                                        "!allow_drop_car"
                                    ]
                                },
                                {
                                    "AdditionalTime": 0,
                                    "Discount": 0,
                                    "FreeTimetable": {
                                        "Restrictions": [
                                            {
                                                "TimeFrom": 2330,
                                                "TimeTo": 500,
                                                "TimezoneShift": 3
                                            }
                                        ]
                                    },
                                    "TagName": "old_state_reservation",
                                    "TagsInPoint": [
                                        "!allow_drop_car"
                                    ]
                                }
                            ],
                            "Discount": 0,
                            "Identifier": "free_time_night",
                            "IsPromoCode": false,
                            "Visible": false
                        },
                        {
                            "Discount": 0.07,
                            "Identifier": "birthday_discount",
                            "IsPromoCode": false,
                            "Visible": true
                        }
                    ]
                },
                "FromScanner": false,
                "GroupName": "Минуты",
                "InstanceType": "standart_offer",
                "Name": "Минуты",
                "NextOfferId": "",
                "ObjectId": "632fce5e-b3a1-b9c0-afcc-3dec31e4c855",
                "ObjectModel": "",
                "OfferId": "f261fc49-c59f2307-33a9bf53-5dcac792",
                "OnlyOriginalOfferCashback": true,
                "OriginalRidingStart": {
                    "X": 36.66936493,
                    "XHP": 36.66936493,
                    "Y": 55.25674438,
                    "YHP": 55.25674438
                },
                "ParentId": "",
                "PaymentDiscretization": 31415,
                "PriceConstructorId": "poc_polo_msc",
                "SelectedCharge": "card",
                "SelectedCreditCard": "card-x9606",
                "ShortName": "Мин",
                "StandartOffer": {
                    "Agreement": "act_default",
                    "DebtThreshold": 102400,
                    "DepositAmount": 0,

                    "FullPricesContext": {
                        "Equilibrium": {
                            "EquilibriumUtilization": 28800,
                            "Km": 10000,
                            "Parking": 10000,
                            "Riding": 10000
                        },
                    },
                    "InsuranceType": "standart",
                    "KmPriceModeling": false,
                    "ParkingPriceModeling": false,
                    "PriceKm": 0,
                    "PriceModel": "correction_offer_corrector_surges1_no_surge",
                    "PriceModelInfo": [
                        {
                            "After": 848,
                            "Before": 848,
                            "Name": "correction_checker_split-2-checker_Other"
                        },
                        {
                            "After": 948,
                            "Before": 848,
                            "Name": "correction_surges.v2_low_surge"
                        },
                        {
                            "After": 948,
                            "Before": 948,
                            "Name": "correction_destination_surges.v2_no_surge"
                        },
                        {
                            "After": 948,
                            "Before": 948,
                            "Name": "correction_offer_corrector_surges1_no_surge"
                        }
                    ],
                    "PriceParking": 368,
                    "PriceRiding": 948,
                    "RidingPriceModeling": false,
                    "UseDefaultShortDescriptions": true,
                    "UseDeposit": false,
                    "UseRounding": false
                },
                "SubName": "(resource:minutes_subname)",
                "Switchable": true,
                "TargetHolderTag": "",
                "Timestamp": 1594414498,
                "TransferType": 0,
                "Transferable": true,
                "TransferredFrom": "8df87368-7e43f644-6ebad80d-a6898684",
                "UserId": "0007e410-c857-4835-9a73-1eee79d55208"
            },
            "segment": {
                "__type": "riding_compilation",
                "bill": [
                    {
                        "cost": 7141,
                        "details": "7 мин 32 с",
                        "duration": 452,
                        "title": "В пути",
                        "type": "old_state_riding"
                    },
                    {
                        "cost": 0,
                        "title": "Скидки",
                        "type": "section"
                    },
                    {
                        "cost": -357,
                        "details": "5%",
                        "icon": "https://carsharing.s3.yandex.net/drive/discounts/icons/discount-plus-new.png",
                        "id": "plus_discount_5",
                        "title": "Скидка Яндекс.Плюс",
                        "type": "discount"
                    },
                    {
                        "cost": -499,
                        "details": "7%",
                        "icon": "https://carsharing.s3.yandex.net/drive/discounts/icons/birthday-discount.png",
                        "id": "birthday_discount",
                        "title": "На День Рождения",
                        "type": "discount"
                    },
                    {
                        "cost": 0,
                        "title": "Способ оплаты",
                        "type": "section"
                    },
                    {
                        "cost": 6284,
                        "details": "Сняты бонусы в счет оплаты полученных услуг",
                        "title": "Сняты бонусы",
                        "type": "billing_bonus"
                    },
                    {
                        "cost": 0,
                        "title": "Итого",
                        "type": "total"
                    }
                ],
                "diff": {
                    "finish": {
                        "course": 186,
                        "latitude": 55.25674438,
                        "longitude": 36.66936493,
                        "since": 1594412256,
                        "timestamp": 1594416387,
                        "type": "GPSCurrent"
                    },
                    "hr_mileage": "0 м",
                    "mileage": 0,
                    "start": {
                        "course": 186,
                        "latitude": 55.25674438,
                        "longitude": 36.66936493,
                        "since": 1594412256,
                        "timestamp": 1594414477,
                        "type": "GPSCurrent"
                    }
                },
                "events": [
                    {
                        "action": "set_performer",
                        "event_id": 911344644,
                        "tag_name": "old_state_reservation",
                        "timestamp": 1594414506
                    },
                    {
                        "action": "evolve",
                        "event_id": 911344919,
                        "tag_name": "old_state_acceptance",
                        "timestamp": 1594414515
                    },
                    {
                        "action": "evolve",
                        "event_id": 911345055,
                        "tag_name": "old_state_riding",
                        "timestamp": 1594414520
                    },
                    {
                        "action": "evolve",
                        "event_id": 911345225,
                        "tag_name": "old_state_parking",
                        "timestamp": 1594414530
                    },
                    {
                        "action": "evolve",
                        "event_id": 911378406,
                        "tag_name": "old_state_riding",
                        "timestamp": 1594415928
                    },
                    {
                        "action": "evolve",
                        "event_id": 911388167,
                        "tag_name": "old_state_parking",
                        "timestamp": 1594416370
                    },
                    {
                        "action": "evolve",
                        "event_id": 911388698,
                        "tag_name": "old_state_reservation",
                        "timestamp": 1594416394
                    },
                    {
                        "action": "drop_performer",
                        "event_id": 911388704,
                        "tag_name": "old_state_reservation",
                        "timestamp": 1594416394
                    }
                ],
                "finish": 1594416394,
                "offer": {
                    "agreement": "act_default",
                    "behaviour_constructor_id": "general_minutes_msc",
                    "constructor_id": "general_minutes_msc",
                    "deadline": 1594414798,
                    "debt": {
                        "threshold": 102400
                    },
                    "device_id": "6486****360E",
                    "discounts": [
                        {
                            "description": "Всем подключившим предоставляется 5% скидка на машины в фильтрах «На каждый день», «На каждый день+», «Огонь» и 12% на «Праздник».",
                            "details": [],
                            "discount": 0.05,
                            "icon": "https://carsharing.s3.yandex.net/drive/discounts/icons/discount-plus__04-12__2.png",
                            "id": "plus_discount_5",
                            "small_icon": "https://carsharing.s3.yandex.net/drive/discounts/icons/discount-plus-new.png",
                            "title": "Скидка Яндекс.Плюс",
                            "visible": true
                        },
                        {
                            "description": "С днем рождения! Три скидки по 7% на любые три поездки в течение 2 недель.",
                            "details": [],
                            "discount": 0.07,
                            "icon": "https://carsharing.s3.yandex.net/drive/discounts/icons/birthday-discount.png",
                            "id": "birthday_discount",
                            "small_icon": "https://carsharing.s3.yandex.net/drive/discounts/icons/birthday-discount.png",
                            "title": "На День Рождения",
                            "visible": true
                        }
                    ],
                    "from_scanner": false,
                    "group_name": "Минуты",
                    "is_corp_session": false,
                    "name": "Минуты",
                    "offer_id": "f261fc49-c59f2307-33a9bf53-5dcac792",
                    "parent_id": "",
                    "price_constructor_id": "poc_polo_msc",
                    "prices": {
                        "deposit": 0,
                        "discount": {
                            "description": "Всем подключившим предоставляется 5% скидка на машины в фильтрах «На каждый день», «На каждый день+», «Огонь» и 12% на «Праздник».",
                            "details": [],
                            "discount": 0.12,
                            "icon": "https://carsharing.s3.yandex.net/drive/discounts/icons/discount-plus__04-12__2.png",
                            "id": "plus_discount_5",
                            "small_icon": "https://carsharing.s3.yandex.net/drive/discounts/icons/discount-plus-new.png",
                            "title": "Скидка Яндекс.Плюс",
                            "visible": true
                        },
                        "insurance_type": "standart",
                        "km": 0,
                        "km_discounted": 0,
                        "parking": 368,
                        "parking_discounted": 324,
                        "riding": 948,
                        "riding_discounted": 835,
                        "use_deposit": false
                    },
                    "short_description": [
                        "Ожидание — 3,24 ₽/мин",
                        "Нет бесплатного ожидания"
                    ],
                    "short_name": "Мин",
                    "subname": "Классика",
                    "summary_discount": {
                        "description": "Всем подключившим предоставляется 5% скидка на машины в фильтрах «На каждый день», «На каждый день+», «Огонь» и 12% на «Праздник».",
                        "details": [],
                        "discount": 0.12,
                        "icon": "https://carsharing.s3.yandex.net/drive/discounts/icons/discount-plus__04-12__2.png",
                        "id": "plus_discount_5",
                        "small_icon": "https://carsharing.s3.yandex.net/drive/discounts/icons/discount-plus-new.png",
                        "title": "Скидка Яндекс.Плюс",
                        "visible": true
                    },
                    "switchable": true,
                    "type": "standart_offer",
                    "visual": {
                        "bottom_color": "#0053F4",
                        "offer_visual_type": "",
                        "top_color": "#0053F4"
                    },
                    "wallets": [
                        {
                            "id": "card",
                            "selected": true
                        },
                        {
                            "id": "bonus"
                        }
                    ]
                },
                "offer_name": "Минуты",
                "session_id": "f261fc49-c59f2307-33a9bf53-5dcac792",
                "start": 1594414506,
                "total_duration": 1888,
                "total_price": 6284
            },
            "start_geo_tags": [
                "msc_area"
            ],
            "trace_tags": [],
            "user_details": {
                "first_name": "",
                "id": "0007e410-c857-4835-9a73-1eee79d55208",
                "last_name": "",
                "pn": "",
                "preliminary_payments": {
                    "amount": 0,
                    "enabled": false
                },
                "setup": {
                    "email": {
                        "address": "keksite@yandex.ru",
                        "verified": true
                    },
                    "phone": {
                        "number": "+79250478631",
                        "verified": true
                    }
                },
                "status": "active",
                "username": "keksite"
            }
        },
        {
            "car": {
                "documents": [
                    {
                        "link": "http://carsharing-car-documents.s3.yandex.net/OSAGO/Z8NFBAJ11ES080443/majorAPI/1590826814.pdf",
                        "title": "ОСАГО"
                    },
                    {
                        "link": "http://carsharing-car-documents.s3.yandex.net/STS/Z8NFBAJ11ES080443/1560333213.pdf",
                        "title": "СТС"
                    }
                ],
                "former_numbers": [],
                "fuel_card_number": "",
                "id": "54ed5a87-0349-4397-862f-3c5ead4bcbe1",
                "imei": "867962041538367",
                "insurance_agreement_number": "AI131408577-86",
                "insurance_provider": "ingos",
                "model_id": "nissan_qashqai",
                "number": "в787ок799",
                "osago_mds_key": "OSAGO/Z8NFBAJ11ES080443/majorAPI/1590826814.pdf",
                "registration_id": "9912446741",
                "registration_mds_key": "STS/Z8NFBAJ11ES080443/1560333213.pdf",
                "responsible_picker": "",
                "sf": [
                    68,
                    83,
                    2544
                ],
                "view": 1,
                "vin": "Z8NFBAJ11ES080443"
            },
            "device_diff": {
                "finish": {
                    "course": 266,
                    "latitude": 55.75402451,
                    "longitude": 37.60312653,
                    "since": 1596626777,
                    "timestamp": 1596632925,
                    "type": "GPSCurrent"
                },
                "hr_mileage": "0 м",
                "mileage": 0,
                "start": {
                    "course": 266,
                    "latitude": 55.75394058,
                    "longitude": 37.60321045,
                    "since": 1596615403,
                    "timestamp": 1596615616,
                    "type": "GPSCurrent"
                }
            },
            "geo_tags": [
                "msc_area"
            ],
            "offer_proto": {
                "BehaviourConstructorId": "general_minutes_msc",
                "CashbackPercent": 5,
                "ChargableAccounts": [
                    "card",
                    "bonus_kazan_no-ride",
                    "bonus",
                    "yandex_account"
                ],
                "ConstructorId": "general_minutes_msc",
                "Corrector": [
                    "disable_corrector_to_hide_plus_discount",
                    "split-2-checker",
                    "free_times_acceptance",
                    "free_time_night",
                    "DRIVEANALYTICS-679.destination_predictor",
                    "surges.msc.v2",
                    "destination_surges.msc.v2",
                    "flows_corrector_2"
                ],
                "Deadline": 1596615922,
                "DeviceIdAccept": "c79fcf5e6a123f59796f395362ad6bc4",
                "DisabledCorrector": [
                    "plus_discount_12",
                    "plus_discount_5"
                ],
                "FromScanner": false,
                "GroupName": "Минуты",
                "InstanceType": "standart_offer",
                "Name": "Минуты",
                "NextOfferId": "",
                "ObjectId": "54ed5a87-0349-4397-862f-3c5ead4bcbe1",
                "ObjectModel": "",
                "OfferId": "d72cfb24-4f8af397-e7eb9478-417df29a",
                "OnlyOriginalOfferCashback": true,
                "ParentId": "",
                "PaymentDiscretization": 31415,
                "PriceConstructorId": "poc_qashqai_msc",
                "SelectedCharge": "yandex_account",
                "SelectedCreditCard": "card-x9606",
                "ShortName": "Мин",
                "StandartOffer": {
                    "Agreement": "act_default",
                    "DebtThreshold": 102400,
                    "DepositAmount": 0,
                    "InsuranceType": "standart",
                    "KmPriceModeling": false,
                    "ParkingPriceModeling": false,
                    "PriceKm": 0,
                    "PriceModel": "correction_destination_surges.msc.v2_high_surge",
                    "PriceModelInfo": [
                        {
                            "After": 1098,
                            "Before": 1098,
                            "Name": "correction_checker_split-2-checker_DRIVEANALYTICS-330-GARDEN_RING"
                        },
                        {
                            "After": 1098,
                            "Before": 1098,
                            "Name": "correction_surges.msc.v2_no_surge"
                        },
                        {
                            "After": 1098,
                            "Before": 1098,
                            "Name": "correction_destination_surges.msc.v2_high_surge"
                        }
                    ],
                    "PriceParking": 518,
                    "PriceRiding": 1098,
                    "RidingPriceModeling": false,
                    "UseDefaultShortDescriptions": true,
                    "UseDeposit": false,
                    "UseRounding": false
                },
                "SubName": "(resource:minutes_subname)",
                "Switchable": true,
                "TargetHolderTag": "",
                "Timestamp": 1596615622,
                "Transferable": true,
                "TransferredFrom": "",
                "UserId": "0007e410-c857-4835-9a73-1eee79d55208"
            },
            "segment": {
                "__type": "riding_compilation",
                "bill": [
                    {
                        "cost": 139022,
                        "details": "4 ч 48 мин 23 с",
                        "duration": 17303,
                        "title": "Бронь",
                        "type": "old_state_reservation"
                    },
                    {
                        "cost": 0,
                        "title": "Способ оплаты",
                        "type": "section"
                    },
                    {
                        "cost": 100000,
                        "details": "Сняты бонусы в счет оплаты полученных услуг",
                        "title": "Сняты бонусы",
                        "type": "billing_bonus"
                    },
                    {
                        "cost": 39022,
                        "title": "Итого",
                        "type": "total"
                    }
                ],
                "diff": {
                    "finish": {
                        "course": 266,
                        "latitude": 55.75402451,
                        "longitude": 37.60312653,
                        "since": 1596626777,
                        "timestamp": 1596632925,
                        "type": "GPSCurrent"
                    },
                    "hr_mileage": "0 м",
                    "mileage": 0,
                    "start": {
                        "course": 266,
                        "latitude": 55.75394058,
                        "longitude": 37.60321045,
                        "since": 1596615403,
                        "timestamp": 1596615616,
                        "type": "GPSCurrent"
                    }
                },
                "events": [
                    {
                        "action": "set_performer",
                        "event_id": 956545914,
                        "tag_name": "old_state_reservation",
                        "timestamp": 1596615632
                    },
                    {
                        "action": "evolve",
                        "event_id": 956921725,
                        "tag_name": "old_state_reservation",
                        "timestamp": 1596632935
                    },
                    {
                        "action": "drop_performer",
                        "event_id": 956921731,
                        "tag_name": "old_state_reservation",
                        "timestamp": 1596632935
                    }
                ],
                "finish": 1596632935,
                "offer": {
                    "agreement": "act_default",
                    "behaviour_constructor_id": "general_minutes_msc",
                    "constructor_id": "general_minutes_msc",
                    "deadline": 1596615922,
                    "debt": {
                        "threshold": 102400
                    },
                    "from_scanner": false,
                    "group_name": "Минуты",
                    "is_corp_session": true,
                    "name": "Минуты",
                    "offer_id": "d72cfb24-4f8af397-e7eb9478-417df29a",
                    "parent_id": "",
                    "price_constructor_id": "poc_qashqai_msc",
                    "prices": {
                        "deposit": 0,
                        "discount": {
                            "details": [],
                            "discount": 0,
                            "id": ""
                        },
                        "insurance_type": "standart",
                        "km": 0,
                        "km_discounted": 0,
                        "parking": 518,
                        "parking_discounted": 518,
                        "riding": 1098,
                        "riding_discounted": 1098,
                        "use_deposit": false
                    },
                    "short_description": [
                        "Ожидание — 5,18 ₽/мин",
                        "20 мин бесплатного ожидания"
                    ],
                    "short_name": "Мин",
                    "subname": "Классика",
                    "switchable": true,
                    "type": "standart_offer",
                    "visual": {
                        "bottom_color": "#0053F4",
                        "offer_visual_type": "",
                        "top_color": "#0053F4"
                    },
                    "wallets": [
                        {
                            "id": "card"
                        },
                        {
                            "id": "bonus_kazan_no-ride"
                        },
                        {
                            "id": "bonus"
                        },
                        {
                            "id": "yandex_account",
                            "selected": true
                        }
                    ]
                },
                "offer_name": "Минуты",
                "session_id": "d72cfb24-4f8af397-e7eb9478-417df29a",
                "start": 1596615632,
                "total_duration": 17303,
                "total_price": 139022
            },
            "start_geo_tags": [
                "msc_area"
            ],
            "trace_tags": [],
            "user_details": {
                "first_name": "",
                "id": "0007e410-c857-4835-9a73-1eee79d55208",
                "last_name": "",
                "pn": "",
                "preliminary_payments": {
                    "amount": 0,
                    "enabled": false
                },
                "setup": {
                    "email": {
                        "address": "keksite@yandex.ru",
                        "verified": true
                    },
                    "phone": {
                        "number": "+79250478631",
                        "verified": true
                    }
                },
                "status": "active",
                "username": "keksite"
            }
        },
        {
            "car": {
                "documents": [
                    {
                        "link": "http://carsharing-car-documents.s3.yandex.net/OSAGO/XW8ZZZ61ZLG023289/majorAPI/1576260954.pdf",
                        "title": "ОСАГО"
                    },
                    {
                        "link": "http://carsharing-car-documents.s3.yandex.net/STS/XW8ZZZ61ZLG023289/1576595746.pdf",
                        "title": "СТС"
                    }
                ],
                "former_numbers": [],
                "fuel_card_number": "",
                "id": "660f7db8-72cc-d44d-9b42-ab7254a37fc9",
                "imei": "861108038347463",
                "insurance_agreement_number": "001AT-20/0267112",
                "insurance_provider": "renins",
                "model_id": "vw_polo",
                "number": "н858ху750",
                "osago_mds_key": "OSAGO/XW8ZZZ61ZLG023289/majorAPI/1576260954.pdf",
                "registration_id": "9917026031",
                "registration_mds_key": "STS/XW8ZZZ61ZLG023289/1576595746.pdf",
                "responsible_picker": "",
                "sf": [
                    68,
                    83,
                    2544
                ],
                "view": 0,
                "vin": "XW8ZZZ61ZLG023289"
            },
            "device_diff": {
                "finish": {
                    "course": 151,
                    "latitude": 55.87618637,
                    "longitude": 37.39827728,
                    "since": 1596726648,
                    "timestamp": 1596727206,
                    "type": "GPSCurrent"
                },
                "hr_mileage": "0 м",
                "mileage": 0,
                "start": {
                    "course": 151,
                    "latitude": 55.87618637,
                    "longitude": 37.39827728,
                    "since": 1596726648,
                    "timestamp": 1596727182,
                    "type": "GPSCurrent"
                }
            },
            "geo_tags": [
                "msc_area"
            ],
            "offer_proto": {
                "BehaviourConstructorId": "general_minutes_msc",
                "CashbackPercent": 0,
                "ChargableAccounts": [
                    "card",
                    "bonus_kazan_no-ride",
                    "bonus",
                    "yandex_account"
                ],
                "ConstructorId": "general_minutes_msc",
                "Corrector": [
                    "disable_corrector_to_hide_plus_discount",
                    "split-2-checker",
                    "free_times_acceptance",
                    "free_time_night",
                    "DRIVEANALYTICS-679.destination_predictor",
                    "surges.msc.v2",
                    "destination_surges.msc.v2",
                    "min_max_price.econom.msc",
                    "flows_corrector_2"
                ],
                "Deadline": 1596727496,
                "DeviceIdAccept": "c79fcf5e6a123f59796f395362ad6bc4",
                "DisabledCorrector": [
                    "plus_discount_12",
                    "plus_discount_5"
                ],
                "DiscountsInfo": {
                    "Discounts": [
                        {
                            "Details": [
                                {
                                    "AdditionalTime": -240,
                                    "Discount": 0,
                                    "TagName": "old_state_reservation"
                                }
                            ],
                            "Discount": 0,
                            "Identifier": "fee",
                            "IsPromoCode": false,
                            "Visible": false
                        },
                        {
                            "Details": [
                                {
                                    "AdditionalTime": -240,
                                    "Discount": 0,
                                    "TagName": "old_state_reservation"
                                }
                            ],
                            "Discount": 0,
                            "Identifier": "fee",
                            "IsPromoCode": false,
                            "Visible": false
                        },
                        {
                            "Details": [
                                {
                                    "AdditionalTime": 180,
                                    "Discount": 0,
                                    "TagName": "old_state_reservation"
                                }
                            ],
                            "Discount": 0,
                            "Identifier": "walking_time",
                            "IsPromoCode": false,
                            "Visible": false
                        },
                        {
                            "Details": [
                                {
                                    "AdditionalTime": 300,
                                    "Discount": 0,
                                    "TagName": "old_state_acceptance"
                                }
                            ],
                            "Discount": 0,
                            "Identifier": "free_times_acceptance",
                            "IsPromoCode": false,
                            "Visible": false
                        },
                        {
                            "Details": [
                                {
                                    "AdditionalTime": 0,
                                    "Discount": 0,
                                    "FreeTimetable": {
                                        "Restrictions": [
                                            {
                                                "TimeFrom": 2330,
                                                "TimeTo": 500,
                                                "TimezoneShift": 3
                                            }
                                        ]
                                    },
                                    "TagName": "old_state_parking",
                                    "TagsInPoint": [
                                        "!allow_drop_car"
                                    ]
                                },
                                {
                                    "AdditionalTime": 0,
                                    "Discount": 0,
                                    "FreeTimetable": {
                                        "Restrictions": [
                                            {
                                                "TimeFrom": 2330,
                                                "TimeTo": 500,
                                                "TimezoneShift": 3
                                            }
                                        ]
                                    },
                                    "TagName": "old_state_reservation",
                                    "TagsInPoint": [
                                        "!allow_drop_car"
                                    ]
                                }
                            ],
                            "Discount": 0,
                            "Identifier": "free_time_night",
                            "IsPromoCode": false,
                            "Visible": false
                        }
                    ]
                },
                "FromScanner": false,
                "GroupName": "Минуты",
                "InstanceType": "standart_offer",
                "Name": "Минуты",
                "NextOfferId": "",
                "ObjectId": "660f7db8-72cc-d44d-9b42-ab7254a37fc9",
                "ObjectModel": "",
                "OfferId": "aedd4329-e7adbda5-8becd865-6fa9bd4a",
                "OnlyOriginalOfferCashback": true,
                "OriginalRidingStart": {
                    "X": 37.39827728,
                    "XHP": 37.39827728,
                    "Y": 55.87618637,
                    "YHP": 55.87618637
                },
                "ParentId": "",
                "PaymentDiscretization": 31415,
                "PriceConstructorId": "poc_polo_msc",
                "SelectedCharge": "card",
                "SelectedCreditCard": "card-x9606",
                "ShortName": "Мин",
                "StandartOffer": {
                    "Agreement": "act_default",
                    "DebtThreshold": 102400,
                    "DepositAmount": 0,

                    "InsuranceType": "standart",
                    "KmPriceModeling": false,
                    "ParkingPriceModeling": false,
                    "PriceKm": 0,
                    "PriceModel": "min_max_price.7_to_15",
                    "PriceModelInfo": [
                        {
                            "After": 898,
                            "Before": 898,
                            "Name": "correction_checker_split-2-checker_Other"
                        },
                        {
                            "After": 898,
                            "Before": 898,
                            "Name": "correction_surges.msc.v2_no_surge"
                        },
                        {
                            "After": 898,
                            "Before": 898,
                            "Name": "correction_destination_surges.msc.v2_no_surge"
                        },
                        {
                            "After": 897.999939,
                            "Before": 897.999939,
                            "Name": "min_max_price.7_to_15"
                        }
                    ],
                    "PriceParking": 368,
                    "PriceRiding": 897,
                    "RidingPriceModeling": true,
                    "UseDefaultShortDescriptions": true,
                    "UseDeposit": false,
                    "UseRounding": false
                },
                "SubName": "(resource:minutes_subname)",
                "Switchable": true,
                "TargetHolderTag": "",
                "Timestamp": 1596727196,
                "Transferable": true,
                "TransferredFrom": "",
                "UserId": "0007e410-c857-4835-9a73-1eee79d55208"
            },
            "segment": {
                "__type": "riding_compilation",
                "bill": [
                    {
                        "cost": 30,
                        "details": "5 с",
                        "duration": 5,
                        "title": "Бронь",
                        "type": "fee_drop_zone_max"
                    },
                    {
                        "cost": 0,
                        "title": "Способ оплаты",
                        "type": "section"
                    },
                    {
                        "cost": 30,
                        "details": "Сняты бонусы в счет оплаты полученных услуг",
                        "title": "Сняты бонусы",
                        "type": "billing_bonus"
                    },
                    {
                        "cost": 0,
                        "title": "Итого",
                        "type": "total"
                    }
                ],
                "diff": {
                    "finish": {
                        "course": 151,
                        "latitude": 55.87618637,
                        "longitude": 37.39827728,
                        "since": 1596726648,
                        "timestamp": 1596727206,
                        "type": "GPSCurrent"
                    },
                    "hr_mileage": "0 м",
                    "mileage": 0,
                    "start": {
                        "course": 151,
                        "latitude": 55.87618637,
                        "longitude": 37.39827728,
                        "since": 1596726648,
                        "timestamp": 1596727182,
                        "type": "GPSCurrent"
                    }
                },
                "events": [
                    {
                        "action": "set_performer",
                        "event_id": 958801695,
                        "tag_name": "old_state_reservation",
                        "timestamp": 1596727199
                    },
                    {
                        "action": "evolve",
                        "event_id": 958801860,
                        "tag_name": "old_state_acceptance",
                        "timestamp": 1596727204
                    },
                    {
                        "action": "evolve",
                        "event_id": 958802578,
                        "tag_name": "old_state_reservation",
                        "timestamp": 1596727225
                    },
                    {
                        "action": "drop_performer",
                        "event_id": 958802579,
                        "tag_name": "old_state_reservation",
                        "timestamp": 1596727225
                    }
                ],
                "finish": 1596727225,
                "offer": {
                    "agreement": "act_default",
                    "behaviour_constructor_id": "general_minutes_msc",
                    "constructor_id": "general_minutes_msc",
                    "deadline": 1596727496,
                    "debt": {
                        "threshold": 102400
                    },
                    "device_id": "c79f****6bc4",
                    "from_scanner": false,
                    "group_name": "Минуты",
                    "is_corp_session": false,

                    "name": "Минуты",
                    "offer_id": "aedd4329-e7adbda5-8becd865-6fa9bd4a",
                    "parent_id": "",
                    "price_constructor_id": "poc_polo_msc",
                    "prices": {
                        "deposit": 0,
                        "discount": {
                            "details": [],
                            "discount": 0,
                            "id": ""
                        },
                        "insurance_type": "standart",
                        "km": 0,
                        "km_discounted": 0,
                        "parking": 368,
                        "parking_discounted": 368,
                        "riding": 897,
                        "riding_discounted": 897,
                        "use_deposit": false
                    },
                    "short_description": [
                        "Ожидание — 3,68 ₽/мин",
                        "Нет бесплатного ожидания"
                    ],
                    "short_name": "Мин",
                    "subname": "Классика",
                    "switchable": true,
                    "type": "standart_offer",
                    "visual": {
                        "bottom_color": "#0053F4",
                        "offer_visual_type": "",
                        "top_color": "#0053F4"
                    },
                    "wallets": [
                        {
                            "id": "card",
                            "selected": true
                        },
                        {
                            "id": "bonus_kazan_no-ride"
                        },
                        {
                            "id": "bonus"
                        },
                        {
                            "id": "yandex_account"
                        }
                    ]
                },
                "offer_name": "Минуты",
                "session_id": "aedd4329-e7adbda5-8becd865-6fa9bd4a",
                "start": 1596727199,
                "total_duration": 26,
                "total_price": 30
            },
            "start_geo_tags": [
                "msc_area"
            ],
            "trace_tags": [],
            "user_details": {
                "first_name": "",
                "id": "0007e410-c857-4835-9a73-1eee79d55208",
                "last_name": "",
                "pn": "",
                "preliminary_payments": {
                    "amount": 0,
                    "enabled": false
                },
                "setup": {
                    "email": {
                        "address": "keksite@yandex.ru",
                        "verified": true
                    },
                    "phone": {
                        "number": "+79250478631",
                        "verified": true
                    }
                },
                "status": "active",
                "username": "keksite"
            }
        },
    ],
    'server_time': 1595922522,
    'has_more': true,
    'views': [{
        'fuel_icon_url': 'https://carsharing.s3.yandex.net/fuel_finger.png',
        'image_angle_url': 'https://carsharing.s3.yandex.net/drive/car-models/Porsche911-Carrera4S-red-3-4.png',
        'image_pin_url_3x': 'https://carsharing.s3.yandex.net/drive/pins/test/2blue-map-pin3x.png',
        'fuel_type': '98',
        'registry_model': '',
        'image_pin_url_2x': 'https://carsharing.s3.yandex.net/drive/pins/test/2blue-map-pin2x.png',
        'fuel_cap_side': 'right',
        'short_name': '911',
        'image_large_url': ' https://carsharing.s3.yandex.net/drive/car-models/Porsche/Porsche911-Carrera4S-red-side_large.png',
        'name': 'Porsche 911 Carrera 4S',
        'registry_manufacturer': '',
        'manufacturer': 'Porsche',
        'visual': {
            'background': {'gradient': {'bottom_color': '#232323', 'top_color': '#232323'}},
            'title': {'color': '#ffffff'}
        },
        'code': 'porsche_carrera',
        'image_map_url_3x': 'https://carsharing.s3.yandex.net/drive/car-models/Porsche/Porsche911-Carrera4S-red-top-2@3x.png',
        'image_map_url_2x': 'https://carsharing.s3.yandex.net/drive/car-models/Porsche/Porsche911-Carrera4S-red-top-2@2x.png',
        'image_small_url': 'https://carsharing.s3.yandex.net/drive/car-models/Porsche/Porsche911-Carrera4S-red-side_small.png'
    }],
    'sf': [{
        'tag_flow': '',
        'index': 78,
        'display_name': 'Регистратор в машине',
        'public_icon': 'https://carsharing.s3.yandex.net/drive/static/tag-icons/v4/registrator.png',
        'name': 'car_registrator',
        'comment': 'Регистратор в машине',
        'is_important': false,
        'tag_flow_priority': 0
    }],
    'property_patches': [],
    'models': {
        'porsche_carrera': {
            'image_map_url_2x': 'https://carsharing.s3.yandex.net/drive/car-models/Porsche/Porsche911-Carrera4S-red-top-2@2x.png',
            'name': 'Porsche 911 Carrera 4S',
            'code': 'porsche_carrera',
            'manufacturer': 'Porsche',
            'image_angle_url': 'https://carsharing.s3.yandex.net/drive/car-models/Porsche911-Carrera4S-red-3-4.png',
            'cars_count': 1,
            'image_map_url_3x': 'https://carsharing.s3.yandex.net/drive/car-models/Porsche/Porsche911-Carrera4S-red-top-2@3x.png',
            'registry_manufacturer': '',
            'short_name': '911',
            'image_large_url': ' https://carsharing.s3.yandex.net/drive/car-models/Porsche/Porsche911-Carrera4S-red-side_large.png',
            'visual': {
                'background': {'gradient': {'bottom_color': '#232323', 'top_color': '#232323'}},
                'title': {'color': '#ffffff'}
            },
            'image_pin_url_3x': 'https://carsharing.s3.yandex.net/drive/pins/test/2blue-map-pin3x.png',
            'image_small_url': 'https://carsharing.s3.yandex.net/drive/car-models/Porsche/Porsche911-Carrera4S-red-side_small.png',
            'specifications': [{
                'name': 'Коробка',
                'position': 0,
                'value': 'автомат',
                'id': '67b55588-82fc-444e-9e1e-caa74ad7947d'
            }, {
                'name': 'Привод',
                'position': 1000000,
                'value': 'полный',
                'id': 'a1048bd8-1ec0-422a-b49c-726dadf7d162'
            }, {
                'name': 'Мощность',
                'position': 2000000,
                'value': '420 л.с.',
                'id': 'b72e27f8-f294-4306-9815-25487815d118'
            }, {
                'name': 'Разгон',
                'position': 3000000,
                'value': '4,2 с',
                'id': '065d584e-a87c-410b-929b-e8a8eab34d1a'
            }, {
                'name': 'Мест',
                'position': 4000000,
                'value': '1+1',
                'id': '7a105b9d-81db-40ae-9e1e-72780e76009b'
            }, {'name': 'Бак', 'position': 5000000, 'value': '68 л', 'id': 'e84def26-fa15-41e9-b77e-493257099909'}],
            'fuel_type': '98',
            'fuel_cap_side': 'right',
            'image_pin_url_2x': 'https://carsharing.s3.yandex.net/drive/pins/test/2blue-map-pin2x.png',
            'registry_model': '',
            'fuel_icon_url': 'https://carsharing.s3.yandex.net/fuel_finger.png'
        }
    }
};
export default mockData;
