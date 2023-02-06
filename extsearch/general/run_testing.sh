#!/bin/sh

case ${a_dc} in
	sas)
		passive_port="40000-41000"
	;;
	man)
		passive_port="50000-51000"
	;;
	vla)
		passive_port="55000-56000"
	;;
	*)
		printf "unknown a_dc\n" >&2
		exit 1
	;;
esac

./vhftpd -host '[::]' -port 21 \
	-s3-endpoint "s3.mdst.yandex.net" -s3-bucket "vh-ftpd" \
	-public-ip "37.9.103.1" -passive-port-range ${passive_port} \
	-tls-cert 7F000B4E1BFC7F274B4018B7930002000B4E1B_certificate -tls-key 7F000B4E1BFC7F274B4018B7930002000B4E1B_private_key \
	-passwd passwd/passwd \
	-debug
