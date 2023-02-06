package main

import (
	"a.yandex-team.ru/apphost/api/service/go/apphost"
	"a.yandex-team.ru/apphost/lib/proto_answers"
	"net/url"
	"strings"
)

var (
	badRequest = protoanswers.THttpResponse{
		StatusCode: 400,
	}

	jsonHeaders = []*protoanswers.THeader{{
		Name:  "Content-Type",
		Value: "application/json",
	}}

	xmlHeaders = []*protoanswers.THeader{{
		Name:  "Content-Type",
		Value: "text/xml",
	}}
)

const (
	xmlTemplate = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<doc>\n" +
		"  <uid hosted=\"0\" mx=\"0\" domain_ena=\"1\" catch_all=\"0\">{{uid}}</uid>\n" +
		"  <login>test{{uid}}</login>\n" +
		"  <aliases/>\n" +
		"  <karma confirmed=\"0\">0</karma>\n" +
		"  <karma_status>0</karma_status>\n" +
		"  <regname>test{{uid}}</regname>\n" +
		"  <display_name>\n" +
		"    <name>{{uid}}</name>\n" +
		"    <public_name>{{uid}}</public-name>\n" +
		"    <avatar>\n" +
		"      <default>0/0-0</default>\n" +
		"      <empty>1</empty>\n" +
		"    </avatar>\n" +
		"  </display_name>\n" +
		"  <attributes/>\n" +
		"  <phones/>\n" +
		"  <emails>\n" +
		"    <email id=\"2\">\n" +
		"      <attribute type=\"1\">{{uid}}@market.test.yandex.net</attribute>\n" +
		"    </email>\n" +
		"  </emails>\n" +
		"  <address-list>\n" +
		"    <address validated=\"1\" default=\"1\" rpop=\"0\" unsafe=\"0\" native=\"1\" born-date=\"2011-11-16 00:00:00\">" +
		"{{uid}}@market.test.yandex.net" +
		"</address>\n" +
		"  </address-list>\n" +
		"</doc>\n\n"

	jsonTemplate = "{" +
		"\n  \"users\": [" +
		"\n  {" +
		"\n    \"id\":\"{{uid}}\"," +
		"\n    \"uid\":" +
		"\n    {" +
		"\n      \"value\":\"{{uid}}\"," +
		"\n      \"hosted\":false," +
		"\n      \"domid\":\"\"," +
		"\n      \"domain\":\"\"," +
		"\n      \"mx\":\"\"," +
		"\n      \"domain_ena\":\"\"," +
		"\n      \"catch_all\":\"\"" +
		"\n    }," +
		"\n    \"login\":\"test{{uid}}\"," +
		"\n    \"aliases\":" +
		"\n    {}," +
		"\n    \"karma\":" +
		"\n    {" +
		"\n      \"value\":0" +
		"\n    }," +
		"\n    \"karma_status\":" +
		"\n    {" +
		"\n      \"value\":0" +
		"\n    }," +
		"\n    \"regname\":\"test{{uid}}\"," +
		"\n    \"display_name\":" +
		"\n    {" +
		"\n      \"name\":\"{{uid}}\"," +
		"\n      \"public_name\":\"{{uid}}\"," +
		"\n      \"avatar\":" +
		"\n      {" +
		"\n        \"default\":\"0/0-0\"," +
		"\n        \"empty\":true" +
		"\n      }" +
		"\n    }," +
		"\n    \"attributes\" : {}," +
		"\n    \"phones\" : []," +
		"\n    \"emails\" : [" +
		"\n    {" +
		"\n      \"id\" : \"2\"," +
		"\n      \"attributes\" : {" +
		"\n         \"1\" : \"{{uid}}@market.test.yandex.net\"" +
		"\n      }" +
		"\n    }" +
		"\n    ]," +
		"\n    \"address-list\": [" +
		"\n    {" +
		"\n      \"address\":\"{{uid}}@market.test.yandex.net\"," +
		"\n      \"validated\":true," +
		"\n      \"default\":true," +
		"\n      \"rpop\":false," +
		"\n      \"unsafe\":false," +
		"\n      \"native\":true," +
		"\n      \"born-date\":\"2011-11-16 00:00:00\"" +
		"\n    }" +
		"\n    ]" +
		"\n  }" +
		"\n  ]" +
		"\n}\n"
)

var (
	xmlParts  = strings.Split(xmlTemplate, "{{uid}}")
	jsonParts = strings.Split(jsonTemplate, "{{uid}}")
)

func userInfo(ctx apphost.Context, query url.Values) error {
	uidStr := query.Get("uid")

	uid, err := parseUID(uidStr)
	if err != nil {
		return redirectToRealBlackbox(ctx)
	}
	if isLoadUID(uid) {
		format := query.Get("format")
		if len(format) == 0 {
			return writeResponse(ctx, buildXMLResponse(uidStr))

		} else if format == "json" {
			return writeResponse(ctx, buildJSONResponse(uidStr))

		} else {
			return writeResponse(ctx, getBadRequest())
		}
	}
	return redirectToRealBlackbox(ctx)
}

func writeResponse(ctx apphost.Context, response *protoanswers.THttpResponse) error {
	return ctx.AddPB("mock-response", response)
}

func redirectToRealBlackbox(ctx apphost.Context) error {
	return ctx.AddFlag("pass-through")
}

func getBadRequest() *protoanswers.THttpResponse {
	return &badRequest
}

func buildXMLResponse(uid string) *protoanswers.THttpResponse {
	response := []byte(strings.Join(xmlParts, uid))
	return &protoanswers.THttpResponse{
		StatusCode:    200,
		Headers:       xmlHeaders,
		Content:       response,
		IsSdchEncoded: false,
		FromHttpProxy: false,
	}
}

func buildJSONResponse(uid string) *protoanswers.THttpResponse {
	response := []byte(strings.Join(jsonParts, uid))
	return &protoanswers.THttpResponse{
		StatusCode:    200,
		Headers:       jsonHeaders,
		Content:       response,
		IsSdchEncoded: false,
		FromHttpProxy: false,
	}
}
