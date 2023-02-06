package util

import (
	"bufio"
	"fmt"
	"os"
	"strings"
)

func parseProps(filename string) map[string]string {
	file, err := os.Open(filename)
	if err != nil {
		panic(fmt.Errorf("cannot open file: '%v'", filename))
	}

	defer file.Close()

	props := map[string]string{}

	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		line := scanner.Text()
		if strings.Contains(line, "=") {
			kv := strings.SplitN(line, "=", 2)
			props[kv[0]] = kv[1]
		}
	}

	return props
}
