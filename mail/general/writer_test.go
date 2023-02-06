package logger

import (
	"testing"
)

func checkChan(t *testing.T) {
	for c := range writer.channel {
		t.Errorf("should not log events with level %v < logger level debug", c.LogLevel)
	}
}

func TestLog(t *testing.T) {
	writer = new(Writer)
	writer.logLevel = InfoLevel
	writer.channel = make(chan TSKV)
	event := TSKV{LogLevel: DebugLevel}
	go checkChan(t)
	log(event)
}

func checkDrainChan(t *testing.T) {
	for c := range writer.channel {
		if c.Reason == "test" {
			return
		}
	}
	t.Error("test event did not drain from channel")
}

func TestFinalize(t *testing.T) {
	writer.channel = make(chan TSKV, BufferSize)
	go checkDrainChan(t)
	writer.channel <- TSKV{Reason: "test"}
	writer.finalize()
}
