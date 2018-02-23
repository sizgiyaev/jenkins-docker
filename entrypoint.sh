#!/bin/bash

set -e

function install-plugins-file() {
    [[ -s $1 ]] && /usr/local/bin/install-plugins.sh < $1 || :
}

function install-plugins-environment() {
    plugins=$(printenv | grep -E "^JENKINS_PLUGIN_" | cut -d"=" -f2)
    [[ ! -z "${plugins}" ]] && /usr/local/bin/install-plugins.sh ${plugins} || :
}

while [[ $# -gt 0 ]]
do
    case $1 in
        --plugins-file)
            install-plugins-file $2
        shift
        ;;
        --plugins-from-environment)
            install-plugins-environment
        ;;
    esac
    shift
done

export JAVA_OPTS="$JAVA_OPTS -Djenkins.install.runSetupWizard=false"
chown -R jenkins:jenkins /var/log/jenkins
chown -R jenkins:jenkins /var/cache/jenkins
chown -R jenkins:jenkins /usr/share/jenkins/ref/plugins
chown -R jenkins:jenkins "$JENKINS_HOME"

# Run Jenkins
exec gosu jenkins /sbin/tini -s -- /usr/local/bin/jenkins.sh
