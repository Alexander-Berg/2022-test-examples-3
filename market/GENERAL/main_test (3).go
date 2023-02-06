package main

import (
	"errors"
	"strings"
	"testing"
)

type FakeRunner struct{}

var (
	anonUsageOutput             = []byte("5000000000\n")
	anonUsageErr          error = nil
	memoryGuaranteeOutput       = []byte("10000000000\n")
	memoryGuaranteeErr    error = nil
	fakeRunnerCalls       [][]string
)

func (r FakeRunner) GetOutput(command string, args ...string) ([]byte, error) {
	call := make([]string, len(args)+1)
	call[0] = command
	for index, arg := range args {
		call[index+1] = arg
	}

	fakeRunnerCalls = append(fakeRunnerCalls, call)

	for _, arg := range args {
		if arg == "anon_usage" {
			return anonUsageOutput, anonUsageErr
		}

		if arg == "memory_guarantee" {
			return memoryGuaranteeOutput, memoryGuaranteeErr
		}
	}

	return nil, errors.New("unknown command")
}

func (r FakeRunner) GetFileProp(path string) ([]byte, error) {
	if path == "/sys/fs/cgroup/memory/memory.limit_in_bytes" {
		return memoryGuaranteeOutput, memoryGuaranteeErr
	}
	return nil, errors.New("unknown path")
}

func prepare() {
	fakeRunnerCalls = make([][]string, 0)
	runner = FakeRunner{}
}

func Test_RunCheck(t *testing.T) {
	prepare()

	anonUsageOutput = []byte("5000000000\n")
	runPositiveCheckTest(t, OK, "OK")

	anonUsageOutput = []byte("6000000000\n")
	runPositiveCheckTest(t, WARN, "60%")

	anonUsageOutput = []byte("7900000000\n")
	runPositiveCheckTest(t, WARN, "79%")

	anonUsageOutput = []byte("8000000000\n")
	runPositiveCheckTest(t, CRIT, "80%")

	anonUsageOutput = []byte("0\n")
	memoryGuaranteeOutput = []byte("0\n")
	runPositiveCheckTest(t, CRIT, "100%")
}

func runPositiveCheckTest(
	t *testing.T,
	expectedStatus Status,
	descriptionPart string,
) {
	status, description := RunCheck(60, 80)
	if status != expectedStatus {
		t.Errorf("Status should be %v, got %v", status, expectedStatus)
	}
	if !strings.Contains(description, descriptionPart) {
		t.Errorf("Description should contain \"%v\", got %v", descriptionPart, description)
	}
}
