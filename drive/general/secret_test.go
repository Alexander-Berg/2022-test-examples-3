package secret

import (
	"encoding/json"
	"fmt"
	"math/rand"
	"os"
	"testing"
)

func TestSimpleSecret(t *testing.T) {
	var sec1 Secret
	if err := json.Unmarshal([]byte(`"test"`), &sec1); err != nil {
		t.Fatal("Error:", err)
	}
	if val := sec1.Secret(); val != "test" {
		t.Fatalf("Expected %q, got %q", "test", val)
	}
}

func TestEnvSecret(t *testing.T) {
	envName := fmt.Sprintf("TEST_SECRET_%d", rand.Int())
	if err := os.Setenv(envName, "test"); err != nil {
		t.Fatal("Error:", err)
	}
	defer func() {
		_ = os.Unsetenv(envName)
	}()
	var sec1 Secret
	if err := json.Unmarshal(
		[]byte(fmt.Sprintf(`{"type": "env", "name": %q}`, envName)),
		&sec1,
	); err != nil {
		t.Fatal("Error:", err)
	}
	if val := sec1.Secret(); val != "test" {
		t.Fatalf("Expected %q, got %q", "test", val)
	}
	var sec2 Secret
	if err := json.Unmarshal(
		[]byte(fmt.Sprintf(`{"type": "environ", "name": %q}`, envName)),
		&sec2,
	); err != nil {
		t.Fatal("Error:", err)
	}
	if val := sec2.Secret(); val != "test" {
		t.Fatalf("Expected %q, got %q", "test", val)
	}
}
