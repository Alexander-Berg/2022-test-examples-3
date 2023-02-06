package model

import "testing"

func TestRandomID(t *testing.T) {
	a := RandomID()
	b := RandomID()
	if a == b {
		t.Fatal("not random")
	}
}

var ShortenHostnameTests = []struct {
	Hostname string
	Short    string
}{
	{
		"vscale-msk.extmon.yandex.net",
		"vscale-msk",
	},
	{
		"localhost",
		"localhost",
	},
	{
		"127.0.0.1",
		"127.0.0.1",
	},
}

func TestShortenHostname(t *testing.T) {
	for _, tt := range ShortenHostnameTests {
		name := ShortenHostname(tt.Hostname)
		if name != tt.Short {
			t.Errorf("%v: want %q, got %q", tt.Hostname, tt.Short, name)
		}
	}
}
