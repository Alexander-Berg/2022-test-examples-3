package vars

import (
	"io/ioutil"
	"os"
	"reflect"
	"testing"
)

var YAMLReadTests = []struct {
	Filename string
	Vars     Vars
	Err      bool
}{
	{
		Filename: "testdata/test.yml",
		Vars: New(map[string]string{
			"a": "b",
			"c": "123",
		}),
	},
	{
		Filename: "testdata/does-not-exist",
		Vars:     New(nil),
	},
	{
		Filename: "testdata/broken-link",
		Vars:     New(nil),
		Err:      true,
	},
}

func TestYAML_Read(t *testing.T) {
	for _, tt := range YAMLReadTests {
		v, err := YAML(tt.Filename).Read()
		if !tt.Err && err != nil {
			t.Fatalf("got error: %s", err)
		}
		if !reflect.DeepEqual(tt.Vars, v) {
			t.Errorf("want %v, got %v", tt.Vars, v)
		}
	}
}

func TestYAML_Write(t *testing.T) {
	f, err := ioutil.TempFile("", "YAML_Store")
	if err != nil {
		t.Fatal(err)
	}
	defer os.Remove(f.Name())

	vars := New(map[string]string{
		"a": "b",
		"c": "123",
	})

	i := YAML(f.Name())
	err = i.Write(vars)
	if err != nil {
		t.Fatal(err)
	}

	v, err := i.Read()
	if err != nil {
		t.Fatal(err)
	}
	if !reflect.DeepEqual(vars, v) {
		t.Errorf("want %v, got %v", vars, v)
	}
}

func TestYAML_Write_emptyVarsWithoutFileCreation(t *testing.T) {
	f, err := ioutil.TempFile("", "YAML_Store")
	if err != nil {
		t.Fatal(err)
	}
	filename := f.Name()
	// Перед использованием файл удаляется, чтобы проверять,
	// что он не создаётся вновь.
	os.Remove(filename)

	empty := New(nil)
	err = YAML(filename).Write(empty)
	if err != nil {
		t.Error(err)
	}
	_, err = os.Stat(filename)
	if !os.IsNotExist(err) {
		t.Fatal("file must not be created")
	}
}
