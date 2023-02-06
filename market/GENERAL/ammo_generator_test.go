package generator

import (
	"fmt"
	"math/rand"
	"testing"
	"time"
)

func TestShots(t *testing.T) {
	rand.Seed(time.Now().UnixNano())
	var conf = ShootConfig{
		MaxRoundsPerUid: 7,
		UidRoundDelay:   5000,
		CheckoutFlagMod: 5,
		PaymentTypeMod:  5,
		MaxItems:        5,
		MaxCoins:        3,
	}
	for i:=0; i < 100; i++ {
		round := GenerateRound(&conf)
		fmt.Println(round)
		time.Sleep(1 * time.Second)
	}
}
