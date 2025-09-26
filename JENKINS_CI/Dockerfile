# Usa la imagen oficial de Jenkins con JDK 17
FROM jenkins/jenkins:jdk17 as jenkins_cd-ci

# Cambia a usuario root para instalar dependencias
USER root

########################################################################

# Instalar herramientas básicas, Git y configurar idioma español
RUN apt-get update && \
    apt-get install -y git ca-certificates locales && \
    echo "es_MX.UTF-8 UTF-8" >> /etc/locale.gen && \
    locale-gen && \
    apt-get clean

ENV LANG=es_MX.UTF-8
ENV LANGUAGE=es_MX:es
ENV LC_ALL=es_MX.UTF-8

# Copiar los certificados SSL al contenedor
COPY ./github.cer /usr/local/share/ca-certificates/github.crt
COPY ./issste.gob.mx.2025.cer /usr/local/share/ca-certificates/issste.gob.mx.2025.crt
COPY ./My_CA_FWeb_Public.cer /usr/local/share/ca-certificates/My_CA_FWeb_Public.crt

# Descargar el certificado
ADD https://curl.se/ca/cacert.pem /usr/local/share/ca-certificates/ca-bundle.crt

# Actualizar la base de certificados
RUN update-ca-certificates

# Configurar Git para usar el certificado raíz actualizado
RUN git config --global http.sslCAInfo /etc/ssl/certs/ca-certificates.crt
RUN git config --global http.sslCAinfo /usr/local/share/ca-certificates/ca-bundle.crt

# Importar certificados al almacén de Java
RUN keytool -import -trustcacerts -alias github -file /usr/local/share/ca-certificates/github.crt \
    -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit -noprompt

########################################################################

# Variables de entorno para Maven
ENV MAVEN_RELASE=3 \
    MAVEN_VERSION=3.9.11 \
    MAVEN_HOME=/opt/maven

# Instalar plugins esenciales para Pipeline y Docker
RUN jenkins-plugin-cli --plugins \
    workflow-aggregator \
    docker-workflow \
    pipeline-stage-view \
    pipeline-rest-api \
    git \
    credentials \
    configuration-as-code \
    blueocean

# Instalar herramientas básicas
RUN apt-get update && \
    apt-get install -y curl wget gnupg2 lsb-release iputils-ping && \
    apt-get clean

# Establecer zona horaria a CDMX
RUN ln -sf /usr/share/zoneinfo/America/Mexico_City /etc/localtime && \
    echo "America/Mexico_City" > /etc/timezone

# Agregar clave GPG de Docker
RUN install -m 0755 -d /etc/apt/keyrings && \
    curl -fsSL https://download.docker.com/linux/debian/gpg -o /etc/apt/keyrings/docker.asc && \
    chmod a+r /etc/apt/keyrings/docker.asc

# Agregar repositorio de Docker
RUN echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/debian \
  $(. /etc/os-release && echo \"$VERSION_CODENAME\") stable" > /etc/apt/sources.list.d/docker.list && \
    apt-get update

# Instalar Docker CLI
RUN apt-get install -y docker-ce-cli && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Agrega el grupo 'docker' y el usuario 'jenkins' a él
RUN groupadd docker || true && usermod -aG docker jenkins

# Descargar e instalar Maven
RUN wget --no-verbose https://downloads.apache.org/maven/maven-${MAVEN_RELASE}/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz -P /tmp/ && \
    tar xzf /tmp/apache-maven-${MAVEN_VERSION}-bin.tar.gz -C /opt/ && \
    ln -s /opt/apache-maven-${MAVEN_VERSION} /opt/maven && \
    ln -s /opt/maven/bin/mvn /usr/local/bin/mvn && \
    rm /tmp/apache-maven-${MAVEN_VERSION}-bin.tar.gz

# Establecer permisos y limpiar
RUN chown -R jenkins:jenkins /opt/maven && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

#Esto te ayuda a verificar que el grupo fue creado correctamente.
RUN getent group docker

# Cambiar de nuevo al usuario Jenkins
USER jenkins

RUN groups

# Agregar Maven al PATH
ENV PATH="${MAVEN_HOME}/bin:${PATH}"

# RUN apt-get update && apt-get install -y gosu

# ENTRYPOINT ["gosu", "jenkins"]
# CMD ["bash"]

# ######################################################################### #########################################
# ###################################################################################################################
