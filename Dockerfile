FROM amazoncorretto:17-alpine

WORKDIR /app
COPY ./out/artifacts/rdfizer/ .

# TODO
#   set rdfizer VERS env var
#   create symbolic link rdfizer-VERS -> rdfizer
#   change entrypoint rdfizer name

ENTRYPOINT ["java", "-jar", "rdfizer-1.1.2.jar"]