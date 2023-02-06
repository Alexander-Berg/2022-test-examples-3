package antirobot

import (
	"errors"
	"fmt"
	"net/http"
	"net/http/httptest"
	"net/url"
	"sync/atomic"
	"testing"
)

const testCaptchaURL = "https://yandex.ru/showcaptcha"

type testReader struct{ counter int64 }

func (s *testReader) ReadFor(_ string) (Spravka, error) { return s.Read() }

func (s *testReader) Read() (Spravka, error) {
	s.counter++
	str := fmt.Sprintf("test%d", s.counter)
	return Cookie([]byte(str)), nil
}

func TestNewClient(t *testing.T) {
	assertCookie := func(t *testing.T, r *http.Request, value string) {
		c, err := r.Cookie(cookieName)
		if err != nil {
			t.Errorf("got no cookie")
			return
		}
		if s := value; s != c.Value {
			t.Errorf("want %q, got %q", s, c.Value)
		}
	}

	var counter int64
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		defer atomic.AddInt64(&counter, 1)
		switch counter {
		case 0:
			// Сделать редирект на первом запросе, чтобы клиент
			// запросил вторую куку.
			assertCookie(t, r, "test1")
			http.Redirect(w, r, testCaptchaURL, 302)
		case 1:
			assertCookie(t, r, "test2")
			w.WriteHeader(404)
		}
	}))
	defer ts.Close()

	client := NewClient(http.DefaultClient, new(testReader))
	req, _ := http.NewRequest("GET", ts.URL, nil)

	resp, err := client.Do(req)
	if err != nil {
		t.Errorf("got error: %s", err)
	}
	if n := 404; n != resp.StatusCode {
		t.Errorf("want %d, got %d", n, resp.StatusCode)
	}
}

var IsCaptchaTests = []struct {
	URL string
	OK  bool
}{
	{"http://yandex.ru", false},
	{"https://yandex.ru", false},
	// Require path
	{"http://yandex.ru/showcaptcha", true},
	{"https://yandex.ru/showcaptcha", true},
	// Allow subdomains
	{"http://news.yandex.ru/showcaptcha", true},
	{"https://news.yandex.ru/showcaptcha", true},
	{"http://news.yandex.com.tr/showcaptcha", true},
	{"https://news.yandex.com.tr/showcaptcha", true},
	// Wrong path
	{"http://yandex.ru/search", false},
	{"https://yandex.ru/search", false},
	// Wrong host
	{"http://notyandex.ru/showcaptcha", false},
	{"https://notyandex.ru/showcaptcha", false},
	{"https://yandex.fake.ru/showcaptcha", false},
	{"https://hack.yandex.fake.ru/showcaptcha", false},
	// Wrong host & path
	{"https://notyandex.ru/search", false},
	{"https://yandex.fake.ru/search", false},
	{"https://hack.yandex.fake.ru/search", false},
}

func TestIsCaptcha(t *testing.T) {
	for _, tt := range IsCaptchaTests {
		t.Run(tt.URL, func(t *testing.T) {
			u, err := url.Parse(tt.URL)
			if err != nil {
				t.Fatal(err)
			}
			if ok := IsCaptcha(u); ok != tt.OK {
				t.Errorf("want %v, got %v", tt.OK, ok)
			}
		})
	}
}

func TestIsCaptchaError(t *testing.T) {
	{
		err := errors.New("not")
		if ok := IsCaptchaError(err); ok {
			t.Error(ok)
		}
	}
	{
		err := &CaptchaError{"yes"}
		if ok := IsCaptchaError(err); !ok {
			t.Error(ok)
		}
	}
	{
		var err error
		err = &CaptchaError{"yes"}
		err = errors.New(err.Error())
		if ok := IsCaptchaError(err); !ok {
			t.Error(ok)
		}
	}
}
