install-package:
	/usr/bin/env python setup.py install

install: install-package
	install scripts/rta-daemon /usr/local/bin/rta-daemon
	install scripts/report-testing-assistant /etc/init.d/report-testing-assistant
	install stuff/13-rta-logs.conf /etc/nginx/sites-enabled/13-rta-logs.conf
	service nginx reload
	install -o thoughteer -d /var/lib/yandex/report-testing-assistant
	install -m 0600 -o thoughteer stuff/ssh-key /var/lib/yandex/report-testing-assistant/ssh-key
	touch /var/log/report-testing-assistant.log && chown thoughteer /var/log/report-testing-assistant.log

uninstall:
	rm -f /usr/local/bin/rta-daemon
	rm -f /etc/init.d/report-testing-assistant
	rm -rf /var/lib/yandex/report-testing-assistant
	rm -f /var/log/report-testing-assistant.log
