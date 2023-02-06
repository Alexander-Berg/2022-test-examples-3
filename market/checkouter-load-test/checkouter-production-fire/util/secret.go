package util

import (
	"errors"
	"fmt"
	"go.uber.org/zap"
	"os"
)

type SecretConfig struct {
	Type     string
	File     string
	Property string
	Name     string
}

const Env = "ENV"
const File = "FILE"
const Undefined = "UNDEFINED"

func ResolveSecret(config SecretConfig) string {
	zap.L().Info("Resolve secret", zap.Reflect("config", config))
	if config.Type == Env {
		return resolveSecretEnv(config.Name)
	} else if config.Type == File {
		return resolveSecretFile(config.File, config.Property)
	} else if config.Type == Undefined {
		panic(errors.New("type of secret undefined, you must specify it"))
	} else {
		panic(fmt.Errorf("unknown type of secret: '%v'", config.Type))
	}
}

func resolveSecretEnv(name string) string {
	value, ok := os.LookupEnv(name)
	if !ok {
		panic(fmt.Errorf("cannot get secret: '%v'", name))
	}
	return value
}

func resolveSecretFile(file string, property string) string {
	return parseProps(file)[property]
}
