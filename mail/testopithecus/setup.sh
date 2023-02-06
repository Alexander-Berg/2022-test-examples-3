#!/bin/bash

#readonly RUBY_VERSION="$(cat .ruby-version | tr -d '\n')"

readonly E_OK=0
readonly E_SUBMODULE_ERROR=55
readonly E_BREW_ERROR=56
readonly E_PROJECT_ERROR=57
readonly E_COMMON_ERROR=58
readonly E_PROJECT_BUILD_ERROR=59

# DISABLE DUE TO WORKFLOW CHANGE: WORK IN YS DIRECTLY.
# echo "Loading submodules..."
# if $(git submodule update --init --quiet &> /dev/null); then
#   echo "Git submodules loaded."
# else
#   echo "Git submodules retrieval error. Exiting."
#   exit $E_SUBMODULE_ERROR
# fi

command chmod +x ./generate.sh
#command chmod +x ./clean.sh

ys_folder="$PWD/packages/ys"
brew_formulae="Node, Yarn, SwiftFormat, ktlint"
echo "Installing dependencies: $brew_formulae..."
brew_install=$(brew install node yarn swiftformat ktlint &> /dev/null)
if [[ $brew_install -eq 0 || $brew_install -eq 1 ]]; then
  echo "$brew_formulae dependencies installed."
else
  echo "$brew_formulae dependencies installation failed. Exiting."
  exit $E_BREW_ERROR
fi
brew_formulae=

#echo "Installing RVM..."
#if $(curl -sSL https://get.rvm.io | bash -s stable &> /dev/null); then
#  echo "RVM installed."
#  echo "Installing Ruby..."
#  if $(rvm install ruby $RUBY_VERSION); then
#    echo "Ruby installed."
#  else
#    echo "Ruby installation failed. Please install Ruby manually. Installation will continue."
#  fi
#else
#  echo "RVM is not installed. Please install RVM and Ruby manually. Installation will continue."
#fi
#
echo "Installing NPM modules' dependencies..."
npm install
#
#echo "Building the Project..."
#if $(yarn --cwd "$PWD" run build &> /dev/null); then
#  echo "The project building done."
#else
#  echo "The project building failed. Exiting."
#  exit $E_PROJECT_BUILD_ERROR
#fi

echo "Setup complete."
echo "Use './generate.sh' to generate iOS and Android files."
exit $E_OK
