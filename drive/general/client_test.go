package solomon

import (
	"testing"
)

func TestSolomon(t *testing.T) {
	client, err := NewClient("test_project", "test_cluster", "test_service")
	if err != nil {
		t.Fatal("Error:", err)
	}
	defer client.Close()
}
