package ctxutil

import (
	// "a.yandex-team.ru/mail/payments-sdk-backend/internal/utils/ctxutil"
	"context"
	"github.com/stretchr/testify/require"
	"testing"
)

func TestCtxUtil_ForceCvv(t *testing.T) {
	tests := []struct {
		name     string
		header   string
		expected bool
	}{
		{
			name:     "No_Header",
			header:   "",
			expected: false,
		},
		{
			name:     "Header_1",
			header:   "1",
			expected: true,
		},
		{
			name:     "Header_Not_1",
			header:   "2",
			expected: false,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			context := context.Background()
			ctx := WithForceCvv(context, test.header)
			actual := GetForceCvv(ctx)
			require.Equal(t, test.expected, actual)
		})
	}
}
