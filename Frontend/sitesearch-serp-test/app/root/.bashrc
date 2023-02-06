Color_Off='\e[0m'
Yellow='\e[0;33m'
BGreen='\e[1;32m'
BBlue='\e[1;34m'

# Terminal tab title
PROMPT_COMMAND='echo -ne "\033]0;★ ${QLOUD_INSTANCE}.${QLOUD_ENVIRONMENT}.${QLOUD_APPLICATION}:$(pwd)\007"'

# Bash prompt
PS1="\n${BBlue}${QLOUD_INSTANCE}${Color_Off}.${BGreen}${QLOUD_ENVIRONMENT}.${QLOUD_APPLICATION}${Color_Off} ${BBlue}$(hostname -s)${Color_Off} in ${Yellow}\w${Color_Off}\n# "
