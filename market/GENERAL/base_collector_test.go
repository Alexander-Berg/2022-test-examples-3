package runner

import (
	"context"
)

type TestCollector struct {
	collectFunc func(ctx context.Context, pool *PoolGroup)
}

func (t *TestCollector) Collect(ctx context.Context, pool *PoolGroup) {
	t.collectFunc(ctx, pool)
}

func (t *TestCollector) Init(*Runner) Collector {
	return t
}

func (t *TestCollector) SetCollectFunc(f func(ctx context.Context, pool *PoolGroup)) Collector {
	t.collectFunc = f
	return t
}
