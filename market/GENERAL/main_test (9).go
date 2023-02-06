package main

import (
	"context"
	"fmt"
	"io"
	"math/rand"
	"os"
	"sort"
	"testing"
	"time"

	"a.yandex-team.ru/market/sre/tools/watcher/src/internal/dispatchers"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/market/sre/tools/watcher/src/internal/global"
)

func RandomString(n int) string {
	var letters = []rune("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-@")

	s := make([]rune, n)
	for i := range s {
		s[i] = letters[rand.Intn(len(letters))]
	}
	return string(s)
}

func TestGetKeys1(t *testing.T) {
	testMapInstances := make(map[string]*global.GroupInfo)
	var assumeKeys []string
	for i := 100; i == 0; i-- {
		assumeKeys = append(assumeKeys, RandomString(30))
	}
	for _, i := range assumeKeys {
		testMapInstances[i] = &global.GroupInfo{}
	}

	keys, err := getKeys(testMapInstances)
	assert.NoError(t, err)
	sort.Strings(keys)
	sort.Strings(assumeKeys)
	assert.Equal(t, assumeKeys, keys)
}

func TestCompareLists1(t *testing.T) {
	a := []string{"1", "2", "3"}
	b := []string{"1", "2", "3", "4"}
	res := ContainsNewItems(a, b)
	assert.True(t, res)
}
func TestCompareLists2(t *testing.T) {
	a := []string{"1", "2", "3"}
	b := []string{"1", "2", "4"}
	res := ContainsNewItems(a, b)
	assert.True(t, res)

}
func TestCompareLists3(t *testing.T) {
	a := []string{"1", "2", "3"}
	b := []string{"1", "2", "3"}
	res := ContainsNewItems(a, b)
	assert.False(t, res)
}
func TestCompareLists4(t *testing.T) {
	a := []string{"1", "2", "3"}
	b := []string{"1", "2"}
	res := ContainsNewItems(a, b)
	assert.False(t, res)
}

func TestCompareLists5(t *testing.T) {
	a := []string{"1"}
	b := []string{"2"}
	res := ContainsNewItems(a, b)
	assert.True(t, res)
}

