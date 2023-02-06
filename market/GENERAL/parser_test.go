package parser

import (
	"strings"
	"testing"
)

func TestFake(t *testing.T) {
	t.Log("Fake test")
}

func TestParseSkippedLines(t *testing.T) {
	var err error
	var lines2parse = []string{
		`Random trash`,
		`2020-03-16T14:09:53.638+03:00 local1.info 2020-03-16T14:09:53+03:00 mslb01h haproxy 3307 - - Connect from 2a02:6b8:0:1a00::ba4a:37139 to 2a02:6b8:0:3400:0:3c9:0:14e:34855 (cs-billing-tms.vs.market.yandex.net:34855/TCP)`,
		`2020-03-16T14:09:52.550+03:00 local0.alert 2020-03-16T14:09:52+03:00 mslb01h haproxy 3307 - - Server marketbuker.yandex.ru:29310/sas3-1464-7bd-sas-market-prod--f27-27494.gencfg-c.yandex.net:27494 is DOWN, reason: Layer7 timeout, check duration: 1000ms. 23 active and 0 backup servers left. 14 sessions active, 0 requeued, 0 remaining in queue.`,
		`2020-03-16T14:09:52.550+03:00 local2.alert 2020-03-16T14:09:52+03:00 mslb01h haproxy 3307 - - Server marketbuker.yandex.ru:29310/sas3-1464-7bd-sas-market-prod--f27-27494.gencfg-c.yandex.net:27494 is DOWN, reason: Layer7 timeout, check duration: 1000ms. 23 active and 0 backup servers left. 14 sessions active, 0 requeued, 0 remaining in queue.`,
		`2021-03-10T06:26:54.559+03:00 local2.info 2021-03-10T06:26:54.559042+03:00 mslb01h haproxy 25291 - [haproxy_http_req@13238 conn_time="10/Mar/2021:06:26:54.559" fe_addr="2a02:6b8:0:3400:0:3c9:0:216" fe_port="8080" handshake_ms="0" idle_ms="0" req_after_connect_ms="-1" http_version="<BADREQ>" http_method="<BADREQ>" req_size="0" be_src_addr="-" be_src_port="-" srv_ip="-" srv_port="-"] 2a02:6b8:0:1a00::ba4b:44679 [10/Mar/2021:06:26:54.559] access-puller.vs.market.yandex.net:8080 access-puller.vs.market.yandex.net:8080/<NOSRV> -1/-1/-1/-1/0 0 0 - - PR-- 2014/1/0/0/0 0/0 "<BADREQ>"`,
	}
	for _, line := range lines2parse {
		var b = []byte(line)
		var r *LogRecord
		r, err = NewLogRecord(&b)
		if err != nil {
			t.Errorf("error \"%v\" on parsing string \"%s\"", err, line)
			continue
		}
		if r != nil {
			t.Errorf("line \"%s\" should be skipped", line)
		}
	}
}

