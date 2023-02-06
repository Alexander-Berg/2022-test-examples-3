package config

import (
	"reflect"
	"testing"
)

func noSecret(s string) (Secret, error) {
	return &secret{value: s, secret: s}, nil
}

var SecretsTests = []struct {
	M      map[string]interface{}
	Field  string
	Func   SecretFunc
	Unsafe []string
	Err    bool
}{
	{
		nil,
		"test",
		noSecret,
		nil,
		false,
	},
	{
		map[string]interface{}{},
		"test",
		noSecret,
		nil,
		true,
	},
	{
		map[string]interface{}{
			"test": []interface{}{"abc", 123, 123.4},
		},
		"test",
		noSecret,
		[]string{"abc", "123", "123.4"},
		false,
	},
}

func TestSecrets(t *testing.T) {
	for _, tt := range SecretsTests {
		v, err := Secrets(tt.M, tt.Field, tt.Func)
		if err != nil && !tt.Err {
			t.Fatal(err)
		}
		if tt.Err {
			continue
		}
		if _, ok := tt.M[tt.Field]; ok {
			t.Fatal("value must be deleted")
		}
		unsafe := Unsafe(v)
		if !reflect.DeepEqual(tt.Unsafe, unsafe) {
			t.Errorf("want %v, got %v", tt.Unsafe, unsafe)
		}
	}
}

var PhoneUnsafeTests = []struct {
	S   string
	Res string
}{
	{"+7 123 456 7890", "+71234567890"},
	{"+7 123 456-7890", "+71234567890"},
	{"+7-123-456-7890", "+71234567890"},
	{"7-123-456-7890", "71234567890"},
}

func TestPhone_Unsafe(t *testing.T) {
	for _, tt := range PhoneUnsafeTests {
		if s, _ := Phone(tt.S); s.Unsafe() != tt.Res {
			t.Errorf("%q: want %q, got %q", tt.S, tt.Res, s.Unsafe())
		}
	}
}

var PhoneStringTests = []struct {
	S   string
	Res string
}{
	{"", ""},
	{"7", "7"},
	{"71", "71"},
	{"712", "71*"},
	{"+71234567890", "+7123****890"},
}

func TestPhone_String(t *testing.T) {
	for _, tt := range PhoneStringTests {
		if s, _ := Phone(tt.S); s.String() != tt.Res {
			t.Errorf("%q: want %q, got %q", tt.S, tt.Res, s.String())
		}
	}
}

var EmailStringTests = []struct {
	S   string
	Res string
}{
	// TODO
}

func TestEmail_String(t *testing.T) {
	for _, tt := range EmailStringTests {
		if s, _ := Email(tt.S); s.String() != tt.Res {
			t.Errorf("%q: want %q, got %q", tt.S, tt.Res, s.String())
		}
	}
}
