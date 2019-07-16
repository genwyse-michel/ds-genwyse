#!/bin/sh
mvn install:install-file \
  -DgroupId=com.genwyse \
  -DartifactId=dsgenwyse \
  -Dpackaging=jar \
  -Dversion=6.6-1.4.1 \
  -Dfile=dsgenwyse-6.6-1.4.1.jar \
  -DgeneratePom=true
