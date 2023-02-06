package util

import "fmt"

func ChooseWeightedIndex(chosenWeight int, weights []int) (idx int, err error) {
	for i, weight := range weights {
		if chosenWeight < weight {
			return i, nil
		} else {
			chosenWeight -= weight
		}
	}
	err = fmt.Errorf("chosen more than total")
	return
}
