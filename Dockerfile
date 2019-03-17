ARG NEXUS_VERSION=3.14.0

FROM maven:3-jdk-8-alpine AS build
ARG NEXUS_VERSION=3.14.0
ARG NEXUS_BUILD=04

COPY . /nexus-repository-vgo/
RUN cd /nexus-repository-vgo/; sed -i "s/3.14.0-04/${NEXUS_VERSION}-${NEXUS_BUILD}/g" pom.xml; \
    mvn clean package;

FROM sonatype/nexus3:$NEXUS_VERSION
ARG NEXUS_VERSION=3.14.0
ARG NEXUS_BUILD=04
ARG VGO_VERSION=0.0.1
ARG TARGET_DIR=/opt/sonatype/nexus/system/org/sonatype/nexus/plugins/nexus-repository-vgo/${VGO_VERSION}/
USER root
RUN mkdir -p ${TARGET_DIR}; \
    sed -i 's@nexus-repository-maven</feature>@nexus-repository-maven</feature>\n        <feature prerequisite="false" dependency="false">nexus-repository-vgo</feature>@g' /opt/sonatype/nexus/system/org/sonatype/nexus/assemblies/nexus-core-feature/${NEXUS_VERSION}-${NEXUS_BUILD}/nexus-core-feature-${NEXUS_VERSION}-${NEXUS_BUILD}-features.xml; \
    sed -i 's@<feature name="nexus-repository-maven"@<feature name="nexus-repository-vgo" description="org.sonatype.nexus.plugins:nexus-repository-vgo" version="0.0.1">\n        <details>org.sonatype.nexus.plugins:nexus-repository-vgo</details>\n        <bundle>mvn:org.sonatype.nexus.plugins/nexus-repository-vgo/0.0.1</bundle>\n           </feature>\n    <feature name="nexus-repository-maven"@g' /opt/sonatype/nexus/system/org/sonatype/nexus/assemblies/nexus-core-feature/${NEXUS_VERSION}-${NEXUS_BUILD}/nexus-core-feature-${NEXUS_VERSION}-${NEXUS_BUILD}-features.xml;
COPY --from=build /nexus-repository-vgo/target/nexus-repository-vgo-${VGO_VERSION}.jar ${TARGET_DIR}
USER nexus
