package antirobot

import (
	"net/http"
	"testing"
)

func TestSpravkaApply(t *testing.T) {
	r, _ := http.NewRequest("GET", "https://yandex.ru", nil)
	if s := r.Header.Get("Cookie"); s != "" {
		t.Errorf("want %q, got %q", "", s)
	}

	// Set cookie value.
	Cookie([]byte("test")).ApplyTo(r)
	if s := r.Header.Get("Cookie"); s != "spravka=test" {
		t.Errorf("want %q, got %q", "spravka=test", s)
	}

	// Update cookie value.
	Cookie([]byte("test2")).ApplyTo(r)
	if s := r.Header.Get("Cookie"); s != "spravka=test2" {
		t.Errorf("want %q, got %q", "spravka=test2", s)
	}
}
