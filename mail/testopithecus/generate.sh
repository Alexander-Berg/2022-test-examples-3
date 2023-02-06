#!/bin/bash

declare -i no_decoration=0
declare -i no_yarn_check=0
declare -i no_brew_check=0
declare -i no_ys_build=0
declare -i no_common_build=0
declare -i skip_swift=0
declare -i skip_kotlin=0
declare -i show_help=0
declare -i force_gen=0

function red {
  if [[ no_decoration -eq 1 ]]; then
    echo "$1"
  else
    echo -e "\033[0;31m✗ $1\033[0m"
  fi
}

function green {
  if [[ no_decoration -eq 1 ]]; then
    echo "$1"
  else
    echo -e "\033[0;32m✓ $1\033[0m"
  fi
}

function yellow {
  if [[ no_decoration -eq 1 ]]; then
    echo "$1"
  else
    echo -e "\033[1;33m$1\033[0m"
  fi
}

function normal {
  if [[ no_decoration -eq 1 ]]; then
    echo "$1"
  else
    echo -e "\033[0m$1"
  fi
}

for i in "$@"; do
case $i in
  -i=*|--ios=*)
  ios_command="${i#*=}"
  ;;
  -l=*|--log=*)
  log_command="${i#*=}"
  ;;
  -n|--no-decoration)
  no_decoration=1
  ;;
  -y|--no-yarn-check)
  no_yarn_check=1
  ;;
  -b|--no-ys-build)
  no_ys_build=1
  ;;
  -c|--no-common-build)
  no_common_build=1
  ;;
  -f|--no-formulae-check)
  no_brew_check=1
  ;;
  -s|--skip-swift)
  skip_swift=1
  ;;
  -k|--skip-kotlin)
  skip_kotlin=1
  ;;
  -h|--help)
  show_help=1
  ;;
  -r|--force-gen)
  force_gen=1
  ;;
  -X|--superskip)
  no_yarn_check=1
  no_ys_build=1
  no_common_build=1
  no_brew_check=1
  ;;
  *)
  yellow "Unknown argument '$i'. It will be ignored. Use -h for help"
  ;;
esac
done

readonly PROJECT_NAME="testopithecus"

readonly E_OK=0
readonly E_NOYS=56
readonly E_BREWDEPS=57
readonly E_YARNDEPS=58
readonly E_GENBUILDFAILED=59
readonly E_LINTFAILED=60
readonly E_YSBUILDFAILED=61
readonly E_SWIFTBUILDFAILED=62
readonly E_ANDROIDBUILDFAILED=63
readonly E_GENPROJFAILED=64
readonly E_GENFAILED=65

function yarn_check {
  $(yarn check --"$1" --cwd "$2" &>/dev/null)
}

function check_brew_deps {
  if ! $(which "$1" &> /dev/null); then
    red "$2 seems uninstalled. Please run './setup.sh'"
    exit $E_BREWDEPS
  fi
}

if [[ $show_help -eq 1 ]]; then
  echo "generate.sh processes Yandex.Script files and generates target language files."
  echo
  echo "When run without arguments it:"
  echo "  1. Checks there's 'ys' subfolder with Yandex.Script;"
  echo "  2. Checks Homebrew dependencies presence and notifies if there're issues;"
  echo "  3. Checks Yarn dependencies consistency and notifies if there're issues;"
  echo "  4. Checks product code with Linter;"
  echo "  5. Generates target language files;"
  echo "  6. Lints and formats the produced language files;"
  echo "  7. Compiles target language files;"
  echo "  8. Xcode project file is built for Swift;"
  echo
  echo "Arguments:"
  echo "  -i=, --ios=<command>    - runs <command> with resulting xcodeproj file."
  echo "                            E.g. '-i=open' will open the xcodeproj file in Xcode."
  echo "  -l=, --log=<command>    - runs <command> with lint.log or build.log should the corresponding step fail."
  echo "                            E.g. '-l=code' will open a log file in VSCode."
  echo "  -n, --no-decoration     - switches off coloring and Unicode symbols in output."
  echo "  -y, --no-yarn-check     - skips Yarn packages integrity check for faster generation."
  echo "  -f, --no-formulae-check - skips Homebrew formulae presence check for faster generation."
  echo "  -b, --no-ys-build       - skips Yandex.Script build assuming it's done."
  echo "  -c, --no-common-build   - skips building Common subfolder assuming it's done."
  echo "  -X, --superskip         - skips all checks and YS builds. Equal to -y -f -b -c."
  echo "  -s, --skip-swift        - skips Swift files processing."
  echo "  -k, --skip-kotlin       - skips Kotlin files processing."
  echo "  -r, --force-gen         - forces sources re-generation."
  echo "  -h, --help              - shows this help message."
  exit $E_OK
fi

path_to_ys=${YS_PATH:-"$PWD/packages/ys"}
path_to_common="$PWD/common"

normal "Using Yandex.Script at path '$path_to_ys'."

if [[ -z "$path_to_ys" ]]; then
  normal "Path to Yandex.Script must either be provided as an YS_PATH env var, or should be in 'packages/ys' folder."
  exit $E_NOYS
fi

if [[ $no_brew_check -eq 0 ]]; then
  check_brew_deps node Node
  check_brew_deps yarn Yarn
  check_brew_deps swiftformat SwiftFormat
  check_brew_deps ktlint ktlint
fi

common_code_path="$path_to_common/code"

if [[ $force_gen -eq 1 ]]; then
  normal "Clean build requested."
  command rm -rf "$common_code_path/.cache"
fi

