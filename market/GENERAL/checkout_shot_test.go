package generator

import (
	"fmt"
	"testing"
	"time"
)

func TestGenerateAmmo(t *testing.T) {
	var conf = ShootConfig{
		MaxRoundsPerUid: 7,
		UidRoundDelay:   5000,
		CheckoutFlagMod: 5,
		PaymentTypeMod:  5,
		MaxItems:        5,
		MaxCoins:        3,
	}
	for i:=0; i < 50; i++ {
		round := GenerateRound(&conf)
		fmt.Println(round)
	}
}

func TestGenerateAmmo2(t *testing.T) {
	var conf = ShootConfig{
		MaxRoundsPerUid: 7,
		UidRoundDelay:   5000,
		CheckoutFlagMod: 5,
		PaymentTypeMod:  5,
		MaxItems:        5,
		MaxCoins:        3,
	}
	for i:=0; i < 20; i++ {
		round := GenerateRound(&conf)
		fmt.Println(round)
		time.Sleep(2 * time.Second)
	}
}

