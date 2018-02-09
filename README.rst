=========================
 Crate JMX HTTP Exporter
=========================

This project implements a `javaagent` which exposes CrateDB specific metrics over
HTTP.

.. warning::

   THIS PROJECT IS CURRENTLY IN EARLY ALPHA STATE!

Building
========

Run::

  ./gradlew buildJar

to build a JAR which can be used as a `javaagent`.


Usage
=====

::

   export CRATE_JAVA_OPTS="-javaagent:<PATH_TO>/crate_jmx_agent-0.0.1.jar=<HOST>:<PORT>"
   ./bin/crate

Afterwards the metrics can be fetched via HTTP under the configure host and port.
