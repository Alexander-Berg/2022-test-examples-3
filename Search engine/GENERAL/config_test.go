package config

import (
	"fmt"
	"testing"

	yaml "gopkg.in/yaml.v2"
)

var MetaStringTests = []struct {
	Meta Meta
	S    string
}{
	{nil, ""},
	{map[string]string{}, ""},
	{map[string]string{"a": "b"}, "a:b"},
	{map[string]string{"a": "b", "c": "d"}, "a:b c:d"},
	{[]string{}, ""},
	{[]string{"a", "b", "c"}, "a b c"},
}

func TestMetaString(t *testing.T) {
	for _, tt := range MetaStringTests {
		if s := MetaString(tt.Meta); s != tt.S {
			t.Errorf("%v: want %q, got %q", tt.Meta, tt.S, s)
		}
	}
}

var MetaConversionTests = []struct {
	YAML string
	S    string
}{
	{
		"- a\n- b",
		"a b",
	},
}

func TestMeta_convertsYAMLtoString(t *testing.T) {
	for _, tt := range MetaConversionTests {
		t.Run(fmt.Sprintf("%#v", tt.YAML), func(t *testing.T) {
			var m Meta
			if err := yaml.Unmarshal([]byte(tt.YAML), &m); err != nil {
				t.Fatal(err)
			}
			if s := MetaString(m); s != tt.S {
				t.Errorf("want %q, got %q", tt.S, s)
			}
		})
	}
}

var WebListenTests = []struct {
	ListenAddress string
	OK            bool
}{
	{"", false},
	{"no", false},
	{"off", false},
	{"localhost:", true},
	{":8990", true},
}

func TestWeb_Listen(t *testing.T) {
	for _, tt := range WebListenTests {
		w := WebConfig{ListenAddress: tt.ListenAddress}
		if ok := w.Listen(); ok != tt.OK {
			t.Errorf("%v: want %v, got %v", tt.ListenAddress, tt.OK, ok)
		}
	}
}

var FileSizeTests = []struct {
	YAML  string
	Bytes int64
}{
	{"", 0},
	{"1", 1},
	{"1kb", 1024},
	{"1MB", 1024 * 1024},
	{"1gB", 1024 * 1024 * 1024},
}

func TestFileSize_UnmarshalYAML(t *testing.T) {
	for _, tt := range FileSizeTests {
		var n Bytes
		err := yaml.Unmarshal([]byte(tt.YAML), &n)
		if err != nil {
			t.Fatal(err)
		}
		if n != Bytes(tt.Bytes) {
			t.Fatalf("want %v, got %v", tt.Bytes, n)
		}
	}
}
