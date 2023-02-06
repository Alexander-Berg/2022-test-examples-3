# If not running interactively, don't do anything
case $- in
    *i*) ;;
      *) return;;
esac

# don't put duplicate lines or lines starting with space in the history.
# See bash(1) for more options
HISTCONTROL=ignoredups

# append to the history file, don't overwrite it
shopt -s histappend

shopt -s checkwinsize

# make less more friendly for non-text input files, see lesspipe(1)
[ -x /usr/bin/lesspipe ] && eval "$(SHELL=/bin/sh lesspipe)"

# multi-line commands in history
shopt -q -s cmdhist

# correct dir spellings
shopt -q -s cdspell

# verify history command before execution
shopt -s  histverify

# prevent first exit if running jobs exists
shopt -s checkjobs

# try to make host completion on @
shopt -s hostcomplete

#save time in history
export HISTTIMEFORMAT='%d.%m.%Y %H:%M:%S  '

# enable color support of ls and also add handy aliases
if [ "$TERM" != "dumb" ]; then
    eval "`dircolors -b`"
    alias ls='ls --color=auto'
fi

# enable programmable completion features (you don't need to enable
# this, if it's already enabled in /etc/bash.bashrc and /etc/profile
# sources /etc/bash.bashrc).
if [ -f /etc/bash_completion ]; then
    . /etc/bash_completion
fi
case "$TERM" in
    xterm*)
        export PS1='\[\e[1;31m\]$(echo "["${?}"]" | sed "s/\\[0\\]//")\[\033[33;1m\][mocks]'" \[\033[38;5;177m\]$QLOUD_APPLICATION.\[\033[38;5;150m\]$QLOUD_ENVIRONMENT.\[\033[38;5;172m\]$QLOUD_COMPONENT.\[\033[38;5;248m\]\h\[$(tput sgr0)\]: \w# " ;;
    dumb*)
        export PS1="[mocks] $QLOUD_APPLICATION.$QLOUD_ENVIRONMENT.$QLOUD_COMPONENT.\h: \w#" ;;
    *)
        PS1='${debian_chroot:+($debian_chroot)}\u@\h:\w\$ ' ;;
esac

PROMPT_COMMAND="${PROMPT_COMMAND:+$PROMPT_COMMAND ; }"'echo -ne "\033]0;[mocks] $QLOUD_APPLICATION.$QLOUD_ENVIRONMENT.$QLOUD_COMPONENT\007"'

msg="\n\033[37;1mtank test mocks:\033[0m
    :80     - http mock with sleep
    :443    - https mock with sleep
"

printf "$msg" > /dev/stderr
