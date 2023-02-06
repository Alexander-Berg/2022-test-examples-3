package worker

import (
	"reflect"
	"testing"
)

var testOptions = TaskOptions{
	"@layers": "sandbox:64323525\n" +
		"sandbox:53535345\n\n" +
		"sandbox:54353533\r\n" +
		"sandbox:35432532\r\n\r\n" +
		"sandbox://14545745",
	"@environ": "TEST_1=helloworld123\n" +
		"TEST_2=testtesttest\n\n" +
		"TEST_3=1234567\r\n" +
		"TEST_4=\r\n\r\n" +
		"TEST_5=",
	"@work_dir": "/home/test",
	"@command":  "python3 test.py",
	"@files": "sandbox:12345 => binary:+x\n" +
		"sandbox:65455 => cfg.json\n\r\n" +
		"sandbox://1235=>data.json\n\r" +
		"arcadia:test.txt => test.txt\n\r" +
		"option:test_option => test_option.txt",
	"test_option": "test_option value",
}

func TestLayersOption(t *testing.T) {
	layers, err := extractLayersOption(testOptions)
	if err != nil {
		t.Fatal("Error:", err)
	}
	expectedLayers := []FileURL{
		{"sandbox", "64323525"},
		{"sandbox", "53535345"},
		{"sandbox", "54353533"},
		{"sandbox", "35432532"},
		{"sandbox", "14545745"},
	}
	if !reflect.DeepEqual(layers, expectedLayers) {
		t.Fatalf("Expected %v, got: %v", expectedLayers, layers)
	}
}

func TestWorkDirOption(t *testing.T) {
	workDir, err := extractWorkDirOption(testOptions)
	if err != nil {
		t.Fatal("Error:", err)
	}
	expectedWorkDir := "/home/test"
	if !reflect.DeepEqual(workDir, expectedWorkDir) {
		t.Fatalf("Expected %v, got: %v", expectedWorkDir, workDir)
	}
	empty, err := extractWorkDirOption(TaskOptions{})
	if err != nil {
		t.Fatal("Error:", err)
	}
	if !reflect.DeepEqual(empty, "/") {
		t.Fatalf("Expected %v, got: %v", "/", empty)
	}
	emptyWorkDir, err := extractWorkDirOption(TaskOptions{
		"@work_dir": "",
		"@layers":   "sandbox:12345",
	})
	if err != nil {
		t.Fatal("Error:", err)
	}
	if !reflect.DeepEqual(emptyWorkDir, "/root") {
		t.Fatalf("Expected %v, got: %v", "/root", emptyWorkDir)
	}
}

func TestEnvironOption(t *testing.T) {
	environ, err := extractEnvironOption(testOptions)
	if err != nil {
		t.Fatal("Error:", err)
	}
	expectedEnviron := []string{
		"TEST_1=helloworld123",
		"TEST_2=testtesttest",
		"TEST_3=1234567",
		"TEST_4=",
		"TEST_5=",
	}
	if !reflect.DeepEqual(environ, expectedEnviron) {
		t.Fatalf("Expected %v, got: %v", expectedEnviron, environ)
	}
}

func TestCommandOption(t *testing.T) {
	command, err := extractCommandOption(testOptions)
	if err != nil {
		t.Fatal("Error:", err)
	}
	expectedCommand := "python3 test.py"
	if !reflect.DeepEqual(command, expectedCommand) {
		t.Fatalf("Expected %v, got: %v", expectedCommand, command)
	}
}

func TestFilesOption(t *testing.T) {
	files, err := extractFilesOption(testOptions)
	if err != nil {
		t.Fatal("Error:", err)
	}
	expectedFiles := []localFileURL{
		{FileURL{"sandbox", "12345"}, "binary", 0777},
		{FileURL{"sandbox", "65455"}, "cfg.json", 0666},
		{FileURL{"sandbox", "1235"}, "data.json", 0666},
		{FileURL{"arcadia", "test.txt"}, "test.txt", 0666},
		{FileURL{"option", "test_option"}, "test_option.txt", 0666},
	}
	if !reflect.DeepEqual(files, expectedFiles) {
		t.Fatalf("Expected %v, got: %v", expectedFiles, files)
	}
}
