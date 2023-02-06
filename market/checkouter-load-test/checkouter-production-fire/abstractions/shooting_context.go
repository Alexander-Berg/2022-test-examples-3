package abstractions

import (
	"context"
	"github.com/yandex/pandora/core"
	"github.com/yandex/pandora/core/aggregator/netsample"
	"go.uber.org/zap"
)

type ShootContext interface {
	GetLogFields() []zap.Field
	GetContext() context.Context
	GetLogger() *zap.Logger
	WithContext(ctx context.Context) ShootContext
	WithLogCustomization(customizer func(logger *zap.Logger) *zap.Logger) ShootContext
	Measure(sample core.Sample)
	Acquire(tag string) *netsample.Sample
}
