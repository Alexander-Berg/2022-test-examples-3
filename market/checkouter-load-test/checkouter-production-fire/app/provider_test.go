package main

import (
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/util"
	"context"
	"github.com/stretchr/testify/require"
	"github.com/yandex/pandora/core"
	"go.uber.org/zap"
	"math/rand"
	"testing"
)

type randFunc func(int) int

//goland:noinspection SpellCheckingInspection
func (f randFunc) Intn(n int) int {
	return f(n)
}

var globalRand = randFunc(rand.Intn)

//Для проверки random
const count = 100

func repeat(n int, f func()) {
	for i := 0; i < n; i++ {
		f()
	}
}

func TestMultiUIDRange(t *testing.T) {
	repeat(count, func() {
		rng := util.UIDRange{From: 2, To: 3}
		require.Contains(t, []int{2, 3}, rng.GetUID(globalRand))
	})
}

func TestSingleUIDRange(t *testing.T) {
	repeat(count, func() {
		rng := util.UIDRange{From: 1, To: 1}
		require.Equal(t, 1, rng.GetUID(globalRand))
	})

}

func TestEmptyUIDRange(t *testing.T) {
	defer func() {
		if r := recover(); r != nil {
			require.Equal(t, "Invalid UIDRange", r)
		}
	}()

	rng := util.UIDRange{From: 0, To: -1}
	rng.GetUID(globalRand)
	require.Fail(t, "Method should panic")
}

func TestMultiUIDArray(t *testing.T) {
	repeat(100, func() {
		arr := util.UIDArray{3, 5, 7}
		require.Contains(t, []int{3, 5, 7}, arr.GetUID(globalRand))
	})
}

func TestSingleUIDArray(t *testing.T) {
	repeat(100, func() {
		arr := util.UIDArray{6}
		require.Contains(t, []int{6}, arr.GetUID(globalRand))
	})
}

func TestEmptyUIDArray(t *testing.T) {
	defer func() {
		if r := recover(); r != nil {
			require.Equal(t, "Invalid UIDArray", r)
		}
	}()

	rng := util.UIDArray{}
	rng.GetUID(globalRand)
	require.Fail(t, "Method should panic")
}

func TestProviderConfigOnlySingle(t *testing.T) {
	c := CustomAmmoProviderConfig{
		ReadonlyUIDs: util.UIDArray{},
	}

	require.Nil(t, c.getReadonlyRange())

}

func TestProviderConfigOnlyRange(t *testing.T) {
	c := CustomAmmoProviderConfig{
		ReadonlyUIDs: util.UIDArray{},
	}

	require.Nil(t, c.getReadonlyRange())

}

func TestProviderConfigSingleWithReadonly(t *testing.T) {
	c := CustomAmmoProviderConfig{
		ReadonlyUIDs: util.UIDArray{3, 5, 7},
	}

	require.Equal(t, util.UIDArray{3, 5, 7}, c.getReadonlyRange())

}

func TestProviderConfigRangeWithReadonly(t *testing.T) {
	c := CustomAmmoProviderConfig{
		ReadonlyUIDs: util.UIDArray{3, 5, 7},
	}

	require.Equal(t, util.UIDArray{3, 5, 7}, c.getReadonlyRange())

}

func TestProviderWithoutReadonly(t *testing.T) {
	c := CustomAmmoProviderConfig{
		ReadonlyUIDs: util.UIDArray{},
	}
	p := CustomAmmoProviderInit(c)
	go func() {
		err := p.Run(context.Background(), core.ProviderDeps{Log: zap.NewNop()})
		if err != nil {
			panic(err)
		}
	}()
	repeat(count, func() {
		ammo := <-p.sink
		uid := ammo.UID
		require.LessOrEqual(t, util.MinUID, ammo.UID)
		require.LessOrEqual(t, ammo.UID, util.MaxUID)
		require.Equal(t, uid, ammo.ReadonlyUID)
	})

}

func TestProviderWithReadonly(t *testing.T) {
	c := CustomAmmoProviderConfig{
		ReadonlyUIDs: util.UIDArray{3, 5, 7},
	}
	p := CustomAmmoProviderInit(c)
	go func() {
		err := p.Run(context.Background(), core.ProviderDeps{Log: zap.NewNop()})
		if err != nil {
			panic(err)
		}
	}()
	repeat(count, func() {
		ammo := <-p.sink
		require.LessOrEqual(t, util.MinUID, ammo.UID)
		require.LessOrEqual(t, ammo.UID, util.MaxUID)
		require.Contains(t, []int{3, 5, 7}, ammo.ReadonlyUID)
	})

}
