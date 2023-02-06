package storage

import (
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"sync"
	"testing"
)

func TestInit(t *testing.T) {
	storage := Storage{}
	require.Equal(t, -1, storage.Size())

	storage.Put(0)
	require.Equal(t, DefaultSize, storage.Size())

	err := storage.SetSize(10)
	require.Error(t, err)

	storage = Storage{}

	err = storage.SetSize(-10)
	assert.Error(t, err)

	err = storage.SetSize(10)
	require.NoError(t, err)
	assert.Equal(t, 10, storage.Size())
}

func TestSaveData(t *testing.T) {
	storage := Storage{}
	err := storage.SetSize(10)
	if err != nil {
		t.Fatal()
	}

	input := []int{1, 2, 3, 4, 5, 6}
	var output []int

	for _, n := range input {
		storage.Put(n)
	}

	storage.GetAll(func(n interface{}) {
		output = append(output, n.(int))
	})
	assert.Equal(t, input, output)
}

func TestReuse(t *testing.T) {
	storage := Storage{}
	err := storage.SetSize(10)
	if err != nil {
		t.Fatal()
	}

	input := []int{1, 2, 3, 4, 5, 6}
	input2 := []int{9, 10, 11, 12, 13}
	var output []int

	for _, n := range input {
		storage.Put(n)
	}

	storage.GetAll(func(n interface{}) {})

	for _, n := range input2 {
		storage.Put(n)
	}

	storage.GetAll(func(n interface{}) {
		output = append(output, n.(int))
	})

	assert.Equal(t, input2, output)
}

func TestOverflow(t *testing.T) {
	storage := Storage{}
	err := storage.SetSize(10)
	if err != nil {
		t.Fatal()
	}

	input := []int{1, 2, 3, 4, 5, 6, 5, 6, 7, 8, 9, 10, 11, 12}
	var output []int

	for _, n := range input {
		storage.Put(n)
	}

	// Peak must not clear storage
	storage.PeakAll(func(n interface{}) {})

	overflow := len(input) - storage.Size()

	storage.GetAll(func(n interface{}) {
		output = append(output, n.(int))
	})

	assert.Equal(t, input[overflow:], output)
}

func TestConcurrency(t *testing.T) {
	wg := sync.WaitGroup{}

	storage := Storage{}

	for i := 0; i < 2048; i++ {
		wg.Add(1)
		go func(i int) {
			for x := 0; x < 100; x++ {
				storage.Put(x)
			}

			storage.PeakAll(func(n interface{}) {})

			storage.GetAll(func(n interface{}) {})

			wg.Done()
		}(i)

	}

	wg.Wait()
}
