#pragma once

#include <common/lang_config.h>

#include <string>

extern const std::string DEFAULT_LANGUAGE_CONFIG;

yimap::LanguageConfig makeLanguageConfig(
    const std::string& serverConfig,
    bool renameEnabled,
    bool localizeImap,
    const std::string& userLanguage);
