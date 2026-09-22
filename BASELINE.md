# Baseline notice

This repository is an unmodified snapshot of the public, Apache-2.0 licensed
[Apache OFBiz framework](https://github.com/apache/ofbiz-framework) at tag
`release18.12.12` (commit `43fd8328bc3dcff24e29b839c09cd632e90cb5c8`,
2024-02-07). It is used as a **public open-source surrogate** for a legacy
Java enterprise application estate when exercising security-finding triage,
runtime validation, remediation and test workflows.

- No application source has been changed relative to the upstream tag. The
  only addition is this file.
- This is **not** the source code of any government system. Any resemblance
  to a specific agency application is limited to the shared characteristics
  of a Java 8 / Gradle / embedded-Tomcat enterprise stack with accounting,
  order, party and web-tools modules.
- Upstream tags `release18.12.13` through `release18.12.17` are included so
  that remediation work can be compared against the maintainers' own fixes
  (see the [Apache OFBiz security page](https://ofbiz.apache.org/security.html)).
- Build and run (Java 8 required):

  ```bash
  export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
  ./gradlew --no-daemon build -x test -x checkstyleMain
  java -jar build/libs/ofbiz.jar --load-data
  java -jar build/libs/ofbiz.jar
  # https://localhost:8443/webtools/control/main
  ```

Known upstream advisories affecting this tag are published by the Apache
OFBiz PMC; consult that page rather than this file for authoritative fix
versions.
