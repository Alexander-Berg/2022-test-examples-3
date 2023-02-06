package configs

import (
	"fmt"
)

func ValidateGunConfig(config GunConfig) error {
	if config.ID == nil {
		return fmt.Errorf("gun.id is undefined")
	}
	if config.CartRepeats <= 0 {
		return fmt.Errorf("cart repeats should be at least one")
	}
	for _, handle := range config.Handles {
		_, err := handle.GetDelay()
		if err != nil {
			return fmt.Errorf("there are handle with undefined delay: %v. %w", handle.Name, err)
		}
	}
	return nil
}

func ValidateAmmo(customAmmo CustomAmmo, options Options) error {
	if customAmmo.Carts > len(options.Addresses) {
		return fmt.Errorf("not enough addresses to shoot this ammo")
	}
	return nil
}
