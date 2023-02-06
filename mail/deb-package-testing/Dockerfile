FROM registry.yandex.net/mail/nwsmtp/nwsmtp:20201113-1455.r7581658-1518997.MAILDLV-4975

RUN apt-get update -qq;
RUN apt-get install yandex-passport-vault-client;
RUN apt-get install yamail-mx-common=100-faked;
RUN apt-get install -y yamail-postfix libicu52:amd64;

COPY deploy /

ENTRYPOINT ["/opt/entrypoint.sh"]

CMD ["supervisord", "-n", "-c", "/etc/supervisor/supervisord.conf"]
