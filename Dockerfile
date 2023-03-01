FROM amazoncorretto:17-alpine

ENV RDFIZERVERS=1.1.2

WORKDIR /data
COPY ./out/artifacts/rdfizer/ /app

RUN cd /app; \
    (echo '#!/bin/sh' && echo 'exec java -jar $0 "$@"' && cat rdfizer-${RDFIZERVERS}.jar) > rdfizer; \
    chmod +x rdfizer; \
    ln -s /app/rdfizer /usr/local/bin/rdfizer; \
    rm rdfizer-${RDFIZERVERS}.jar

ENTRYPOINT ["rdfizer"]