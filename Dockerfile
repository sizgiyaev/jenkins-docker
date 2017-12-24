FROM jenkins/jenkins:lts

LABEL maintainer="Sergey Izgiyaev <sergo27@gmail.com>"

USER root

RUN mkdir -p /var/log/jenkins /var/jenkins_home /var/cache/jenkins && \
    chown -R jenkins:jenkins /var/log/jenkins && \
    chown -R jenkins:jenkins /var/cache/jenkins && \
    chown -R jenkins:jenkins /var/jenkins_home

USER jenkins
