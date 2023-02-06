package loader

import (
	"io/ioutil"
	"os"
	"path"
	"regexp"
	"strings"
	"testing"

	"go.uber.org/zap"

	"rex/common/model"
	"rex/config"
)

func init() {
	dir := "testdata/common"
	files, err := ioutil.ReadDir(dir)
	if err != nil {
		panic(err)
	}
	for _, info := range OnlyYAML(files) {
		test := &LoadErrorTest{
			Filename: path.Join(dir, info.Name()),
			Err:      strings.HasPrefix(info.Name(), "fail"),
		}
		LoadErrorTests = append(LoadErrorTests, test)
	}
}

type LoadErrorTest struct {
	Filename string
	Err      bool
}

var LoadErrorTests = []*LoadErrorTest{}

func TestLoad_worksWithoutErrors(t *testing.T) {
	logger := zap.NewExample()

	for _, tt := range LoadErrorTests {
		t.Run(tt.Filename, func(t *testing.T) {
			_, _, err := Load(config.RulesConfig{
				Files: []string{tt.Filename},
			}, logger)
			hasErr := err != nil
			if hasErr != tt.Err {
				t.Fatalf("want error %v, got %v", tt.Err, err)
			}
		})
	}
}

func TestLoad_overridesChokerSettings(t *testing.T) {
	tc := &config.TasksConfig{
		Global: config.TaskConfig{
			Match: model.Match{
				Choker: &model.Choker{
					Base:       0,
					Hysteresis: 1,
				},
			},
		},
	}
	tc.Normalize()

	rules, _, err := Load(config.RulesConfig{
		Files:       []string{"testdata/choker.yaml"},
		TasksConfig: *tc,
	}, zap.NewExample())

	if err != nil {
		t.Fatal(err)
	}
	if len(rules) != 1 {
		t.Fatalf("want 1 rule, got %d rules", len(rules))
	}

	c := rules[0].Match.Choker
	if n := c.Base; n != 10 {
		t.Errorf("base: want 10, got %d", n)
	}
	if n := c.Hysteresis; n != 2 {
		t.Errorf("hysteresis: want 2, got %d", n)
	}
}

// YAML file extension expression.
var yamlExt = regexp.MustCompile(`\.ya?ml$`)

// OnlyYAML returns a list of YAML file infos from infos.
func OnlyYAML(infos []os.FileInfo) []os.FileInfo {
	var yaml []os.FileInfo
	for _, info := range infos {
		if !info.Mode().IsRegular() {
			continue
		}
		if !yamlExt.MatchString(info.Name()) {
			continue
		}
		yaml = append(yaml, info)
	}
	return yaml
}