func TestDiscoveredSetHasNewInstances1(t *testing.T) {
	w := new(Watcher)
	w.Config = &DefaultConfig
	w.Config.LogPath = "./log.txt"
	err := w.InitLogger()
	assert.NoError(t, err)
	w.PrimerTopology = &global.TopologyInfo{}
	w.DiscoveredTopology = &global.TopologyInfo{}
	_, err = w.DiscoveredSetHasNewInstances()
	assert.Error(t, err)
}
func TestDiscoveredSetHasNewInstances2(t *testing.T) {
	w := new(Watcher)
	w.Config = &DefaultConfig
	w.Config.LogPath = "./log.txt"
	err := w.InitLogger()
	assert.NoError(t, err)
	w.PrimerTopology = &global.TopologyInfo{GroupCount: 1, InstanceCount: 1,
		Groups: map[string]*global.GroupInfo{"one": &global.GroupInfo{InstanceCount: 1, Instances: []string{"one"}}}}
	w.DiscoveredTopology = &global.TopologyInfo{GroupCount: 1, InstanceCount: 1,
		Groups: map[string]*global.GroupInfo{"one": &global.GroupInfo{InstanceCount: 1, Instances: []string{"one"}}}}
	res, err := w.DiscoveredSetHasNewInstances()
	assert.NoError(t, err)
	assert.False(t, res)
}
func TestDiscoveredSetHasNewInstances3(t *testing.T) {
	w := new(Watcher)
	w.Config = &DefaultConfig
	w.Config.LogPath = "./log.txt"
	err := w.InitLogger()
	assert.NoError(t, err)
	assert.NoError(t, err)
	w.PrimerTopology = &global.TopologyInfo{GroupCount: 1, InstanceCount: 1,
		Groups: map[string]*global.GroupInfo{"one": &global.GroupInfo{InstanceCount: 1, Instances: []string{"one"}}}}
	w.DiscoveredTopology = &global.TopologyInfo{GroupCount: 1, InstanceCount: 1,
		Groups: map[string]*global.GroupInfo{"one": &global.GroupInfo{InstanceCount: 1, Instances: []string{"two"}}}}
	res, err := w.DiscoveredSetHasNewInstances()
	assert.NoError(t, err)
	assert.True(t, res)
}
func TestDiscoveredSetHasNewInstances4(t *testing.T) {
	w := new(Watcher)
	w.Config = &DefaultConfig
	w.Config.LogPath = "./log.txt"
	err := w.InitLogger()
	assert.NoError(t, err)
	w.PrimerTopology = &global.TopologyInfo{GroupCount: 1, InstanceCount: 1,
		Groups: map[string]*global.GroupInfo{"one": &global.GroupInfo{InstanceCount: 1, Instances: []string{"one"}}}}
	w.DiscoveredTopology = &global.TopologyInfo{GroupCount: 1, InstanceCount: 2,
		Groups: map[string]*global.GroupInfo{"one": &global.GroupInfo{InstanceCount: 2, Instances: []string{"one", "two"}}}}
	res, err := w.DiscoveredSetHasNewInstances()
	assert.NoError(t, err)
	assert.True(t, res)
}
func TestDiscoveredSetHasNewInstances5(t *testing.T) {
	w := new(Watcher)
	w.Config = &DefaultConfig
	w.Config.LogPath = "./log.txt"
	err := w.InitLogger()
	assert.NoError(t, err)
	w.PrimerTopology = &global.TopologyInfo{GroupCount: 1, InstanceCount: 1,
		Groups: map[string]*global.GroupInfo{"one": &global.GroupInfo{InstanceCount: 1, Instances: []string{"one"}}}}
	w.DiscoveredTopology = &global.TopologyInfo{GroupCount: 1, InstanceCount: 2,
		Groups: map[string]*global.GroupInfo{"different": &global.GroupInfo{InstanceCount: 2, Instances: []string{"one", "two"}}}}
	_, err = w.DiscoveredSetHasNewInstances()
	assert.Error(t, err)
}

func TestHandBreak1(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	h := dispatchers.ToolHandBrake{CTX: ctx, CMD: fmt.Sprintf("/bin/echo '%s'", HandBrakeFreeMessage),
		LockedMessage: HandBrakeLockedMessage, FreeMessage: HandBrakeFreeMessage}

	locked, err := h.IsLocked()
	assert.NoError(t, err)
	assert.False(t, locked)
}

func TestHandBreak2(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	filePath := "./TestHandBreak2"
	file, err := os.Create(filePath)
	if err != nil {
		panic(err)
	}
	defer func() {
		_ = file.Close()
		_ = os.Remove(filePath)
	}()

	_, err = io.WriteString(file, fmt.Sprintf("#!/bin/sh\n/bin/echo %s 1>&2", HandBrakeLockedMessage))
	if err != nil {
		panic(err)
	}
	_ = file.Close()
	err = os.Chmod(filePath, 0755)
	if err != nil {
		panic(err)
	}
	h := dispatchers.ToolHandBrake{CTX: ctx, CMD: filePath,
		LockedMessage: HandBrakeLockedMessage, FreeMessage: HandBrakeFreeMessage}

	locked, err := h.IsLocked()
	assert.NoError(t, err)
	assert.True(t, locked)
}

func TestHandBreak3(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 1*time.Second)
	defer cancel()

	h := dispatchers.ToolHandBrake{CTX: ctx, CMD: "sleep 5",
		LockedMessage: HandBrakeLockedMessage, FreeMessage: HandBrakeFreeMessage}

	locked, err := h.IsLocked()
	assert.Error(t, err)
	assert.False(t, locked)
}
