VERSION=`date "+%Y-%m-%d-%H-%M"`;
BUILDNAME="collectors-mocks:r${VERSION}"
BUILDTAG=registry.yandex.net/mail/xiva/${BUILDNAME}

docker build --pull --network=host --tag ${BUILDTAG} -f Dockerfile .
docker push ${BUILDTAG}
