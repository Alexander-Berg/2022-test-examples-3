package server

import (
	"testing"

	"github.com/labstack/echo/v4"
	"github.com/stretchr/testify/assert"
)

func TestPanicLoggerMiddleware(t *testing.T) {
	e := echo.New()
	ctx := e.AcquireContext()
	misbehavedFunction := func(c echo.Context) error {
		s := make([]int, 0)
		return c.JSON(200, s[0])
	}
	err := PanicLoggerMiddleware(misbehavedFunction)(ctx)
	e.ReleaseContext(ctx)
	assert.Error(t, err)
}