func TestParseLines(t *testing.T) {
	var err error
	var lines2parse = []string{
		"2020-03-16T14:09:50.276+03:00 local2.info 2020-03-16T14:09:50+03:00 mslb01h haproxy 3307 - [haproxy_http_req@13238 conn_time=\"16/Mar/2020:14:09:49.775\" fe_addr=\"::1\" fe_port=\"29510\" handshake_ms=\"0\" idle_ms=\"0\" req_after_connect_ms=\"0\" http_version=\"HTTP/1.1\" http_method=\"GET\" req_size=\"545\" be_src_addr=\"-\" be_src_port=\"-\" srv_ip=\"-\" srv_port=\"-\"] ::1:54494 [16/Mar/2020:14:09:49.775] marketbuker.yandex.ru:29310 marketbuker.yandex.ru:29310/sas3-1464-7bd-sas-market-prod--f27-27494.gencfg-c.yandex.net:27494 0/501/-1/-1/501 503 237 - - sQ-- 1373/31/16/13/0 1/0 {1584356989693/4483bf277c9ff2ffc26e91dbf6a00500/11/2|2a02:6b8:c08:7a9c:10d:a6ef:0:4c2f|2a02:6b8:c08:7a9c:10d:a6ef:0:4c2f|marketbuker.yandex.ru} \"GET /buker/GetCards?collection=cms-context-relations&device=desktop&domain=ru&format=json&one_of=show_explicit_content*region,region,&product_id=10967880&rearr-factors=&region=14&show_explicit_content=medicine&skip=region,product_id&type=product_card&zoom=full HTTP/1.1\"\n",
		`[haproxy_http_req@13238 conn_time="16/Mar/2020:14:09:49.656" fe_addr="::1" fe_port="29510" handshake_ms="0" idle_ms="1" req_after_connect_ms="1" http_version="HTTP/1.1" http_method="GET" req_size="357" be_src_addr="-" be_src_port="-" srv_ip="-" srv_port="-"] ::1:54498 [16/Mar/2020:14:09:49.657] marketbuker.yandex.ru:29310 marketbuker.yandex.ru:29310/sas3-1464-7bd-sas-market-prod--f27-27494.gencfg-c.yandex.net:27494 0/502/-1/-1/502 503 237 - - sQ-- 1373/31/16/13/0 1/0 {1584356989641/008b904db63e2b1aaea290dbf6a00500/2|85.140.7.220, 2a02:6b8:c1b:3823:10b:5646:0:228c|2a02:6b8:c1b:3823:10b:5646:0:228c|marketbuker.yandex.ru} "GET /buker/GetCards?collection=models-rating&ids=647828080 HTTP/1.1"` + "\n",
		`fdee:fdee:0:3400:0:3c9:0:183:53610 [16/Mar/2020:14:09:49.785] cashback-epn-requester.vs.market.yandex.net:443 cashback-epn-requester.vs.market.yandex.net:443/sas1-1201-088-sas-market-prod--2b4-24150.gencfg-c.yandex.net:24150 0/0/0/37/37 503 266 - - ---- 1379/18/2/2/0 0/0 {1584356989766/6cf80066bbeb2c36d980a4dd7d1d6b34|2a02:6b8:c14:d90:10b:11e5:c37:0, 2a02:6b8:c08:7b0a:10e:f4c9:0:585f|2a02:6b8:c14:d90:10b:11e5:c37:0|cashback-epn-requester.vs.market.yandex.net} "GET /wallet_info_front?puid=636368980 HTTP/1.1"`,
		`2020-03-16T11:15:53.107+03:00 local2.info 2020-03-16T11:15:53+03:00 mslb01h haproxy 3307 - [haproxy_http_req@13238 conn_time="16/Mar/2020:11:15:52.874" fe_addr="fdee:fdee:0:3400:0:3c9:0:1ba" fe_port="80" handshake_ms="0" idle_ms="226" req_after_connect_ms="226" http_version="HTTP/1.1" http_method="GET" req_size="623" be_src_addr="2a02:6b8:c02:45a:0:577:e299:ec6c" be_src_port="43868" srv_ip="2a02:6b8:c16:200b:10e:f3e4:0:4ffa" srv_port="20474"] fdee:fdee:0:3400:0:3c9:0:1ba:39692 [16/Mar/2020:11:15:53.100] dj-recommender-fast.vs.market.yandex.net:443 be1:dj-recommender-fast.vs.market.yandex.net:443/sas3-0336-2fa-sas-market-prod--708-20474.gencfg-c.yandex.net:20474 0/1/0/6/7 200 218 - - ---- 1231/30/0/0/+1 0/0 {1584346552956/61bb4ac71fc2a41305757d6df4a00500/27|2a02:6b8:c1b:3804:10d:1bb3:0:62a7|2a02:6b8:c1b:3804:10d:1bb3:0:62a7|dj-recommender-fast.vs.market.yandex.net} "GET /record_hit?yandexuid=6954027551561095749&type=question&question-id=1845099&question-id=1828623&question-id=1841861&question-id=1744073&question-id=1803337&question-id=1880739&`,
		`2021-03-10T06:26:36.347+03:00 local2.info 2021-03-10T06:26:36.347017+03:00 mslb01h haproxy 25291 - [haproxy_http_req@13238 conn_time="10/Mar/2021:06:26:36.206" fe_addr="2a02:6b8:0:3400:0:3c9:0:91" fe_port="8080" handshake_ms="0" idle_ms="40" req_after_connect_ms="40" http_version="HTTP/2.0" http_method="POST" req_size="2533" be_src_addr="2a02:6b8:c02:45a:0:577:e299:ec6c" be_src_port="42630" srv_ip="2a02:6b8:c08:7792:10e:f91e:0:43c9" srv_port="17355"] 2a02:6b8:c1b:360d:10b:7acd:0:429a:45228 [10/Mar/2021:06:26:36.246] combinator.vs.market.yandex.net:8080 be1:combinator.vs.market.yandex.net:8080/sas4-6785-266-sas-market-prod--fc2-17353.gencfg-c.yandex.net:17355 0/0/0/-1/100 -1 0 - - CD-- 2006/58/0/0/0 0/0 {|||combinator.vs.market.yandex.net:8080} "POST http://combinator.vs.market.yandex.net:8080/yandex.market.combinator.v0.Combinator/GetOffersDeliveryStats HTTP/2.0"`,
	}
	var results2compare = []LogRecord{
		{XMarketReqID: "1584356989693/4483bf277c9ff2ffc26e91dbf6a00500/11/2",
			Protocol:             "HTTP/1.1",
			RequestTimestampISO:  "2020-03-16T14:09:49.775+03:00",
			ResponseTimestampISO: "2020-03-16T14:09:50.276+03:00",
			Retries:              0},
		{XMarketReqID: "1584356989641/008b904db63e2b1aaea290dbf6a00500/2",
			Protocol:             "HTTP/1.1",
			RequestTimestampISO:  "2020-03-16T14:09:49.657+03:00",
			ResponseTimestampISO: "2020-03-16T14:09:49.657+03:00",
			Retries:              0},
		{XMarketReqID: "1584356989766/6cf80066bbeb2c36d980a4dd7d1d6b34",
			Protocol:             "HTTP/1.1",
			RequestTimestampISO:  "2020-03-16T14:09:49.785+03:00",
			ResponseTimestampISO: "2020-03-16T14:09:49.785+03:00",
			Retries:              0},
		{XMarketReqID: "1584346552956/61bb4ac71fc2a41305757d6df4a00500/27",
			Protocol:             "HTTP/1.1",
			RequestTimestampISO:  "2020-03-16T11:15:53.100+03:00",
			ResponseTimestampISO: "2020-03-16T11:15:53.107+03:00",
			Retries:              1},
		{XMarketReqID: "",
			Protocol:             "HTTP/2.0",
			RequestTimestampISO:  "2021-03-10T06:26:36.246+03:00",
			ResponseTimestampISO: "2021-03-10T06:26:36.347+03:00",
			Retries:              0},
	}

	for idx, line := range lines2parse {
		var b = []byte(line)
		var r *LogRecord
		r, err = NewLogRecord(&b)
		if err != nil {
			t.Errorf("error \"%v\" on parsing line \"%s\"", err, strings.TrimSpace(line))
			continue
		}
		if r == nil {
			t.Errorf("line \"%s\" should be acceptable", strings.TrimSpace(line))
			continue
		}

		var cmp = results2compare[idx]
		if cmp.XMarketReqID != r.XMarketReqID {
			t.Errorf("Invalid XMarketReqID [\"%s\" != \"%s\"] in \"%s\"", r.XMarketReqID, cmp.XMarketReqID, strings.TrimSpace(line))
		}
		if cmp.RequestTimestampISO != r.RequestTimestampISO {
			t.Errorf("Invalid RequestTimestampISO [\"%s\" != \"%s\"] in \"%s\"", r.RequestTimestampISO, cmp.RequestTimestampISO, strings.TrimSpace(line))
		}
		if cmp.ResponseTimestampISO != r.ResponseTimestampISO {
			t.Errorf("Invalid ResponseTimestampISO [\"%s\" != \"%s\"] in \"%s\"", r.ResponseTimestampISO, cmp.ResponseTimestampISO, strings.TrimSpace(line))
		}
		if cmp.Retries != r.Retries {
			t.Errorf("Invalid Retries [\"%d\" != \"%d\"] in \"%s\"", r.Retries, cmp.Retries, strings.TrimSpace(line))
		}
		if cmp.Protocol != r.Protocol {
			t.Errorf("Invalid protocol [\"%s\" != \"%s\"] in \"%s\"", r.Protocol, cmp.Protocol, strings.TrimSpace(line))
		}
	}
}
