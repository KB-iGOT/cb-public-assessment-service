FROM eclipse-temurin:11-jdk

RUN useradd -ms /bin/bash appuser

RUN mkdir -p /opt

RUN apt-get update \
    && apt-get install -y \
        curl \
        libxrender1 \
        libjpeg62-turbo \
        fontconfig \
        libxtst6 \
        xfonts-75dpi \
        xfonts-base \
        xz-utils \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

COPY public-assessment-service-0.0.1-SNAPSHOT.jar /opt/
RUN chown -R appuser:appuser /opt
USER appuser

#HEALTHCHECK --interval=30s --timeout=30s CMD curl --fail http://localhost:7001/actuator/health || exit 1
CMD ["/bin/bash", "-c", "java -XX:+PrintFlagsFinal $JAVA_OPTIONS -XX:+UnlockExperimentalVMOptions -jar /opt/public-assessment-service-0.0.1-SNAPSHOT.jar"]
