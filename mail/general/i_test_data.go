package list

const (
	postNoTypes string = `{
      "envelopes":[
         {
            "mid":"159033361841522057",
            "from":[{"local":"callcenter","domain":"aeroflot.ru","displayName":"DN"}],
            "subject":"SUBJ",
            "firstline":"FL",
            "types":[]
         }]}`
	postTypes5And16 string = `{
      "envelopes":[
         {
            "mid":"159033361841522057",
            "from":[{"local":"callcenter","domain":"aeroflot.ru","displayName":"DN"}],
            "subject":"SUBJ",
            "firstline":"FL",
            "types":[5, 16]
         }]}`
	postTypes19And35 string = `{
      "envelopes":[
         {
            "mid":"160440736725076058",
            "from":[{"local":"reservation","domain":"booking.com","displayName":"DN"}],
            "subject":"SUBJ",
            "firstline":"FL",
            "types":[19, 35]
         }]}`
	postTypes8 string = `{
      "envelopes":[
         {
            "mid":"160440736725076157",
            "from":[{"local":"mailer-daemon","domain":"yandex.ru"}],
            "subject":"SUBJ",
            "firstline":"FL",
            "types":[8]
         }]}`
	postTypes2 string = `{
      "envelopes":[
         {
            "mid":"159877786771718402",
            "from":[{"local":"mailer-daemon","domain":"yandex.ru"}],
            "subject":"SUBJ",
            "firstline":"FL",
            "types":[2]
         }]}`
	postTypes6 string = `{
      "envelopes":[
         {
            "mid":"160440736725076157",
            "from":[{"local":"mailer-daemon","domain":"yandex.ru"}],
            "subject":"SUBJ",
            "firstline":"FL",
            "types":[6]
         }]}`
	postTypes4 string = `{
      "envelopes":[
         {
            "mid":"160440736725076157",
            "from":[{"local":"mailer-daemon","domain":"yandex.ru"}],
            "subject":"SUBJ",
            "firstline":"FL",
            "types":[4]
         }]}`
	postTypes61 string = `{
      "envelopes":[
         {
            "mid":"160440736725076157",
            "types":[61]
         }]}`
	postTypes42 string = `{
      "envelopes":[
         {
            "mid":"160440736725076157",
            "from":[{"local":"local","domain":"domain","displayName":"DN"}],
            "subject":"SUBJ",
            "firstline":"FL",
            "types":[42]
         }]}`
	goodIexAviaAnswer string = `{
   "159033361841522057" : [
      {
         "ticket" : [
            {
               "transfer" : "aeroexpress",
               "airport_dep_iata" : "SVO",
               "city_arr" : "Брюссель",
               "airport_arr" : "Брюссель(Националь)",
               "airlinename_back" : "Аэрофлот",
               "to_country_geoid" : "114",
               "reservationNumber_back" : "OVXCMD",
               "airline_iata" : "SU",
               "@type" : "FlightReservation",
               "airport_dep" : "Шереметьево",
               "print_parts" : [
                  "1.2"
               ],
               "airport_arr_iata" : "BRU",
               "flight_number_back" : "SU2619",
               "city_dep_back" : "Брюссель",
               "reservationStatus_back" : "http://schema.org/ReservationConfirmed",
               "date_dep_back_rfc" : "2016-03-07T23:40:00+02:00",
               "reservationStatus" : "http://schema.org/ReservationConfirmed",
               "departureAirport@type" : "Airport",
               "arrivalAirport@type_back" : "Airport",
               "airport_dep_back" : "Брюссель(Националь)",
               "widget_subtype" : "eticket",
               "airport_arr_back" : "Шереметьево",
               "airlinename" : "Аэрофлот",
               "@type_back" : "FlightReservation",
               "to_country_geoid_back" : "225",
               "city_arr_country" : "Бельгия",
               "from_country_geoid" : "225",
               "city_arr_back" : "Москва",
               "city_dep" : "Москва",
               "arrivalAirport@type" : "Airport",
               "date_arr_rfc" : "2016-03-04T11:15:00+02:00",
               "reservationNumber" : "OVXCMD",
               "origin" : "micro",
               "origin_back" : "micro",
               "airport_dep_iata_back" : "BRU",
               "airport_arr_iata_back" : "SVO",
               "airline@type" : "Airline",
               "date_dep_rfc" : "2016-03-04T08:40:00+03:00",
               "airline_iata_back" : "SU",
               "city_dep_country" : "Россия",
               "date_arr_back_rfc" : "2016-03-08T04:50:00+03:00",
               "date_dep_ts" : "1457070000",
               "city_dep_country_back" : "Бельгия",
               "flight_number" : "SU2168",
               "from_country_geoid_back" : "114",
               "city_arr_country_back" : "Россия",
               "airline@type_back" : "Airline"
            }
         ],
         "taksa_widget_type_1234543456546" : "ticket"
      }]}`
	goodIexHotelAnswer string = `{
   "160440736725076058" : [
      {
         "postalCode" : "83110",
         "people" : "2",
         "city" : "Bang Tao Beach",
         "date_dep_ts" : "1481457600",
         "checkin_date_rfc" : "2016-12-11T15:00:00+07:00",
         "modifyReservationUrl" : "https://secure.booking.com/myreservations.ru.html?aid=304142&auth_key=1Dp4pLMefo3k6ssR&&pbsource=email_changeDates&et=UmFuZG9tSVYkc2RlIyh9YWJdm48m5cJDDoHmqqGNfQPdV2rG7bvZeQMFQ61KQM8mXn61CMm78wxY40fMGRe57XeXj5L91I6r+uZQzU+QG9ODsvcXR5ztGUq5LfUaiPk0yOZISAuq4sA5WaPwzmYgnw==",
         "price" : "68 070,60 THB",
         "city_geoid" : 10622,
         "address_geoid" : 10622,
         "streetAddress" : "22 Moo 2, Bangtao Beach,Cheang Thalay, Thalang, Phuket",
         "geocoder_lat" : 7.890429,
         "print_parts" : [],
         "number_of_nights" : "14",
         "origin" : "micro,iex-patterns",
         "address" : "Таиланд, 83110, Bang Tao Beach, 22 Moo 2, Bangtao Beach,Cheang Thalay, Thalang, Phuket",
         "reservationStatus" : "http://schema.org/Confirmed",
         "domain" : "booking.com",
         "reservation_number" : "1816747370",
         "geocoder_lon" : 98.389796,
         "uniq_id" : "5740ef500820d94910d7f252a4c675f9",
         "widget_subtype" : "booking",
         "taksa_widget_type_1234543456546" : "hotels",
         "priceCurrency" : "THB",
         "hotel" : "Sunwing Resort & Spa Bangtao Beach",
         "checkout_date_rfc" : "2016-12-25T12:00:00+07:00",
         "country" : "Таиланд",
         "cancellation_info" : "26.11.2016 23:59:00"
      }
   ]}`
	goodIexBounceAnswer string = `{
   "160440736725076157" : [
      {
         "status" : "5.1.1",
         "action" : "failed",
         "original_recipient" : "fjvdkdkcjfhdjfjgvjfjdkkskeff@ya.ru",
         "diagnostic_code" : "554",
         "remote_mta" : "127.0.0.1",
         "final_recipient" : "fjvdkdkcjfhdjfjgvjfjdkkskeff@ya.ru",
         "reporting_mta" : "mxback1o.mail.yandex.net",
         "taksa_widget_type_1234543456546" : "bounce",
         "bounce_type" : "1"
      }
   ]}`
	goodIexOneLinkAnswer string = `{
   "159877786771718402" : [
      {
         "taksa_widget_type_1234543456546" : "action",
         "widget_subtype" : "restore_password",
         "url" : "http://www.mvideo.ru/reset-password/?ID=0199d0a3-d26c-43c7-a045-3914949dbd5b",
         "origin" : "regexp"
      }
   ]}`
	goodIexEshopAnswer string = `{
   "160440736725076157" : [
      {
         "taksa_widget_type_1234543456546" : "eshop",
         "widget_subtype" : "eshop",
         "order_number":"500640838920107",
         "order_status_txt":" отправил",
         "order": "USB-флешка\n SANDISK Cruzer Edge 8Gb (SDCZ51-008G-B35)",
         "url" : "http://aliexpress.com/sfgdfhghd",
         "price":"$1",
         "date_delivery":"09.11.2016 21:35:00"
      }
   ]}`
	goodIexSnippetAnswer string = `{
   "160440736725076157" : [
      {
         "taksa_widget_type_1234543456546" : "snippet-text",
         "text_html" : "\n<blockquote>\n\n<text>\n <div>qwerty</div>\r \n</text>\n\n</blockquote>\n",
         "text" : "qwerty"
      }
   ]}`
	goodIexTrackerAnswer string = `{
   "160440736725076157" : [
      {
         "taksa_widget_type_1234543456546" : "action",
         "url" : "link"
      }
   ]}`
	goodIexCalendarAnswer string = `{
   "160440736725076157" : [
      {
         "title" : "Test Event Message",
         "organizer" : {
            "name" : "",
            "email" : "juliyas@yandex-team.ru",
            "decision" : "accepted",
            "type" : "externalUser"
         },
         "start_date_ts" : 1540638000000,
         "origin" : "ics",
         "people" : 3,
         "raw_data" : {
            "request" : "/api/mail/getEventInfoByIcsUrl?uid=570905802&icsUrl=http://webattach.mail.yandex.net/message_part_real/?sid=67wQ6Hcc0wl5XzJNW8gmCiyPb6QV/pbNPr3LOQV5DBe6Y*UQtpsK36BcSLrMauiAZe/Sne6Wq4kbFhdnXBaNgpAqr0n*dMqTgdJ7D8MwIL3PaT/Y6e7p52OJTI3SZmrx&lang=ru",
            "data" : {
               "invocation-info" : {
                  "action" : "getEventInfoByIcsUrl",
                  "app-name" : "web",
                  "hostname" : "cal-back2o.cmail.yandex.net",
                  "host-id" : "2o",
                  "app-version" : "17.31.2.feeeba796",
                  "exec-duration-millis" : "230",
                  "req-id" : "TzbkwHPP"
               },
               "eventInfo" : {
                  "location" : "Помидор (СТ 4-2)",
                  "attendees" : [
                     {
                        "type" : "externalUser",
                        "name" : "",
                        "decision" : "accepted",
                        "email" : "conf_st_4_2@yandex-team.ru"
                     },
                     {
                        "type" : "externalUser",
                        "name" : "",
                        "decision" : "undecided",
                        "email" : "uliyana-u@yandex.ru"
                     }
                  ],
                  "name" : "Test Event Message",
                  "isNoRsvp" : false,
                  "description" : "",
                  "isPast" : false,
                  "end" : 1540639800000,
                  "start" : 1540638000000,
                  "calendarUrl" : "https://calendar.yandex.ru/event?event_id=442086083",
                  "calendarMailType" : "event_invitation",
                  "isCancelled" : false,
                  "organizer" : {
                     "name" : "",
                     "email" : "juliyas@yandex-team.ru",
                     "decision" : "accepted",
                     "type" : "externalUser"
                  },
                  "instanceKey" : "Y7Vo1sYLrVksX5wKQHwD4Q==",
                  "isAllDay" : false,
                  "actions" : [
                     "accept",
                     "decline"
                  ],
                  "decision" : "undecided",
                  "externalId" : "Z2mvL5zUyandex.ru"
               }
            },
            "uid" : "570905802",
            "stid" : "320.mail:570905802.E470864:3576109177522397014881947524",
            "lang" : "ru",
            "hid" : "1.2"
         },
         "instanceKey" : "Y7Vo1sYLrVksX5wKQHwD4Q==",
         "end_date_ts" : 1540639800000,
         "taksa_widget_type_1234543456546" : "event-ticket",
         "isAllDay" : false,
         "actions" : [
            "accept",
            "decline"
         ],
         "end_date_rfc" : "2018-10-27 14:30:00",
         "start_date_rfc" : "2018-10-27 14:00:00",
         "externalId" : "Z2mvL5zUyandex.ru",
         "decision" : "undecided",
         "location" : "Помидор (СТ 4-2)",
         "attendees" : [
            {
               "name" : "",
               "decision" : "accepted",
               "email" : "conf_st_4_2@yandex-team.ru",
               "type" : "externalUser"
            },
            {
               "type" : "externalUser",
               "name" : "",
               "decision" : "undecided",
               "email" : "uliyana-u@yandex.ru"
            }
         ],
         "domain" : "calendar.yandex-team.ru",
         "isNoRsvp" : false,
         "special_parts" : [
            "1.2"
         ],
         "isPast" : false,
         "description" : "",
         "widget_subtype" : "calendar"
      }
   ]}`
)
