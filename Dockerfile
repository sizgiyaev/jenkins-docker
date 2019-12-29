FROM jenkins/jenkins:2.210

ARG GOSU_VERSION=1.10

USER root

RUN set -x && \
    apt-get update && apt-get install -y --no-install-recommends ca-certificates wget && \
    curl -sSL https://get.docker.com/ | sh && \
    dpkgArch="$(dpkg --print-architecture | awk -F- '{ print $NF }')" && \
    wget -qO /usr/local/bin/gosu "https://github.com/tianon/gosu/releases/download/$GOSU_VERSION/gosu-$dpkgArch" && \
    chmod +x /usr/local/bin/gosu && \
    gosu nobody true && \
    apt-get purge wget -y && \
    apt-get clean && \ 
    rm -rf /var/lib/apt/lists/* && \
    mkdir -p /var/log/jenkins /var/jenkins_home /var/cache/jenkins
    
COPY entrypoint.sh /usr/bin/entrypoint.sh
COPY base-plugins.txt /usr/share/jenkins/ref/plugins/base-plugins.txt
COPY ConfigureSecurity.groovy /var/jenkins_home/init.groovy.d/

# Install base plugins
RUN /usr/local/bin/install-plugins.sh </usr/share/jenkins/ref/plugins/base-plugins.txt && \
    chmod +x /usr/bin/entrypoint.sh

ENTRYPOINT ["/usr/bin/entrypoint.sh"]
CMD ["$@"]
