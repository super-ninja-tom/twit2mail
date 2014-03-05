1. Deploy a postgresql dc
2. Deploy a wildfly dc with the source code and environment variables:
JAVA_OPTS_EXT -DhostNameOfApp=<INSERT>
POSTGRESQL_DATASOURCE twit2mail
POSTGRESQL_DATABASE sampledb
POSTGRESQL_SERVICE_HOST postgresql
POSTGRESQL_SERVICE_PORT 5432
POSTGRESQL_USER
POSTGRESQL_PASSWORD
3. Liveness probe /twit2mail.jsf
4. oc rsh `oc get pods | grep Running | grep twit2mail | cut -f 1 -d " "` rm -rf /opt/app-root/src/config/; oc rsh `oc get pods | grep Running | grep twit2mail |
cut -f 1 -d " "` mkdir /opt/app-root/src/config/; oc rsync config/ `oc get pods | grep Running | grep twit2mail | cut -f 1 -d " "`:/opt/app-root/src/config/