package model

import (
	"fmt"
	"net/http"
	"net/url"
	"reflect"
	"regexp"
	"rex/common/types"
	"testing"

	yaml "gopkg.in/yaml.v2"
)

var statusCodeRangeOK = statusCodeRanges["2xx"]

var HTTPResponseYAMLTests = []struct {
	YAML         string
	HTTPResponse HTTPResponse
}{
	{
		`status_code: 200`,
		HTTPResponse{StatusCode: StatusCode{N: 200}},
	},
	{
		`status_code: '200'`,
		HTTPResponse{StatusCode: StatusCode{N: 200}},
	},
	{
		`status_code: 2xx`,
		HTTPResponse{StatusCode: StatusCode{Range: &statusCodeRangeOK}},
	},
}

func TestHTTPResponse_UnmarshalYAML(t *testing.T) {
	for _, tt := range HTTPResponseYAMLTests {
		var r HTTPResponse
		if err := yaml.Unmarshal([]byte(tt.YAML), &r); err != nil {
			t.Fatal(err)
		}
		if !reflect.DeepEqual(r, tt.HTTPResponse) {
			t.Fatalf("want %s, got %s", tt.HTTPResponse, r)
		}
	}
}

var StatusCodeMatchTests = []struct {
	StatusCode StatusCode
	Code       int
	OK         bool
}{
	// By default status code matches every code.
	{
		StatusCode{},
		http.StatusOK,
		true,
	},
	{
		StatusCode{},
		http.StatusFound,
		true,
	},
	{
		StatusCode{},
		http.StatusNotFound,
		false,
	},
	// When N is set status code matches passed code strictly.
	{
		StatusCode{N: http.StatusOK},
		http.StatusOK,
		true,
	},
	{
		StatusCode{N: http.StatusOK},
		http.StatusCreated,
		false,
	},
}

func TestStatusCode_Match(t *testing.T) {
	for _, tt := range StatusCodeMatchTests {
		if ok := tt.StatusCode.Match(tt.Code); ok != tt.OK {
			t.Errorf("%s & %d: want %t, got %t", tt.StatusCode, tt.Code, tt.OK, ok)
		}
	}
}

var HTTPRequestNewRequestTests = []struct {
	Request HTTPRequest
	MustErr bool
}{
	{
		HTTPRequest{URL: "https://yandex.ru/?lr=213"},
		false,
	},
	{
		HTTPRequest{URL: "/not/absolute/url"},
		true,
	},
}

func TestRequest(t *testing.T) {
	for _, tt := range HTTPRequestNewRequestTests {
		tt := tt
		t.Run(fmt.Sprint(tt.Request), func(t *testing.T) {
			r, err := (&HTTP{Request: tt.Request}).NewRequest()
			if err != nil && !tt.MustErr {
				t.Fatal(err)
			} else if err != nil && tt.MustErr {
				return
			} else if err == nil && tt.MustErr {
				t.Fatalf("want error, got <nil>")
			}

			u, err := url.Parse(tt.Request.URL)
			if err != nil {
				t.Fatal(err)
			}

			if !reflect.DeepEqual(r.URL, u) {
				t.Errorf("url: want %s, got %s", u, r.URL)
			}
		})
	}
}

var HTTPRequestUnmarshalYAMLTests = []struct {
	YAML string
	HTTP HTTPRequest
}{
	{
		`url: https://yandex.ru`,
		HTTPRequest{
			URL:             "https://yandex.ru",
			FollowRedirects: types.TrueByDefault{},
		},
	},
	{
		`url: https://yandex.ru
follow_redirects: true
`,
		HTTPRequest{
			URL:             "https://yandex.ru",
			FollowRedirects: types.TrueByDefault{types.True},
		},
	},
	{
		`url: https://yandex.ru
follow_redirects: false
`,
		HTTPRequest{
			URL:             "https://yandex.ru",
			FollowRedirects: types.TrueByDefault{types.False},
		},
	},
}

func TestHTTPRequest_UnmarshalYAML(t *testing.T) {
	for _, tt := range HTTPRequestUnmarshalYAMLTests {
		var req HTTPRequest
		if err := yaml.Unmarshal([]byte(tt.YAML), &req); err != nil {
			t.Fatal(err)
		}
		if !reflect.DeepEqual(req, tt.HTTP) {
			t.Fatalf("want %v, got %v", tt.HTTP, req)
		}
	}
}

var URLMatchTests = []struct {
	URL URL
	Raw string
	OK  bool
}{
	{
		URL{S: "https://yandex.ru/images"},
		"https://yandex.ru/images/",
		true,
	},
	{
		URL{S: "https://yandex.ru/showcaptcha"},
		"https://yandex.ru/showcaptcha?some=params&that=does&not=influence",
		true,
	},
	{
		URL{Re: regexp.MustCompile("showcaptcha")},
		"https://yandex.ru/showcaptcha?some=params&that=does&not=influence",
		true,
	},
	// Handle escaped paths.
	{
		URL{S: "https://yandex.ru/video/запрос/сериалы"},
		"https://yandex.ru/video/%D0%B7%D0%B0%D0%BF%D1%80%D0%BE%D1%81/%D1%81%D0%B5%D1%80%D0%B8%D0%B0%D0%BB%D1%8B",
		true,
	},
	{
		URL{Re: regexp.MustCompile("^https://yandex.ru/video/.*/сериалы")},
		"https://yandex.ru/video/%D0%B7%D0%B0%D0%BF%D1%80%D0%BE%D1%81/%D1%81%D0%B5%D1%80%D0%B8%D0%B0%D0%BB%D1%8B",
		true,
	},
	{
		URL{Re: regexp.MustCompile(`^https://yandex.ru/search\?text=test`)},
		"https://yandex.ru/search?text=test&something=else",
		true,
	},
}

func TestURL_Match(t *testing.T) {
	for _, tt := range URLMatchTests {
		if ok := tt.URL.Match(tt.Raw); ok != tt.OK {
			t.Errorf("%v: want %v, got %v", tt.URL, tt.OK, ok)
		}
	}
}

var HTTPResponseMatchTests = []struct {
	HTTPResponse HTTPResponse
	Response     http.Response
	OK           bool
}{
	// allow 3xx status when location matches
	{
		HTTPResponse{
			Location: &URL{S: "http://yandex.ru"},
		},
		http.Response{
			StatusCode: http.StatusFound,
			Header: http.Header{
				"Location": {"http://yandex.ru/?some=params"},
			},
			Request: &http.Request{
				URL: MustParseURL("http://yandex.ru"),
			},
		},
		true,
	},
}

func MustParseURL(rawurl string) *url.URL {
	u, err := url.Parse(rawurl)
	if err != nil {
		panic(err)
	}
	return u
}

func TestHTTPResponse_Match(t *testing.T) {
	for _, tt := range HTTPResponseMatchTests {
		err := tt.HTTPResponse.Match(&tt.Response)
		if ok := (err == nil); ok != tt.OK {
			t.Fatalf("%s: %s", tt.HTTPResponse, err)
		}
	}
}