if [[ $no_yarn_check -eq 0 ]]; then
  if ! ( $(yarn_check integrity "$PWD") && $(yarn_check "verify-tree" "$PWD") ); then
    red "The Project Yarn dependencies seem unsynced with the upstream. Please update with 'yarn'"
    exit $E_YARNDEPS
  fi
fi

if [[ $no_ys_build -eq 0 ]]; then
  normal "Building Yandex.Script Generators..."

  if $(yarn --cwd "$path_to_ys" run build &> /dev/null); then
    green "Build done."
  else
    red "Build failed."
    exit $E_GENBUILDFAILED
  fi
fi

base_ios="$PWD/ios"

if [[ $no_common_build -eq 0 ]]; then
  normal "Linting Common Yandex.Script..."

  lint_log="$base_ios/lint.log"
  if $(yarn --cwd "$path_to_common" run lint &> "$lint_log"); then
    green "Linting done."
  else
    red "Lint check failed. See the logfile at $lint_log."
    if [[ -n "$log_command" ]]; then
      normal "Attempting running '$log_command $lint_log'..."
      $($log_command "$lint_log" &)
    fi
    exit $E_LINTFAILED
  fi

  normal "Building Common..."

  build_log="$base_ios/build.log"
  if $(yarn --cwd "$path_to_common" run build &> "$build_log"); then
    green "Common Building done."
  else
    red "Common Build failed. See the logfile at $build_log."
    if [[ -n "$log_command" ]]; then
      normal "Attempting running '$log_command $build_log'..."
      $($log_command "$build_log" &)
    fi
    exit $E_YSBUILDFAILED
  fi
else
  yellow "Skip Common building."
fi

app_path="$path_to_ys/build/src/app.js"

if [[ $skip_swift -eq 0 ]]; then
  normal "Generating Swift platform code..."

  ios_generated="$base_ios/Sources/$PROJECT_NAME/generated"
  if [[ $force_gen -eq 1 ]]; then
    yellow "Cleanup iOS generated files at '$ios_generated'."
    command rm -rf "$ios_generated"
  fi

  swift_generator="$path_to_ys/build/src/generators/swift"
  swift_config="$path_to_ys/src/generators/swift/config.json"
  if $(node "$app_path" -i "$common_code_path" -o "$ios_generated" -g "$swift_generator" -c "$swift_config"); then
    green "Generating done. See $ios_generated for the results."
  else
    red "Failed generating Swift code from YS."
    exit $E_GENFAILED
  fi

  normal "Autoformatting Swift code..."
  if $(swiftformat --config "$base_ios/.swiftformat" "$ios_generated" &> /dev/null); then
    green "Autoformatting done."
  else
    yellow "Autoformatting failed."
  fi

  normal "Compiling Swift code..."

  swift_build_results="$PWD/build/ios"
  swift_build_log="$base_ios/build.log"
  $(swift build --build-path "$swift_build_results" --package-path "$base_ios" &> "$swift_build_log")
  swift_build_status=$?
  if [[ $swift_build_status -eq 0 ]]; then
    green "Compilation complete. See $swift_build_results for results."
  else
    red "Compilation failed. See the logfile at $swift_build_log."
    if [[ -n "$log_command" ]]; then
      normal "Attempting running '$log_command $swift_build_log'..."
      $($log_command "$swift_build_log" &)
    fi
  fi

  if $(swift package --package-path "$base_ios" generate-xcodeproj --output "$base_ios" >> "$swift_build_log"); then
    green "Xcode Project file created. See $base_ios/$PROJECT_NAME.xcodeproj for the project file."
  else
    red "Xcode Project file generation failed."
    exit $E_GENPROJFAILED
  fi

  if [[ -n "$ios_command" ]]; then
    normal "Attempting running '$ios_command $PROJECT_NAME.xcodeproj'..."
    $("$ios_command" "$base_ios/$PROJECT_NAME.xcodeproj" &)
  fi

  if [[ $swift_build_status -ne 0 ]]; then
    exit $E_SWIFTBUILDFAILED
  fi
else
  yellow "Skip Swift code generation."
fi

if [[ $skip_kotlin -eq 0 ]]; then
  normal "Generating Kotlin platform code..."

  base_android="$PWD/android"
  base_android_sources="$base_android/Sources"
  android_generated="$base_android_sources/$PROJECT_NAME/generated"
  if [[ $force_gen -eq 1 ]]; then
    yellow "Cleanup Android generated files at '$android_generated'."
    command rm -rf "$android_generated"
  fi

  android_generator="$path_to_ys/build/src/generators/kotlin"
  android_config="$path_to_ys/src/generators/kotlin/config.json"
  if $(node "$app_path" -i "$common_code_path" -o "$android_generated" -g "$android_generator" -c "$android_config"); then
    green "Generating done. See $android_generated for the results."
  else
    red "Failed generating Kotlin code from YS."
    exit $E_GENFAILED
  fi

  android_lint_log="$base_android/lint.log"
  normal "Linting and formatting generated Kotlin code..."
  $(ktlint --format --android --editorconfig="$base_android" &> "$android_lint_log")
  green "Kotlin linting and formatting completed. See $android_lint_log for results."

  normal "Compiling Kotlin code..."
  android_build_results="$base_android_sources/build"
  android_build_log="$base_android/build.log"
  if $("$base_android/gradlew" -p "$base_android_sources" assemble &> "$android_build_log"); then
    green "Kotlin compilation complete. See $android_build_results for results."
  else
    red "Kotlin compilation failed. See the logfile at $android_build_log."
    if [[ -n "$log_command" ]]; then
      normal "Attempting running '$log_command $android_build_log'..."
      $($log_command "$android_build_log" &)
    fi
    exit $E_ANDROIDBUILDFAILED
  fi
else
  yellow "Skip Kotlin code generation."
fi

exit $E_OK
