CREATE TABLE market.nginx_front (date Date, timestamp UInt32, host String, vhost String, url String, http_method String, http_code UInt16, resptime_ms Int32, dynamic UInt8) ENGINE = MergeTree(date, (timestamp, vhost, http_code), 16384)

;

SELECT toStartOfHour(datetime), quantiles(1, 0.999, 0.997, 0.995, 0.99,0.97,0.95,0.90)(resptime_ms) FROM market_nginx WHERE date = '2014-11-23' and (vhost = 'partner.market.yandex.ru' and dynamic = 1 and http_code = 200)  GROUP BY toStartOfHour(datetime)

SELECT toStartOfMinute(datetime), quantiles(1)(resptime_ms) FROM market_nginx WHERE date = toDate('2014-11-23') and toHour(datetime) = 18 and (vhost = 'partner.market.yandex.ru' and dynamic = 1 and http_code = 200)  GROUP BY toStartOfMinute(datetime)

SELECT toStartOfMinute(datetime), quantiles(1, 0.999, 0.997, 0.995, 0.99,0.97,0.95,0.90)(resptime_ms) FROM market_nginx WHERE date = toDate('2014-11-23') and toHour(datetime) = 18 and (vhost = 'partner.market.yandex.ru' and dynamic = 1 and http_code = 200)  GROUP BY toStartOfMinute(datetime)

SELECT toStartOfHour(datetime), quantiles(1, 0.999, 0.997, 0.995, 0.99,0.97,0.95,0.90)(resptime_ms) FROM market_nginx WHERE date = toDate('2014-11-23') and toHour(datetime) = 18 and (vhost = 'partner.market.yandex.ru' and dynamic = 1 and http_code = 200)  GROUP BY toStartOfHour(datetime)



SELECT plus(multiply(intDiv(timestamp, 3600), 3600), 3600) as hour, count() FROM market_nginx GROUP BY hour ORDER BY hour


CREATE TABLE market.nginx_front (date Date, timestamp UInt32, host String, vhost String, url String, http_method String, http_code UInt16, resptime_ms Int32, dynamic UInt8) ENGINE = MergeTree(date, (timestamp, vhost, http_code), 16384)



