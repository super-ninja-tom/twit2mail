#!/bin/bash +e
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"
# echo Running twit2mail from $DIR
#cd $DIR && mvn -q test -Dtest=TestDownloadAll#test
cd $DIR
sed -i "s/\${env.OPENSHIFT_POSTGRESQL_DATASOURCE}/ExampleDS/g" src/main/resources/META-INF/persistence.xml
my_trap() {
  _FOO=1+1
}
trap my_trap INT
rm -rf deployments/wildfly-run/wildfly-14.0.0.Final/standalone/data/content
cp deployments/wildfly-run/wildfly-14.0.0.Final/standalone/configuration/standalone.xml.bak deployments/wildfly-run/wildfly-14.0.0.Final/standalone/configuration/standalone.xml
mvn wildfly:run -DskipTests
sed -i "s/ExampleDS/\${env.OPENSHIFT_POSTGRESQL_DATASOURCE}/g" src/main/resources/META-INF/persistence.xml
cd -