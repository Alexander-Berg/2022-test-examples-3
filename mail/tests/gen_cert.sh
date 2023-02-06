openssl genrsa > privkey.pem
openssl req -new -x509 -key privkey.pem -out cert.pem -days 365 -nodes
